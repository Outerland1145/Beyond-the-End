package org.starrepublic.beyond_the_end.Compat.Jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.starrepublic.beyond_the_end.Entitys.blockentity.FireEssenceAltarBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class FireAltarSmeltingProvider
        implements IBlockComponentProvider,
        IServerDataProvider<BlockAccessor> {

    public static final FireAltarSmeltingProvider INSTANCE =
            new FireAltarSmeltingProvider();

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(
                    "beyond_the_end",
                    "fire_altar_smelting"
            );

    private static final String MODE =
            "BTEFireSmeltingMode";

    private static final String ACTIVE =
            "BTEFireSmeltingActive";

    private static final String SPEED =
            "BTEFireSmeltingSpeed";

    private static final String FUEL_FREE =
            "BTEFireFuelFree";

    private FireAltarSmeltingProvider() {
    }

    // ========================================================
    // 伺服器：讀取祭壇真實工作狀態
    // ========================================================

    @Override
    public void appendServerData(
            CompoundTag data,
            BlockAccessor accessor
    ) {

        if (!(accessor.getBlockEntity()
                instanceof FireEssenceAltarBlockEntity altar)) {
            return;
        }

        if (!"smelting".equals(altar.getPowderMode())) {
            return;
        }

        data.putBoolean(MODE, true);

        boolean active = altar.isSmeltingActive();

        data.putBoolean(ACTIVE, active);

        if (!active) {
            return;
        }

        data.putInt(
                SPEED,
                altar.getSmeltingSpeedPercent()
        );

        data.putBoolean(
                FUEL_FREE,
                altar.canSmeltWithoutFuel()
        );
    }

    // ========================================================
    // 客戶端：Jade 顯示
    // ========================================================

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
    ) {

        CompoundTag data = accessor.getServerData();

        // 非熔煉加速模式，不顯示本功能
        if (!data.getBoolean(MODE)) {
            return;
        }

        // 精華供能未啟動
        if (!data.getBoolean(ACTIVE)) {

            tooltip.add(
                    Component.translatable(
                            "jade.beyond_the_end.smelting_inactive"
                    )
            );

            return;
        }

        // ====================================================
        // 工作效率
        // ====================================================

        int speed = data.getInt(SPEED);

        tooltip.add(
                Component.translatable(
                        "jade.beyond_the_end.smelting_speed",
                        speed
                )
        );

        // ====================================================
        // 無燃料熔煉
        // 只有達到門檻時才顯示
        // ====================================================

        if (data.getBoolean(FUEL_FREE)) {

            tooltip.add(
                    Component.translatable(
                            "jade.beyond_the_end.fuel_free_enabled"
                    )
            );
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

