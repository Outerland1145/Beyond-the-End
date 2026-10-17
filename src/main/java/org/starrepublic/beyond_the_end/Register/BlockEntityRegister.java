package org.starrepublic.beyond_the_end.Register;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Entitys.blockentity.DecorativeEndGatewayBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EndCampfireBlockEntity;
import org.starrepublic.beyond_the_end.Entitys.blockentity.EndNetherPortalBlockEntity;

import java.util.function.Supplier;

public class BlockEntityRegister {
    private BlockEntityRegister() {}
    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITIES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    Beyond_the_end.MODID
            );

    public static final Supplier<
                BlockEntityType<DecorativeEndGatewayBlockEntity>>
            DECORATIVE_END_GATEWAY =
            BLOCK_ENTITIES.register(
                    "decorative_end_gateway",
                    () -> BlockEntityType.Builder.of(
                            DecorativeEndGatewayBlockEntity::new,
                            BlockRegister.DECORATIVE_END_GATEWAY.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndNetherPortalBlockEntity>>
            END_NETHER_PORTAL =
            BLOCK_ENTITIES.register(
                    "end_nether_portal",
                    () -> BlockEntityType.Builder.of(
                            EndNetherPortalBlockEntity::new,
                            BlockRegister.END_NETHER_PORTAL.get()
                    ).build(null)
            );
    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<EndCampfireBlockEntity>
            > END_CAMPFIRE =

            BLOCK_ENTITIES.register(
                    "end_campfire",
                    () -> BlockEntityType.Builder.of(
                            EndCampfireBlockEntity::new,
                            BlockRegister.END_FIRE.campfire.get()
                    ).build(null)
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
