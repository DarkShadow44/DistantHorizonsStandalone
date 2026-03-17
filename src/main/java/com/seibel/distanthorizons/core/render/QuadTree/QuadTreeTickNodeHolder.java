package com.seibel.distanthorizons.core.render.QuadTree;

import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D;
import com.seibel.distanthorizons.core.util.objects.quadTree.QuadNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/**
 * Holds all the data retrieved
 * while running {@link LodQuadTree#tryTick(DhBlockPos2D)}.
 * This allows for running different logic at different times for each node
 * based on whether it should be rendered and it's place in the tree.
 */
public class QuadTreeTickNodeHolder
{
	/** Nodes that should be pulled in from the disk */
	private final HashSet<LodRenderSection> sectionsToLoad = new HashSet<>();
	
	private final HashSet<QuadNode<LodRenderSection>> presentNodes = new HashSet<>();
	
	private final HashSet<QuadNode<LodRenderSection>> nodesToEnable = new HashSet<>();
	private final HashSet<QuadNode<LodRenderSection>> nodesToDisable = new HashSet<>();
	private final ArrayList<QuadNode<LodRenderSection>> nodesToEnableDeleteChildrenList = new ArrayList<>();
	
	/** 
	 * not included in {@link #clear()} to allow for use on the {@link LodQuadTree}'s
	 * queuing thread.
	 * Always generated based on other 
	 */
	private final ArrayList<QuadNode<LodRenderSection>> nodesForWorldGen = new ArrayList<>();
	
	private final QuadNodeNearComparator quadNodeNearComparator = new QuadNodeNearComparator();
	
	
	
	//=========//
	// methods //
	//=========//
	///region
	
	public void clear()
	{
		this.sectionsToLoad.clear();
		
		this.presentNodes.clear();
		
		this.nodesToEnable.clear();
		this.nodesToDisable.clear();
		this.nodesToEnableDeleteChildrenList.clear();
	}
	
	
	// loading
	public void addLoadSection(LodRenderSection section) { this.sectionsToLoad.add(section); }
	public HashSet<LodRenderSection> getLoadSections() { return this.sectionsToLoad; }
	
	
	// enable
	public void addEnableNode(QuadNode<LodRenderSection> node)
	{
		if(this.presentNodes.add(node))
		{
			// not a big fan of having to check every node to prevent overlaps, but it does work
			this.nodesToEnable.removeIf((QuadNode<LodRenderSection> checkNode) ->
			{
				boolean contained = DhSectionPos.contains(node.sectionPos, checkNode.sectionPos);
				if (contained)
				{
					this.nodesToDisable.add(checkNode);
				}
				
				return contained;
			});
			
			this.nodesToEnable.add(node);
		}
	}
	public HashSet<QuadNode<LodRenderSection>> getEnabledNodes() { return this.nodesToEnable; }
	
	
	// disable
	public void addDisableNode(QuadNode<LodRenderSection> node)
	{
		if(this.presentNodes.add(node))
		{
			this.nodesToDisable.add(node);
		}
	}
	public HashSet<QuadNode<LodRenderSection>> getDisableNodes() { return this.nodesToDisable; }
	
	
	// enable - delete children
	public void addEnableDeleteChildrenNode(QuadNode<LodRenderSection> node)
	{
		if(this.presentNodes.add(node))
		{
			this.nodesToEnableDeleteChildrenList.add(node);
		}
	}
	public ArrayList<QuadNode<LodRenderSection>> getEnableDeleteChildrenNodes() { return this.nodesToEnableDeleteChildrenList; }
	public ArrayList<QuadNode<LodRenderSection>> getWorldGenNodesNearToFar(DhBlockPos2D centerPos) 
	{
		this.quadNodeNearComparator.centerPos = centerPos;
		this.nodesToEnableDeleteChildrenList.sort(this.quadNodeNearComparator);
		
		// this 
		this.nodesForWorldGen.clear();
		this.nodesForWorldGen.addAll(this.nodesToEnableDeleteChildrenList);
		
		return this.nodesForWorldGen;
	}
	
	///endregion
	
	
	
	//================//
	// helper classes //
	//================//
	///region
	
	/** orders closest LODs first */
	private static class QuadNodeNearComparator implements Comparator<QuadNode<LodRenderSection>>
	{
		public DhBlockPos2D centerPos = DhBlockPos2D.ZERO;
		
		@Override
		public int compare(QuadNode<LodRenderSection> nodeA, QuadNode<LodRenderSection> nodeB)
		{
			// closer LODs first
			int aDist = DhSectionPos.getManhattanBlockDistance(nodeA.sectionPos, this.centerPos);
			int bDist = DhSectionPos.getManhattanBlockDistance(nodeB.sectionPos, this.centerPos);
			return Integer.compare(aDist, bDist); // smaller numbers first
		}
	}
	
	///endregion
	
	
	
}
