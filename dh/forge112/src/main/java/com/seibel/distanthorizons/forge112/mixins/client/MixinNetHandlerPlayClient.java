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

package com.seibel.distanthorizons.forge112.mixins.client;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient
{
	@Inject(method = "handleJoinGame", at = @At("RETURN"))
	private void onHandleJoinGameEnd(SPacketJoinGame packet, CallbackInfo ci)
	{
		ClientApi.INSTANCE.onClientOnlyConnected();
	}
	
	@Inject(method = "onDisconnect", at = @At("RETURN"))
	private void onDisconnect(ITextComponent reason, CallbackInfo ci)
	{
		ClientApi.INSTANCE.onClientOnlyDisconnected();
	}
	
}
