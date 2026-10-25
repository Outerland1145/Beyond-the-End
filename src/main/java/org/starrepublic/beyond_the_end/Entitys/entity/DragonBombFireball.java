package org.starrepublic.beyond_the_end.Entitys.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.starrepublic.beyond_the_end.Register.BlockRegister;

public class DragonBombFireball extends LargeFireball {

    // 三倍 TNT 爆炸威力
    public static final float EXPLOSION_POWER = 12.0F;

    // 飛行初速：4 格/Tick
    public static final double FLIGHT_SPEED = 4.0D;

    // 地面燃燒半徑
    private static final int FIRE_RADIUS = 4;

    // 火球最長存在時間
    private static final int MAX_LIFETIME = 100;

    public DragonBombFireball(
            EntityType<? extends LargeFireball> type,
            Level level
    ) {
        super(type, level);
    }

    /**
     * 設定火球發射方向與速度。
     */
    public void launch(
            EnderDragon dragon,
            Vec3 origin,
            Vec3 direction
    ) {
        this.setOwner(dragon);
        this.setPos(origin.x, origin.y, origin.z);

        Vec3 velocity = direction.normalize().scale(FLIGHT_SPEED);
        this.setDeltaMovement(velocity);
        this.hasImpulse = true;
    }

    /**
     * 不讓原版的空氣阻力逐漸降低火球速度。
     */
    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected float getLiquidInertia() {
        return 1.0F;
    }

    /**
     * 火球不可直接撞擊終界龍本身或龍的身體部位。
     */
    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof EnderDragon
                || entity instanceof EnderDragonPart) {
            return false;
        }

        return super.canHitEntity(entity);
    }

    /**
     * 超時清除，避免未擊中目標的火球無限飛行。
     */
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.tickCount >= MAX_LIFETIME) {
            this.discard();
        }
    }

    /**
     * 無論撞擊方塊或實體，都只執行一次自訂爆炸。
     *
     * 不呼叫 LargeFireball.onHit()，
     * 避免原版火球額外產生第二次爆炸。
     */
    @Override
    protected void onHit(HitResult result) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.isRemoved()) {
            return;
        }

        Vec3 hitPosition = result.getLocation();

        // 爆炸不能傷害終界龍與龍的分段實體。
        ExplosionDamageCalculator calculator =
                new ExplosionDamageCalculator() {

                    @Override
                    public boolean shouldDamageEntity(
                            Explosion explosion,
                            Entity entity
                    ) {
                        return !(entity instanceof EnderDragon)
                                && !(entity instanceof EnderDragonPart);
                    }
                };

        /*
         * 產生三倍 TNT 威力的爆炸。
         *
         * ExplosionInteraction.NONE：
         * 不破壞方塊。
         *
         * fire = false：
         * 不使用原版爆炸附帶的火焰生成，
         * 改由下方的 igniteGround() 自行控制。
         */
        serverLevel.explode(
                this,
                null,
                calculator,
                hitPosition.x,
                hitPosition.y,
                hitPosition.z,
                EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.NONE
        );

        // 點燃爆炸區域的地面
        igniteGround(serverLevel, hitPosition);

        this.discard();
    }

    /**
     * 在爆炸附近搜尋可以燃燒的地表。
     *
     * 不會清除、替換或破壞任何固體方塊。
     * 只會在符合火焰生存條件的空氣位置生成火焰。
     */
    private void igniteGround(
            ServerLevel level,
            Vec3 center
    ) {
        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        BlockState endfireState = BlockRegister.END_FIRE.fireblock.get().defaultBlockState();
        BlockState fireState = Blocks.FIRE.defaultBlockState();

        for (int dx = -FIRE_RADIUS; dx <= FIRE_RADIUS; dx++) {
            for (int dz = -FIRE_RADIUS; dz <= FIRE_RADIUS; dz++) {

                if (dx * dx + dz * dz > FIRE_RADIUS * FIRE_RADIUS) {
                    continue;
                }

                // 約 40% 的候選位置產生火焰
                if (this.random.nextFloat() >= 0.4F) {
                    continue;
                }

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                // 從爆炸高度附近向下尋找地面
                for (int dy = 2; dy >= -14; dy--) {

                    int y = origin.getY() + dy;

                    cursor.set(x, y, z);

                    if (!level.isInWorldBounds(cursor)) {
                        continue;
                    }

                    if (!level.isLoaded(cursor)) {
                        break;
                    }

                    if (!level.getBlockState(cursor).isAir()) {
                        continue;
                    }

                    BlockPos below = cursor.below();

                    if (!level.getBlockState(below)
                            .isFaceSturdy(level, below, Direction.UP)) {
                        continue;
                    }

                    if (!endfireState.canSurvive(level, cursor)) {
                        continue;
                    }

                    level.setBlockAndUpdate(
                            cursor.immutable(),
                            endfireState
                    );

                    break;
                }
            }
        }
    }
}

