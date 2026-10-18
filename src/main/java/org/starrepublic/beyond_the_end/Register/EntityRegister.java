package org.starrepublic.beyond_the_end.Register;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Entitys.client.renderer.SmallEndermanRenderer;
import org.starrepublic.beyond_the_end.Entitys.mob.AngryEndermanEntity;
import org.starrepublic.beyond_the_end.Entitys.mob.SmallEndermanEntity;
import org.starrepublic.beyond_the_end.Register.helper.EntityHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 唯一的實體註冊中心。
 *
 * 這個類唯一擁有：
 * 1. EntityType / Spawn Egg 的 DeferredRegister
 * 2. 生物的註冊清單
 * 3. Attributes / Spawn Placements 的 Mod Bus 事件
 * 4. 客戶端 Renderer / Model Layer 的 Mod Bus 事件
 *
 * EntityHelper 沒有自己的 DeferredRegister / Listener / 全域清單。
 */
public final class EntityRegister {

    private EntityRegister() {
    }

    // =========================================================
    // 唯一的 Entity / Spawn Egg Registry
    // =========================================================

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Beyond_the_end.MODID);

    public static final DeferredRegister.Items ENTITY_ITEMS =
            DeferredRegister.createItems(Beyond_the_end.MODID);

    // 必須宣告在任何 registerMob(...) 欄位之前。
    public static final List<EntityHelper.MobEntry<?>> MOB_ENTRIES = new ArrayList<>();

    // =========================================================
    // 實體清單：新增生物只需要在這裡宣告名稱與 Entity 類 SPEC
    // =========================================================

    public static final EntityHelper.MobEntry<SmallEndermanEntity> SMALL_ENDERMAN =
            EntityHelper.registerMob("small_enderman", SmallEndermanEntity.SPEC);

    public static final EntityHelper.MobEntry<AngryEndermanEntity>
            ANGRY_ENDERMAN = EntityHelper.registerMob("angry_enderman", AngryEndermanEntity.SPEC);

    // =========================================================
    // 共同端入口：主 Mod 建構子只呼叫這一次
    // =========================================================

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        ENTITY_ITEMS.register(modEventBus);
        modEventBus.addListener(EntityRegister::registerAttributes);
        modEventBus.addListener(EntityRegister::registerSpawnPlacements);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        for (EntityHelper.MobEntry<?> entry : MOB_ENTRIES) {
            putAttributes(event, entry);
        }
    }

    private static <T extends Mob> void putAttributes(
            EntityAttributeCreationEvent event,
            EntityHelper.MobEntry<T> entry
    ) {
        event.put(entry.type().get(), EntityHelper.createAttributes(entry.spec()));
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (EntityHelper.MobEntry<?> entry : MOB_ENTRIES) {
            putSpawnPlacement(event, entry);
        }
    }

    private static <T extends Mob> void putSpawnPlacement(
            RegisterSpawnPlacementsEvent event,
            EntityHelper.MobEntry<T> entry
    ) {
        EntityHelper.MobSpec<T> spec = entry.spec();
        event.register(
                entry.type().get(),
                spec.placementType(),
                spec.heightmap(),
                spec.spawnPredicate(),
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    // =========================================================
    // 客戶端入口：只交給你現有的 RenderRegistry 呼叫
    // =========================================================

    public static void registerClient(IEventBus modEventBus) {
        Client.register(modEventBus);
    }

    // 將客戶端專用類型隔離到巢狀類，Dedicated Server 不呼叫。
    private static final class Client {

        private Client() {
        }

        // =====================================================
        // Renderer + Model 清單：這裡決定每隻實體使用哪個
        // =====================================================
        //小安德渲染註冊
        private static final EntityHelper.Client.RenderSpec<EnderMan> SMALL_ENDERMAN_RENDER =
                EntityHelper.Client.<EnderMan>bind(
                        SMALL_ENDERMAN.type(),
                        SmallEndermanRenderer::new,
                        EntityHelper.Client.vanillaModel(
                                net.minecraft.client.model.geom.ModelLayers.ENDERMAN
                        )
                );
        private static final EntityHelper.Client.RenderSpec<EnderMan>
                ANGRY_ENDERMAN_RENDER =
                EntityHelper.Client.<EnderMan>bind(
                        ANGRY_ENDERMAN.type(),

                        // 直接使用原版 Renderer
                        EndermanRenderer::new,

                        // 直接使用原版模型
                        EntityHelper.Client.vanillaModel(ModelLayers.ENDERMAN)
                );



        private static final List<EntityHelper.Client.RenderSpec<?>> RENDERS =
                List.of(SMALL_ENDERMAN_RENDER,
                        ANGRY_ENDERMAN_RENDER);
        //註冊部分，不要動
        private static void register(IEventBus modEventBus) {
            modEventBus.addListener(Client::registerRenderers);
            modEventBus.addListener(Client::registerModelLayers);
        }

        private static void registerRenderers(
                EntityRenderersEvent.RegisterRenderers event
        ) {
            for (EntityHelper.Client.RenderSpec<?> definition : RENDERS) {
                registerRenderer(event, definition);
            }
        }

        private static <T extends Entity> void registerRenderer(
                EntityRenderersEvent.RegisterRenderers event,
                EntityHelper.Client.RenderSpec<T> definition
        ) {
            // 只有進入 Renderer Event 才取 EntityType.get()。
            event.registerEntityRenderer(
                    definition.entityType().get(),
                    definition.renderer()
            );
        }

        private static void registerModelLayers(
                EntityRenderersEvent.RegisterLayerDefinitions event
        ) {
            Set<net.minecraft.client.model.geom.ModelLayerLocation> registered =
                    new HashSet<>();

            for (EntityHelper.Client.RenderSpec<?> definition : RENDERS) {
                EntityHelper.Client.ModelSpec model = definition.model();

                // 原版 Enderman 模型已經由 Minecraft 註冊，不要重複註冊。
                if (model.needsRegistration() && registered.add(model.layer())) {
                    event.registerLayerDefinition(model.layer(), model.factory());
                }
            }
        }
    }
}
