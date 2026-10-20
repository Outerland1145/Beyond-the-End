package org.starrepublic.beyond_the_end.recipes.essence_altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import org.starrepublic.beyond_the_end.Entitys.blockentity.AltarPedestalBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DragonEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.FireEssenceAltarBlockEntity;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FireAltarLogic {

    private static final int PEDESTAL_INTERVAL = 20;
    private static final int PROCESS_INTERVAL = 2;

    private FireAltarLogic() {
    }

    // ========================================================
    // Main Tick
    // ========================================================

    public static void tick(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar,
            int ticks
    ) {

        if (ticks == 1 || ticks % PEDESTAL_INTERVAL == 0) {
            refreshPedestals(level, altar);
        }

        if (ticks % PROCESS_INTERVAL != 0) {
            return;
        }

        List<AltarPedestalBlockEntity> inputs = new ArrayList<>();

        for (BlockPos pos : altar.getFirePedestals()) {

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal == null
                    || !pedestal.isInputMode()
                    || pedestal.isEmpty()) {
                continue;
            }

            if (!isAvailableToAltar(level, altar, pedestal)) {
                continue;
            }

            // 只有找到配方的輸入基座才需要輸出位置
            if (altar.findMatchingRecipe(pedestal.getItem()).isEmpty()) {
                continue;
            }

            inputs.add(pedestal);
        }

        // 沒有任何可執行配方，不顯示輸出失敗
        if (inputs.isEmpty()) {
            altar.setRecipeFailure("");
            return;
        }

        // ====================================================
        // 執行前檢查輸出基座
        // ====================================================

        String status = checkOutputStatus(level, altar);

        if (!status.isEmpty()) {

            altar.setRecipeFailure(status);

            // 不消耗精華，不清空原有注魔進度
            return;
        }

        altar.setRecipeFailure("");

        // ====================================================
        // 正式執行注魔
        // ====================================================

        for (AltarPedestalBlockEntity pedestal : inputs) {

            // 執行上一份配方後，空位可能已經被占用
            String currentStatus = checkOutputStatus(level, altar);

            if (!currentStatus.isEmpty()) {
                altar.setRecipeFailure(currentStatus);
                return;
            }

            processPedestal(level, altar, pedestal);
        }
    }

    // ========================================================
    // Pedestal
    // ========================================================
    private static String checkOutputStatus(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar
    ) {

        boolean hasOutput = false;
        boolean hasFreeOutput = false;

        for (BlockPos pos : altar.getFirePedestals()) {

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal == null || !pedestal.isOutputMode()) {
                continue;
            }

            hasOutput = true;

            if (pedestal.isEmpty()
                    && isAvailableToAltar(level, altar, pedestal)) {

                hasFreeOutput = true;
                break;
            }
        }

        if (!hasOutput) {
            return "no_output";
        }

        if (!hasFreeOutput) {
            return "output_full";
        }

        return "";
    }
    @Nullable
    private static AltarPedestalBlockEntity getPedestal(
            ServerLevel level,
            BlockPos pos
    ) {

        if (!level.hasChunkAt(pos)) {
            return null;
        }

        // 硬編碼：只接受專用基座
        if (!level.getBlockState(pos).is(
                BlockRegister.ALTAR_PEDESTAL.get()
        )) {
            return null;
        }

        BlockEntity entity = level.getBlockEntity(pos);

        return entity instanceof AltarPedestalBlockEntity pedestal
                ? pedestal
                : null;
    }

    private static void refreshPedestals(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        // 與目前龍祭壇相同，固定 7×7
        int range = 3;

        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                if (x == 0 && z == 0) {
                    continue;
                }

                BlockPos pos = center.offset(x, 0, z);

                if (getPedestal(level, pos) != null) {
                    candidates.add(pos.immutable());
                }
            }
        }

        candidates.sort(
                Comparator
                        .comparingDouble(
                                (BlockPos pos) ->
                                        pos.distSqr(center)
                        )
                        .thenComparingInt(BlockPos::getX)
                        .thenComparingInt(BlockPos::getZ)
        );

        // 釋放已經不在範圍內的基座
        for (BlockPos oldPos : altar.getFirePedestals()) {

            if (candidates.contains(oldPos)) {
                continue;
            }

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, oldPos);

            if (pedestal != null
                    && center.equals(pedestal.getOwnerAltar())) {

                refundAndRelease(altar, pedestal);
            }
        }

        altar.setFirePedestals(candidates);
    }

    // ========================================================
    // Ownership
    // ========================================================

    private static boolean isAvailableToAltar(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity pedestal
    ) {

        BlockPos owner = pedestal.getOwnerAltar();

        if (owner == null || owner.equals(altar.getBlockPos())) {
            return true;
        }

        if (!level.hasChunkAt(owner)) {
            return false;
        }

        BlockEntity ownerEntity = level.getBlockEntity(owner);

        if (ownerEntity instanceof EssenceAltarBlockEntity) {
            return false;
        }

        pedestal.release(owner);

        return true;
    }

    // ========================================================
    // Empty output pedestal
    // ========================================================

    @Nullable
    private static AltarPedestalBlockEntity findEmptyPedestal(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar,
            BlockPos inputPos
    ) {

        List<AltarPedestalBlockEntity> available =
                new ArrayList<>();

        for (BlockPos pos : altar.getFirePedestals()) {

            if (pos.equals(inputPos)) {
                continue;
            }

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal == null) {
                continue;
            }

            // 必須是輸出狀態
            if (!pedestal.isOutputMode()) {
                continue;
            }

            // 必須完全沒有物品
            if (!pedestal.isEmpty()) {
                continue;
            }

            if (!isAvailableToAltar(level, altar, pedestal)) {
                continue;
            }

            available.add(pedestal);
        }

        if (available.isEmpty()) {
            return null;
        }

        available.sort(
                Comparator
                        .comparingDouble(
                                (AltarPedestalBlockEntity pedestal) ->
                                        pedestal.getBlockPos().distSqr(inputPos)
                        )
                        .thenComparingInt(
                                pedestal -> pedestal.getBlockPos().getX()
                        )
                        .thenComparingInt(
                                pedestal -> pedestal.getBlockPos().getZ()
                        )
        );

        return available.get(0);
    }

    // ========================================================
    // Processing
    // ========================================================

    private static void processPedestal(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity inputPedestal
    ) {
        // 只有輸入模式的基座可以執行配方
        if (!inputPedestal.isInputMode()) {
            return;
        }
        BlockPos altarPos = altar.getBlockPos();

        if (!isAvailableToAltar(level, altar, inputPedestal)) {
            return;
        }

        ItemStack inputStack = inputPedestal.getItem();

        if (inputStack.isEmpty()) {
            refundAndRelease(altar, inputPedestal);
            return;
        }

        Optional<RecipeHolder<EssenceAltarRecipe>> found =
                altar.findMatchingRecipe(inputStack);

        if (found.isEmpty()) {

            if (altarPos.equals(inputPedestal.getOwnerAltar())) {
                refundAndRelease(altar, inputPedestal);
            }

            return;
        }

        AltarPedestalBlockEntity outputPedestal =
                findEmptyPedestal(
                        level,
                        altar,
                        inputPedestal.getBlockPos()
                );

        // 沒有空基座，不消耗精華也不增加進度
        if (outputPedestal == null) {
            return;
        }

        if (!inputPedestal.claim(altarPos)) {
            return;
        }

        RecipeHolder<EssenceAltarRecipe> holder = found.get();
        EssenceAltarRecipe recipe = holder.value();

        // ====================================================
        // Mode and cost
        // ====================================================

        String mode = altar.getEffectivePowderMode();

        if ("smelting".equals(mode)) {
            return;
        }

        int baseCost = recipe.getCost();

        int requiredCost = baseCost;
        int requiredTicks = baseCost * 20;

        if ("speed".equals(mode)) {

            requiredCost = baseCost * 2;
            requiredTicks = baseCost * 10;

        } else if ("efficiency".equals(mode)) {

            requiredCost = (baseCost + 1) / 2;
            requiredTicks = baseCost * 40;
        }

        // ====================================================
        // Recipe signature
        // ====================================================

        String signature =
                holder.id()
                        + "#"
                        + mode
                        + "#"
                        + altar.getPowderSession()
                        + "#"
                        + requiredCost
                        + "#"
                        + ItemStack.hashItemAndComponents(inputStack)
                        + "#"
                        + ItemStack.hashItemAndComponents(recipe.getResult())
                        + "#"
                        + recipe.getResult().getCount();

        // 工作模式、效果階段或配方發生改變
        if (!signature.equals(inputPedestal.getActiveRecipe())) {

            int refund = inputPedestal.getProgress();

            if (refund > 0) {
                altar.addEssence(refund);
            }

            inputPedestal.setActiveRecipe(signature);
        }

        // 給 Jade 的進度條使用
        inputPedestal.setRequiredTicks(requiredTicks);

        // ====================================================
        // Already completed but waiting for output
        // ====================================================

        if (inputPedestal.getProcessingTicks() >= requiredTicks) {

            finishRecipe(
                    level,
                    altar,
                    inputPedestal,
                    outputPedestal,
                    recipe
            );

            return;
        }

        // ====================================================
        // Essence check
        // ====================================================

        if (altar.getEssence() <= 0) {
            return;
        }

        spawnParticleStream(
                level,
                altarPos,
                inputPedestal.getBlockPos()
        );

        int currentTicks = inputPedestal.getProcessingTicks();

        int nextTicks = Math.min(
                requiredTicks,
                currentTicks + PROCESS_INTERVAL
        );

        // 按照當前工作比例分配精華成本
        int targetSpent = (int) (
                (long) requiredCost * nextTicks / requiredTicks
        );

        int toConsume =
                targetSpent - inputPedestal.getProgress();

        if (toConsume > 0) {

            if (!altar.consumeEssence(toConsume)) {
                return;
            }

            for (int i = 0; i < toConsume; i++) {
                inputPedestal.advanceProgress();
            }
        }

        inputPedestal.advanceProcessingTicks(
                nextTicks - currentTicks
        );

        // ====================================================
        // Completion
        // ====================================================

        if (inputPedestal.getProcessingTicks() >= requiredTicks) {

            AltarPedestalBlockEntity destination =
                    findEmptyPedestal(
                            level,
                            altar,
                            inputPedestal.getBlockPos()
                    );

            if (destination != null) {

                finishRecipe(
                        level,
                        altar,
                        inputPedestal,
                        destination,
                        recipe
                );
            }
        }
    }

    // ========================================================
    // Finish
    // ========================================================


    private static boolean finishRecipe(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity inputPedestal,
            AltarPedestalBlockEntity outputPedestal,
            EssenceAltarRecipe recipe
    ) {

        BlockPos altarPos = altar.getBlockPos();

        // ========================================================
        // 驗證輸入與輸出基座
        // ========================================================

        if (!inputPedestal.isInputMode()
                || !outputPedestal.isOutputMode()) {
            return false;
        }

        if (inputPedestal == outputPedestal) {
            return false;
        }

        if (inputPedestal.isEmpty() || !outputPedestal.isEmpty()) {
            return false;
        }

        if (!isAvailableToAltar(level, altar, outputPedestal)) {
            return false;
        }

        // ========================================================
        // 組裝炎之精華配方
        // ========================================================

        EssenceAltarRecipeInput recipeInput =
                new EssenceAltarRecipeInput(
                        "fire",
                        List.of(inputPedestal.getItem())
                );

        ItemStack result = recipe.assemble(
                recipeInput,
                level.registryAccess()
        );

        if (result.isEmpty()
                || result.getCount() > result.getMaxStackSize()) {
            return false;
        }

        // ========================================================
        // 使用基座專用的配方產物驗證
        // ========================================================

        if (!outputPedestal.canAcceptCraftedResult(result)) {
            return false;
        }

        // ========================================================
        // 鎖定輸出基座
        // ========================================================

        if (!outputPedestal.claim(altarPos)) {
            return false;
        }

        // ========================================================
        // 將配方產物放入輸出基座
        // ========================================================

        if (!outputPedestal.placeCraftedResult(result)) {

            outputPedestal.release(altarPos);
            return false;
        }

        // ========================================================
        // 成功輸出後才消耗輸入材料
        // ========================================================

        ItemStack consumed = inputPedestal.extract(1);

        if (consumed.isEmpty()) {

            // 回滾輸出，避免產生重複物品
            outputPedestal.extract(result.getCount());
            outputPedestal.release(altarPos);

            return false;
        }

        // ========================================================
        // 配方完成
        // ========================================================

        inputPedestal.release(altarPos);
        outputPedestal.release(altarPos);

        spawnCompletionParticles(
                level,
                outputPedestal.getBlockPos()
        );

        return true;
    }


    // ========================================================
    // Refund and reset
    // ========================================================

    private static void refundAndRelease(
            FireEssenceAltarBlockEntity altar,
            AltarPedestalBlockEntity pedestal
    ) {

        if (!altar.getBlockPos().equals(pedestal.getOwnerAltar())) {
            return;
        }

        int refund = pedestal.getProgress();

        if (refund > 0) {
            altar.addEssence(refund);
        }

        pedestal.release(altar.getBlockPos());
    }

    // 熔煉模式：終止進行中的祭壇配方並退回已消耗精華
    private static void cancelActiveInfusions(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar
    ) {

        for (BlockPos pos : altar.getFirePedestals()) {

            AltarPedestalBlockEntity pedestal =
                    getPedestal(level, pos);

            if (pedestal != null) {
                refundAndRelease(altar, pedestal);
            }
        }
    }

    // ========================================================
    // Particle stream
    // ========================================================

    private static void spawnParticleStream(
            ServerLevel level,
            BlockPos altar,
            BlockPos pedestal
    ) {

        Vec3 start = new Vec3(
                altar.getX() + 0.5D,
                altar.getY() + 1.25D,
                altar.getZ() + 0.5D
        );

        Vec3 end = new Vec3(
                pedestal.getX() + 0.5D,
                pedestal.getY() + 1.0D,
                pedestal.getZ() + 0.5D
        );

        double phase =
                (level.getGameTime() % 20L) / 20.0D;

        for (int i = 0; i < 4; i++) {

            double t = (phase + i * 0.25D) % 1.0D;

            Vec3 point = start.lerp(end, t);

            double arc = Math.sin(t * Math.PI) * 0.30D;

            level.sendParticles(
                    ParticleTypes.FLAME,
                    point.x,
                    point.y + arc,
                    point.z,
                    1,
                    0.015D,
                    0.015D,
                    0.015D,
                    0.0D
            );
        }
    }

    private static void spawnCompletionParticles(
            ServerLevel level,
            BlockPos pos
    ) {

        level.sendParticles(
                ParticleTypes.FLAME,
                pos.getX() + 0.5D,
                pos.getY() + 1.0D,
                pos.getZ() + 0.5D,
                30,
                0.25D,
                0.25D,
                0.25D,
                0.08D
        );
    }

    // ========================================================
    // Altar removal
    // ========================================================

    public static void releaseAll(
            ServerLevel level,
            FireEssenceAltarBlockEntity altar
    ) {

        BlockPos center = altar.getBlockPos();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {

                BlockPos pos = center.offset(x, 0, z);

                AltarPedestalBlockEntity pedestal =
                        getPedestal(level, pos);

                if (pedestal != null) {
                    pedestal.release(center);
                }
            }
        }

        altar.setFirePedestals(List.of());
    }
}

