package org.starrepublic.beyond_the_end.Register.helper;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Register.EntityRegister;

import java.util.function.Supplier;

/**
 * 實體定義 / 建構 Helper。
 *
 * 注意：
 * - 這裡沒有 DeferredRegister 實例。
 * - 這裡不呼叫 IEventBus，也不註冊任何事件。
 * - 這裡不持有生物或 Renderer 的全域清單。
 *
 * Registry 與所有事件的唯一管理者是 EntityRegister。
 */
public final class EntityHelper {

    private EntityHelper() {
    }
    public static <T extends Mob> EntityHelper.MobEntry<T> registerMob(
            String name,
            EntityHelper.MobSpec<T> spec
    ) {
        DeferredHolder<EntityType<?>, EntityType<T>> type = EntityRegister.ENTITIES.register(
                name,
                id -> EntityHelper.createMobType(id, spec)
        );

        DeferredItem<DeferredSpawnEggItem> egg = EntityRegister.ENTITY_ITEMS.register(
                name + "_spawn_egg",
                () -> EntityHelper.createSpawnEgg(type, spec)
        );

        EntityHelper.MobEntry<T> entry = EntityHelper.createEntry(type, egg, spec);
        EntityRegister.MOB_ENTRIES.add(entry);
        return entry;
    }
    /**
     * 由每隻生物自己的 Entity 類建立。AI 仍由該實體類管理，
     * 此處只保存需要交給 NeoForge 的基礎設定。
     *
     * 欄位順序相容於原本的 SmallEndermanEntity.SPEC。
     */
    public record MobSpec<T extends Mob>(
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            float width,
            float height,
            Supplier<AttributeSupplier.Builder> attributes,
            SpawnPlacementType placementType,
            Heightmap.Types heightmap,
            SpawnPlacements.SpawnPredicate<T> spawnPredicate,
            int eggPrimaryColor,
            int eggSecondaryColor
    ) {
    }

    /** EntityRegister 保存的註冊結果，並非另一份 Registry。 */
    public record MobEntry<T extends Mob>(
            DeferredHolder<EntityType<?>, EntityType<T>> type,
            DeferredItem<DeferredSpawnEggItem> spawnEgg,
            MobSpec<T> spec
    ) {
    }

    /**
     * 只負責「建立」 EntityType，註冊由 EntityRegister.ENTITIES.register(...) 執行。
     * 也可供日後的投射物 / 非 Mob 實體使用。
     */
    public static <T extends Entity> EntityType<T> createEntityType(
            ResourceLocation id,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            float width,
            float height
    ) {
        return EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(8)
                .updateInterval(3)
                .build(id.toString()); // Minecraft / NeoForge 1.21.1
    }

    /** 將生物自己的 MobSpec 轉成 EntityType。 */
    public static <T extends Mob> EntityType<T> createMobType(
            ResourceLocation id,
            MobSpec<T> spec
    ) {
        return createEntityType(
                id,
                spec.factory(),
                spec.category(),
                spec.width(),
                spec.height()
        );
    }

    /**
     * 只建立生成蛋。EntityRegister.ENTITY_ITEMS.register(...) 決定註冊 ID。
     * 不提前呼叫 type.get()，避免未綁定的 EntityType。
     */
    public static <T extends Mob> DeferredSpawnEggItem createSpawnEgg(
            DeferredHolder<EntityType<?>, EntityType<T>> type,
            MobSpec<T> spec
    ) {
        return new DeferredSpawnEggItem(
                type,
                spec.eggPrimaryColor(),
                spec.eggSecondaryColor(),
                new Item.Properties()
        );
    }

    /** 只建立屬性物件；event.put(...) 由 EntityRegister 負責。 */
    public static <T extends Mob> AttributeSupplier createAttributes(MobSpec<T> spec) {
        return spec.attributes().get().build();
    }

    /** 將已建立的 Holder / Egg / Spec 打包回傳。 */
    public static <T extends Mob> MobEntry<T> createEntry(
            DeferredHolder<EntityType<?>, EntityType<T>> type,
            DeferredItem<DeferredSpawnEggItem> spawnEgg,
            MobSpec<T> spec
    ) {
        return new MobEntry<>(type, spawnEgg, spec);
    }

    /** 自訂貼圖位置：assets/<modid>/textures/entity/<path>.png */
    public static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                Beyond_the_end.MODID,
                "textures/entity/" + path + ".png"
        );
    }

    /** 原版貼圖位置：assets/minecraft/textures/entity/<path>.png */
    public static ResourceLocation vanillaTexture(String path) {
        return ResourceLocation.withDefaultNamespace(
                "textures/entity/" + path + ".png"
        );
    }

    /**
     * 僅在客戶端 EntityRegister.Client 中使用。
     * 這裡同樣只回傳 Renderer / Model 的定義，不綁事件、不做註冊。
     */
    public static final class Client {

        private Client() {
        }

        /**
         * factory 為 null 代表這是 Minecraft 已經註冊的原版模型圖層。
         * 自訂模型的 factory 應指向 YourModel::createBodyLayer。
         */
        public record ModelSpec(
                ModelLayerLocation layer,
                Supplier<LayerDefinition> factory
        ) {
            public boolean needsRegistration() {
                return factory != null;
            }
        }

        /**
         * Renderer 的描述資料。真正註冊由 EntityRegister.Client 完成。
         * 使用自訂 Model 時，Renderer 建構子仍須用 context.bakeLayer(model.layer())。
         */
        public record RenderSpec<T extends Entity>(
                Supplier<? extends EntityType<? extends T>> entityType,
                EntityRendererProvider<T> renderer,
                ModelSpec model
        ) {
        }

        public static ModelSpec vanillaModel(ModelLayerLocation layer) {
            return new ModelSpec(layer, null);
        }

        public static ModelSpec customModel(
                String name,
                Supplier<LayerDefinition> factory
        ) {
            return new ModelSpec(
                    new ModelLayerLocation(
                            ResourceLocation.fromNamespaceAndPath(
                                    Beyond_the_end.MODID,
                                    name
                            ),
                            "main"
                    ),
                    factory
            );
        }

        /**
         * 只建立 Renderer / Model 配對並回傳。
         * 不在此呼叫 DeferredHolder.get()，也不會建立事件 listener。
         */
        public static <T extends Entity> RenderSpec<T> bind(
                Supplier<? extends EntityType<? extends T>> entityType,
                EntityRendererProvider<T> renderer,
                ModelSpec model
        ) {
            return new RenderSpec<>(entityType, renderer, model);
        }
    }
}
