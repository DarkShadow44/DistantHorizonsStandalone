/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.core.render.QuadTree;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.config.listeners.IConfigListener;
import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataSourceProviderV2;
import com.seibel.distanthorizons.core.file.fullDatafile.V2.FullDataUpdatePropagatorV2;
import com.seibel.distanthorizons.core.generation.tasks.DataSourceRetrievalResult;
import com.seibel.distanthorizons.core.generation.tasks.ERetrievalResultState;
import com.seibel.distanthorizons.core.level.DhClientServerLevel;
import com.seibel.distanthorizons.core.level.IDhClientLevel;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.render.renderer.IDebugRenderable;
import com.seibel.distanthorizons.core.render.renderer.generic.BeaconRenderHandler;
import com.seibel.distanthorizons.core.render.renderer.generic.GenericObjectRenderer;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.WorldGenUtil;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadNode;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadTree;
import com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker;
import com.seibel.distanthorizons.core.util.threading.ThreadPoolUtil;
import com.seibel.distanthorizons.coreapi.util.MathUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.WillNotClose;
import java.awt.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This quadTree structure is our core data structure and holds
 * all rendering data.
 */
public class LodQuadTree extends QuadTree<LodRenderSection> implements IDebugRenderable, IConfigListener, AutoCloseable
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	/** there should only ever be one {@link LodQuadTree} so having the thread static should be fine */
	private static final ThreadPoolExecutor FULL_DATA_RETRIEVAL_QUEUE_THREAD = ThreadUtil.makeSingleThreadPool("LodQuadTree Data Retrieval Queue");
	
	
	public final int blockRenderDistanceDiameter;
	@WillNotClose
	private final FullDataSourceProviderV2 fullDataSourceProvider;
	
	/**
	 * This holds every {@link DhSectionPos} that should be reloaded next tick. <br>
	 * This is a {@link ConcurrentLinkedQueue} because new sections can be added to this list via the world generator threads.
	 */
	private final ConcurrentLinkedQueue<Long> sectionsToReload = new ConcurrentLinkedQueue<>();
	private final IDhClientLevel level;
	/** 
	 * Note: this doesn't lock all operations as some other threads/operations
	 * that may traverse the tree while it's being modified.
	 * IE {@link RenderBufferHandler} will walk through the tree each frame.
	 */
	private final ReentrantLock treeTickLock = new ReentrantLock();
	
	private final AtomicBoolean requeueAllRetrievalTasksRef = new AtomicBoolean(false);
	private final AtomicBoolean queueThreadRunningRef = new AtomicBoolean(false);
	
	
	@Nullable
	public final BeaconRenderHandler beaconRenderHandler;
	
	/** the smallest numerical detail level number that can be rendered */
	private byte maxLeafRenderDetailLevel;
	/** the largest numerical detail level number that can be rendered */
	private byte minRootRenderDetailLevel;
	
	/** used to calculate when a detail drop will occur */
	private double detailDropOffDistanceUnit;
	/** used to calculate when a detail drop will occur */
	private double detailDropOffLogBase;
	
	/** the {@link DhSectionPos} that need to be retrieved/generated */
	private final Set<Long> missingGenerationPosSet = Collections.newSetFromMap(new ConcurrentHashMap<>()); // concurrency is annoying but required due to needing to add/remove items in the world gen future
	private final Set<Long> queuedGenerationPosSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/** cached array to prevent having to re-allocate it each tick */
	private final ArrayList<Long> sortedMissingPosList = new ArrayList<>();
	private final ArrayList<LodRenderSection> debugNodeList = new ArrayList<>();
	/** cached to prevent re-allocating each tick */
	private final QuadTreeTickNodeHolder tickNodeHolder = new QuadTreeTickNodeHolder();
	
	/** list of sections that should be rendered */
	private ArrayList<LodRenderSection> enabledSections = new ArrayList<>();
	/** alternate list for thread safety  */
	private ArrayList<LodRenderSection> altEnabledSections = new ArrayList<>();
	/** This lock should be very quick since it will be used on the render thread */ 
	private final ReentrantLock enabledRenderSectionLock = new ReentrantLock();
	
	
	
	//=============//
	// constructor //
	//=============//
	//region constructor
	
	public LodQuadTree(
			IDhClientLevel level, int viewDiameterInBlocks,
			int initialPlayerBlockX, int initialPlayerBlockZ,
			FullDataSourceProviderV2 fullDataSourceProvider)
	{
		super(viewDiameterInBlocks, new DhBlockPos2D(initialPlayerBlockX, initialPlayerBlockZ), DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL);
		
		DebugRenderer.register(this, Config.Client.Advanced.Debugging.DebugWireframe.showQuadTreeRenderStatus);
		
		this.level = level;
		this.fullDataSourceProvider = fullDataSourceProvider;
		this.blockRenderDistanceDiameter = viewDiameterInBlocks;
		
		GenericObjectRenderer genericObjectRenderer = this.level.getGenericRenderer();
		this.beaconRenderHandler = (genericObjectRenderer != null) ? new BeaconRenderHandler(genericObjectRenderer) : null;
		
		Config.Common.WorldGenerator.enableDistantGeneration.addListener(this);
		Config.Server.enableServerGeneration.addListener(this);
		
	}
	
	//endregion constructor
	
	
	
	//==================//
	// property getters //
	//==================//
	//region
	
	public void populateListWithEnabledRenderSections(ArrayList<LodRenderSection> tempProcessNodeList)
	{
		try
		{
			// lock for thread safety
			this.enabledRenderSectionLock.lock();
			
			tempProcessNodeList.clear();
			tempProcessNodeList.addAll(this.enabledSections);
		}
		finally
		{
			this.enabledRenderSectionLock.unlock();
		}
	}
	
	//endregion
	
	
	
	//=============//
	// tick update //
	//=============//
	//region tick update
	
	/** 
	 * update the quadTree using the playerPos 
	 * and queue any necessary work based on the tree's state.
	 */
	public void tryTick(DhBlockPos2D playerPos)
	{
		if (this.level == null)
		{
			// the quad tree was created before a level reference was created
			return;
		}
		
		
		
		// don't tick the tree if a modification is still going
		if (this.treeTickLock.tryLock())
		{
			// this shouldn't be updated while the tree is being iterated through
			this.updateDetailLevelVariables();
			
			try
			{
				this.updateAllRenderSections(playerPos);
			}
			catch (Exception e)
			{
				LOGGER.error("Quad Tree tick exception for level: [" + this.level.getLevelWrapper().getDhIdentifier() + "], error: [" + e.getMessage() + "].", e);
			}
			finally
			{
				this.treeTickLock.unlock();
			}
		}
	}
	private void updateAllRenderSections(DhBlockPos2D playerPos)
	{
		// this data will be updated as we walk through the tree
		this.tickNodeHolder.clear();
		
		
		//===================//
		// recenter the tree //
		//===================//
		//region
		
		this.setCenterBlockPos(playerPos, (renderSection) ->
		{
			// removing out of bounds sections
			if (renderSection != null)
			{
				this.fullDataSourceProvider.removeRetrievalRequestIf((long genPos) -> DhSectionPos.contains(renderSection.pos, genPos));
				this.missingGenerationPosSet.remove(renderSection.pos);
				this.queuedGenerationPosSet.remove(renderSection.pos);
				renderSection.close();
			}
		});
		
		//endregion
		
		
		
		//=======================//
		// walk through the tree //
		//=======================//
		//region
		
		// walk through each root node
		LongIterator rootPosIterator = this.rootNodePosIterator();
		while (rootPosIterator.hasNext())
		{
			// make sure all root nodes have been created
			long rootPos = rootPosIterator.nextLong();
			if (this.getNode(rootPos) == null)
			{
				this.setValue(rootPos, new LodRenderSection(rootPos, this, this.level, this.fullDataSourceProvider));
			}
			
			QuadNode<LodRenderSection> rootNode = this.getNode(rootPos);
			LodUtil.assertTrue(rootNode != null, "All root nodes should have been created by this point.");
			this.recursivelyUpdateRenderSectionNode(
				playerPos, 
				rootNode, null, rootNode, rootNode.sectionPos);
		}
		
		//endregion
		
		
		
		//============//
		// queue work //
		//============//
		//region
		
		if (this.requeueAllRetrievalTasksRef.getAndSet(false))
		{
			Iterator<QuadNode<LodRenderSection>> nodeIterator = this.nodeIterator();
			while (nodeIterator.hasNext())
			{
				QuadNode<LodRenderSection> node = nodeIterator.next();
				if (node == null || node.value == null)
				{
					continue;
				}
				
				node.value.retreivedMissingSectionsForRetreival = false;
			}
		}
		
		// reloading is for sections that have been loaded once already
		this.reloadQueuedSections();
		
		// loading is for sections that haven't rendered yet
		this.loadQueuedSections(playerPos, this.tickNodeHolder.getLoadSections());
		
		//endregion
		
		
		
		//==================//
		// enable rendering //
		//==================//
		//region
		
		this.altEnabledSections.clear();
		
		for (QuadNode<LodRenderSection> node : this.tickNodeHolder.getEnabledNodes())
		{
			// shouldn't happen, but just in case
			if (node == null || node.value == null) { continue; }
			
			node.value.setRenderingEnabled(true);
			this.altEnabledSections.add(node.value);
		}
		for (QuadNode<LodRenderSection> node : this.tickNodeHolder.getEnableDeleteChildrenNodes())
		{
			if (node == null || node.value == null) { continue; }
			
			node.value.setRenderingEnabled(true);
			this.altEnabledSections.add(node.value);
		}
		
		//endregion
		
		
		
		//====================//
		// update render list //
		//====================//
		//region
		
		try
		{
			this.enabledRenderSectionLock.lock();
			
			ArrayList<LodRenderSection> temp = this.enabledSections;
			this.enabledSections = this.altEnabledSections;	
			this.altEnabledSections = temp;
		}
		finally
		{
			this.enabledRenderSectionLock.unlock();
		}
		
		//endregion
		
		
		
		//=========================//
		// node disabling/deletion //
		//=========================//
		//region
		
		// also handles disabling beacons
		
		for (QuadNode<LodRenderSection> node : this.tickNodeHolder.getDisableNodes())
		{
			if (node == null || node.value == null) { continue; }
			
			node.value.setRenderingEnabled(false);
			node.value.tryDisableBeacons();
		}
		
		for (QuadNode<LodRenderSection> node : this.tickNodeHolder.getEnableDeleteChildrenNodes())
		{
			if (node == null || node.value == null) { continue; }
			
			node.deleteAllChildren((childRenderSection) ->
			{
				if (childRenderSection != null)
				{
					childRenderSection.setRenderingEnabled(false);
					childRenderSection.tryDisableBeacons();
					childRenderSection.close();
				}
			});
		}
		
		//endregion
		
		
		
		//=================//
		// beacon enabling //
		//=================//
		//region
		
		// must be handled after beacon disabling
		// otherwise the beacons will be missing
		
		for (QuadNode<LodRenderSection> node : this.tickNodeHolder.getEnabledNodes())
		{
			if (node == null || node.value == null) { continue; }

			node.value.tryEnableBeacons();
		}
		for (QuadNode<LodRenderSection> node : this.tickNodeHolder.getEnableDeleteChildrenNodes())
		{
			if (node == null || node.value == null) { continue; }
			
			node.value.tryEnableBeacons();
		}
		
		//endregion
		
		
		
		//===================//
		// world gen queuing //
		//===================//
		//region
		
		// queue full data retrieval (world gen) requests if needed
		if (threadPoolCanAcceptWorldGenTasks()
			&& this.fullDataSourceProvider.canQueueRetrievalNow()
			&& !this.queueThreadRunningRef.get())
		{
			this.queueThreadRunningRef.set(true); // don't run multiple threads at once
			
			// needs to be called outside the queue thread to prevent concurrency issues
			ArrayList<QuadNode<LodRenderSection>> worldGenNodes = this.tickNodeHolder.getWorldGenNodesNearToFar(playerPos);
			
			// running on a separate thread allows for faster loading
			// of finished LODs
			FULL_DATA_RETRIEVAL_QUEUE_THREAD.execute(() ->
			{
				try
				{
					for (int i = 0; i < worldGenNodes.size(); i++)
					{
						QuadNode<LodRenderSection> node = worldGenNodes.get(i);
						if (node == null || node.value == null) { continue; }
						
						// since this section wants to render
						// check if it needs any generation to do so
						if (!node.value.retreivedMissingSectionsForRetreival)
						{
							node.value.retreivedMissingSectionsForRetreival = true;
							this.tryQueuePosForRetrieval(node.value.pos); // can be quite slow
						}
					}
					
					this.startQueuedRetrievalTasks(playerPos);
				}
				catch (Exception e)
				{
					LOGGER.error("Unexpected error starting queued retrieval tasks, error: [" + e.getMessage() + "].", e);
				}
				finally
				{
					this.queueThreadRunningRef.set(false);
				}
			});
		}
		
		//endregion
		
	}
	
	//=========================//
	// tick - recursive update //
	//=========================//
	///region
	
	private void recursivelyUpdateRenderSectionNode(
		@NotNull DhBlockPos2D playerPos, 
		@NotNull QuadNode<LodRenderSection> rootNode, 
		@Nullable QuadNode<LodRenderSection> parentNode, 
		@Nullable QuadNode<LodRenderSection> quadNode, 
		long sectionPos // section pos is needed here since the quad node may be null
		)
	{
		//=====================//
		// get/create the node //
		// and render section  //
		//=====================//
		///region
		
		// create the node
		if (quadNode == null)
		{   
			rootNode.setValue(sectionPos, new LodRenderSection(sectionPos, this, this.level, this.fullDataSourceProvider));
			quadNode = rootNode.getNode(sectionPos);
		}
		if (quadNode == null)
		{
			LodUtil.assertNotReach("Unable to add node with pos ["+DhSectionPos.toString(sectionPos)+"] to tree root ["+rootNode+"].");
		}
		
		// make sure the render section is created (shouldn't be necessary, but just in case)
		LodRenderSection renderSection = quadNode.value;
		if (renderSection == null)
		{
			renderSection = new LodRenderSection(sectionPos, this, this.level, this.fullDataSourceProvider);
			quadNode.setValue(sectionPos, renderSection);
		}
		
		///endregion
		
		
		
		//===============================//
		// handle enabling, loading,     //
		// and disabling render sections //
		//===============================//
		///region
		
		// load every node for rendering
		if (!renderSection.gpuUploadInProgress()
			&& !renderSection.gpuUploadComplete())
		{
			this.tickNodeHolder.addLoadSection(renderSection);
		}
		
		
		byte expectedDetailLevel = this.calcExpectedDetailLevel(playerPos, quadNode.sectionPos);
		expectedDetailLevel = (byte) Math.min(expectedDetailLevel, this.minRootRenderDetailLevel);
		expectedDetailLevel += DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL;
		
		if (DhSectionPos.getDetailLevel(quadNode.sectionPos) > expectedDetailLevel)
		{
			this.onDetailLevelTooHigh(playerPos, rootNode, quadNode);
		}
		// the (expectedDetailLevel-1) fixes corners being cut out due to distance calculations using the LOD center 
		else if (DhSectionPos.getDetailLevel(quadNode.sectionPos) == expectedDetailLevel 
			|| DhSectionPos.getDetailLevel(quadNode.sectionPos) == expectedDetailLevel - 1)
		{
			this.onDesiredDetailLevel(quadNode, parentNode);
		}
		else
		{
			throw new IllegalStateException("LodQuadTree shouldn't be updating renderSections below the expected detail level: [" + expectedDetailLevel + "].");
		}
		
		///endregion
	}
	private void onDetailLevelTooHigh(
		@NotNull DhBlockPos2D playerPos, 
		@NotNull QuadNode<LodRenderSection> rootNode, @NotNull QuadNode<LodRenderSection> quadNode)
	{
		// recursively update each child node
		boolean allChildNodesCanRender = true;
		for (int i = 0; i < 4; i++)
		{
			QuadNode<LodRenderSection> childNode = quadNode.getChildByIndex(i);
			long childPos = DhSectionPos.getChildByIndex(quadNode.sectionPos, i);
			this.recursivelyUpdateRenderSectionNode(
				playerPos, 
				rootNode, quadNode, childNode, childPos);
			childNode = quadNode.getChildByIndex(i); // needs to be gotten again in case a new node was added to the tree (this will often happen when moving into new areas where the children were deleted)
			
			// nodes shouldn't be null, but just in case
			if (childNode != null
				&& childNode.value != null
				&& !childNode.value.gpuUploadComplete())
			{
				// the node is present but not uploaded yet
				allChildNodesCanRender = false;
			}
		}
		
		
		if (allChildNodesCanRender)
		{
			// all child nodes can render, this node isn't needed
			this.tickNodeHolder.addDisableNode(quadNode);
		}
		else
		{
			// not all child positions are loaded yet, this one should be rendered instead
			this.tickNodeHolder.addEnableNode(quadNode);
		}
	}
	private void onDesiredDetailLevel(
		@NotNull QuadNode<LodRenderSection> quadNode, @Nullable QuadNode<LodRenderSection> parentNode)
	{
		boolean allAdjNodesCanRender = true;
		
		// if the parent node is null, that means we're at the root node
		// and we should always render
		if (parentNode != null)
		{
			// check if all adjacent nodes are ready to render
			// this check is done to prevent some overlapping due to the parent node
			// still being active
			for (int i = 0; i < 4; i++)
			{
				QuadNode<LodRenderSection> adjNode = parentNode.getChildByIndex(i);
				// nodes shouldn't be null, but just in case there's an issue
				if (adjNode != null
					&& adjNode.value != null
					&& !adjNode.value.gpuUploadComplete())
				{
					// the node is present but not uploaded yet
					allAdjNodesCanRender = false;
				}
			}
		}
		
		if (allAdjNodesCanRender
			&& quadNode.value != null 
			&& quadNode.value.gpuUploadComplete())
		{
			this.tickNodeHolder.addEnableDeleteChildrenNode(quadNode);
		}
	}
	
	
	///endregion
	
	//=====================//
	// tick - work queuing //
	//=====================//
	//region
	
	private void reloadQueuedSections()
	{
		Long pos;
		HashSet<Long> positionsToRequeue = new HashSet<>();
		while ((pos = this.sectionsToReload.poll()) != null)
		{
			if (positionsToRequeue.contains(pos))
			{
				// don't attempt to re-load positions that are already in the process of reloading
				continue;
			}
			
			LodRenderSection renderSection = this.tryGetValue(pos);
			if (renderSection != null)
			{
				// the section only needs to be updated if a buffer is currently present 
				if (renderSection.gpuUploadComplete())
				{
					if (renderSection.gpuUploadInProgress()
						|| !renderSection.uploadRenderDataToGpuAsync())
					{
						// if a section is already loading or failed to start upload
						// we need to wait to trigger it again
						// if we don't trigger it again the LOD will be out of date
						// and may be invisible/missing
						positionsToRequeue.add(pos);
					}
				}
			}
		}
		this.sectionsToReload.addAll(positionsToRequeue);
	}
	private void loadQueuedSections(DhBlockPos2D playerPos, HashSet<LodRenderSection> nodesNeedingLoading)
	{
		ArrayList<LodRenderSection> loadSectionList = new ArrayList<>(nodesNeedingLoading);
		loadSectionList.sort((LodRenderSection a, LodRenderSection b) ->
		{
			// lower-detail LODs first
			byte aDetailLevel = DhSectionPos.getDetailLevel(a.pos);
			byte bDetailLevel = DhSectionPos.getDetailLevel(b.pos);
			if (aDetailLevel != bDetailLevel)
			{
				return Byte.compare(bDetailLevel, aDetailLevel); // larger numbers first
			}
			
			// closer LODs first
			int aDist = DhSectionPos.getManhattanBlockDistance(a.pos, playerPos);
			int bDist = DhSectionPos.getManhattanBlockDistance(b.pos, playerPos);
			return Integer.compare(aDist, bDist); // smaller numbers first
		});
		
		for (int i = 0; i < loadSectionList.size(); i++)
		{
			LodRenderSection renderSection = loadSectionList.get(i);
			if (!renderSection.gpuUploadInProgress() 
				&& !renderSection.gpuUploadComplete())
			{
				renderSection.uploadRenderDataToGpuAsync();
			}
		}
	}
	
	//endregion
	//endregion tick update
	
	
	
	//=================================//
	// full data retrieval (world gen) //
	//=================================//
	//region world gen
	
	private void startQueuedRetrievalTasks(DhBlockPos2D playerPos)
	{
		// sort the nodes from nearest to farthest
		this.sortedMissingPosList.clear();
		this.sortedMissingPosList.addAll(this.missingGenerationPosSet);
		this.sortedMissingPosList.sort((posA, posB) ->
		{
			int aDist = DhSectionPos.getManhattanBlockDistance(posA, playerPos);
			int bDist = DhSectionPos.getManhattanBlockDistance(posB, playerPos);
			return Integer.compare(aDist, bDist);
		});
		
		
		
		//==================================//
		// add retrieval tasks to the queue //
		//==================================//
		
		for (int i = 0; i < this.sortedMissingPosList.size(); i++)
		{
			if (!this.fullDataSourceProvider.canQueueRetrievalNow())
			{
				break;
			}
			
			long missingPos = this.sortedMissingPosList.get(i);
			
			// is this position within acceptable generator range?
			boolean posInRange = WorldGenUtil.isPosInWorldGenRange(
				missingPos,
				Config.Common.WorldGenerator.generationCenterChunkX.get(), Config.Common.WorldGenerator.generationCenterChunkZ.get(),
				Config.Common.WorldGenerator.generationMaxChunkRadius.get()
			);
			if (!posInRange)
			{
				continue;
			}
			
			CompletableFuture<DataSourceRetrievalResult> genFuture = this.fullDataSourceProvider.queuePositionForRetrieval(missingPos);
			boolean positionQueued = (genFuture != null && !genFuture.isCompletedExceptionally());
			if (positionQueued)
			{
				this.queuedGenerationPosSet.add(missingPos);
				this.missingGenerationPosSet.remove(missingPos);
				
				genFuture.exceptionally((Throwable throwable) ->
				{
					// gen task failed,
					// requeue so we can try again in the future
					
					this.queuedGenerationPosSet.remove(missingPos);
					this.missingGenerationPosSet.add(missingPos);
					return null;
				});
				genFuture.thenAccept((DataSourceRetrievalResult result) ->
				{
					// task finished
					this.queuedGenerationPosSet.remove(missingPos);
					
					if (result.state == ERetrievalResultState.REQUIRES_SPLITTING)
					{
						DhSectionPos.forEachChild(missingPos, (long childPos) ->
						{
							this.tryQueuePosForRetrieval(childPos);
						});
					}
				});
			}
		}
		
		
		
		//==========================//
		// calc task count estimate //
		//==========================//
		
		// calculate an estimate for the max number of chunks for the queue
		int totalWorldGenChunkCount = 0;
		int totalWorldGenTaskCount = 0;
		for (int i = 0; i < this.sortedMissingPosList.size(); i++)
		{
			long missingPos = this.sortedMissingPosList.get(i);
			
			// chunk count
			int sectionWidthInChunks = DhSectionPos.getChunkWidth(missingPos);
			totalWorldGenChunkCount += sectionWidthInChunks * sectionWidthInChunks;
			
			// task count
			totalWorldGenTaskCount++;
		}
		
		this.fullDataSourceProvider.setEstimatedRemainingRetrievalChunkCount(totalWorldGenChunkCount);
		this.fullDataSourceProvider.setTotalRetrievalPositionCount(totalWorldGenTaskCount);
	}
	
	@Override
	public void onConfigValueSet()
	{
		boolean generatorEnabled = this.level instanceof DhClientServerLevel
			? Config.Common.WorldGenerator.enableDistantGeneration.get()
			: Config.Server.enableServerGeneration.get();
		if (generatorEnabled)
		{
			// world gen tasks will need to be re-queued
			// since all the render sections will already have been loaded
			this.requeueAllRetrievalTasksRef.set(true);
		}
		else
		{
			// generation is disabled, clear the queues
			this.missingGenerationPosSet.clear();
			this.queuedGenerationPosSet.clear();
			
			this.requeueAllRetrievalTasksRef.set(false);
		}
	}
	
	/** Does nothing if the missing positions are already queued. */
	private void tryQueuePosForRetrieval(long pos)
	{
		LongArrayList missingPosList = this.fullDataSourceProvider.getPositionsToRetrieve(pos);
		if (missingPosList == null)
		{
			return;
		}
		
		for (int i = 0; i < missingPosList.size(); i++)
		{
			long missingPos = missingPosList.getLong(i);
			if (!this.queuedGenerationPosSet.contains(missingPos))
			{
				this.missingGenerationPosSet.add(missingPos);
			}
		}
	}
	
	/**
	 * Prevents DH from
	 * accidentally starting to queue chunks out of order
	 * if the thread pool suddenly become (un)available while we're walking
	 * through the missing positions.
	 */
	private static boolean threadPoolCanAcceptWorldGenTasks()
	{
		// don't queue additional world gen requests if the render loader is busy
		PriorityTaskPicker.Executor renderLoadExecutor = ThreadPoolUtil.getRenderLoadingExecutor();
		if (renderLoadExecutor == null
			|| renderLoadExecutor.getQueueSize() >= FullDataUpdatePropagatorV2.getMaxPropagateTaskCount() / 2)
		{
			return false;
		}
		
		// don't queue additional world gen requests if the file handler is busy
		PriorityTaskPicker.Executor fileHandlerExecutor = ThreadPoolUtil.getFileHandlerExecutor();
		if (fileHandlerExecutor == null
			|| fileHandlerExecutor.getQueueSize() >= FullDataUpdatePropagatorV2.getMaxPropagateTaskCount() / 2)
		{
			return false;
		}
		
		return true;
	}
	
	//endregion world gen
	
	
	
	//====================//
	// detail level logic //
	//====================//
	//region detail level logic
	
	/**
	 * Determines the detail level expected for the given position based on player position.
	 *
	 * @param playerPos player position as a reference for calculating the detail level
	 * @param sectionPos section position
	 * @return detail level of this section pos
	 */
	public byte calcExpectedDetailLevel(DhBlockPos2D playerPos, long sectionPos) 
	{
		double blockDistance = playerPos.dist(DhSectionPos.getCenterBlockPosX(sectionPos), DhSectionPos.getCenterBlockPosZ(sectionPos));
		return this.calcDetailLevelFromDistance(blockDistance); 
	}
	
	private void updateDetailLevelVariables()
	{
		this.detailDropOffDistanceUnit = Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().distanceUnitInBlocks * LodUtil.CHUNK_WIDTH;
		this.detailDropOffLogBase = Math.log(Config.Client.Advanced.Graphics.Quality.horizontalQuality.get().quadraticBase);
		
		this.maxLeafRenderDetailLevel = Config.Client.Advanced.Graphics.Quality.maxHorizontalResolution.get().detailLevel;
		
		// The minimum detail level is done to prevent single corner sections rendering 1 detail level lower than the others.
		// If not done corners may not be flush with the other LODs, which looks bad.
		byte minSectionDetailLevel = this.calcDetailLevelFromDistance(this.blockRenderDistanceDiameter); // get the minimum allowed detail level
		minSectionDetailLevel -= 1; // -1 so corners can't render lower than their adjacent neighbors. space
		minSectionDetailLevel = (byte) Math.min(minSectionDetailLevel, this.treeRootDetailLevel); // don't allow rendering lower detail sections than what the tree contains
		this.minRootRenderDetailLevel = (byte) Math.max(minSectionDetailLevel, this.maxLeafRenderDetailLevel); // respect the user's selected max resolution if it is lower detail (IE they want 2x2 block, but minSectionDetailLevel is specifically for 1x1 block render resolution)
	}
	
	private byte calcDetailLevelFromDistance(double blockDistance)
	{
		int detailLevel = (int) (Math.log(blockDistance / this.detailDropOffDistanceUnit) / this.detailDropOffLogBase);
		return (byte) MathUtil.clamp(this.maxLeafRenderDetailLevel, detailLevel, FullDataSourceProviderV2.ROOT_SECTION_DETAIL_LEVEL);
	}
	
	//endregion detail level logic
	
	
	
	//==========================//
	// external render requests //
	//==========================//
	//region external render requests
	
	/**
	 * Re-creates the color, render data.
	 * This method should be called after resource packs are changed or LOD settings are modified.
	 */
	public void clearRenderDataCache()
	{
		try
		{
			this.treeTickLock.lock();
			LOGGER.info("Disposing render data...");
			
			// clear the tree
			Iterator<QuadNode<LodRenderSection>> nodeIterator = this.nodeIterator();
			while (nodeIterator.hasNext())
			{
				QuadNode<LodRenderSection> quadNode = nodeIterator.next();
				if (quadNode.value != null)
				{
					quadNode.value.close();
					quadNode.value = null;
				}
			}
			
			LOGGER.info("Render data cleared, please wait a moment for everything to reload...");
		}
		catch (Exception e)
		{
			LOGGER.error("Unexpected error when clearing LodQuadTree render cache: " + e.getMessage(), e);
		}
		finally
		{
			this.treeTickLock.unlock();
		}
	}
	
	/**
	 * Can be called whenever a render section's data needs to be refreshed. <br>
	 * This should be called whenever a world generation task is completed or if the connected server has new data to show.
	 */
	public void queuePosToReload(long pos)
	{		
		// queue reloads //
		
		// only queue each section for reloading
		// after the cache has been cleared,
		// this is done to prevent accidentally using old cached data
		
		this.sectionsToReload.add(pos);
		
		// the adjacent locations also need to be updated to make sure lighting
		// and water updates correctly, otherwise oceans may have walls
		// and lights may not show up over LOD borders
		for (EDhDirection direction : EDhDirection.CARDINAL_COMPASS)
		{
			long adjacentPos = DhSectionPos.getAdjacentPos(pos, direction);
			this.sectionsToReload.add(adjacentPos);
		}
	}
	
	//endregion external render requests
	
	
	
	//===========//
	// debugging //
	//===========//
	//region debugging
	
	@Override
	public void debugRender(DebugRenderer debugRenderer)
	{
		this.populateListWithEnabledRenderSections(this.debugNodeList);
		
		for (int i = 0; i < this.debugNodeList.size(); i++)
		{
			LodRenderSection renderSection = this.debugNodeList.get(i);
			
			Color color = Color.BLACK;
			if (renderSection.gpuUploadInProgress())
			{
				color = Color.ORANGE;
			}
			else if (!renderSection.gpuUploadComplete())
			{
				// uploaded but the buffer is missing
				color = Color.PINK;
			}
			else if (renderSection.renderBufferContainer.hasNonNullVbos())
			{
				if (renderSection.renderBufferContainer.vboBufferCount() != 0)
				{
					color = Color.GREEN;
				}
				else
				{
					// This section is probably rendering an empty chunk
					color = Color.RED;
				}
			}
			
			
			int levelMinY = this.level.getLevelWrapper().getMinHeight();
			int levelMaxY = this.level.getLevelWrapper().getMaxHeight();
			// show the wireframe a bit lower than world max height,
			// since most worlds don't render all the way up to the max height
			int levelHeightRange = (levelMaxY - levelMinY);
			int maxY = levelMaxY - (levelHeightRange / 2);
			
			debugRenderer.renderBox(new DebugRenderer.Box(renderSection.pos, levelMinY, maxY, 0.05f, color));
		}
	}
	
	//endregion debugging
	
	
	
	//==============//
	// base methods //
	//==============//
	//region base methods
	
	@Override
	public void close()
	{
		LOGGER.info("Shutting down LodQuadTree...");
		
		DebugRenderer.unregister(this, Config.Client.Advanced.Debugging.DebugWireframe.showQuadTreeRenderStatus);
		Config.Common.WorldGenerator.enableDistantGeneration.removeListener(this);
		Config.Server.enableServerGeneration.removeListener(this);
		
		
		ThreadPoolExecutor mainCleanupExecutor = ThreadPoolUtil.getCleanupExecutor();
		// closing every node may take a few moments
		// so this is run on a separate thread to prevent lagging the render thread
		mainCleanupExecutor.execute(() -> 
		{
			this.treeTickLock.lock();
			try
			{
				// walk through each node
				Iterator<QuadNode<LodRenderSection>> nodeIterator = this.nodeIterator();
				while (nodeIterator.hasNext())
				{
					QuadNode<LodRenderSection> quadNode = nodeIterator.next();
					LodRenderSection renderSection = quadNode.value;
					if (renderSection != null)
					{
						renderSection.close();
						quadNode.value = null;
					}
				}
			}
			finally
			{
				this.treeTickLock.unlock();
			}
		});
		
		
		LOGGER.info("Finished shutting down LodQuadTree");
	}
	
	//endregion base methods
	
	
	
}
