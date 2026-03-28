package com.seibel.distanthorizons.common.wrappers.worldGeneration;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import copy.com.gtnewhorizons.angelica.compat.mojang.ChunkPos;

public class ChunkPosGenStream {

    public static Iterator<ChunkPos> getIterator(int genMinX, int genMinZ, int width, int extraRadius) {
        return getStream(genMinX, genMinZ, width, extraRadius).iterator();
    }

    /** @param extraRadius in both the positive and negative directions */
    public static Stream<ChunkPos> getStream(int genMinX, int genMinZ, int width, int extraRadius) {
        return StreamSupport.stream(new InclusiveChunkPosIterator(genMinX, genMinZ, width, extraRadius), false);
    }

    private static class InclusiveChunkPosIterator extends Spliterators.AbstractSpliterator<ChunkPos> {

        private final int minX;
        private final int minZ;

        private final int maxX;
        private final int maxZ;

        /** current X pos */
        int x;
        /** current Z pos */
        private int z;

        // =============//
        // constructor //
        // =============//

        protected InclusiveChunkPosIterator(int genMinX, int genMinZ, int width, int extraRadius) {
            super(getCount(width, extraRadius), Spliterator.SIZED);

            this.minX = genMinX - extraRadius;
            this.minZ = genMinZ - extraRadius;

            this.maxX = genMinX + (width - 1) + extraRadius;
            this.maxZ = genMinZ + (width - 1) + extraRadius;

            // X starts at 1 minus the minX so we can immediately re-add 1 in the tryAdvance() loop
            this.x = this.minX - 1;
            this.z = this.minZ;
        }

        private static int getCount(int width, int extraRadius) {
            int widthPlusExtra = width + (extraRadius * 2);
            return widthPlusExtra * widthPlusExtra;
        }

        // =================//
        // iterator method //
        // =================//

        @Override
        public boolean tryAdvance(Consumer<? super ChunkPos> consumer) {
            if (this.x == this.maxX && this.z == this.maxZ) {
                // the last returned position was the final valid position
                return false;
            }

            if (this.x == this.maxX) {
                // we reached the max X position, loop back around in the next Z row
                this.x = this.minX;
                this.z++;
            } else {
                this.x++;
            }

            consumer.accept(new ChunkPos(this.x, this.z));
            return true;
        }
    }

}
