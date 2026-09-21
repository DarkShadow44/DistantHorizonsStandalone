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

package testItems.worldGeneratorInjection.objects;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.coreapi.DependencyInjection.OverrideInjector;
import com.seibel.distanthorizons.core.util.LodUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Dummy test implementation object for world generator injection unit tests.
 *
 * @author James Seibel
 * @version 2022-12-5
 */
public class TestWorldGenerator implements IDhApiWorldGenerator
{
	public static final byte SMALLEST_DETAIL_LEVEL = 1;
	
	
	// testable methods //
	
	@Override
	public int getPriority() { return OverrideInjector.CORE_PRIORITY; }
	
	@Override
	public byte getSmallestDataDetailLevel() { return SMALLEST_DETAIL_LEVEL; }
	
	
	
	
	// not used when unit testing //
	
	//======================//
	// generator parameters //
	//======================//
	
	@Override
	public byte getLargestDataDetailLevel() { return LodUtil.BLOCK_DETAIL_LEVEL; }
	
	
	
	//===================//
	// generator methods //
	//===================//
	
	@Override
	public void close() { }
	
	@Override
	public CompletableFuture<Void> generateChunks(int chunkPosMinX, int chunkPosMinZ, int generationRequestChunkWidthCount, byte targetDataDetail, EDhApiDistantGeneratorMode maxGenerationStep, ExecutorService executorService, Consumer<Object[]> resultConsumer) { return null; }
	
	@Override
	public void preGeneratorTaskStart() { }
	
	
}

