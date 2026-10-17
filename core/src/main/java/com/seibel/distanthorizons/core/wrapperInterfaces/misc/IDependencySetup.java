package com.seibel.distanthorizons.core.wrapperInterfaces.misc;

import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingEngine;
import com.seibel.distanthorizons.api.interfaces.render.renderDef.AbstractDhApiRenderDefinition;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;

import java.util.concurrent.CompletableFuture;

public interface IDependencySetup extends IBindable
{
	CompletableFuture<String> setRenderingApiBindingsFromConfigAsync() throws Exception;
	
	/** Will run at the beginning of the next frame. */
	CompletableFuture<String> trySetRenderDefinitionAsync(AbstractDhApiRenderDefinition renderDefinition, EDhApiRenderingEngine renderingApiEnum, String apiUserDisplayName);
	/** Will run at the beginning of the next frame. */
	CompletableFuture<Void> clearRenderDefinitionOverrideAsync();
	
	
	
}
