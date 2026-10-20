package org.starrepublic.beyond_the_end.Blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DragonEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.FireEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.SoulEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.ItemRegister;
import org.starrepublic.beyond_the_end.Register.Tags;
import org.starrepublic.beyond_the_end.recipes.essence_altar.DragonAltarLogic;
import org.starrepublic.beyond_the_end.recipes.essence_altar.FireAltarLogic;

public class EssenceAltarBlock extends BaseEntityBlock {

    public static final MapCodec<EssenceAltarBlock> CODEC =
            simpleCodec(EssenceAltarBlock::new);

    // 與附魔台相同
    private static final VoxelShape SHAPE =
            Block.box(0, 0, 0, 16, 12, 16);

    public EssenceAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    // ========================================================
    // Fire Essence Altar Mode Switching
    // ========================================================

    public static void onFireAltarModeRightClick(
            PlayerInteractEvent.RightClickBlock event
    ) {

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (!player.isShiftKeyDown() || player.isSpectator()) {
            return;
        }

        if (!level.getBlockState(pos).is(
                BlockRegister.Essence_Altar.fires.get()
        )) {
            return;
        }

        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof FireEssenceAltarBlockEntity fireAltar) {

            fireAltar.cyclePowderMode();

            String name = switch (fireAltar.getPowderMode()) {

                case "speed" -> "加速模式";

                case "efficiency" -> "效率模式";

                case "smelting" -> "熔煉加速模式";

                default -> "普通模式";
            };

            player.displayClientMessage(
                    Component.literal(
                            "炎之精華祭壇：" + name
                    ),
                    true
            );
        }

        event.setCancellationResult(
                InteractionResult.sidedSuccess(level.isClientSide)
        );

        event.setCanceled(true);
    }
    // ========================================================
    // 潛行右鍵：切換龍之精華祭壇模式
    // ========================================================

    public static void onDragonAltarModeRightClick(
            PlayerInteractEvent.RightClickBlock event
    ) {

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (!player.isShiftKeyDown() || player.isSpectator()) {
            return;
        }

        if (!level.getBlockState(pos).is(
                BlockRegister.Essence_Altar.dragon.get()
        )) {
            return;
        }

        if (!level.isClientSide
                && level.getBlockEntity(pos)
                instanceof DragonEssenceAltarBlockEntity altar) {

            altar.cyclePowderMode();

            String modeName;

            if (altar.getPowderMode().equals("speed")) {
                modeName = "加速模式";

            } else if (altar.getPowderMode().equals("efficiency")) {
                modeName = "效率模式";

            } else {
                modeName = "普通模式";
            }

            player.displayClientMessage(
                    Component.literal("龍之精華祭壇：" + modeName),
                    true
            );
        }

        event.setCancellationResult(
                InteractionResult.sidedSuccess(level.isClientSide)
        );

        event.setCanceled(true);
    }
    // ========================================================
    // Codec
    // ========================================================

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    // ========================================================
    // Shape
    // ========================================================

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ========================================================
    // BlockEntity 建立
    // ========================================================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        if (state.is(BlockRegister.Essence_Altar.dragon.get())) {
            return new DragonEssenceAltarBlockEntity(pos, state);
        }

        if (state.is(BlockRegister.Essence_Altar.soul.get())) {
            return new SoulEssenceAltarBlockEntity(pos, state);
        }

        if (state.is(BlockRegister.Essence_Altar.fires.get())) {
            return new FireEssenceAltarBlockEntity(pos, state);
        }

        return null;
    }

    // ========================================================
    // Ticker
    // ========================================================

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {

        if (level.isClientSide) {
            return null;
        }

        // 目前只有龍之精華祭壇需要 Tick
        if (state.is(BlockRegister.Essence_Altar.dragon.get())) {

            return createTickerHelper(
                    type,
                    BlockEntityRegister.DRAGON_ESSENCE_ALTAR.get(),
                    DragonEssenceAltarBlockEntity::serverTick
            );
        }
        // ========================================================
        // Fire Essence Altar Ticker
        // ========================================================

        if (state.is(BlockRegister.Essence_Altar.fires.get())) {

            return createTickerHelper(
                    type,
                    BlockEntityRegister.FIRE_ESSENCE_ALTAR.get(),
                    FireEssenceAltarBlockEntity::serverTick
            );
        }

        return null;
    }

    // ========================================================
    // 玩家右鍵互動
    // ========================================================

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

        // ========================================================
// Fire Essence Altar Interaction
// ========================================================

        if (level.getBlockEntity(pos)
                instanceof FireEssenceAltarBlockEntity fireAltar) {

            // ----------------------------------------------------
            // 投入炎之精華
            // ----------------------------------------------------

            if (stack.is(Tags.FIRE_ESSENCE)) {

                if (!level.isClientSide) {

                    int amount = Math.min(32, stack.getCount());

                    if (player.getAbilities().instabuild) {
                        amount = 32;
                    }

                    int accepted = fireAltar.addEssence(
                            stack,
                            amount
                    );

                    if (accepted > 0
                            && !player.getAbilities().instabuild) {

                        stack.shrink(accepted);
                    }
                }

                return ItemInteractionResult.sidedSuccess(
                        level.isClientSide
                );
            }

            // ----------------------------------------------------
            // 投入末影粉
            // ----------------------------------------------------

            if (stack.is(Tags.END_POWDER)) {

                if (!level.isClientSide) {

                    if (fireAltar.addPowderMinute()) {

                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }

                        player.displayClientMessage(
                                Component.literal(
                                        "炎之精華祭壇：剩餘 "
                                                + ((fireAltar.getPowderTicks() + 19) / 20)
                                                + " 秒"
                                ),
                                true
                        );

                    } else {

                        player.displayClientMessage(
                                Component.literal(
                                        "目前模式不能使用末影粉"
                                ),
                                true
                        );
                    }
                }

                return ItemInteractionResult.sidedSuccess(
                        level.isClientSide
                );
            }

            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos)
                instanceof DragonEssenceAltarBlockEntity altar)) {

            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // ========================================================
        // 末影粉：每個增加一分鐘模式效果
        // ========================================================

        if (stack.is(Tags.END_POWDER)) {

            if (!level.isClientSide) {

                if (altar.addPowderMinute()) {

                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    player.displayClientMessage(
                            Component.literal(
                                    "模式效果剩餘：" +
                                            ((altar.getPowderTicks() + 19) / 20) +
                                            " 秒"
                            ),
                            true
                    );

                } else {

                    player.displayClientMessage(
                            Component.literal("請先切換至加速或效率模式"),
                            true
                    );
                }
            }

            return ItemInteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }
        // ====================================================
        // 注入龍之精華
        // ====================================================

        if (stack.is(Tags.DRAGON_ESSENCE)) {

            if (!level.isClientSide) {

                int amount = player.isShiftKeyDown()
                        ? 1
                        : Math.min(32, stack.getCount());

                if (player.getAbilities().instabuild) {
                    amount = player.isShiftKeyDown() ? 1 : 32;
                }

                int accepted = altar.addEssence(stack, amount);

                if (accepted > 0
                        && !player.getAbilities().instabuild) {

                    stack.shrink(accepted);
                }
            }

            return ItemInteractionResult.sidedSuccess(
                    level.isClientSide
            );
        }

        // ====================================================
        // 龍祭壇升級
        // ====================================================

        //boolean speedItem = stack.is(Items.REDSTONE_BLOCK);
        //boolean rangeItem = stack.is(Items.LAPIS_BLOCK);
        //boolean efficiencyItem = stack.is(Items.EMERALD_BLOCK);

        //if (speedItem || rangeItem || efficiencyItem) {

            //if (!level.isClientSide) {

                //boolean alreadyUpgraded =
                        //(speedItem && altar.isDragonSpeedUpgraded())
                                //|| (rangeItem && altar.isDragonRangeUpgraded())
                                //|| (efficiencyItem
                                //&& altar.isDragonEfficiencyUpgraded());

                //if (!alreadyUpgraded) {

                    //boolean creative =
                            //player.getAbilities().instabuild;

                    //if (creative || countEndPowder(player) >= 4) {

                        //boolean applied = false;

                        //if (speedItem) {
                            //applied = altar.upgradeDragonSpeed();
                        //}
                        //else if (rangeItem) {
                            //applied = altar.upgradeDragonRange();
                        //}
                        //else if (efficiencyItem) {
                            //applied = altar.upgradeDragonEfficiency();
                        //}

                        //if (applied && !creative) {

                            //consumeEndPowder(player, 4);

                            //stack.shrink(1);
                        //}
                    //}
                //}
            //}

            //return ItemInteractionResult.sidedSuccess(
                    //level.isClientSide
            //);
        //}

        return ItemInteractionResult
                .PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ========================================================
    // 末影粉末計算
    // ========================================================

    private static int countEndPowder(Player player) {

        int amount = 0;

        for (ItemStack stack : player.getInventory().items) {

            if (stack.is(Tags.END_POWDER)) {
                amount += stack.getCount();
            }
        }

        return amount;
    }

    private static void consumeEndPowder(
            Player player,
            int amount
    ) {

        for (ItemStack stack : player.getInventory().items) {

            if (amount <= 0) {
                break;
            }

            if (!stack.is(Tags.END_POWDER)) {
                continue;
            }

            int consumed = Math.min(
                    amount,
                    stack.getCount()
            );

            stack.shrink(consumed);

            amount -= consumed;
        }
    }

    // ========================================================
    // 移除祭壇
    // ========================================================

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {

        // 只有真正替換成另一種方塊時才返還
        if (state.getBlock() != newState.getBlock()) {

            if (level instanceof ServerLevel serverLevel
                    && level.getBlockEntity(pos)
                    instanceof EssenceAltarBlockEntity altar) {

                // 龍祭壇先解除與基座的連接
                if (altar instanceof DragonEssenceAltarBlockEntity dragon) {

                    DragonAltarLogic.releaseAll(
                            serverLevel,
                            dragon
                    );
                }
                if (altar instanceof FireEssenceAltarBlockEntity fireAltar) {

                    FireAltarLogic.releaseAll(
                            serverLevel,
                            fireAltar
                    );
                }

                // 三種精華祭壇共用的返還功能
                altar.dropAllEssence(serverLevel);
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }
}

