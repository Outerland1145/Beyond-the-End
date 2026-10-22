package org.starrepublic.beyond_the_end.recipes.CharmRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import org.starrepublic.beyond_the_end.Beyond_the_end;
import org.starrepublic.beyond_the_end.Items.charm.CharmTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CharmBuiltinRecipes {

    /*
     * HEE 1.7.10 原始符文組合。
     *
     * 每一行：
     * 原始 HEE 配方 ID + 符文組合
     *
     * 不區分排列順序。
     *
     * ID 59 需要六個符文，保留定義，
     * 但原版五格合成介面無法製作。
     */
    private static final String RECIPES = """
            0 PPP
            1 PPPP
            2 PPPPP
            3 AAA
            4 AAAA
            5 AAAAA
            6 VVV
            7 VVVV
            8 VVVVV
            9 DDD
            10 DDDD
            11 DDDDD
            12 MMM
            13 MMMM
            14 MMMMM

            18 PAVDM

            19 PDO
            20 PDDO
            21 PDDDO
            22 PDOM
            23 PDDOM
            24 PPDOM
            71 PDOA
            72 PDDOA
            73 PDOAA

            25 PVO
            26 PVVO
            27 PVVVO
            28 PVMO
            30 PPVVO

            31 ADM
            32 AADM
            33 AAADM
            34 ADDM
            35 AADDM
            36 ADDDM

            37 PPM
            38 PPPM
            39 PPPPM
            40 PPPMM
            41 PMM
            42 PMMM
            43 PMMMM

            45 PMVVO
            46 PMMVO
            65 PPMVO

            74 DMO
            75 DDMO
            76 DDDMO

            49 AAAD
            50 AAAAD

            51 AAO
            52 AAAO
            53 AAAAO

            54 PPA
            55 PPPA
            56 PPPPA
            57 PPAA
            58 PPAAA
            59 PPPAAA

            60 VMO
            61 VVMO
            62 VVVMO
            63 VMMO
            64 VMMMO

            17 PMO
            66 PPMO
            67 PPPMO
            68 PMMO
            69 PMMMO
            70 PPMMO

            77 DAMO
            78 DAAMO
            79 DAMMO
            """;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                Beyond_the_end.MODID,
                path
        );
    }

    public static ResourceLocation recipeId(int heeId) {
        return id("charm_rune/hee_" + heeId);
    }

    /**
     * 將精簡符文格式轉成 RuneData 使用的 ID。
     */
    public static List<ResourceLocation> parseRunes(
            String pattern
    ) {

        List<ResourceLocation> result =
                new ArrayList<>();

        String normalized =
                pattern.replaceAll("\\s+", "")
                        .toUpperCase(java.util.Locale.ROOT);

        for (char rune : normalized.toCharArray()) {

            ResourceLocation type = switch (rune) {

                case 'P' -> id("power");
                case 'A' -> id("agility");

                // 沿用你先前符文系統中的 vitality ID。
                // 若已改為 vigor，修改此處即可。
                case 'V' -> id("vitality");

                case 'D' -> id("defense");
                case 'M' -> id("magic");
                case 'O' -> id("void");

                default -> throw new IllegalArgumentException(
                        "Unknown rune symbol: " + rune
                );
            };

            result.add(type);
        }

        if (result.size() < 3 || result.size() > 6) {
            throw new IllegalArgumentException(
                    "Invalid rune pattern: " + pattern
            );
        }

        return List.copyOf(result);
    }

    /**
     * 建立全部 HEE Java 內建配方。
     *
     * 結果種類與變種從現有 CharmTypes
     * 的 HEE ID 對照取得。
     */
    public static List<RecipeHolder<CharmRuneRecipe>>
    createAll() {

        List<RecipeHolder<CharmRuneRecipe>> result =
                new ArrayList<>();

        Set<Integer> knownIds =
                new HashSet<>();

        for (String line : RECIPES.split("\\R")) {

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("\\s+");

            if (parts.length != 2) {
                throw new IllegalStateException(
                        "Invalid recipe line: " + line
                );
            }

            int heeId = Integer.parseInt(parts[0]);

            if (!knownIds.add(heeId)) {
                throw new IllegalStateException(
                        "Duplicate HEE recipe: " + heeId
                );
            }

            CharmTypes.CharmReference reference =
                    CharmTypes.fromHeeId(heeId)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Missing HEE variant: " + heeId
                                    )
                            );

            CharmRuneRecipe recipe =
                    new CharmRuneRecipe(
                            reference.type(),
                            reference.level(),
                            parseRunes(parts[1])
                    );

            result.add(
                    new RecipeHolder<>(
                            recipeId(heeId),
                            recipe
                    )
            );
        }

        if (result.size() != 74) {
            throw new IllegalStateException(
                    "Expected 74 HEE recipes, got "
                            + result.size()
            );
        }

        // 避免兩組不同 ID 的配方實際具有相同符文組合。
        Set<String> patterns = new HashSet<>();

        for (RecipeHolder<CharmRuneRecipe> holder : result) {

            String signature = holder.value()
                    .runes()
                    .stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .collect(
                            java.util.stream.Collectors.joining("|")
                    );

            if (!patterns.add(signature)) {
                throw new IllegalStateException(
                        "Duplicate rune combination: "
                                + holder.id()
                );
            }
        }

        return List.copyOf(result);
    }

    private CharmBuiltinRecipes() {
    }
}
