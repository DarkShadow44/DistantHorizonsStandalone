package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.coreapi.ModInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all the generic objects that can
 * be rendered in a given level.
 */
public class GenericRenderHandler implements IDhApiCustomRenderRegister
{
	public static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	/**
	 * Can be used to troubleshoot the renderer. 
	 * If enabled several debug objects will render around (0,150,0). 
	 */
	public static final boolean RENDER_DEBUG_OBJECTS = false;
	
	private final ConcurrentHashMap<Long, RenderableBoxGroup> boxGroupById = new ConcurrentHashMap<>();
	/** this variable is immutable to prevent incorrect API modification during render */
	private final Collection<RenderableBoxGroup> apiBoxCollection = Collections.unmodifiableCollection(this.boxGroupById.values());
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	public GenericRenderHandler()
	{
		if (RENDER_DEBUG_OBJECTS)
		{
			this.addGenericDebugObjects();
		}
	}
	private void addGenericDebugObjects()
	{
		GenericRenderObjectFactory factory = GenericRenderObjectFactory.INSTANCE;
		
		
		// single giant box
		IDhApiRenderableBoxGroup singleGiantBoxGroup = factory.createForSingleBox(
			ModInfo.NAME + ":CyanChunkBox",
			new DhApiRenderableBox(
				new DhApiVec3d(0,0,0), new DhApiVec3d(16,190,16),
				new Color(Color.CYAN.getRed(), Color.CYAN.getGreen(), Color.CYAN.getBlue(), 125),
				EDhApiBlockMaterial.WATER)
		);
		singleGiantBoxGroup.setSkyLight(LodUtil.MAX_MC_LIGHT);
		singleGiantBoxGroup.setBlockLight(LodUtil.MAX_MC_LIGHT);
		this.add(singleGiantBoxGroup);
		
		
		// single slender box
		IDhApiRenderableBoxGroup singleTallBoxGroup = factory.createForSingleBox(
			ModInfo.NAME + ":GreenBeacon",
			new DhApiRenderableBox(
				new DhApiVec3d(16,0,31), new DhApiVec3d(17,2000,32),
				new Color(Color.GREEN.getRed(), Color.GREEN.getGreen(), Color.GREEN.getBlue(), 125),
				EDhApiBlockMaterial.ILLUMINATED)
		);
		singleTallBoxGroup.setSkyLight(LodUtil.MAX_MC_LIGHT);
		singleTallBoxGroup.setBlockLight(LodUtil.MAX_MC_LIGHT);
		this.add(singleTallBoxGroup);
		
		
		// absolute box group
		ArrayList<DhApiRenderableBox> absBoxList = new ArrayList<>();
		for (int i = 0; i < 18; i++)
		{
			absBoxList.add(new DhApiRenderableBox(
					new DhApiVec3d(i,150+i,24), new DhApiVec3d(1+i,151+i,25),
					new Color(Color.ORANGE.getRed(), Color.ORANGE.getGreen(), Color.ORANGE.getBlue()),
					EDhApiBlockMaterial.LAVA
				)
			);
		}
		IDhApiRenderableBoxGroup absolutePosBoxGroup = factory.createAbsolutePositionedGroup(ModInfo.NAME + ":OrangeStairs", absBoxList);
		this.add(absolutePosBoxGroup);
		
		
		// relative box group
		ArrayList<DhApiRenderableBox> relBoxList = new ArrayList<>();
		for (int i = 0; i < 8; i+=2)
		{
			relBoxList.add(new DhApiRenderableBox(
					new DhApiVec3d(0,i,0), new DhApiVec3d(1,1+i,1),
					new Color(Color.MAGENTA.getRed(), Color.MAGENTA.getGreen(), Color.MAGENTA.getBlue()),
					EDhApiBlockMaterial.METAL
				)
			);
		}
		IDhApiRenderableBoxGroup relativePosBoxGroup = factory.createRelativePositionedGroup(
			ModInfo.NAME + ":MovingMagentaGroup",
			new DhApiVec3d(24, 140, 24),
			relBoxList);
		relativePosBoxGroup.setPreRenderFunc((event) ->
		{
			DhApiVec3d pos = relativePosBoxGroup.getOriginBlockPos();
			pos.x += event.partialTicks / 2;
			pos.x %= 32;
			relativePosBoxGroup.setOriginBlockPos(pos);
		});
		this.add(relativePosBoxGroup);
		
		
		// massive relative box group
		ArrayList<DhApiRenderableBox> massRelBoxList = new ArrayList<>();
		for (int x = 0; x < 50*2; x+=2)
		{
			for (int z = 0; z < 50*2; z+=2)
			{
				massRelBoxList.add(new DhApiRenderableBox(
						new DhApiVec3d(-x, 0, -z), new DhApiVec3d(1-x, 1, 1-z),
						new Color(Color.RED.getRed(), Color.RED.getGreen(), Color.RED.getBlue()),
						EDhApiBlockMaterial.TERRACOTTA
					)
				);
			}
		}
		IDhApiRenderableBoxGroup massRelativePosBoxGroup = factory.createRelativePositionedGroup(
			ModInfo.NAME + ":MassRedGroup",
			new DhApiVec3d(-25, 140, 0),
			massRelBoxList);
		massRelativePosBoxGroup.setPreRenderFunc((event) ->
		{
			DhApiVec3d blockPos = massRelativePosBoxGroup.getOriginBlockPos();
			blockPos.y += event.partialTicks / 4;
			if (blockPos.y > 150f)
			{
				blockPos.y = 140f;
				
				Color newColor = (massRelativePosBoxGroup.get(0).color == Color.RED) ? Color.RED.darker() : Color.RED;
				massRelativePosBoxGroup.forEach((box) -> { box.color = newColor; });
				massRelativePosBoxGroup.triggerBoxChange();
			}
			
			massRelativePosBoxGroup.setOriginBlockPos(blockPos);
		});
		this.add(massRelativePosBoxGroup);
	}
	
	//endregion
	
	
	
	//==============//
	// registration //
	//==============//
	//region
	
	@Override
	public void add(IDhApiRenderableBoxGroup iBoxGroup) throws IllegalArgumentException
	{
		if (!(iBoxGroup instanceof RenderableBoxGroup))
		{
			throw new IllegalArgumentException("Box group must be of type ["+ RenderableBoxGroup.class.getSimpleName()+"], type received: ["+(iBoxGroup != null ? iBoxGroup.getClass() : "NULL")+"].");
		}
		RenderableBoxGroup boxGroup = (RenderableBoxGroup) iBoxGroup;
		if (boxGroup.size() != 0)
		{
			// trigger a box change to make sure the initial data is uploaded
			boxGroup.triggerBoxChange();
		}
		
		long id = boxGroup.getId();
		if (this.boxGroupById.containsKey(id))
		{
			throw new IllegalArgumentException("A box group with the ID [" + id + "] is already present.");
		}
		
		this.boxGroupById.put(id, boxGroup);
	}
	
	@Override
	public IDhApiRenderableBoxGroup remove(long id) { return this.boxGroupById.remove(id); }
	
	@Override
	public Collection<? extends IDhApiRenderableBoxGroup> getRenderBoxList() { return this.apiBoxCollection; }
	
	public void clear() { this.boxGroupById.clear(); }
	
	//endregion
	
	
	
	//=========//
	// F3 menu //
	//=========//
	//region
	
	public String getVboRenderDebugMenuString()
	{
		// get counts
		int totalGroupCount = this.boxGroupById.size();
		int totalBoxCount = 0;
		
		int activeGroupCount = 0;
		int activeBoxCount = 0;
		
		for (long key : this.boxGroupById.keySet())
		{
			RenderableBoxGroup renderGroup = this.boxGroupById.get(key);
			if (renderGroup.active)
			{
				activeGroupCount++;
				activeBoxCount += renderGroup.size();
			}
			totalBoxCount += renderGroup.size();
		}
		
		
		return "Generic Obj #: " + F3Screen.NUMBER_FORMAT.format(activeGroupCount) + "/" + F3Screen.NUMBER_FORMAT.format(totalGroupCount) + ", " +
			"Cube #: " + F3Screen.NUMBER_FORMAT.format(activeBoxCount) + "/" + F3Screen.NUMBER_FORMAT.format(totalBoxCount);
	}
	
	//endregion
	
	
	
}
