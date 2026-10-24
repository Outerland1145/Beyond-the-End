package org.starrepublic.beyond_the_end.Entitys.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.GUI.EnhancedBrewingMenu;
import org.starrepublic.beyond_the_end.Register.BlockEntityRegister;
import org.starrepublic.beyond_the_end.recipes.enhancedbrewing.EnhancedBrewingRecipes;

import java.util.ArrayList;
import java.util.List;

public class EnhancedBrewingStandBlockEntity
        extends BaseContainerBlockEntity
        implements WorldlyContainer {

    public static final int SLOT_COUNT = 5;
    public static final int INGREDIENT_SLOT = 3;
    public static final int FUEL_SLOT = 4;

    // 普通配方的速度為原版兩倍。
    public static final int NORMAL_BREW_TIME = 200;

    // 強化釀造台限定配方。
    public static final int EXCLUSIVE_BREW_TIME = 400;

    public static final int FUEL_USES = 20;

    private static final ResourceLocation END_POWDER =
            ResourceLocation.fromNamespaceAndPath(
                    Beyond_the_end.MODID,
                    "end_powder"
            );

    private static final int[] TOP_SLOTS = {3};
    private static final int[] BOTTOM_SLOTS = {0, 1, 2, 3};
    private static final int[] SIDE_SLOTS = {0, 1, 2, 4};

    private NonNullList<ItemStack> items =
            NonNullList.withSize(
                    SLOT_COUNT,
                    ItemStack.EMPTY
            );

    private final NonNullList<ItemStack> startedBottles =
            NonNullList.withSize(
                    3,
                    ItemStack.EMPTY
            );

    private ItemStack startedIngredient = ItemStack.EMPTY;

    private int brewTime;
    private int brewDuration;
    private int fuel;

    private final ContainerData data = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> brewTime;
                case 1 -> fuel;
                case 2 -> brewDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> brewTime = value;
                case 1 -> fuel = value;
                case 2 -> brewDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public EnhancedBrewingStandBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                BlockEntityRegister.ENCHANTEDBREWING.get(),
                pos,
                state
        );
    }

    public static boolean isFuel(ItemStack stack) {

        return !stack.isEmpty()
                && BuiltInRegistries.ITEM
                .getKey(stack.getItem())
                .equals(END_POWDER);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(
            NonNullList<ItemStack> items
    ) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatableWithFallback(
                "container.beyond_the_end.enhanced_brewing_stand",
                "Enhanced Brewing Stand"
        );
    }

    @Override
    protected AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory
    ) {
        return new EnhancedBrewingMenu(
                containerId,
                inventory,
                this,
                data
        );
    }

    @Override
    public boolean canPlaceItem(
            int index,
            ItemStack stack
    ) {

        if (stack.isEmpty()) {
            return true;
        }

        if (index == FUEL_SLOT) {
            return isFuel(stack);
        }

        if (level == null) {
            return false;
        }

        PotionBrewing brewing = level.potionBrewing();

        if (index == INGREDIENT_SLOT) {
            return brewing.isIngredient(stack)
                    || EnhancedBrewingRecipes.isIngredient(
                    level,
                    stack
            );
        }

        if (index >= 0 && index < 3) {
            return brewing.isInput(stack)
                    || EnhancedBrewingRecipes.isInput(
                    level,
                    stack
            );
        }

        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {

        if (direction == Direction.UP) {
            return TOP_SLOTS;
        }

        if (direction == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }

        return SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int index,
            ItemStack stack,
            @Nullable Direction direction
    ) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(
            int index,
            ItemStack stack,
            Direction direction
    ) {
        return index != INGREDIENT_SLOT
                || stack.is(Items.GLASS_BOTTLE);
    }

    private record BottlePlan(
            ItemStack output,
            boolean normal
    ) {}

    private record BrewingPlan(
            List<ItemStack> outputs,
            int duration,
            boolean valid
    ) {}

    /**
     * 查詢一個藥水瓶的釀造結果。
     *
     * 優先使用強化釀造台限定配方。
     * 若不存在，則使用原版 PotionBrewing。
     *
     * PotionBrewing 包含：
     * 原版配方、其他模組註冊的普通配方、
     * 本模組 allow_vanilla_stand=true 的配方。
     */
    private static BottlePlan findBottleRecipe(
            Level level,
            ItemStack bottle,
            ItemStack ingredient
    ) {

        if (bottle.isEmpty() || ingredient.isEmpty()) {
            return new BottlePlan(
                    ItemStack.EMPTY,
                    false
            );
        }

        var exclusive =
                EnhancedBrewingRecipes.findExclusive(
                        level,
                        bottle,
                        ingredient
                );

        if (exclusive.isPresent()) {

            ItemStack result = exclusive.get()
                    .value()
                    .output()
                    .copy();

            return new BottlePlan(
                    result,
                    false
            );
        }

        PotionBrewing brewing = level.potionBrewing();

        /*
         * 注意兩個方法的參數順序。
         *
         * hasMix(input, ingredient)
         * mix(ingredient, input)
         */
        if (brewing.hasMix(bottle, ingredient)) {

            ItemStack result =
                    brewing.mix(ingredient, bottle);

            if (!result.isEmpty()
                    && !ItemStack.isSameItemSameComponents(
                    bottle,
                    result
            )) {

                return new BottlePlan(
                        result,
                        true
                );
            }
        }

        return new BottlePlan(
                ItemStack.EMPTY,
                false
        );
    }

    /**
     * 計算本輪三瓶藥水的釀造計畫。
     *
     * 有任一瓶使用限定配方：
     * 400 ticks。
     *
     * 全部都是普通配方：
     * 200 ticks。
     */
    private BrewingPlan calculatePlan(Level level) {

        ItemStack reagent =
                items.get(INGREDIENT_SLOT);

        List<ItemStack> outputs =
                new ArrayList<>(3);

        boolean any = false;
        boolean allNormal = true;

        for (int i = 0; i < 3; i++) {

            BottlePlan bottle = findBottleRecipe(
                    level,
                    items.get(i),
                    reagent
            );

            outputs.add(bottle.output());

            if (!bottle.output().isEmpty()) {

                any = true;

                if (!bottle.normal()) {
                    allNormal = false;
                }
            }
        }

        return new BrewingPlan(
                List.copyOf(outputs),
                allNormal
                        ? NORMAL_BREW_TIME
                        : EXCLUSIVE_BREW_TIME,
                any
        );
    }

    /**
     * 確認釀造開始後，
     * 材料與三個瓶子沒有被更換。
     */
    private boolean inputsUnchanged() {

        if (!ItemStack.isSameItemSameComponents(
                startedIngredient,
                items.get(INGREDIENT_SLOT)
        )) {
            return false;
        }

        for (int i = 0; i < 3; i++) {

            if (!ItemStack.isSameItemSameComponents(
                    startedBottles.get(i),
                    items.get(i)
            )) {
                return false;
            }
        }

        return true;
    }

    private void rememberInputs() {

        startedIngredient =
                items.get(INGREDIENT_SLOT).copyWithCount(1);

        for (int i = 0; i < 3; i++) {

            startedBottles.set(
                    i,
                    items.get(i).copy()
            );
        }
    }

    private void clearBrewing() {

        brewTime = 0;
        brewDuration = 0;

        startedIngredient = ItemStack.EMPTY;

        for (int i = 0; i < 3; i++) {
            startedBottles.set(i, ItemStack.EMPTY);
        }
    }

    /**
     * 伺服器 Tick。
     */
    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            EnhancedBrewingStandBlockEntity brewing
    ) {

        boolean changed = false;

        BrewingPlan plan =
                brewing.calculatePlan(level);

        if (brewing.brewTime > 0) {

            boolean valid =
                    plan.valid()
                            && brewing.inputsUnchanged()
                            && plan.duration()
                            == brewing.brewDuration;

            if (!valid) {

                brewing.clearBrewing();
                changed = true;

            } else {

                brewing.brewTime--;
                changed = true;

                if (brewing.brewTime == 0) {

                    brewing.finishBrewing(
                            level,
                            pos,
                            plan
                    );

                    brewing.clearBrewing();
                }
            }

        } else if (plan.valid()) {

            /*
             * 只有存在有效釀造配方時，
             * 才嘗試補充燃料。
             */
            if (brewing.fuel <= 0) {

                ItemStack fuelStack =
                        brewing.items.get(FUEL_SLOT);

                if (isFuel(fuelStack)) {

                    fuelStack.shrink(1);

                    brewing.fuel = FUEL_USES;

                    changed = true;
                }
            }

            if (brewing.fuel > 0) {

                brewing.brewDuration =
                        plan.duration();

                brewing.brewTime =
                        plan.duration();

                brewing.rememberInputs();

                brewing.fuel--;

                changed = true;
            }
        }

        if (brewing.updateBottleStates(
                level,
                pos,
                state
        )) {
            changed = true;
        }

        if (changed) {
            brewing.setChanged();
        }
    }

    /**
     * 完成釀造。
     *
     * 一次處理三瓶，但只消耗一份材料。
     */
    private void finishBrewing(
            Level level,
            BlockPos pos,
            BrewingPlan plan
    ) {

        for (int i = 0; i < 3; i++) {

            ItemStack result =
                    plan.outputs().get(i);

            if (!result.isEmpty()) {
                items.set(
                        i,
                        result.copy()
                );
            }
        }

        ItemStack ingredient =
                items.get(INGREDIENT_SLOT);

        ItemStack remaining =
                ingredient.getCraftingRemainingItem();

        ingredient.shrink(1);

        if (!remaining.isEmpty()) {

            if (ingredient.isEmpty()) {

                items.set(
                        INGREDIENT_SLOT,
                        remaining.copy()
                );

            } else {

                Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        remaining.copy()
                );
            }
        }

        level.levelEvent(
                1035,
                pos,
                0
        );

        setChanged();
    }

    /**
     * 更新原版釀造台的三瓶方塊狀態。
     */
    private boolean updateBottleStates(
            Level level,
            BlockPos pos,
            BlockState state
    ) {

        BlockState updated = state;

        for (int i = 0; i < 3; i++) {

            updated = updated.setValue(
                    BrewingStandBlock.HAS_BOTTLE[i],
                    !items.get(i).isEmpty()
            );
        }

        if (updated == state) {
            return false;
        }

        level.setBlock(
                pos,
                updated,
                2
        );

        return true;
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        super.loadAdditional(
                tag,
                registries
        );

        items = NonNullList.withSize(
                SLOT_COUNT,
                ItemStack.EMPTY
        );

        ContainerHelper.loadAllItems(
                tag,
                items,
                registries
        );

        brewTime = tag.getInt("BrewTime");
        brewDuration = tag.getInt("BrewDuration");
        fuel = tag.getInt("Fuel");

        startedIngredient = ItemStack.parseOptional(
                registries,
                tag.getCompound("StartedIngredient")
        );

        for (int i = 0; i < 3; i++) {

            startedBottles.set(
                    i,
                    ItemStack.parseOptional(
                            registries,
                            tag.getCompound("StartedBottle" + i)
                    )
            );
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {

        super.saveAdditional(
                tag,
                registries
        );

        ContainerHelper.saveAllItems(
                tag,
                items,
                registries
        );

        tag.putInt("BrewTime", brewTime);
        tag.putInt("BrewDuration", brewDuration);
        tag.putInt("Fuel", fuel);

        if (!startedIngredient.isEmpty()) {

            tag.put(
                    "StartedIngredient",
                    startedIngredient.save(registries)
            );
        }

        for (int i = 0; i < 3; i++) {

            ItemStack bottle =
                    startedBottles.get(i);

            if (!bottle.isEmpty()) {

                tag.put(
                        "StartedBottle" + i,
                        bottle.save(registries)
                );
            }
        }
    }
}
