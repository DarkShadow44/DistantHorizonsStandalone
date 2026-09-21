package com.seibel.distanthorizons.common.wrappers.gui;

#if MC_VER < MC_1_21_9
// not supported for older MC versions
#else

import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import com.seibel.distanthorizons.coreapi.ModInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

#if MC_VER <= MC_1_21_10
import net.minecraft.resources.ResourceLocation;
#else
import net.minecraft.resources.Identifier;
#endif

#endif

#if MC_VER < MC_1_21_9
// not supported for older MC versions
public class DhDebugScreenEntry
{}
#else
public class DhDebugScreenEntry implements net.minecraft.client.gui.components.debug.DebugScreenEntry
{
	public static void register()
	{
		#if MC_VER <= MC_1_21_11
		// This method is private, so its access will need to be widened
		DebugScreenEntries.register(
				// The id, this will be displayed on the options screen
				#if MC_VER <= MC_1_21_10
				ResourceLocation.fromNamespaceAndPath(ModInfo.RESOURCE_NAMESPACE, "distant_horizons"),
				#else
				"distant_horizons",
				#endif	
				
				// The screen entry
				new DhDebugScreenEntry()
		);
		#elif MC_VER <= MC_1_21_11
		DebugScreenEntries.allEntries().put(
			// The id, this will be displayed on the options screen
			Identifier.fromNamespaceAndPath(ModInfo.RESOURCE_NAMESPACE, "distant_horizons"),

			// The screen entry
			new DhDebugScreenEntry()
		);
		#else
		DebugScreenEntries.register(
			// The id, this will be displayed on the options screen
			ModInfo.RESOURCE_NAMESPACE,
			
			// The screen entry
			new DhDebugScreenEntry()
		);
		#endif
	}
	
	
	@Override
	public void display(@NotNull DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk)
	{
		List<String> messageList = new ArrayList<>();
		F3Screen.addStringToDisplay(messageList);
		
		for (String message : messageList)
		{
			displayer.addLine(message);
		}
		
		//region
		
		//// The following will display like so if it is the only entry on the screen:
		//// First left!                                                First Right!
		//// 
		//// Hello world!                                               Random text!
		//// Lorem ipsum.
		////                                                            I am another group!
		//// I am one group                                             This will appear after with no line breaks!
		//// All in a row
		//// Provided in a list.
		////
		//
		//displayer.addLine("Hello world!");
		//displayer.addLine("Lorem ipsum.");
		//displayer.addLine("Random text!");
		//
		//// These will be displayed first
		//displayer.addPriorityLine("First left!");
		//displayer.addPriorityLine("First right!");
		//
		//// These will be grouped separately based on the key
		//displayer.addToGroup(GROUP_ONE, List.of(
		//		"I am one group",
		//		"All in a row",
		//		"Provided in a list."
		//));
		//
		//displayer.addToGroup(GROUP_TWO, "I am another group!");
		//displayer.addToGroup(GROUP_TWO, "This will appear after with no line breaks!");
		
		//endregion
	}
	
	@Override
	public boolean isAllowed(boolean reducedDebugInfo)
	{
		// Always show regardless of accessibility option
		return true;
	}
	
	
	
}
#endif
