package org.starrepublic.beyond_the_end.Register;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Data.EndAnchorData;

import java.util.function.Supplier;

public final class DataRegister {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    Beyond_the_end.MODID
            );

    public static final Supplier<AttachmentType<EndAnchorData>> END_ANCHOR =
            ATTACHMENT_TYPES.register(
                    "end_anchor",
                    () -> AttachmentType
                            .builder(() -> EndAnchorData.EMPTY)
                            .serialize(EndAnchorData.CODEC)
                            .copyOnDeath()
                            .build()
            );
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
