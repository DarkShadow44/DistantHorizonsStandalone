package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBoxGroupShading;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.render.objects.IDhGenericObjectVertexBufferContainer;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.*;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class RenderableBoxGroup 
			extends AbstractList<DhApiRenderableBox> 
			implements IDhApiRenderableBoxGroup, Closeable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);
	private static final IWrapperFactory WRAPPER_FACTORY = SingletonInjector.INSTANCE.get(IWrapperFactory.class);
	
	public static final AtomicInteger NEXT_ID_ATOMIC_INT = new AtomicInteger(0);
	
	
	
	public final long id;
	
	public final String resourceLocationNamespace;
	public final String resourceLocationPath;
	
	/** If false the boxes will be positioned relative to the level's origin */
	public final boolean positionBoxesRelativeToGroupOrigin;
	
	private final List<DhApiRenderableBox> boxList;
	/** backup list which allows for uploading the boxes even it the main list is being modified on a different thread. */
	private final List<DhApiRenderableBox> uploadBoxList;
	private final DhApiVec3d originBlockPos;
	
	
	public boolean active = true;
	public boolean ssaoEnabled = true;
	private boolean vertexDataDirty = false;
	
	public byte skyLight = LodUtil.MAX_MC_LIGHT;
	public byte blockLight = LodUtil.MIN_MC_LIGHT;
	public DhApiRenderableBoxGroupShading shading = DhApiRenderableBoxGroupShading.getDefaultShaded();
	
	@Nullable
	public Consumer<DhApiRenderParam> beforeRenderFunc;
	public Consumer<DhApiRenderParam> afterRenderFunc;
	
	// instance data
	public IDhGenericObjectVertexBufferContainer vertexBufferContainer = WRAPPER_FACTORY.createGenericObjectVboContainer();
	/** double buffering for thread safety and to prevent locking the render thread during update */
	private IDhGenericObjectVertexBufferContainer altVertexBufferContainer = WRAPPER_FACTORY.createGenericObjectVboContainer();
	
	
	
	//=================//
	// getters/setters //
	//=================//
	//region
	
	@Override
	public long getId() { return this.id; }
	
	@Override 
	public String getResourceLocationNamespace() { return this.resourceLocationNamespace; }
	@Override 
	public String getResourceLocationPath() { return this.resourceLocationPath; }
	
	@Override
	public void setOriginBlockPos(DhApiVec3d pos)
	{
		this.originBlockPos.x = pos.x;
		this.originBlockPos.y = pos.y;
		this.originBlockPos.z = pos.z;
	}
	
	@Override
	public DhApiVec3d getOriginBlockPos() { return new DhApiVec3d(this.originBlockPos.x, this.originBlockPos.y, this.originBlockPos.z); }
	
	
	@Override
	public void setSkyLight(int skyLight) 
	{
		if (skyLight < LodUtil.MIN_MC_LIGHT 
			|| skyLight > LodUtil.MAX_MC_LIGHT)
		{
			throw new IllegalArgumentException("Sky light ["+skyLight+"] must be between ["+LodUtil.MIN_MC_LIGHT+"] and ["+LodUtil.MAX_MC_LIGHT+"] (inclusive).");
		}
		this.skyLight = (byte)skyLight; 
	}
	@Override
	public int getSkyLight() { return this.skyLight; }
	
	@Override
	public void setBlockLight(int blockLight) 
	{
		if (blockLight < LodUtil.MIN_MC_LIGHT 
			|| blockLight > LodUtil.MAX_MC_LIGHT)
		{
			throw new IllegalArgumentException("Block light ["+blockLight+"] must be between ["+LodUtil.MIN_MC_LIGHT+"] and ["+LodUtil.MAX_MC_LIGHT+"] (inclusive).");
		}
		this.blockLight = (byte)blockLight; 
	}
	@Override
	public int getBlockLight() { return this.blockLight; }
	
	@Override
	public void setPreRenderFunc(Consumer<DhApiRenderParam> func) { this.beforeRenderFunc = func; }
	
	@Override
	public void setPostRenderFunc(Consumer<DhApiRenderParam> func) { this.afterRenderFunc = func; }
	
	@Override
	public void setActive(boolean active) { this.active = active; }
	@Override
	public boolean isActive() { return this.active; }
	
	@Override
	public void setSsaoEnabled(boolean ssaoEnabled) { this.ssaoEnabled = ssaoEnabled; }
	@Override
	public boolean isSsaoEnabled() { return this.ssaoEnabled; }
	
	@Override
	public void setShading(DhApiRenderableBoxGroupShading shading) { this.shading = shading; }
	@Override
	public DhApiRenderableBoxGroupShading getShading() { return this.shading; }
	
	//endregion
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public RenderableBoxGroup(
			String resourceLocation, 
			DhApiVec3d originBlockPos, List<DhApiRenderableBox> boxList, 
			boolean positionBoxesRelativeToGroupOrigin) throws IllegalArgumentException
	{
		String[] splitResourceLocation =  resourceLocation.split(":");
		if (splitResourceLocation.length != 2)
		{
			throw new IllegalArgumentException("Resource Location must be a string that's separated by a single colon, for example: [DistantHorizons:Beacons], your namespace ["+resourceLocation+"], contains ["+(splitResourceLocation.length-1)+"] colons.");
		}
		
		this.resourceLocationNamespace = splitResourceLocation[0];
		this.resourceLocationPath = splitResourceLocation[1];
		
		this.id = NEXT_ID_ATOMIC_INT.getAndIncrement();
		this.boxList = Collections.synchronizedList(new ArrayList<>(boxList));
		this.uploadBoxList = Collections.synchronizedList(new ArrayList<>(boxList));
		
		this.originBlockPos = originBlockPos;
		this.positionBoxesRelativeToGroupOrigin = positionBoxesRelativeToGroupOrigin;
	}
	
	//endregion
	
	
	
	//=================//
	// render building //
	//=================//
	//region
	
	@Override
	public void triggerBoxChange() { this.vertexDataDirty = true; }
	
	/** 
	 * Does nothing if the vertex data is already up-to-date 
	 * and is meaningless if using direct rendering.
	 */
	public void tryUpdateInstancedDataAsync()
	{
		// if the alt container is done, swap it in
		if (this.altVertexBufferContainer.getState() == IDhGenericObjectVertexBufferContainer.EState.READY_TO_UPLOAD)
		{
			this.altVertexBufferContainer.uploadDataToGpu();
			this.altVertexBufferContainer.setState(IDhGenericObjectVertexBufferContainer.EState.RENDER);
			
			// swap VBO references for rendering
			IDhGenericObjectVertexBufferContainer temp = this.vertexBufferContainer;
			this.vertexBufferContainer = this.altVertexBufferContainer;
			this.altVertexBufferContainer = temp;
			
			this.vertexDataDirty = false;
			
			return;
		}
		
		
		
		// if the vertex data is already up to date, do nothing
		if (!this.vertexDataDirty)
		{
			return;
		}
		
		PriorityTaskPicker.Executor executor = ThreadPoolUtil.getRenderLoadingExecutor();
		if (executor == null || executor.isTerminated())
		{
			return;
		}
		
		// if the alternate container is already updating, don't double-queue it
		if (this.altVertexBufferContainer.getState() == IDhGenericObjectVertexBufferContainer.EState.UPDATING_DATA)
		{
			return;
		}
		this.altVertexBufferContainer.setState(IDhGenericObjectVertexBufferContainer.EState.UPDATING_DATA);
		
		
		
		//this.altInstancedVbos.tryRunRenderThreadSetup();
		
		// copy over the box list so we can upload without concurrent modification issues
		this.uploadBoxList.clear();
		synchronized (this.uploadBoxList)
		{
			this.uploadBoxList.addAll(this.boxList);
		}
		
		try
		{
			executor.runTask(() ->
			{
				try
				{
					this.altVertexBufferContainer.updateVertexData(this.uploadBoxList);
					this.altVertexBufferContainer.setState(IDhGenericObjectVertexBufferContainer.EState.READY_TO_UPLOAD);
				}
				catch (Exception e)
				{
					LOGGER.error("Unexpected error updating instanced VBO data for: ["+this+"], error: ["+e.getMessage()+"].", e);
					this.altVertexBufferContainer.setState(IDhGenericObjectVertexBufferContainer.EState.ERROR);
				}
			});
		}
		catch (RejectedExecutionException ignore) 
		{
			// the executor was shut down, it should be back up shortly and able to accept new jobs
			this.altVertexBufferContainer.setState(IDhGenericObjectVertexBufferContainer.EState.NEW);
		}
	}
	
	//endregion
	
	
	
	//===============//
	// render events //
	//===============//
	//region
	
	/** 
	 * This is called before every frame, even if {@link this#isActive()} returns false. <br>
	 * {@link this#isActive()} can be changed at this point before the object is rendered to the frame.
	 */
	public void preRender(DhApiRenderParam renderEventParam) 
	{
		if (this.beforeRenderFunc != null)
		{
			this.beforeRenderFunc.accept(renderEventParam);
		}
	}
	/**
	 * Called after rendering is completed. <br>
	 * Can be used to handle any necessary cleanup.
	 */
	public void postRender(DhApiRenderParam renderEventParam) 
	{
		if (this.afterRenderFunc != null)
		{
			this.afterRenderFunc.accept(renderEventParam);
		}
	}
	
	//endregion
	
	
	
	//================//
	// List Overrides //
	//================//
	//region
	
	@Override
	public boolean add(DhApiRenderableBox box) { return this.boxList.add(box); }
	@Override
	public DhApiRenderableBox get(int index) { return this.boxList.get(index); }
	@Override 
	public int size() { return this.boxList.size(); }
	@Override 
	public boolean removeIf(Predicate<? super DhApiRenderableBox> filter) { return this.boxList.removeIf(filter); }
	@Override 
	public boolean remove(Object obj) { return this.boxList.remove(obj); }
	@Override 
	public DhApiRenderableBox remove(int index) { return this.boxList.remove(index); }
	@Override 
	public void replaceAll(UnaryOperator<DhApiRenderableBox> operator) { this.boxList.replaceAll(operator); }
	@Override 
	public void sort(Comparator<? super DhApiRenderableBox> comparator) { this.boxList.sort(comparator); }
	@Override 
	public void forEach(Consumer<? super DhApiRenderableBox> action) { this.boxList.forEach(action); }
	@Override 
	public Spliterator<DhApiRenderableBox> spliterator() { return this.boxList.spliterator(); }
	@Override 
	public Stream<DhApiRenderableBox> stream() { return this.boxList.stream(); }
	@Override 
	public Stream<DhApiRenderableBox> parallelStream() { return this.boxList.parallelStream(); }
	@Override 
	public void clear() { this.boxList.clear(); }
	
	//endregion
	
	
	
	//================//
	// base overrides //
	//================//
	//region
	
	@Override
	public String toString() { return "["+this.resourceLocationNamespace+":"+this.resourceLocationPath+"]  ID:["+this.id+"], pos:[("+this.originBlockPos.x+", "+this.originBlockPos.y+", "+this.originBlockPos.z+")], size:["+this.size()+"], active:["+this.active+"]"; }
	
	@Override 
	public void close()
	{
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("RenderBoxGroup Close", () ->
		{
			this.vertexBufferContainer.close();
			this.altVertexBufferContainer.close();
		});
	}
	
	//endregion
	
	
	
}
	