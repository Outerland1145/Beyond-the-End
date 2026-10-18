package org.starrepublic.beyond_the_end.Entitys.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.starrepublic.beyond_the_end.Register.helper.EntityHelper;
import org.starrepublic.beyond_the_end.Beyond_the_end;

import java.util.EnumSet;
import java.util.List;

/**
 * BTE 小安德：
 * - 中立、和平模式存在、不受玩家注視激怒、不主動攻擊、無瞬移
 * - 玩家攻擊時，玩家周圍 50 格成年 Enderman 會攻擊該玩家
 * - 偷取玩家背包內 1 個物品、拾取地面物品，或搬走 Enderman 可搬運方塊
 * - 不掉原版珍珠、裝備或經驗；只透過 dynamic loot table 返還持有物
 *
 * AI / 屬性 / 貨物 NBT 都在這裡；無須修改 EntityHelper / EntityRegister。
 */
public class SmallEndermanEntity extends EnderMan {

    /** 物品貨物的客戶端同步資料：只在本實體類定義，不能借用 EnderMan 的 ID。 */
    private static final EntityDataAccessor<ItemStack> DATA_STOLEN_ITEM =
            SynchedEntityData.defineId(SmallEndermanEntity.class, EntityDataSerializers.ITEM_STACK);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STOLEN_ITEM, ItemStack.EMPTY);
    }

    /** 僅給客戶端渲染使用；普通搬運方塊仍使用 EnderMan 原版同步資料。 */
    public ItemStack getDisplayHeldItem() {
        if (this.getCarriedBlock() != null) {
            return ItemStack.EMPTY;
        }
        ItemStack loot = this.entityData.get(DATA_STOLEN_ITEM);
        if (!loot.isEmpty()) {
            return loot;
        }
        // 也支援指令或其他模組給的手部裝備，不讓它們變成隱形。
        if (!this.getMainHandItem().isEmpty()) {
            return this.getMainHandItem();
        }
        return this.getOffhandItem();
    }

    /** 貨物修改的唯一入口：伺服器修改真實貨物，同時同步可見 ItemStack。 */
    private void setStolenItem(ItemStack stack) {
        this.stolenItem = stack.copy();
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_STOLEN_ITEM, this.stolenItem.copy());
        }
    }

    public static final double SCALE = 0.6D;
    public static final double ALERT_RADIUS = 50.0D;
    private static final double ALERT_RADIUS_SQR = ALERT_RADIUS * ALERT_RADIUS;
    private static final int PEARL_MIN_TICKS = 20 * 30 + 1;
    private static final int PEARL_MAX_TICKS = 20 * 60 + 1;
    private static final String PEARL_TIMER_NBT_KEY = "BTEPearlHoldTicks";
    private static final double THEFT_SEARCH_RADIUS = 6.0D;
    private static final double THEFT_REACH_SQR = 1.8D * 1.8D;
    private static final int ESCAPE_TICKS = 100;
    private static final String STOLEN_NBT_KEY = "BTEStolenItem";

    /** 與 data/beyond_the_end/loot_table/entities/small_enderman.json 的 dynamic.name 一致。 */
    private static final ResourceLocation CARRIED_LOOT_ID = ResourceLocation.fromNamespaceAndPath(
            Beyond_the_end.MODID, "small_enderman_carried"
    );

    public static final EntityHelper.MobSpec<SmallEndermanEntity> SPEC =
            new EntityHelper.MobSpec<>(
                    SmallEndermanEntity::new,
                    MobCategory.CREATURE,
                    0.6F,
                    2.9F,
                    SmallEndermanEntity::createAttributes,
                    SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mob::checkMobSpawnRules,
                    0x17121F,
                    0xAF65E5
            );

    /** 非裝備欄：保留原始 ItemStack 及全部 data components。 */
    private ItemStack stolenItem = ItemStack.EMPTY;
    private int escapeTicks = 0;
    private int pearlHoldTicks = 0;
    private Vec3 escapeFrom = Vec3.ZERO;

    public SmallEndermanEntity(EntityType<? extends SmallEndermanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        // 不使用 Mob 預設的裝備拾取機制，否則會繞過自訂貨物欄並可能重複掉落。
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EnderMan.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.SCALE, SCALE);
    }

    @Override
    protected void registerGoals() {
        // 不呼叫 super.registerGoals()：移除原版注視仇恨、攻擊、拾放方塊 AI。
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new EscapeWithLootGoal());
        this.goalSelector.addGoal(3, new StealFromPlayerGoal());
        this.goalSelector.addGoal(4, new TakeGroundItemGoal());
        this.goalSelector.addGoal(5, new TakeBlockGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // 完全不添加 targetSelector Goal。
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(null);
    }

    @Override
    protected boolean teleport() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** 持有玩家物品時禁止一般距離型自然消失，以免造成物品遺失。 */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.hasCargo() && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    /** 允許進入戰利品表流程，但不會產生任何原版安德掉落。 */
    @Override
    protected boolean shouldDropLoot() {
        return true;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    private static boolean canStealItem(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(Items.ENDER_PEARL);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {

        ItemStack pearl = player.getItemInHand(hand);
        // 不是終界珍珠，維持正常互動
        if (!pearl.is(Items.ENDER_PEARL)) {
            return super.mobInteract(player, hand);
        }

        // 客戶端立即攔截，禁止繼續使用終界珍珠
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 伺服器：交易期間或沒有貨物時，
        // 一樣攔截珍珠，但不消耗它
        if (this.pearlHoldTicks > 0 || !this.hasCargo()) {
            return InteractionResult.CONSUME;
        }
        // 只能使用終界珍珠，並且小安德必須有貨物
        if (!pearl.is(Items.ENDER_PEARL) || !this.hasCargo()) {
            return super.mobInteract(player, hand);
        }

        // 客戶端只處理互動回傳
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // 已收下珍珠時，暫時拒絕再次交易
        if (this.pearlHoldTicks > 0) {
            return InteractionResult.PASS;
        }
        ItemStack returned;

        // 優先返還搬運中的方塊
        if (this.getCarriedBlock() != null) {
            returned = new ItemStack(this.getCarriedBlock().getBlock());
        }
        // 其次返還偷來的物品
        else if (!this.stolenItem.isEmpty()) {
            returned = this.stolenItem.copy();
        }
        // 最後處理其他方式給予的手持物品
        else if (!this.getMainHandItem().isEmpty()) {
            returned = this.getMainHandItem().copy();
        }
        else {
            returned = this.getOffhandItem().copy();
        }

        if (returned.isEmpty()) {
            return InteractionResult.PASS;
        }

        // 在小安德面前產生返還的物品
        ItemEntity dropped = new ItemEntity(
                this.level(),
                this.getX(),
                this.getY() + 0.65D,
                this.getZ(),
                returned
        );

        // 向玩家方向吐出物品
        Vec3 direction = player.position().subtract(this.position());

        if (direction.lengthSqr() > 0.0001D) {
            Vec3 velocity = direction.normalize().scale(0.28D);

            dropped.setDeltaMovement(
                    velocity.x,
                    0.18D,
                    velocity.z
            );
        }

        // 生成失敗不能收取珍珠或清空貨物
        if (!this.level().addFreshEntity(dropped)) {
            return InteractionResult.FAIL;
        }

        // 清空剛剛交易的貨物
        if (this.getCarriedBlock() != null) {
            this.setCarriedBlock(null);
        }
        else if (!this.stolenItem.isEmpty()) {
            this.setStolenItem(ItemStack.EMPTY);
        }
        else if (!this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        // 收下玩家支付的終界珍珠
        this.setStolenItem(new ItemStack(Items.ENDER_PEARL));

        // 隨機持有 30～60 秒
        this.pearlHoldTicks = PEARL_MIN_TICKS
                + this.random.nextInt(
                PEARL_MAX_TICKS - PEARL_MIN_TICKS + 1
        );

        // 消耗一顆終界珍珠，創造模式除外
        if (!player.getAbilities().instabuild) {
            pearl.shrink(1);
        }

        // 交易完成後逃離玩家
        this.beginEscape(player.position());

        return InteractionResult.CONSUME;
    }

    private boolean hasCargo() {
        return !stolenItem.isEmpty()
                || this.getCarriedBlock() != null
                || !this.getMainHandItem().isEmpty()
                || !this.getOffhandItem().isEmpty();
    }

    private boolean canSteal() {
        return !this.level().isClientSide()
                && !this.hasCargo()
                && this.escapeTicks == 0
                && this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }
    @Override
    public void tick() {
        super.tick();

        // 倒數只由伺服器負責
        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }

        if (this.pearlHoldTicks > 0) {

            --this.pearlHoldTicks;

            if (this.pearlHoldTicks == 0) {

                // 只移除交易取得的珍珠
                if (this.stolenItem.is(Items.ENDER_PEARL)) {
                    this.setStolenItem(ItemStack.EMPTY);
                }
            }
        }
    }
    private void beginEscape(Vec3 threat) {
        this.escapeFrom = threat;
        this.escapeTicks = ESCAPE_TICKS;
        this.getNavigation().stop();
    }

    private static boolean stealablePlayer(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    /** 玩家快捷欄 0..8、主背包 9..35；不偷玩家選取中的快捷欄格。 */
    private static boolean hasStealableSlot(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            if (i != inv.selected && canStealItem(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    /** 等機率選出一個符合條件的非空背包格，僅偷 1 個物品。 */
    private boolean stealOneFrom(Player player) {
        if (!canSteal() || !stealablePlayer(player)) {
            return false;
        }

        Inventory inv = player.getInventory();
        int chosen = -1;
        int eligible = 0;
        for (int i = 0; i < 36; ++i) {
            if (i == inv.selected || !canStealItem(inv.getItem(i))) {
                continue;
            }
            if (this.random.nextInt(++eligible) == 0) {
                chosen = i;
            }
        }

        if (chosen < 0) {
            return false;
        }
        ItemStack removed = inv.removeItem(chosen, 1);
        if (removed.isEmpty()) {
            return false;
        }
        this.setStolenItem(removed);
        inv.setChanged();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }
        this.beginEscape(player.position());
        return true;
    }

    private final class StealFromPlayerGoal extends Goal {
        private Player victim;

        private StealFromPlayerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!canSteal() || random.nextInt(30) != 0) {
                return false;
            }
            List<Player> players = level().getEntitiesOfClass(
                    Player.class,
                    getBoundingBox().inflate(THEFT_SEARCH_RADIUS),
                    p -> stealablePlayer(p) && hasStealableSlot(p)
            );
            Player closest = null;
            double bestDistance = Double.MAX_VALUE;
            for (Player player : players) {
                double distance = distanceToSqr(player);
                if (distance < bestDistance) {
                    closest = player;
                    bestDistance = distance;
                }
            }
            this.victim = closest;
            return closest != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canSteal() && victim != null && stealablePlayer(victim)
                    && hasStealableSlot(victim)
                    && distanceToSqr(victim) <= 100.0D;
        }

        @Override
        public void start() {
            if (victim != null) {
                getNavigation().moveTo(victim, 1.1D);
            }
        }

        @Override
        public void tick() {
            if (victim == null) {
                return;
            }
            getLookControl().setLookAt(victim, 30.0F, 30.0F);
            if (distanceToSqr(victim) <= THEFT_REACH_SQR) {
                stealOneFrom(victim);
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(victim, 1.1D);
            }
        }

        @Override
        public void stop() {
            victim = null;
            getNavigation().stop();
        }
    }

    private final class EscapeWithLootGoal extends Goal {
        private EscapeWithLootGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return escapeTicks > 0;
        }

        @Override
        public boolean canContinueToUse() {
            return escapeTicks > 0;
        }

        @Override
        public void start() {
            findEscapePath();
        }

        private void findEscapePath() {
            Vec3 pos = DefaultRandomPos.getPosAway(SmallEndermanEntity.this, 16, 7, escapeFrom);
            if (pos != null) {
                getNavigation().moveTo(pos.x, pos.y, pos.z, 1.3D);
            }
        }

        @Override
        public void tick() {
            if (--escapeTicks <= 0) {
                escapeTicks = 0;
                return;
            }
            if (getNavigation().isDone() && escapeTicks % 10 == 0) {
                findEscapePath();
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** 使用我們自己的 ItemStack 貨物欄，不交給原版 Mob 裝備拾取。 */
    private final class TakeGroundItemGoal extends Goal {
        private ItemEntity target;

        private TakeGroundItemGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!canSteal() || random.nextInt(30) != 0) {
                return false;
            }
            List<ItemEntity> items = level().getEntitiesOfClass(
                    ItemEntity.class,
                    getBoundingBox().inflate(6.0D),
                    item -> item.isAlive() && canStealItem(item.getItem())
            );
            target = null;
            double best = Double.MAX_VALUE;
            for (ItemEntity item : items) {
                double dist = distanceToSqr(item);
                if (dist < best) {
                    best = dist;
                    target = item;
                }
            }
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return canSteal() && target != null && target.isAlive()
                    && !canStealItem(target.getItem()) && distanceToSqr(target) < 100.0D;
        }

        @Override
        public void start() {
            if (target != null) {
                getNavigation().moveTo(target, 1.0D);
            }
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            if (distanceToSqr(target) < 1.5D * 1.5D
                    && canSteal()
                    && canStealItem(target.getItem())) {
                // 整組地面掉落物轉移到貨物欄，原 ItemEntity 被移除，不複製來源。
                setStolenItem(target.getItem());
                target.discard();
                getNavigation().stop();
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(target, 1.0D);
            }
        }

        @Override
        public void stop() {
            target = null;
            getNavigation().stop();
        }
    }

    /** 輕量版原版安德搬運：僅搬走 #minecraft:enderman_holdable 方塊。 */
    private final class TakeBlockGoal extends Goal {
        private BlockPos selected;

        private TakeBlockGoal() {
            this.setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            if (!canSteal() || random.nextInt(160) != 0) {
                return false;
            }
            BlockPos pos = blockPosition().offset(
                    random.nextInt(7) - 3,
                    random.nextInt(5) - 2,
                    random.nextInt(7) - 3
            );
            if (!level().isLoaded(pos)) {
                return false;
            }
            BlockState state = level().getBlockState(pos);
            if (!state.is(BlockTags.ENDERMAN_HOLDABLE)
                    || state.hasBlockEntity()
                    || !state.getFluidState().isEmpty()) {
                return false;
            }
            selected = pos;
            return true;
        }

        @Override
        public void start() {
            if (selected == null || !canSteal()) {
                return;
            }
            BlockState state = level().getBlockState(selected);
            if (state.is(BlockTags.ENDERMAN_HOLDABLE)
                    && !state.hasBlockEntity()
                    && state.getFluidState().isEmpty()
                    && level().removeBlock(selected, false)) {
                setCarriedBlock(state);
            }
        }

        @Override
        public void stop() {
            selected = null;
        }
    }

    // =========================================================
    // NBT: 跨存檔/重載保留偷走的物品與其附魔、自訂名稱等 components
    // =========================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); // 包含 Enderman 原版 carriedBlock
        if (!stolenItem.isEmpty()) {
            tag.put(STOLEN_NBT_KEY, stolenItem.save(this.level().registryAccess()));
        }
        if (this.pearlHoldTicks > 0) {
            tag.putInt(PEARL_TIMER_NBT_KEY, this.pearlHoldTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStolenItem(tag.contains(STOLEN_NBT_KEY, 10)
                ? ItemStack.parseOptional(this.level().registryAccess(), tag.getCompound(STOLEN_NBT_KEY))
                : ItemStack.EMPTY);
        this.pearlHoldTicks = Math.max(
                0,
                tag.getInt(PEARL_TIMER_NBT_KEY)
        );

        // 防止資料異常導致空手狀態仍然存在交易倒數
        if (!this.stolenItem.is(Items.ENDER_PEARL)) {
            this.pearlHoldTicks = 0;
        }
    }

    // =========================================================
    // Death loot: 故意不呼叫 super.dropAllDeathLoot()，避免珍珠/裝備/額外掉落
    // =========================================================

    @Override
    protected void dropAllDeathLoot(ServerLevel serverLevel, DamageSource source) {
        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer)
                .withDynamicDrop(CARRIED_LOOT_ID, output -> {
                    if (!stolenItem.isEmpty()) {
                        output.accept(stolenItem.copy());
                    }
                    // 其他模組或指令給小安德的主手、副手裝備也算「持有物」。
                    if (!getMainHandItem().isEmpty()) {
                        output.accept(getMainHandItem().copy());
                    }
                    if (!getOffhandItem().isEmpty()) {
                        output.accept(getOffhandItem().copy());
                    }
                    BlockState carried = getCarriedBlock();
                    if (carried != null && carried.getBlock().asItem() != Items.AIR) {
                        output.accept(new ItemStack(carried.getBlock()));
                    }
                })
                .create(LootContextParamSets.ENTITY);

        LootTable lootTable = serverLevel.getServer()
                .reloadableRegistries()
                .getLootTable(this.getLootTable());

        // Raw：不允許全域 Loot Modifier 額外插入終界珍珠等獎勵。
        // JSON 內唯一的 dynamic entry 將返還貨物原物。
        lootTable.getRandomItemsRaw(params, stack -> {
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
            }
        });

        setStolenItem(ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        setCarriedBlock(null);
    }

    // =========================================================
    // 原本的 50 格成年安德求援
    // =========================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide()
                && amount > 0.0F
                && source.getEntity() instanceof Player player
                && !player.isSpectator()
                && !player.isCreative()) {
            alertAdultEndermen(player);
        }
        return super.hurt(source, amount);
    }

    private void alertAdultEndermen(Player attacker) {
        AABB box = attacker.getBoundingBox().inflate(ALERT_RADIUS);
        for (EnderMan adult : this.level().getEntitiesOfClass(
                EnderMan.class,
                box,
                enderman -> enderman.isAlive()
                        && !(enderman instanceof SmallEndermanEntity)
                        && enderman.distanceToSqr(attacker) <= ALERT_RADIUS_SQR
        )) {
            adult.setPersistentAngerTarget(attacker.getUUID());
            adult.startPersistentAngerTimer();
            adult.setTarget(attacker);
        }
    }
}
