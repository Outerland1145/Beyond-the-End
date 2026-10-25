package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starrepublic.beyond_the_end.World.DFM.BTEEndFightSettingsSavedData;

import java.util.List;

public class BTEEndSpikeFeature extends Feature<SpikeConfiguration> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(BTEEndSpikeFeature.class);

    public BTEEndSpikeFeature() {
        super(SpikeConfiguration.CODEC);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<SpikeConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        SpikeConfiguration config = context.config();

        // 世界存檔優先，不直接讀取 TOML
        boolean customStructures =
                BTEEndFightSettingsSavedData
                        .get(level.getLevel())
                        .isCustomSpikeStructuresEnabled();

        List<SpikeFeature.EndSpike> spikes =
                config.getSpikes().isEmpty()
                        ? BTEEndSpikeLayout.getSpikes(level.getLevel())
                        : config.getSpikes();

        StructureTemplateManager structureManager =
                level.getServer().getStructureManager();

        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();

        boolean generated = false;

        for (SpikeFeature.EndSpike spike : spikes) {

            // 只在柱子中心所屬區塊執行
            if (!spike.isCenterWithinChunk(context.origin())) {
                continue;
            }

            int centerX = spike.getCenterX();
            int centerZ = spike.getCenterZ();
            int radius = spike.getRadius();
            int height = spike.getHeight();

            if (radius < 1
                    || height <= 65
                    || height + 6 >= level.getMaxBuildHeight()) {
                continue;
            }

            // ==========================================
            // 1. 取得自訂塔頂、塔底 NBT
            // ==========================================

            StructureTemplate topTemplate = null;
            StructureTemplate bottomTemplate = null;

            if (customStructures) {

                /*
                 * 受保護柱子使用 guarded 塔頂。
                 * 籠子、水晶等內容全部由 NBT 決定。
                 */
                String topPath = spike.isGuarded()
                        ? "end_spike/top_radius_"
                        + radius + "_guarded"
                        : "end_spike/top_radius_" + radius;

                String bottomPath =
                        "end_spike/bottom_radius_" + radius;

                ResourceLocation topId =
                        ResourceLocation.fromNamespaceAndPath(
                                "beyond_the_end",
                                topPath
                        );

                ResourceLocation bottomId =
                        ResourceLocation.fromNamespaceAndPath(
                                "beyond_the_end",
                                bottomPath
                        );

                topTemplate =
                        structureManager.get(topId).orElse(null);

                bottomTemplate =
                        structureManager.get(bottomId).orElse(null);

                if (topTemplate == null || bottomTemplate == null) {

                    LOGGER.error(
                            "Missing End Spike NBT: top={}, bottom={}",
                            topId, bottomId
                    );

                    continue;
                }

                int topX = topTemplate.getSize().getX();
                int topY = topTemplate.getSize().getY();
                int topZ = topTemplate.getSize().getZ();

                int bottomX = bottomTemplate.getSize().getX();
                int bottomY = bottomTemplate.getSize().getY();
                int bottomZ = bottomTemplate.getSize().getZ();

                // 長寬必須為奇數，才能精確對齊中心
                if (topX <= 0 || topY <= 0 || topZ <= 0
                        || topX % 2 == 0 || topZ % 2 == 0
                        || bottomX <= 0 || bottomY <= 0 || bottomZ <= 0
                        || bottomX % 2 == 0 || bottomZ % 2 == 0) {

                    LOGGER.error(
                            "Invalid End Spike NBT dimensions: " +
                                    "top={} ({}x{}x{}), bottom={} ({}x{}x{})",
                            topId, topX, topY, topZ,
                            bottomId, bottomX, bottomY, bottomZ
                    );

                    continue;
                }

                // 防止塔頂超出世界高度
                if ((long) height + topY
                        > level.getMaxBuildHeight()) {

                    LOGGER.error(
                            "End Spike top exceeds build height: {}",
                            topId
                    );

                    continue;
                }

            }

            // ==========================================
            // 2. 搜尋島嶼表面
            // ==========================================

            /*
             * 不使用高度圖直接取得柱體中心最高點。
             *
             * 龍復活時舊柱體仍存在，
             * 否則容易把原本的柱頂當成島嶼表面。
             */
            int surfaceY = Integer.MIN_VALUE;
            if (customStructures) {
                int sampleDistance = radius + 2;

                if (bottomTemplate != null) {

                    sampleDistance = Math.max(
                            sampleDistance,
                            Math.max(
                                    bottomTemplate.getSize().getX() / 2,
                                    bottomTemplate.getSize().getZ() / 2
                            ) + 2
                    );
                }

                int[][] offsets = {
                        {sampleDistance, 0},
                        {-sampleDistance, 0},
                        {0, sampleDistance},
                        {0, -sampleDistance}
                };

                for (int[] offset : offsets) {

                    for (int y = Math.min(
                            height - 1,
                            level.getMaxBuildHeight() - 2
                    ); y >= level.getMinBuildHeight(); y--) {

                        mutable.set(
                                centerX + offset[0],
                                y,
                                centerZ + offset[1]
                        );

                        if (level.getBlockState(mutable)
                                .is(Blocks.END_STONE)) {

                            surfaceY = Math.max(surfaceY, y);
                            break;
                        }
                    }
                }

                // 周圍找不到地表，再檢查中心下方的終界石
                if (surfaceY == Integer.MIN_VALUE) {

                    for (int y = height - 1;
                         y >= level.getMinBuildHeight(); y--) {

                        mutable.set(centerX, y, centerZ);

                        if (level.getBlockState(mutable)
                                .is(Blocks.END_STONE)) {

                            surfaceY = y;
                            break;
                        }
                    }
                }

                // ==========================================
                // 3. 沒有地表時建立終界石浮島
                // ==========================================

                if (surfaceY == Integer.MIN_VALUE) {

                    surfaceY = 64;

                    // 浮島頂部半徑比柱體多三格
                    int islandRadius = radius + 3;

                    RandomSource islandRandom =
                            RandomSource.create(
                                    level.getSeed()
                                            ^ ((long) centerX * 341873128712L)
                                            ^ ((long) centerZ * 132897987541L)
                            );

                    for (int dx = -islandRadius;
                         dx <= islandRadius; dx++) {

                        for (int dz = -islandRadius;
                             dz <= islandRadius; dz++) {

                            int distanceSquared =
                                    dx * dx + dz * dz;

                            if (distanceSquared >
                                    islandRadius * islandRadius) {
                                continue;
                            }

                            double distance =
                                    Math.sqrt(distanceSquared);

                            int depth = Math.max(
                                    1,
                                    (int) Math.ceil(
                                            (islandRadius - distance) * 1.6D
                                    ) + 1 + islandRandom.nextInt(3)
                            );

                            for (int dy = 0; dy < depth; dy++) {

                                int y = surfaceY - dy;

                                if (y < level.getMinBuildHeight()) {
                                    break;
                                }

                                mutable.set(
                                        centerX + dx,
                                        y,
                                        centerZ + dz
                                );

                                level.setBlock(
                                        mutable,
                                        Blocks.END_STONE.defaultBlockState(),
                                        2
                                );
                            }
                        }
                    }
                }
            }


            int baseY = customStructures
                    ? surfaceY + 1
                    : Math.max(0, level.getMinBuildHeight());

            if (baseY >= height) {

                LOGGER.error(
                        "End Spike base is above its top at X={}, Z={}",
                        centerX, centerZ
                );

                continue;
            }

            if (customStructures
                    && baseY + bottomTemplate.getSize().getY() > height) {

                LOGGER.error(
                        "End Spike bottom NBT overlaps top: X={}, Z={}",
                        centerX, centerZ
                );

                continue;
            }

            // ==========================================
            // 4. 生成黑曜石中段柱體
            // ==========================================

            /*
             * 無論自訂 NBT 是否開啟，
             * 柱體表面都會混入哭泣黑曜石。
             *
             * 使用固定種子，避免龍復活後裝飾分布改變。
             */
            RandomSource bodyRandom =
                    RandomSource.create(
                            level.getSeed()
                                    ^ ((long) centerX * 42317861L)
                                    ^ ((long) centerZ * 89137417L)
                                    ^ 0x5DEECE66DL
                    );

            int radiusSquared = radius * radius + 1;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    int distanceSquared =
                            dx * dx + dz * dz;

                    if (distanceSquared > radiusSquared) {
                        continue;
                    }

                    // 判斷是否為柱體水平截面的外表面
                    boolean exterior =
                            (dx + 1) * (dx + 1) + dz * dz > radiusSquared
                                    || (dx - 1) * (dx - 1) + dz * dz > radiusSquared
                                    || dx * dx + (dz + 1) * (dz + 1) > radiusSquared
                                    || dx * dx + (dz - 1) * (dz - 1) > radiusSquared;

                    for (int y = baseY; y < height; y++) {

                        mutable.set(
                                centerX + dx,
                                y,
                                centerZ + dz
                        );

                        BlockState state =
                                exterior
                                        && bodyRandom.nextFloat() < 0.09F
                                        ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                        : Blocks.OBSIDIAN.defaultBlockState();

                        level.setBlock(
                                mutable,
                                state,
                                2
                        );
                    }
                }
            }

            // ==========================================
            // 5. 清理柱頂生成空間
            // ==========================================

            /*
             * 自訂模式根據 NBT 尺寸清理空間。
             * 非自訂模式則沿用一般柱頂範圍。
             */
            int clearRadius = radius + 2;
            int clearHeight = 5;

            if (customStructures) {

                clearRadius = Math.max(
                        clearRadius,
                        Math.max(
                                topTemplate.getSize().getX() / 2,
                                topTemplate.getSize().getZ() / 2
                        )
                );

                clearHeight = Math.max(
                        clearHeight,
                        topTemplate.getSize().getY() + 1
                );
            }

            for (int x = centerX - clearRadius;
                 x <= centerX + clearRadius; x++) {

                for (int z = centerZ - clearRadius;
                     z <= centerZ + clearRadius; z++) {

                    for (int y = height;
                         y <= Math.min(
                                 height + clearHeight,
                                 level.getMaxBuildHeight() - 1
                         ); y++) {

                        mutable.set(x, y, z);

                        level.setBlock(
                                mutable,
                                Blocks.AIR.defaultBlockState(),
                                2
                        );
                    }
                }
            }

            // ==========================================
            // 6. 自訂 NBT 放置
            // ==========================================

            if (customStructures) {

                /*
                 * 必須允許實體生成：
                 * NBT 裡的終界水晶就是透過此設定載入。
                 *
                 * 不由 Java 生成籠子、水晶、火或基岩。
                 */
                StructurePlaceSettings placeSettings =
                        new StructurePlaceSettings()
                                .setMirror(Mirror.NONE)
                                .setRotation(Rotation.NONE)
                                .setIgnoreEntities(false);

                /*
                 * 塔底：
                 * XZ 中心對齊柱體。
                 * NBT 的 Y=0 貼在地表上方。
                 */
                BlockPos bottomStart = new BlockPos(
                        centerX - bottomTemplate.getSize().getX() / 2,
                        baseY,
                        centerZ - bottomTemplate.getSize().getZ() / 2
                );

                boolean bottomPlaced =
                        bottomTemplate.placeInWorld(
                                level,
                                bottomStart,
                                bottomStart,
                                placeSettings,
                                context.random(),
                                2
                        );

                /*
                 * 塔頂：
                 * XZ 中心對齊柱體。
                 * NBT 的 Y=0 貼在柱體最上方。
                 */
                BlockPos topStart = new BlockPos(
                        centerX - topTemplate.getSize().getX() / 2,
                        height,
                        centerZ - topTemplate.getSize().getZ() / 2
                );

                boolean topPlaced =
                        topTemplate.placeInWorld(
                                level,
                                topStart,
                                topStart,
                                placeSettings,
                                context.random(),
                                2
                        );

                if (!bottomPlaced || !topPlaced) {

                    LOGGER.error(
                            "Failed to place custom End Spike: X={}, Z={}, " +
                                    "bottom={}, top={}",
                            centerX,
                            centerZ,
                            bottomPlaced,
                            topPlaced
                    );
                }

                generated |= bottomPlaced && topPlaced;

                /*
                 * 自訂模式到此結束。
                 *
                 * 不建立基岩。
                 * 不建立籠子。
                 * 不建立火。
                 * 不建立水晶。
                 *
                 * 全部交給 NBT。
                 */
                continue;
            }

            // ==========================================
            // 7. 非自訂模式：原本的柱頂設計
            // ==========================================

            BlockPos top = new BlockPos(
                    centerX,
                    height,
                    centerZ
            );

            level.setBlock(
                    top,
                    Blocks.BEDROCK.defaultBlockState(),
                    2
            );

            // 原本受保護柱子的鐵柵欄籠子
            if (spike.isGuarded()) {

                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        for (int y = 0; y <= 3; y++) {

                            boolean xEdge = Math.abs(x) == 2;
                            boolean zEdge = Math.abs(z) == 2;
                            boolean roof = y == 3;

                            if (!xEdge && !zEdge && !roof) {
                                continue;
                            }

                            boolean connectNS = xEdge || roof;
                            boolean connectEW = zEdge || roof;

                            BlockState bars = Blocks.IRON_BARS
                                    .defaultBlockState()
                                    .setValue(
                                            IronBarsBlock.NORTH,
                                            connectNS && z != -2
                                    )
                                    .setValue(
                                            IronBarsBlock.SOUTH,
                                            connectNS && z != 2
                                    )
                                    .setValue(
                                            IronBarsBlock.WEST,
                                            connectEW && x != -2
                                    )
                                    .setValue(
                                            IronBarsBlock.EAST,
                                            connectEW && x != 2
                                    );

                            level.setBlock(
                                    top.offset(x, y, z),
                                    bars,
                                    3
                            );
                        }
                    }
                }
            }

            level.setBlock(
                    top.above(),
                    Blocks.FIRE.defaultBlockState(),
                    3
            );

            EndCrystal crystal = new EndCrystal(
                    level.getLevel(),
                    centerX + 0.5,
                    height + 1.0,
                    centerZ + 0.5
            );

            crystal.setBeamTarget(
                    config.getCrystalBeamTarget()
            );

            crystal.setInvulnerable(
                    config.isCrystalInvulnerable()
            );

            level.addFreshEntity(crystal);

            generated = true;
        }

        return generated;
    }
}


