package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.FluidRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnderGooLakeFeature extends Feature<NoneFeatureConfiguration> {

    /*
     * 整體湖群大小
     */
    private static final int MIN_RADIUS = 20;
    private static final int MAX_RADIUS = 50;

    /*
     * Blob 數量越多，形狀越不規則、越像自然蔓延
     */
    private static final int MIN_BLOBS = 4;
    private static final int MAX_BLOBS = 7;

    /*
     * 地形高差容忍值
     *
     * 太誇張的高差不要生成，
     * 否則會看起來亂七八糟。
     */
    private static final int MAX_ALLOWED_HEIGHT_VARIATION = 8;

    /*
     * 候選區塊太少代表 blob 太小或地形不適合
     */
    private static final int MIN_REQUIRED_CELLS = 180;

    public EnderGooLakeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int radius = MIN_RADIUS + random.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int blobCount = MIN_BLOBS + random.nextInt(MAX_BLOBS - MIN_BLOBS + 1);

        /*
         * 建立不規則湖區遮罩
         */
        List<Blob> blobs = new ArrayList<>();

        for (int i = 0; i < blobCount; i++) {
            int offsetX = random.nextInt(radius + 1) - radius / 2;
            int offsetZ = random.nextInt(radius + 1) - radius / 2;

            int blobRadiusX = 5 + random.nextInt(7); // 5~11
            int blobRadiusZ = 4 + random.nextInt(6); // 4~9

            blobs.add(new Blob(
                    origin.getX() + offsetX,
                    origin.getZ() + offsetZ,
                    blobRadiusX,
                    blobRadiusZ
            ));
        }

        /*
         * 收集所有候選表面格
         *
         * key = x,z
         * value = surfaceY
         */
        Map<Long, Integer> candidates = new HashMap<>();

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                if (!insideAnyBlob(x, z, blobs)) {
                    continue;
                }

                int surfaceY = level.getHeight(
                        Heightmap.Types.WORLD_SURFACE_WG,
                        x,
                        z
                ) - 1;

                BlockPos surfacePos = new BlockPos(x, surfaceY, z);
                BlockState surfaceState = level.getBlockState(surfacePos);

                /*
                 * 必須是附魔之地地表
                 */
                if (!surfaceState.is(BlockRegister.END_STONE_ENCHANTED.get())) {
                    continue;
                }

                /*
                 * 上方必須是空氣
                 */
                if (!level.getBlockState(surfacePos.above()).isAir()) {
                    continue;
                }

                /*
                 * 下方不能是空洞/液體，避免鋪在脆弱位置造成外洩
                 */
                BlockState below = level.getBlockState(surfacePos.below());
                if (below.isAir() || !below.getFluidState().isEmpty()) {
                    continue;
                }

                long key = pack(x, z);
                candidates.put(key, surfaceY);

                minY = Math.min(minY, surfaceY);
                maxY = Math.max(maxY, surfaceY);
            }
        }

        if (candidates.size() < MIN_REQUIRED_CELLS) {
            return false;
        }

        if (maxY - minY > MAX_ALLOWED_HEIGHT_VARIATION) {
            return false;
        }

        BlockState gooState = FluidRegister.ENDER_GOO.get()
                .getSource(false)
                .createLegacyBlock();

        int placed = 0;

        /*
         * 真正鋪設 Goo
         *
         * 只鋪「穩定格」
         * 也就是：
         * 不直接臨接向下落差的邊界
         *
         * 這樣可以保留一圈天然堤岸，避免 Goo 往低處流。
         */
        for (Map.Entry<Long, Integer> entry : candidates.entrySet()) {
            int x = unpackX(entry.getKey());
            int z = unpackZ(entry.getKey());
            int y = entry.getValue();

            if (!isStableLakeCell(level, candidates, x, y, z)) {
                continue;
            }

            BlockPos pos = new BlockPos(x, y, z);
            level.setBlock(pos, gooState, Block.UPDATE_CLIENTS);
            placed++;
        }

        return placed > 0;
    }

    private boolean isStableLakeCell(
            WorldGenLevel level,
            Map<Long, Integer> candidates,
            int x,
            int y,
            int z
    ) {

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            int nx = x + direction.getStepX();
            int nz = z + direction.getStepZ();

            int neighborSurfaceY = level.getHeight(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    nx,
                    nz
            ) - 1;

            /*
             * 如果鄰居比當前低，
             * 這格就作為「堤岸」保留，不鋪 Goo。
             *
             * 這是避免流出去的核心。
             */
            if (neighborSurfaceY < y) {
                return false;
            }

            /*
             * 如果鄰居也在候選區中，但高度更低，
             * 一樣不鋪，讓台階之間保留分隔帶。
             */
            Integer neighborCandidateY = candidates.get(pack(nx, nz));
            if (neighborCandidateY != null && neighborCandidateY < y) {
                return false;
            }
        }

        return true;
    }

    private boolean insideAnyBlob(int x, int z, List<Blob> blobs) {
        for (Blob blob : blobs) {
            double nx = (x - blob.cx()) / (double) blob.rx();
            double nz = (z - blob.cz()) / (double) blob.rz();

            if (nx * nx + nz * nz <= 1.0D) {
                return true;
            }
        }
        return false;
    }

    private long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) << 32 | ((long) z & 0xFFFFFFFFL);
    }

    private int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private int unpackZ(long packed) {
        return (int) packed;
    }

    private record Blob(int cx, int cz, int rx, int rz) {
    }
}
