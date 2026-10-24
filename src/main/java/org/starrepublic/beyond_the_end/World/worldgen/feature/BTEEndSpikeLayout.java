package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.starrepublic.beyond_the_end.configs.BTEConfigs;

import java.util.ArrayList;
import java.util.List;

public final class BTEEndSpikeLayout {

    private BTEEndSpikeLayout() {
    }

    /**
     * 統一取得指定世界的黑曜石柱排列。
     *
     * 所有結果只依賴世界種子與配置：
     * - 數量
     * - 環形半徑
     * - 柱子高度與粗細
     * - 籠子位置
     *
     * 不依賴區塊生成順序或目前遊戲 Tick。
     */
    public static List<SpikeFeature.EndSpike> getSpikes(
            long worldSeed
    ) {
        int count = BTEConfigs.SERVER.SPIKE_COUNT.get();
        int distance = BTEConfigs.SERVER.SPIKE_RING_RADIUS.get();

        RandomSource random = RandomSource.create(
                worldSeed ^ 0x6A09E667F3BCC909L
        );

        // 確定要生成籠子的柱子索引。
        // 預設最多兩根，且不會重複選中同一根。
        int guardedA = random.nextInt(count);
        int guardedB = -1;

        if (count > 1) {
            guardedB = random.nextInt(count - 1);

            if (guardedB >= guardedA) {
                guardedB++;
            }
        }

        List<SpikeFeature.EndSpike> spikes =
                new ArrayList<>(count);

        for (int i = 0; i < count; i++) {

            double angle =
                    Math.PI * 2.0 * i / count;

            int x = (int) Math.round(
                    Math.cos(angle) * distance
            );

            int z = (int) Math.round(
                    Math.sin(angle) * distance
            );

            // 第一版保留原版風格的隨機粗細與高度。
            int radius = 2 + random.nextInt(3);
            int height = 76 + random.nextInt(27);

            boolean guarded =
                    i == guardedA || i == guardedB;

            spikes.add(
                    new SpikeFeature.EndSpike(
                            x,
                            z,
                            radius,
                            height,
                            guarded
                    )
            );
        }

        return List.copyOf(spikes);
    }
}

