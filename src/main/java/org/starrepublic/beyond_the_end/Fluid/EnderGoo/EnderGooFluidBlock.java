package org.starrepublic.beyond_the_end.Fluid.EnderGoo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.FluidRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

import java.util.ArrayList;
import java.util.List;

public class EnderGooFluidBlock extends LiquidBlock {
    private static final int FLUID_INTERACTION_INTERVAL = 6;
    private static final int LAVA_MEAN_INFECTION_TIME = 20 * 30;
    private static final float LAVA_INFECTION_CHANCE =
            (float) FLUID_INTERACTION_INTERVAL
                    / LAVA_MEAN_INFECTION_TIME;
    private static final Vec3 MOVEMENT_MULTIPLIER =
            new Vec3(0.85D, 0.85D, 0.85D);

    /*
     * Potion Effect 不需要給很長。
     *
     * entityInside 每 tick 都會重新補，
     * 離開 Goo 後大約半秒就消失。
     */
    private static final int EFFECT_DURATION = 10;

    /*
     * Goo 與水反應間隔。
     *
     * 不要每 tick 做水搜索，
     * 避免大型湖泊產生大量更新。
     */
    private static final int WATER_REACTION_DELAY = 6;

    private static final int WATER_BATTLE_INTERVAL = 4;


    public EnderGooFluidBlock(
            FlowingFluid fluid,
            Properties properties
    ) {
        super(fluid, properties);
    }


    /*
     * ========================================
     * Entity interaction
     * ========================================
     */

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity
    ) {

        super.entityInside(
                state,
                level,
                pos,
                entity
        );


        if (EnderGooEffects.isImmune(entity)) {
            return;
        }


        /*
         * 真正流體才有黏滯。
         */
        entity.makeStuckInBlock(
                state,
                MOVEMENT_MULTIPLIER
        );


        EnderGooEffects.applyHazard(
                level,
                entity
        );
    }


    /*
     * ========================================
     * Fluid placement
     * ========================================
     */

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean isMoving
    ) {

        if (!EnderGooRules.canExist(level)) {

            if (!level.isClientSide) {
                vaporize(level, pos);
            }

            return;
        }

        super.onPlace(
                state,
                level,
                pos,
                oldState,
                isMoving
        );


        if (!level.isClientSide) {

            infectAdjacentEndStone(
                    level,
                    pos
            );

            scheduleFluidInteraction(
                    level,
                    pos
            );
        }
    }


    /*
     * ========================================
     * Neighbor updates
     * ========================================
     */
    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {

        BlockState result = super.updateShape(
                state,
                direction,
                neighborState,
                level,
                pos,
                neighborPos
        );


        if (level instanceof Level realLevel) {

            if (
                    !realLevel.isClientSide
                            && EnderGooRules.canExist(realLevel)
            ) {

                scheduleFluidInteraction(
                        realLevel,
                        pos
                );
            }
        }


        return result;
    }
    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {

        super.neighborChanged(
                state,
                level,
                pos,
                block,
                fromPos,
                isMoving
        );


        if (level.isClientSide) {
            return;
        }


        if (!EnderGooRules.canExist(level)) {

            vaporize(
                    level,
                    pos
            );

            return;
        }


        infectAdjacentEndStone(
                level,
                pos
        );


        /*
         * 不管是哪一個鄰居造成更新，
         * 都重新檢查一次水。
         */
        scheduleFluidInteraction(
                level,
                pos
        );
    }


    /*
     * ========================================
     * Scheduled water battle
     * ========================================
     */

    @Override
    protected void tick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {

        super.tick(
                state,
                level,
                pos,
                random
        );


        if (
                !level.getFluidState(pos)
                        .is(Tags.ENDER_GOO)
        ) {
            return;
        }


        /*
         * 非白名單維度：
         * Goo 強制揮發。
         */
        if (!EnderGooRules.canExist(level)) {

            vaporize(level, pos);

            return;
        }


        /*
         * 裸露終界石感染。
         */
        infectAdjacentEndStone(
                level,
                pos
        );


        /*
         * 水戰鬥。
         *
         * 保留你目前已經確認正常的實作。
         */
        if (hasAdjacentWater(level, pos)) {

            reactWithWater(
                    level,
                    pos,
                    random
            );
        }


        /*
         * Water Battle 有可能已經把這格 Goo 消耗掉。
         */
        if (
                !level.getFluidState(pos)
                        .is(Tags.ENDER_GOO)
        ) {
            return;
        }


        /*
         * 岩漿感染。
         */
        if (
                hasAdjacentLava(level, pos)
                        && tryLavaInfection(
                        level,
                        pos,
                        random
                )
        ) {

            /*
             * 已變成 Lava，
             * 後面不再安排 Goo tick。
             */
            return;
        }


        /*
         * 還有水或 Lava 接觸：
         * 繼續下一輪。
         */
        scheduleFluidInteraction(
                level,
                pos
        );
    }


    /*
     * ========================================
     * End Stone infection
     * ========================================
     */

    private void infectAdjacentEndStone(
            Level level,
            BlockPos gooPos
    ) {

        for (Direction direction : Direction.values()) {

            BlockPos targetPos =
                    gooPos.relative(direction);

            BlockState targetState =
                    level.getBlockState(targetPos);


            /*
             * 感染必須同時滿足：
             *
             * 1. 方塊屬於 ENDER_GOO_INFECTABLE
             *
             * 2. 方塊上方必須是 Air
             *
             * 因此只感染暴露於地表的 End Stone，
             * 不會把地下整片終界石轉成附魔終界石。
             */
            if (
                    targetState.is(Tags.ENDER_GOO_INFECTABLE)
                            && level.getBlockState(targetPos.above()).isAir()
            ) {

                level.setBlock(
                        targetPos,
                        BlockRegister.END_STONE_ENCHANTED
                                .get()
                                .defaultBlockState(),
                        Block.UPDATE_ALL
                );
            }
        }
    }


    /*
     * ========================================
     * Water battle
     * ========================================
     */

    private void scheduleFluidInteraction(
            Level level,
            BlockPos pos
    ) {

        if (
                level.isClientSide
                        || !EnderGooRules.canExist(level)
                        || !level.getFluidState(pos)
                        .is(Tags.ENDER_GOO)
        ) {
            return;
        }


        if (
                hasAdjacentWater(level, pos)
                        || hasAdjacentLava(level, pos)
        ) {

            level.scheduleTick(
                    pos,
                    this,
                    FLUID_INTERACTION_INTERVAL
            );
        }
    }


    private void reactWithWater(
            ServerLevel level,
            BlockPos gooPos,
            RandomSource random
    ) {

        List<BlockPos> waterBlocks =
                findAdjacentWater(
                        level,
                        gooPos
                );


        if (waterBlocks.isEmpty()) {
            return;
        }


        /*
         * 從所有直接接觸 Goo 的水中，
         * 隨機挑一格進行這一次戰鬥。
         */
        BlockPos waterPos =
                waterBlocks.get(
                        random.nextInt(
                                waterBlocks.size()
                        )
                );


        /*
         * 在允許 Ender Goo 存在的維度中，
         * Goo 有很高的戰鬥優勢。
         *
         * 先從 85% 開始測。
         */
        boolean gooWins =
                random.nextFloat() < 0.85F;


        if (gooWins) {

            /*
             * 水被 Goo 入侵。
             *
             * 注意：
             * 不要直接產生 Source Goo，
             * 否則 Goo 在水池裡可能大量建立永久 Source。
             *
             * 使用 Flowing Goo。
             */
            level.setBlock(
                    waterPos,

                    FluidRegister.FLOWING_ENDER_GOO
                            .get()
                            .getFlowing(
                                    6,
                                    false
                            )
                            .createLegacyBlock(),

                    Block.UPDATE_ALL
            );


            /*
             * 戰鬥需要消耗 Goo 本身。
             *
             * 35% 機率，發動攻擊的這格 Goo 消失。
             *
             * 這就是：
             *
             * Goo 打贏了水，
             * 但自己也可能在反應中消耗掉。
             */
            if (random.nextFloat() < 0.35F) {

                level.setBlock(
                        gooPos,
                        Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL
                );
            }


            spawnBattleEffects(
                    level,
                    waterPos,
                    true
            );
        }

        else {

            /*
             * 少數情況 Water 戰勝 Goo。
             *
             * Goo 消失。
             *
             * 不需要手動把這裡改成 Water，
             * 周圍的 Water 自然會流過來。
             */
            level.setBlock(
                    gooPos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL
            );


            spawnBattleEffects(
                    level,
                    gooPos,
                    false
            );


            return;
        }


        /*
         * 如果攻擊者仍然存在，
         * 而且還接觸著水，
         * 就進行下一輪。
         */
        if (
                level.getFluidState(gooPos)
                        .is(Tags.ENDER_GOO)
        ) {

            scheduleFluidInteraction(
                    level,
                    gooPos
            );
        }


        /*
         * 新生成的 Goo 也必須可以繼續戰鬥。
         *
         * 否則戰線只會前進一格。
         */
        if (
                level.getFluidState(waterPos)
                        .is(Tags.ENDER_GOO)
        ) {

            scheduleFluidInteraction(
                    level,
                    waterPos
            );
        }
    }
    private boolean hasAdjacentLava(
            Level level,
            BlockPos pos
    ) {

        for (Direction direction : Direction.values()) {

            BlockPos targetPos =
                    pos.relative(direction);

            if (
                    level.getFluidState(targetPos)
                            .is(FluidTags.LAVA)
            ) {
                return true;
            }
        }

        return false;
    }
    private void spawnBattleEffects(
            ServerLevel level,
            BlockPos pos,
            boolean gooWon
    ) {

        /*
         * 少量煙霧：
         * 表示兩種液體發生劇烈反應。
         */
        level.sendParticles(
                ParticleTypes.SMOKE,

                pos.getX() + 0.5D,
                pos.getY() + 0.6D,
                pos.getZ() + 0.5D,

                4,

                0.25D,
                0.15D,
                0.25D,

                0.01D
        );


        /*
         * 不要每次都播放很大聲，
         * 大型水池戰鬥會非常吵。
         */
        if (level.random.nextFloat() < 0.25F) {

            level.playSound(
                    null,
                    pos,

                    SoundEvents.FIRE_EXTINGUISH,

                    SoundSource.BLOCKS,

                    0.25F,

                    gooWon
                            ? 1.6F
                            : 2.0F
            );
        }
    }
    private boolean tryLavaInfection(
            ServerLevel level,
            BlockPos pos,
            RandomSource random
    ) {

        FluidState gooState =
                level.getFluidState(pos);


        /*
         * 這格已經不是 Ender Goo。
         */
        if (
                !gooState.is(
                        Tags.ENDER_GOO
                )
        ) {
            return false;
        }


        /*
         * 必須直接接觸岩漿。
         */
        if (!hasAdjacentLava(level, pos)) {
            return false;
        }


        /*
         * 尚未感染成功。
         */
        if (
                random.nextFloat()
                        >= LAVA_INFECTION_CHANCE
        ) {
            return false;
        }


        /*
         * 保留 Source / Flowing 的差異。
         */
        FluidState lavaState;

        if (gooState.isSource()) {

            lavaState =
                    Fluids.LAVA.getSource(false);
        }

        else {

            /*
             * Goo Amount 是 1~8。
             *
             * Flowing Lava 接受 flowing level。
             */
            int amount =
                    Mth.clamp(
                            gooState.getAmount(),
                            1,
                            7
                    );

            lavaState =
                    Fluids.LAVA.getFlowing(
                            amount,
                            false
                    );
        }


        level.setBlock(
                pos,
                lavaState.createLegacyBlock(),
                Block.UPDATE_ALL
        );


        /*
         * 少量視覺效果。
         */
        level.sendParticles(
                ParticleTypes.SMOKE,

                pos.getX() + 0.5,
                pos.getY() + 0.65,
                pos.getZ() + 0.5,

                5,

                0.2,
                0.15,
                0.2,

                0.01
        );


        level.playSound(
                null,
                pos,
                SoundEvents.LAVA_POP,
                SoundSource.BLOCKS,
                0.35F,
                0.8F + random.nextFloat() * 0.3F
        );


        return true;
    }
    private boolean hasAdjacentWater(
            Level level,
            BlockPos pos
    ) {

        for (Direction direction : Direction.values()) {

            BlockPos targetPos =
                    pos.relative(direction);

            if (isPureWaterBlock(level, targetPos)) {
                return true;
            }
        }

        return false;
    }


    private List<BlockPos> findAdjacentWater(
            Level level,
            BlockPos pos
    ) {

        List<BlockPos> result =
                new ArrayList<>();


        for (Direction direction : Direction.values()) {

            BlockPos targetPos =
                    pos.relative(direction);

            if (isPureWaterBlock(level, targetPos)) {
                result.add(targetPos);
            }
        }


        return result;
    }


    /*
     * 不直接：
     *
     * level.getFluidState(pos).is(FluidTags.WATER)
     *
     * 因為現代 Minecraft 有 Waterlogged block。
     *
     * 如果直接把任何 WATER FluidState 都變 AIR，
     * 可能會把：
     *
     * - waterlogged stairs
     * - fences
     * - plants
     * - 其他模組方塊
     *
     * 整個刪掉。
     */
    private boolean isPureWaterBlock(
            Level level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        return
                state.getBlock() instanceof LiquidBlock
                        && state.getFluidState().is(FluidTags.WATER);
    }


    /*
     * ========================================
     * Dimension
     * ========================================
     */


    /*
     * ========================================
     * Forced vaporization
     * ========================================
     */

    private void vaporize(
            Level level,
            BlockPos pos
    ) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }


        /*
         * 先刪除 Goo。
         */
        level.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
        );


        /*
         * 模仿 Nether Water。
         */
        level.playSound(
                null,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F
                        + (level.random.nextFloat()
                        - level.random.nextFloat())
                        * 0.8F
        );


        serverLevel.sendParticles(
                ParticleTypes.LARGE_SMOKE,

                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,

                8,

                0.35D,
                0.35D,
                0.35D,

                0.01D
        );
    }
}
