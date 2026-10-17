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

package com.seibel.distanthorizons.common.wrappers;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingEngine;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.common.render.blaze.BlazeDhRenderApiDefinition;
import com.seibel.distanthorizons.common.render.openGl.GlDhRenderApiDefinition;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingApi;
import com.seibel.distanthorizons.core.render.DhApiRenderProxy;
import com.seibel.distanthorizons.core.render.RenderThreadTaskHandler;
import com.seibel.distanthorizons.core.render.renderer.DebugWireframeHandler;
import com.seibel.distanthorizons.core.render.renderer.GenericRenderObjectFactory;
import com.seibel.distanthorizons.common.wrappers.gui.classicConfig.ClassicConfigGUI;
import com.seibel.distanthorizons.common.wrappers.gui.LangWrapper;
import com.seibel.distanthorizons.common.wrappers.level.KeyedClientLevelManager;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftServerWrapper;
import com.seibel.distanthorizons.core.level.IKeyedClientLevelManager;
import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.common.wrappers.block.BlockStateTextureProvider;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateFaceTextureProvider;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.IConfigGui;
import com.seibel.distanthorizons.core.wrapperInterfaces.config.ILangWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftRenderWrapper;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.IVersionConstants;
import com.seibel.distanthorizons.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftSharedWrapper;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.AbstractDhApiRenderDefinition;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IDependencySetup;
import com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector;

import java.util.concurrent.CompletableFuture;

/**
 * Binds all necessary dependencies, so we
 * can access them in Core. <br>
 * This needs to be called before any Core classes
 * are loaded.
 *
 * @author James Seibel
 * @author Ran
 * @version 12-1-2021
 */
public class DependencySetup implements IDependencySetup
{
	protected static final DhLogger LOGGER = new DhLoggerBuilder().build();
	
	public static final DependencySetup INSTANCE = new DependencySetup();
	
	public EDhApiRenderingEngine coreRenderingEngine = null;
	
	
	
	//=============//
	// constructor //
	//=============//
	//region
	
	private DependencySetup() {}
	
	//endregion
	
	
	
	//==========//
	// bindings //
	//==========//
	//region
	
	public void createSharedBindings()
	{
		SingletonInjector.INSTANCE.bind(ILangWrapper.class, LangWrapper.INSTANCE);
		SingletonInjector.INSTANCE.bind(IVersionConstants.class, VersionConstants.INSTANCE);
		SingletonInjector.INSTANCE.bind(IWrapperFactory.class, WrapperFactory.INSTANCE);
		SingletonInjector.INSTANCE.bind(IKeyedClientLevelManager.class, new KeyedClientLevelManager());
		SingletonInjector.INSTANCE.bind(IDhApiCustomRenderObjectFactory.class, GenericRenderObjectFactory.INSTANCE);
		SingletonInjector.INSTANCE.bind(IDependencySetup.class, DependencySetup.INSTANCE);
	}
	
	public void createServerBindings()
	{ SingletonInjector.INSTANCE.bind(IMinecraftSharedWrapper.class, MinecraftServerWrapper.INSTANCE); }
	
	public void createClientBindings()
	{
		SingletonInjector.INSTANCE.bind(IMinecraftClientWrapper.class, MinecraftClientWrapper.INSTANCE);
		SingletonInjector.INSTANCE.bind(IMinecraftSharedWrapper.class, MinecraftClientWrapper.INSTANCE);
		SingletonInjector.INSTANCE.bind(IMinecraftRenderWrapper.class, MinecraftRenderWrapper.INSTANCE);
		SingletonInjector.INSTANCE.bind(IConfigGui.class, ClassicConfigGUI.CONFIG_CORE_INTERFACE);
		SingletonInjector.INSTANCE.bind(IBlockStateFaceTextureProvider.class, BlockStateTextureProvider.INSTANCE);
		SingletonInjector.INSTANCE.bind(DebugWireframeHandler.class, DebugWireframeHandler.INSTANCE);
	}
	
	//endregion
	
	
	
	//================//
	// render binding //
	//================//
	//region
	
	/** 
	 * will be called from a DH thread, not the render thread
	 * @return a future containing: null if the binding was successful, otherwise the error message
	 */
	@Override
	public CompletableFuture<String> setRenderingApiBindingsFromConfigAsync()
	{
		EDhApiRenderingEngine renderingApiEnum = Config.Client.Advanced.Graphics.Experimental.renderingEngine.get();
		if (renderingApiEnum == EDhApiRenderingEngine.AUTO)
		{
			IVersionConstants versionConstants = SingletonInjector.INSTANCE.get(IVersionConstants.class);
			renderingApiEnum = versionConstants.getDefaultRenderingEngine();
		}
		
		LOGGER.info("Setting DH Rendering Engine to: ["+renderingApiEnum+"]...");
		
		
		
		boolean validApi;
		AbstractDhApiRenderDefinition renderDefinition;
		if (renderingApiEnum == EDhApiRenderingEngine.OPEN_GL)
		{
			validApi = true;
			renderDefinition = new GlDhRenderApiDefinition();
		}
		else if (renderingApiEnum == EDhApiRenderingEngine.BLAZE_3D)
		{
			#if MC_VER <= MC_1_21_10
			validApi = false;
			renderDefinition = null;
			#else
			validApi = true;
			renderDefinition = new BlazeDhRenderApiDefinition();
			#endif
		}
		else
		{
			String message = "No ["+ AbstractDhApiRenderDefinition.class.getSimpleName()+"] concrete implementation found for the value: ["+renderingApiEnum+"].";
			return CompletableFuture.completedFuture(message);
		}
		
		
		// crash if an invalid API is set
		if (!validApi)
		{
			String message = "The Distant Horizons rendering engine ["+renderDefinition.getName()+"]-["+renderingApiEnum+"] is not supported with this Minecraft config, reverting to ["+ EDhApiRenderingEngine.AUTO+"].";
			Config.Client.Advanced.Graphics.Experimental.renderingEngine.set(EDhApiRenderingEngine.AUTO);
			return CompletableFuture.completedFuture(message);
		}
		
		
		CompletableFuture<String> setResultFuture = this.trySetRenderDefinitionAsync(renderDefinition, renderingApiEnum, null);
		setResultFuture.thenAccept((String errorMessage) -> 
		{
			if (errorMessage != null)
			{
				Config.Client.Advanced.Graphics.Experimental.renderingEngine.set(EDhApiRenderingEngine.AUTO);
			}
		});
		return setResultFuture;
	}
	
	/**
	 * Will run at the beginning of the next frame.
	 * @return null String on success, otherwise the error message.
	 */
	@Override
	public CompletableFuture<String> trySetRenderDefinitionAsync(AbstractDhApiRenderDefinition renderDefinition, EDhApiRenderingEngine renderingApiEnum, String apiUserDisplayName)
	{
		// complain if the rendering API set doesn't match Minecraft's
		EDhApiRenderingApi mcRenderApi = MinecraftRenderWrapper.INSTANCE.getMcRenderingApi();
		if (mcRenderApi != renderDefinition.getRenderApi())
		{
			String message = "The rendering definition ["+renderDefinition.getName()+"]-["+renderDefinition.getRenderApi().name()+"] cannot be used since it's API doesn't match what Minecraft is currently set to use: ["+mcRenderApi.name()+"]. Please either change Minecraft's rendering API or Distant Horizons'.";
			return CompletableFuture.completedFuture(message);
		}
		
		
		LOGGER.info("Queueing new DH rendering definition: ["+renderDefinition.getName()+"]-["+renderDefinition.getRenderApi().name()+"], type: ["+renderingApiEnum.name()+"]...");
		
		CompletableFuture<String> future = new CompletableFuture<>();
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("API Render Engine Change", () -> 
		{
			try
			{
				// clearing render data before and after setting the new render definition
				// is necessary to prevent the new renderer from accessing old objects
				DhApi.Delayed.renderProxy.clearRenderDataCache();
				
				LOGGER.info("DH rendering definition bound to: [" + renderDefinition.getName() + "]-[" + renderDefinition.getRenderApi().name() + "], name: [" + renderingApiEnum.name() + "].");
				OverrideInjector.INSTANCE.bind(AbstractDhApiRenderDefinition.class, renderDefinition);
				DhApiRenderProxy.INSTANCE.currentRenderingEngine = renderingApiEnum;
				
				if (renderingApiEnum != EDhApiRenderingEngine.API)
				{
					this.coreRenderingEngine = renderingApiEnum;
					
					// if the render def was an API defined one
					// clear the API lock so the internal DH render def could be changed
					if (Config.Client.Advanced.Graphics.Experimental.renderingEngine.getApiValue() == EDhApiRenderingEngine.API)
					{
						Config.Client.Advanced.Graphics.Experimental.renderingEngine.setApiValueWithoutFiringEvents(null, null);	
					}
				}
				else
				{
					Config.Client.Advanced.Graphics.Experimental.renderingEngine.setApiValueWithoutFiringEvents(EDhApiRenderingEngine.API, apiUserDisplayName);
				}
				
				DhApi.Delayed.renderProxy.clearRenderDataCache();
			}
			finally
			{
				future.complete(null);
			}
		});
		
		return future;
	}
	
	/** Will run at the beginning of the next frame. */
	@Override
	public CompletableFuture<Void> clearRenderDefinitionOverrideAsync()
	{
		CompletableFuture<Void> future = new CompletableFuture<>();
		RenderThreadTaskHandler.INSTANCE.queueRunningOnRenderThread("API Render Engine Clear", () ->
		{
			try
			{
				// clearing render data before and after setting the new render definition
				// is necessary to prevent the new renderer from accessing old objects
				DhApi.Delayed.renderProxy.clearRenderDataCache();
				
				// replace all bindings with DH's core one
				AbstractDhApiRenderDefinition coreDef = OverrideInjector.INSTANCE.get(AbstractDhApiRenderDefinition.class, OverrideInjector.CORE_PRIORITY);
				OverrideInjector.INSTANCE.unbindAll(AbstractDhApiRenderDefinition.class);
				OverrideInjector.INSTANCE.bind(AbstractDhApiRenderDefinition.class, coreDef);
				
				DhApiRenderProxy.INSTANCE.currentRenderingEngine = this.coreRenderingEngine;
				Config.Client.Advanced.Graphics.Experimental.renderingEngine.setApiValueWithoutFiringEvents(null, null);
				
				LOGGER.info("DH Rendering reverted to: [" + coreDef.getName() + "].");
				
				DhApi.Delayed.renderProxy.clearRenderDataCache();
			}
			finally
			{
				future.complete(null);
			}
		});
		
		return future;
	}
	
	//endregion
	
	
	
}
