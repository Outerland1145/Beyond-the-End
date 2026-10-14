package org.starrepublic.beyond_the_end.Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EndCampfireBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

import java.util.Optional;

public class EndCampfireBlock extends CampfireBlock {

    public EndCampfireBlock(
            boolean spawnParticles,
            int fireDamage,
            BlockBehaviour.Properties properties
    ) {
        super(
                spawnParticles,
                fireDamage,
                properties
        );
    }

    /*
     * ============================================================
     * 自己的 BlockEntity
     * ============================================================
     */

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new EndCampfireBlockEntity(
                pos,
                state
        );
    }

    /*
     * ============================================================
     * Ticker
     * ============================================================
     *
     * Client:
     *   particleTick -> 煙霧
     *
     * Server + Lit:
     *   cookTick -> 烹飪
     *
     * Server + Unlit:
     *   cooldownTick -> 烹飪進度倒退
     *
     * 這裡不能再像上一版一樣 client 直接 return null。
     */

    @Nullable
    @Override
    public <T extends BlockEntity>
    BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {

        /*
         * -------------------------
         * Client
         * -------------------------
         */
        if (level.isClientSide) {

            if (state.getValue(LIT)) {

                return createTickerHelper(
                        type,
                        BlockEntityRegister.END_CAMPFIRE.get(),
                        EndCampfireBlockEntity::particleTick
                );
            }

            return null;
        }

        /*
         * -------------------------
         * Server
         * -------------------------
         */

        if (state.getValue(LIT)) {

            return createTickerHelper(
                    type,
                    BlockEntityRegister.END_CAMPFIRE.get(),
                    EndCampfireBlockEntity::cookTick
            );
        }

        return createTickerHelper(
                type,
                BlockEntityRegister.END_CAMPFIRE.get(),
                EndCampfireBlockEntity::cooldownTick
        );
    }
    @Override
    public @Nullable BlockState getToolModifiedState(
            BlockState state,
            UseOnContext context,
            ItemAbility itemAbility,
            boolean simulate
    ) {

        /*
         * ================================================
         * 打火石 / 火焰彈：點燃終界營火
         * ================================================
         */
        if (itemAbility == ItemAbilities.FIRESTARTER_LIGHT) {

            /*
             * 已經點燃 -> 不處理
             */
            if (state.getValue(LIT)) {
                return null;
            }

            /*
             * 泡水中的營火不能點燃。
             *
             * 這與原版營火一致。
             */
            if (state.getValue(WATERLOGGED)) {
                return null;
            }

            return state.setValue(
                    LIT,
                    true
            );
        }


        /*
         * ================================================
         * 鏟子：熄滅終界營火
         * ================================================
         */
        if (itemAbility == ItemAbilities.SHOVEL_DOUSE) {

            if (!state.getValue(LIT)) {
                return null;
            }

            /*
             * simulate == true 時不能真正修改世界。
             *
             * NeoForge 有時會先詢問：
             *
             * 「如果我這樣使用工具，會發生什麼？」
             *
             * 所以只有 simulate == false 才播放
             * 原版營火熄滅效果。
             */
            if (!simulate) {

                CampfireBlock.dowse(
                        context.getPlayer(),
                        context.getLevel(),
                        context.getClickedPos(),
                        state
                );
            }

            return state.setValue(
                    LIT,
                    false
            );
        }


        /*
         * 其他工具行為交還父類。
         */
        return super.getToolModifiedState(
                state,
                context,
                itemAbility,
                simulate
        );
    }
    /*
     * ============================================================
     * 右鍵互動
     * ============================================================
     *
     * 1. 鏟子 -> 讓自己的 BE 先執行 dowse()
     * 2. 可熔煉物 -> 放到營火
     * 3. 其他物品 -> PASS，讓打火石等繼續自己的 Item.useOn()
     */

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {

        /*
         * ========================================================
         * 鏟子熄滅
         * ========================================================
         *
         * NeoForge 不應該直接判斷 instanceof ShovelItem，
         * 因為其他模組可能有自己的鏟子。
         *
         * SHOVEL_DOUSE 才是正確的能力判斷。
         */

        if (
                state.getValue(LIT)
                        &&
                        stack.canPerformAction(
                                ItemAbilities.SHOVEL_DOUSE
                        )
        ) {

            if (!level.isClientSide) {

                BlockEntity blockEntity =
                        level.getBlockEntity(pos);

                if (
                        blockEntity
                                instanceof
                                EndCampfireBlockEntity endCampfire
                ) {

                    /*
                     * 原版 CampfireBlock.dowse()
                     * 只認 CampfireBlockEntity。
                     *
                     * 我們自己的 BE 必須自己處理。
                     */
                    endCampfire.dowse();
                }
            }

            /*
             * 接著交給 CampfireBlock，
             * 讓原版處理：
             *
             * LIT = false
             * 音效
             * 遊戲事件
             * 鏟子耐久
             */
            return super.useItemOn(
                    stack,
                    state,
                    level,
                    pos,
                    player,
                    hand,
                    hitResult
            );
        }

        /*
         * ========================================================
         * 放置需要熔煉的物品
         * ========================================================
         */

        BlockEntity rawBlockEntity =
                level.getBlockEntity(pos);

        if (
                rawBlockEntity
                        instanceof
                        EndCampfireBlockEntity blockEntity
        ) {

            Optional<RecipeHolder<SmeltingRecipe>>
                    recipeOptional =
                    blockEntity.getCookableRecipe(stack);

            if (recipeOptional.isPresent()) {

                if (!level.isClientSide) {

                    SmeltingRecipe recipe =
                            recipeOptional
                                    .get()
                                    .value();

                    blockEntity.placeFood(
                            player,
                            stack,
                            recipe.getCookingTime()
                    );
                }

                return ItemInteractionResult.sidedSuccess(
                        level.isClientSide
                );
            }
        }

        /*
         * 例如：
         *
         * Flint and Steel
         * Fire Charge
         *
         * 讓物品自己的 useOn() 繼續執行。
         */
        return ItemInteractionResult
                .PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /*
     * ============================================================
     * 水熄滅
     * ============================================================
     *
     * super.placeLiquid() 仍然負責：
     *
     * WATERLOGGED
     * LIT = false
     * 水 Tick
     * 音效
     *
     * 但原版 CampfireBlock.dowse()
     * 不認 EndCampfireBlockEntity，
     * 所以我們先自己通知 BE。
     */

    @Override
    public boolean placeLiquid(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            FluidState fluidState
    ) {

        if (
                state.getValue(LIT)
                        &&
                        fluidState.is(FluidTags.WATER)
        ) {

            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (
                    blockEntity
                            instanceof
                            EndCampfireBlockEntity endCampfire
            ) {

                endCampfire.dowse();
            }
        }

        return super.placeLiquid(
                level,
                pos,
                state,
                fluidState
        );
    }

    /*
     * ============================================================
     * 方塊被真正移除
     * ============================================================
     */

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving
    ) {

        /*
         * LIT true -> false
         *
         * WATERLOGGED false -> true
         *
         * 都還是同一個 Block，
         * 不可以當成真正拆除。
         */
        if (!state.is(newState.getBlock())) {

            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (
                    blockEntity
                            instanceof
                            EndCampfireBlockEntity endCampfire
            ) {

                endCampfire.dropAllItems();
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                isMoving
        );
    }
}
