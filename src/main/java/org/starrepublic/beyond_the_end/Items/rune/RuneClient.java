package org.starrepublic.beyond_the_end.Items.rune;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Items.charm.CharmClientExtensions;
import org.starrepublic.beyond_the_end.Register.ItemRegister;

public final class RuneClient {

    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            ItemProperties.register(
                    ItemRegister.RUNEITEM.get(),

                    ResourceLocation.fromNamespaceAndPath(
                            Beyond_the_end.MODID,
                            "rune_model"
                    ),

                    (stack, level, entity, seed) -> {

                        var type = RuneHelper.getType(stack);

                        if (type.isEmpty()) {
                            return 0.0F;
                        }

                        HolderLookup.Provider registries = null;

                        if (level != null) {

                            registries = level.registryAccess();

                        } else {

                            var connection =
                                    Minecraft.getInstance()
                                            .getConnection();

                            if (connection != null) {
                                registries =
                                        connection.registryAccess();
                            }
                        }

                        return (float)
                                RuneTypes.getModelIndex(
                                        type.get(),
                                        registries
                                );
                    }
            );
        });
    }

    private RuneClient() {
    }
}
