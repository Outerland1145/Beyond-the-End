package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;

import net.minecraft.core.particles.ParticleTypes;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import net.minecraft.world.Clearable;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.CampfireBlock;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;

import java.util.Arrays;
import java.util.Optional;

public class EndCampfireBlockEntity
        extends BlockEntity
        implements Clearable {

    /*
     * 與原版營火相同：
     * 一次最多四個物品。
     */
    public static final int SLOT_COUNT = 4;

    /*
     * 熄火時每 Tick 倒退 2 Tick。
     */
    private static final int COOL_DOWN_SPEED = 2;

    /*
     * 正在烹飪的四個物品。
     */
    private final NonNullList<ItemStack> items =
            NonNullList.withSize(
                    SLOT_COUNT,
                    ItemStack.EMPTY
            );

    /*
     * 已完成時間。
     */
    private final int[] cookingProgress =
            new int[SLOT_COUNT];

    /*
     * 所需總時間。
     */
    private final int[] cookingTime =
            new int[SLOT_COUNT];

    /*
     * ============================================================
     * 關鍵：
     *
     * 我們根本不使用 CAMPFIRE_COOKING。
     *
     * 直接使用：
     *
     * minecraft:smelting
     *
     * 所有正常註冊到 SMELTING 的模組配方
     * 都會自動支援。
     * ============================================================
     */

    private final RecipeManager.CachedCheck<
            SingleRecipeInput,
            SmeltingRecipe
            > smeltingRecipes =
            RecipeManager.createCheck(
                    RecipeType.SMELTING
            );

    public EndCampfireBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        super(
                BlockEntityRegister.END_CAMPFIRE.get(),
                pos,
                state
        );
    }

    /*
     * ============================================================
     * 查詢配方
     * ============================================================
     */

    public Optional<RecipeHolder<SmeltingRecipe>>
    getCookableRecipe(
            ItemStack stack
    ) {

        if (
                this.level == null
                        ||
                        stack.isEmpty()
        ) {
            return Optional.empty();
        }

        /*
         * 沒位置就不能放。
         */
        boolean hasEmptySlot = false;

        for (ItemStack item : this.items) {

            if (item.isEmpty()) {
                hasEmptySlot = true;
                break;
            }
        }

        if (!hasEmptySlot) {
            return Optional.empty();
        }

        return this.smeltingRecipes.getRecipeFor(
                new SingleRecipeInput(stack),
                this.level
        );
    }

    /*
     * ============================================================
     * 放一個物品
     * ============================================================
     */

    public boolean placeFood(
            @Nullable LivingEntity entity,
            ItemStack sourceStack,
            int cookTime
    ) {

        if (sourceStack.isEmpty()) {
            return false;
        }

        for (
                int slot = 0;
                slot < SLOT_COUNT;
                slot++
        ) {

            if (this.items.get(slot).isEmpty()) {

                ItemStack inserted =
                        sourceStack.consumeAndReturn(
                                1,
                                entity
                        );

                if (inserted.isEmpty()) {
                    return false;
                }

                this.items.set(
                        slot,
                        inserted
                );

                this.cookingProgress[slot] = 0;

                this.cookingTime[slot] =
                        Math.max(
                                1,
                                cookTime
                        );

                markUpdated();

                return true;
            }
        }

        return false;
    }

    /*
     * ============================================================
     * SERVER：點燃時烹飪
     * ============================================================
     */

    public static void cookTick(
            Level level,
            BlockPos pos,
            BlockState state,
            EndCampfireBlockEntity blockEntity
    ) {

        boolean changed = false;

        for (
                int slot = 0;
                slot < SLOT_COUNT;
                slot++
        ) {

            ItemStack stack =
                    blockEntity.items.get(slot);

            if (stack.isEmpty()) {
                continue;
            }

            /*
             * 正常推進一 Tick。
             */
            blockEntity.cookingProgress[slot]++;

            /*
             * 還沒完成。
             */
            if (
                    blockEntity.cookingProgress[slot]
                            <
                            blockEntity.cookingTime[slot]
            ) {
                continue;
            }

            /*
             * ====================================================
             * 完成時再次查 RecipeManager。
             *
             * 原因：
             * Datapack 有可能在烹飪途中 Reload。
             * ====================================================
             */

            SingleRecipeInput input =
                    new SingleRecipeInput(stack);

            Optional<RecipeHolder<SmeltingRecipe>>
                    recipeOptional =
                    blockEntity.smeltingRecipes
                            .getRecipeFor(
                                    input,
                                    level
                            );

            ItemStack output;

            if (recipeOptional.isPresent()) {

                SmeltingRecipe recipe =
                        recipeOptional
                                .get()
                                .value();

                output =
                        recipe.assemble(
                                input,
                                level.registryAccess()
                        );

            } else {

                /*
                 * 如果烤到一半配方被資料包刪掉，
                 * 不吞玩家物品。
                 */
                output = stack.copy();
            }

            if (!output.isEmpty()) {

                Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        output
                );
            }

            /*
             * 清空槽位。
             */
            blockEntity.items.set(
                    slot,
                    ItemStack.EMPTY
            );

            blockEntity.cookingProgress[slot] = 0;
            blockEntity.cookingTime[slot] = 0;

            changed = true;
        }

        if (changed) {

            blockEntity.markUpdated();

        } else {

            /*
             * cookingProgress 改變了，
             * 還是需要標記儲存。
             */
            blockEntity.setChanged();
        }
    }

    /*
     * ============================================================
     * SERVER：熄滅後進度逐漸倒退
     * ============================================================
     */

    public static void cooldownTick(
            Level level,
            BlockPos pos,
            BlockState state,
            EndCampfireBlockEntity blockEntity
    ) {

        boolean changed = false;

        for (
                int slot = 0;
                slot < SLOT_COUNT;
                slot++
        ) {

            int oldProgress =
                    blockEntity
                            .cookingProgress[slot];

            if (oldProgress <= 0) {
                continue;
            }

            int newProgress =
                    Mth.clamp(
                            oldProgress
                                    -
                                    COOL_DOWN_SPEED,
                            0,
                            blockEntity.cookingTime[slot]
                    );

            if (
                    newProgress
                            !=
                            oldProgress
            ) {

                blockEntity
                        .cookingProgress[slot] =
                        newProgress;

                changed = true;
            }
        }

        if (changed) {
            blockEntity.setChanged();
        }
    }

    /*
     * ============================================================
     * CLIENT：煙霧
     * ============================================================
     *
     * 這就是上一版漏掉的東西。
     */

    public static void particleTick(
            Level level,
            BlockPos pos,
            BlockState state,
            EndCampfireBlockEntity blockEntity
    ) {

        RandomSource random =
                level.random;

        /*
         * ========================================================
         * 主煙柱
         * ========================================================
         *
         * SIGNAL_FIRE = true：
         *   稻草捆上的高煙柱
         *
         * false：
         *   普通營火煙柱
         */

        if (random.nextFloat() < 0.11F) {

            int count =
                    random.nextInt(2)
                            +
                            2;

            for (
                    int i = 0;
                    i < count;
                    i++
            ) {

                CampfireBlock.makeParticles(
                        level,
                        pos,
                        state.getValue(
                                CampfireBlock.SIGNAL_FIRE
                        ),
                        false
                );
            }
        }

        /*
         * ========================================================
         * 正在烤的物品產生小煙
         * ========================================================
         */

        int facing =
                state.getValue(
                        CampfireBlock.FACING
                ).get2DDataValue();

        for (
                int slot = 0;
                slot < SLOT_COUNT;
                slot++
        ) {

            ItemStack item =
                    blockEntity.items.get(slot);

            if (
                    item.isEmpty()
                            ||
                            random.nextFloat() >= 0.20F
            ) {
                continue;
            }

            /*
             * 根據營火方向，
             * 找到這個 Slot 在營火上的位置。
             */
            Direction direction =
                    Direction.from2DDataValue(
                            Math.floorMod(
                                    slot + facing,
                                    4
                            )
                    );

            Direction clockwise =
                    direction.getClockWise();

            double offset =
                    0.3125D;

            double x =
                    pos.getX()
                            +
                            0.5D
                            -
                            direction.getStepX()
                                    *
                                    offset
                            +
                            clockwise.getStepX()
                                    *
                                    offset;

            double y =
                    pos.getY()
                            +
                            0.5D;

            double z =
                    pos.getZ()
                            +
                            0.5D
                            -
                            direction.getStepZ()
                                    *
                                    offset
                            +
                            clockwise.getStepZ()
                                    *
                                    offset;

            /*
             * 和原版營火一樣，
             * 食物附近額外冒小煙。
             */
            for (
                    int i = 0;
                    i < 4;
                    i++
            ) {

                level.addParticle(
                        ParticleTypes.SMOKE,
                        x,
                        y,
                        z,
                        0.0D,
                        5.0E-4D,
                        0.0D
                );
            }
        }
    }

    /*
     * ============================================================
     * 熄滅
     * ============================================================
     *
     * 原版 CampfireBlockEntity 的 dowse()
     * 實際概念就是把正在烤的物品吐出。
     */

    public void dowse() {

        if (
                this.level == null
                        ||
                        this.level.isClientSide
        ) {
            return;
        }

        Containers.dropContents(
                this.level,
                this.worldPosition,
                this.items
        );

        clearContent();
    }

    /*
     * ============================================================
     * 方塊被破壞
     * ============================================================
     */

    public void dropAllItems() {

        if (
                this.level == null
                        ||
                        this.level.isClientSide
        ) {
            return;
        }

        Containers.dropContents(
                this.level,
                this.worldPosition,
                this.items
        );

        clearContent();
    }

    /*
     * ============================================================
     * Renderer 使用
     * ============================================================
     */

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    /*
     * ============================================================
     * Clearable
     * ============================================================
     */

    @Override
    public void clearContent() {

        this.items.clear();

        Arrays.fill(
                this.cookingProgress,
                0
        );

        Arrays.fill(
                this.cookingTime,
                0
        );

        markUpdated();
    }

    /*
     * ============================================================
     * NBT SAVE
     * ============================================================
     */

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        super.saveAdditional(
                tag,
                registries
        );

        /*
         * Minecraft 自己的 Inventory NBT 格式。
         */
        ContainerHelper.saveAllItems(
                tag,
                this.items,
                registries
        );

        tag.putIntArray(
                "CookingProgress",
                this.cookingProgress
        );

        tag.putIntArray(
                "CookingTime",
                this.cookingTime
        );
    }

    /*
     * ============================================================
     * NBT LOAD
     * ============================================================
     */

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        super.loadAdditional(
                tag,
                registries
        );

        /*
         * 避免重複載入舊資料。
         */
        this.items.clear();

        ContainerHelper.loadAllItems(
                tag,
                this.items,
                registries
        );

        Arrays.fill(
                this.cookingProgress,
                0
        );

        Arrays.fill(
                this.cookingTime,
                0
        );

        int[] progress =
                tag.getIntArray(
                        "CookingProgress"
                );

        int[] totalTime =
                tag.getIntArray(
                        "CookingTime"
                );

        System.arraycopy(
                progress,
                0,
                this.cookingProgress,
                0,
                Math.min(
                        progress.length,
                        SLOT_COUNT
                )
        );

        System.arraycopy(
                totalTime,
                0,
                this.cookingTime,
                0,
                Math.min(
                        totalTime.length,
                        SLOT_COUNT
                )
        );
    }

    /*
     * ============================================================
     * Server -> Client Sync
     * ============================================================
     */

    @Override
    public ClientboundBlockEntityDataPacket
    getUpdatePacket() {

        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {

        return this.saveWithoutMetadata(
                registries
        );
    }

    /*
     * ============================================================
     * 統一更新
     * ============================================================
     */

    private void markUpdated() {

        this.setChanged();

        if (this.level == null) {
            return;
        }

        BlockState state =
                this.getBlockState();

        this.level.sendBlockUpdated(
                this.worldPosition,
                state,
                state,
                3
        );
    }
}
