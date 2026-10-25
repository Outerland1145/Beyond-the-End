package org.starrepublic.beyond_the_end.World.worldgen.feature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import org.starrepublic.beyond_the_end.World.DFM.BTEEndFightSettingsSavedData;
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
            ServerLevel level
    ) {
        BTEEndFightSettingsSavedData settings =
                BTEEndFightSettingsSavedData.get(level);

        int count = settings.getSpikeCount();
        int distance = settings.getSpikeRadius();
        int guardedCount = Math.min(
                count,
                settings.getGuardedCrystalCount()
        );
        long worldSeed = level.getSeed();

        RandomSource random = RandomSource.create(
                worldSeed ^ 0x6A09E667F3BCC909L
        );

        // 確定要生成籠子的柱子索引。
        // 預設最多兩根，且不會重複選中同一根。

// 紀錄哪些柱子會生成鐵柵欄籠子
        boolean[] guarded = new boolean[count];

// 第一根：保留原本的隨機抽選方式
        int first = random.nextInt(count);
        guarded[first] = true;

// 第二根：保留原本不重複的抽選方式
        if (count > 1) {

            int second = random.nextInt(count - 1);

            if (second >= first) {
                second++;
            }

            guarded[second] = true;
        }

        /*
         * 超過兩根時，從剩餘柱子中抽選。
         *
         * 使用獨立亂數來源，避免額外抽選次數
         * 影響原本柱子的高度與粗細。
         */
        if (guardedCount > 2) {

            RandomSource protectionRandom =
                    RandomSource.create(
                            worldSeed ^ 0x3C6EF372FE94F82BL
                    );

            List<Integer> remaining = new ArrayList<>();

            for (int i = 0; i < count; i++) {

                if (!guarded[i]) {
                    remaining.add(i);
                }
            }

            for (int i = 2; i < guardedCount; i++) {

                int selected = protectionRandom.nextInt(
                        remaining.size()
                );

                int spikeIndex = remaining.remove(selected);

                guarded[spikeIndex] = true;
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

            boolean isGuarded = guarded[i];

            spikes.add(
                    new SpikeFeature.EndSpike(
                            x,
                            z,
                            radius,
                            height,
                            isGuarded
                    )
            );
        }

        return List.copyOf(spikes);
    }
}

