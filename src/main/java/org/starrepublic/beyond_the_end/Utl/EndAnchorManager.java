package org.starrepublic.beyond_the_end.Utl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.starrepublic.beyond_the_end.Data.EndAnchorData;
import org.starrepublic.beyond_the_end.Register.BlockRegister;
import org.starrepublic.beyond_the_end.Register.DataRegister;

public final class EndAnchorManager {

    private EndAnchorManager() {

    }


    @Nullable
    public static Vec3 getAnchorDestination(
            ServerPlayer player,
            ServerLevel endLevel
    ) {

        /*
         * 玩家根本沒有錨定資料。
         *
         * 不顯示任何訊息，
         * 直接讓原本的黑曜石平台邏輯處理。
         */
        if (!player.hasData(
                DataRegister.END_ANCHOR.get()
        )) {
            return null;
        }


        EndAnchorData data =
                player.getData(
                        DataRegister.END_ANCHOR.get()
                );


        /*
         * Attachment 存在，
         * 但沒有實際錨點。
         */
        if (!data.active()) {
            return null;
        }


        BlockPos anchorPos =
                data.getBlockPos();


        /*
         * 非常重要。
         *
         * 錨點可能離玩家數萬甚至數十萬格，
         * 所以該 Chunk 很可能根本沒有載入。
         *
         * 在檢查方塊之前先載入 Chunk。
         */
        endLevel.getChunk(
                anchorPos.getX() >> 4,
                anchorPos.getZ() >> 4
        );


        /*
         * 檢查原本位置現在是不是錨定方塊。
         */
        if (!endLevel
                .getBlockState(anchorPos)
                .is(BlockRegister.END_ANCHOR_BLOCK.get())) {

            /*
             * 錨點已遺失。
             *
             * 清除玩家資料，
             * 避免之後每次進終界都一直提示。
             */
            player.setData(
                    DataRegister.END_ANCHOR.get(),
                    EndAnchorData.EMPTY
            );


            player.displayClientMessage(
                    Component.translatable(
                            "message.beyond_the_end.anchor_lost"
                    ),
                    true
            );


            /*
             * null =
             * 使用正常黑曜石平台入口。
             */
            return null;
        }


        /*
         * 錨定方塊存在。
         *
         * 玩家出現在它上面一格。
         */
        BlockPos destination =
                anchorPos.above();


        /*
         * +0.5 讓玩家出現在方塊正中央。
         */
        return new Vec3(
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D
        );
    }
}
