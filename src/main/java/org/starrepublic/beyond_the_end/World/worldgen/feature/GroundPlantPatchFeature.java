package org.starrepublic.beyond_the_end.World.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;


public class GroundPlantPatchFeature
        extends Feature<GroundPlantPatchFeature.Configuration> {

    public GroundPlantPatchFeature(Codec<Configuration> codec) {
        super(codec);
    }

    /*
     * ============================================================
     * 單個植物定義
     * ============================================================
     *
     * provider：
     *     具體生成什麼方塊。
     *
     * weight：
     *     在這個 Patch 中被選中的相對權重。
     *
     * 例如：
     *
     * 小蕨 weight = 4
     * 大蕨 weight = 1
     *
     * 就代表：
     *
     * 小蕨約 80%
     * 大蕨約 20%
     */
    public record PlantEntry(
            BlockStateProvider provider,
            int weight
    ) {

        public static final Codec<PlantEntry> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(

                        BlockStateProvider.CODEC
                                .fieldOf("provider")
                                .forGetter(PlantEntry::provider),

                        Codec.intRange(1, 10000)
                                .optionalFieldOf("weight", 1)
                                .forGetter(PlantEntry::weight)

                ).apply(instance, PlantEntry::new));
    }

    /*
     * ============================================================
     * Feature 配置
     * ============================================================
     *
     * plants:
     *     可以生成的植物。
     *
     * tries:
     *     每個斑塊最多嘗試多少次。
     *
     * xz_spread:
     *     斑塊水平擴散範圍。
     *
     * y_search:
     *     因為終界地形不一定完全平，
     *     所以每個 XZ 位置允許上下搜尋多少格地面。
     */
    public record Configuration(
            List<PlantEntry> plants,
            int tries,
            int xzSpread,
            int ySearch
    ) implements FeatureConfiguration {

        public static final Codec<Configuration> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(

                        PlantEntry.CODEC
                                .listOf()
                                .fieldOf("plants")
                                .forGetter(Configuration::plants),

                        Codec.intRange(1, 512)
                                .optionalFieldOf("tries", 24)
                                .forGetter(Configuration::tries),

                        Codec.intRange(0, 32)
                                .optionalFieldOf("xz_spread", 5)
                                .forGetter(Configuration::xzSpread),

                        Codec.intRange(0, 16)
                                .optionalFieldOf("y_search", 3)
                                .forGetter(Configuration::ySearch)

                ).apply(instance, Configuration::new));
    }

    /*
     * ============================================================
     * Feature 主體
     * ============================================================
     */

    @Override
    public boolean place(
            FeaturePlaceContext<Configuration> context
    ) {

        WorldGenLevel level =
                context.level();

        BlockPos origin =
                context.origin();

        RandomSource random =
                context.random();

        Configuration config =
                context.config();

        /*
         * plants 不允許是空的。
         *
         * 不直接丟例外，
         * 避免一份寫錯的 datapack 直接把世界生成炸掉。
         */
        if (config.plants().isEmpty()) {
            return false;
        }

        int placedCount = 0;

        /*
         * ========================================================
         * 一個 Patch 中進行多次嘗試
         * ========================================================
         */

        for (int attempt = 0;
             attempt < config.tries();
             attempt++) {

            /*
             * 使用類似 vanilla RandomPatch 的三角分布：
             *
             * random(A) - random(A)
             *
             * 相比：
             *
             * random(-A ~ +A)
             *
             * 中央位置會稍微更密，
             * 邊緣逐漸變稀。
             *
             * 所以植物斑塊不會像一個方形框。
             */

            int dx =
                    random.nextInt(
                            config.xzSpread() + 1
                    )
                            -
                            random.nextInt(
                                    config.xzSpread() + 1
                            );

            int dz =
                    random.nextInt(
                            config.xzSpread() + 1
                    )
                            -
                            random.nextInt(
                                    config.xzSpread() + 1
                            );

            BlockPos horizontalPos =
                    origin.offset(
                            dx,
                            0,
                            dz
                    );

            /*
             * 按權重選一種植物。
             */
            PlantEntry plant =
                    selectPlant(
                            config.plants(),
                            random
                    );

            if (plant == null) {
                continue;
            }

            /*
             * 在這個 XZ 位置尋找可以放置植物的高度。
             */
            if (tryPlaceAtColumn(
                    level,
                    horizontalPos,
                    plant,
                    config.ySearch(),
                    random
            )) {

                placedCount++;
            }
        }

        return placedCount > 0;
    }

    /*
     * ============================================================
     * 權重隨機
     * ============================================================
     */

    private PlantEntry selectPlant(
            List<PlantEntry> plants,
            RandomSource random
    ) {

        int totalWeight = 0;

        for (PlantEntry plant : plants) {
            totalWeight += plant.weight();
        }

        if (totalWeight <= 0) {
            return null;
        }

        int value =
                random.nextInt(totalWeight);

        for (PlantEntry plant : plants) {

            value -= plant.weight();

            if (value < 0) {
                return plant;
            }
        }

        /*
         * 理論上不可能跑到這裡。
         */
        return plants.getLast();
    }

    /*
     * ============================================================
     * 尋找適合生成植物的位置
     * ============================================================
     */

    private boolean tryPlaceAtColumn(
            WorldGenLevel level,
            BlockPos center,
            PlantEntry entry,
            int ySearch,
            RandomSource random
    ) {

        /*
         * 從：
         *
         * originY + ySearch
         *
         * 一路搜尋到：
         *
         * originY - ySearch
         *
         * 因此即使地表附近有：
         *
         *  +2 格凸起
         *  -2 格凹陷
         *
         * 仍然能正常找到地面。
         */

        for (int yOffset = ySearch;
             yOffset >= -ySearch;
             yOffset--) {

            BlockPos pos =
                    center.offset(
                            0,
                            yOffset,
                            0
                    );

            if (!isInsideBuildHeight(
                    level,
                    pos
            )) {
                continue;
            }

            BlockState plantState =
                    entry.provider()
                            .getState(
                                    random,
                                    pos
                            );

            if (tryPlacePlant(
                    level,
                    pos,
                    plantState
            )) {

                return true;
            }
        }

        return false;
    }

    /*
     * ============================================================
     * 放置植物
     * ============================================================
     */

    private boolean tryPlacePlant(
            WorldGenLevel level,
            BlockPos pos,
            BlockState plantState
    ) {

        /*
         * 不覆蓋現有方塊。
         *
         * 草與蕨只生成在空氣中。
         */
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        Block block =
                plantState.getBlock();

        /*
         * ========================================================
         * 雙格植物
         * ========================================================
         *
         * 例如：
         *
         * Tall Fern
         * Tall Grass
         * 自訂 DoublePlantBlock
         *
         * 全部自動處理。
         */

        if (block instanceof DoublePlantBlock) {

            BlockPos upperPos =
                    pos.above();

            if (!isInsideBuildHeight(
                    level,
                    upperPos
            )) {
                return false;
            }

            /*
             * 上半部也必須有空間。
             */
            if (!level
                    .getBlockState(upperPos)
                    .isAir()) {

                return false;
            }

            /*
             * 強制轉成 LOWER。
             *
             * 即使 datapack 不小心把：
             *
             * half=upper
             *
             * 寫進去，
             * Feature 仍然會從下半部開始正確生成。
             */
            if (plantState.hasProperty(
                    DoublePlantBlock.HALF
            )) {

                plantState =
                        plantState.setValue(
                                DoublePlantBlock.HALF,
                                DoubleBlockHalf.LOWER
                        );
            }

            /*
             * 最重要的地方：
             *
             * 完全尊重植物自己的 canSurvive。
             *
             * Feature 不寫死：
             *
             * END_STONE
             * GRASS_BLOCK
             * DIRT
             *
             * 所以你的侵蝕蕨能長在哪裡，
             * 應該由侵蝕蕨自己的 Block class 控制。
             */
            if (!plantState.canSurvive(
                    level,
                    pos
            )) {

                return false;
            }

            /*
             * DoublePlantBlock.placeAt
             * 自動生成 LOWER + UPPER。
             */
            DoublePlantBlock.placeAt(
                    level,
                    plantState,
                    pos,
                    Block.UPDATE_CLIENTS
            );

            return true;
        }

        /*
         * ========================================================
         * 普通單格植物
         * ========================================================
         */

        if (!plantState.canSurvive(
                level,
                pos
        )) {

            return false;
        }

        level.setBlock(
                pos,
                plantState,
                Block.UPDATE_CLIENTS
        );

        return true;
    }

    /*
     * ============================================================
     * 世界高度檢查
     * ============================================================
     */

    private boolean isInsideBuildHeight(
            WorldGenLevel level,
            BlockPos pos
    ) {

        return pos.getY()
                >= level.getMinBuildHeight()

                && pos.getY()
                < level.getMaxBuildHeight();
    }
}
