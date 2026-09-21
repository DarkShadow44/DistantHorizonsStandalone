package com.seibel.distanthorizons.common.commonMixins;

import com.seibel.distanthorizons.api.enums.config.EDhApiUpdateBranch;
import com.seibel.distanthorizons.common.wrappers.gui.DhScreenUtil;
import com.seibel.distanthorizons.common.wrappers.gui.updater.UpdateModScreen;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.jar.installer.GitlabGetter;
import com.seibel.distanthorizons.core.jar.installer.ModrinthGetter;
import com.seibel.distanthorizons.core.jar.updater.SelfUpdater;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import net.minecraft.client.Minecraft;
#if MC_VER <= MC_1_12_2
import net.minecraft.client.gui.GuiMainMenu;
#else
import net.minecraft.client.gui.screens.TitleScreen;
#endif
import java.util.ArrayList;

public class DhUpdateScreenBase
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	#if MC_VER <= MC_1_12_2
	private static final Minecraft MC = Minecraft.getMinecraft();
	#else
	private static final Minecraft MC = Minecraft.getInstance();
	#endif
	
	public static void tryShowUpdateScreenAndRunAutoUpdateStartup(Runnable runnable)
	{
		// always needs to be called, otherwise auto update setup won't be completed
		boolean newUpdateAvailable = SelfUpdater.onStart();
		if (!newUpdateAvailable)
		{
			return;
		}
		
		
		runnable = () ->
		{
			String versionId;
			EDhApiUpdateBranch updateBranch = EDhApiUpdateBranch.convertAutoToStableOrNightly(Config.Client.Advanced.AutoUpdater.updateBranch.get());
			if (updateBranch == EDhApiUpdateBranch.STABLE)
			{
				versionId = ModrinthGetter.getLatestIDForVersion(SingletonInjector.INSTANCE.get(IVersionConstants.class).getMinecraftVersion());
			}
			else
			{
				ArrayList<com.electronwill.nightconfig.core.Config> pipelines = GitlabGetter.INSTANCE.projectPipelines;
				if (pipelines != null
					&& pipelines.size() > 0)
				{
					versionId = pipelines.get(0).get("sha");
				}
				else
				{
					versionId = null;
				}
			}
			
			if (versionId == null)
			{
				LOGGER.info("Unable to find new DH update for the ["+updateBranch+"] branch. Assuming DH is up to date...");
				return;
			}
			
			
			// running on the render thread is required since setting the MC screen may trigger
			// before its allowed, silently failing
			RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("Update Screen", () -> 
			{
				try
				{
					#if MC_VER <= MC_1_12_2
					DhScreenUtil.setScreen(new UpdateModScreen(
						new GuiMainMenu(),
						versionId
					));
					#else
					DhScreenUtil.setScreen(new UpdateModScreen(
						new TitleScreen(false),
						versionId
					));
					#endif
				}
				catch (Exception e)
				{
					// info instead of error since this can be ignored and probably just means
					// there isn't a new DH version available
					LOGGER.error("Unable to show DH update screen, reason: ["+e.getMessage()+"].");
				}
			});
		};
		runnable.run();
	}
	
}
