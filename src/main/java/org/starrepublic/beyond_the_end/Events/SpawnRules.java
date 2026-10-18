package org.starrepublic.beyond_the_end.Events;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.starrepublic.beyond_the_end.Register.EffectRegister;
import org.starrepublic.beyond_the_end.Register.EntityRegister;
import org.starrepublic.beyond_the_end.Register.Tags;

public class SpawnRules {
    private SpawnRules() {}
    public static void registerSpawnPlacements(
            RegisterSpawnPlacementsEvent event
    ) {
        event.register(
                EntityRegister.ANGRY_ENDERMAN.type().get(),

                (type, level, spawnType, pos, random) -> {

                    // 僅作用於原版終界維度
                    if (level.getLevel().dimension() != Level.END) {
                        return false;
                    }

                    // 只有指定群系允許忽略光照
                    if (!level.getBiome(pos).is(Tags.IGNORE_MONSTER_SPAWN_LIGHT)) {
                        return false;
                    }

                    // 保留怪物基本生成檢查，但忽略亮度
                    return Monster.checkAnyLightMonsterSpawnRules(
                            type,
                            level,
                            spawnType,
                            pos,
                            random
                    );
                },

                RegisterSpawnPlacementsEvent.Operation.OR
        );
        event.register(
                EntityRegister.SMALL_ENDERMAN.type().get(),

                (type, level, spawnType, pos, random) -> {

                    // 僅作用於原版終界維度
                    if (level.getLevel().dimension() != Level.END) {
                        return false;
                    }

                    // 只有指定群系允許忽略光照
                    if (!level.getBiome(pos).is(Tags.IGNORE_MONSTER_SPAWN_LIGHT)) {
                        return false;
                    }

                    // 保留怪物基本生成檢查，但忽略亮度
                    return Monster.checkAnyLightMonsterSpawnRules(
                            type,
                            level,
                            spawnType,
                            pos,
                            random
                    );
                },

                RegisterSpawnPlacementsEvent.Operation.OR
        );
        // 原版終界使者
        event.register(
                EntityType.ENDERMAN,

                (type, level, spawnType, pos, random) -> {

                    // 僅作用於原版終界維度
                    if (level.getLevel().dimension() != Level.END) {
                        return false;
                    }

                    // 只有指定群系允許忽略光照
                    if (!level.getBiome(pos).is(Tags.IGNORE_MONSTER_SPAWN_LIGHT)) {
                        return false;
                    }

                    // 保留怪物基本生成檢查，但忽略亮度
                    return Monster.checkAnyLightMonsterSpawnRules(
                            type,
                            level,
                            spawnType,
                            pos,
                            random
                    );
                },

                RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}
