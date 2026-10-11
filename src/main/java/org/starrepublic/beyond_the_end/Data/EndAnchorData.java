package org.starrepublic.beyond_the_end.Data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record EndAnchorData(boolean active, long pos) {

    public static final EndAnchorData EMPTY =
            new EndAnchorData(false, 0L);

    public static final Codec<EndAnchorData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(

                    Codec.BOOL
                            .fieldOf("active")
                            .forGetter(EndAnchorData::active),

                    Codec.LONG
                            .fieldOf("pos")
                            .forGetter(EndAnchorData::pos)

            ).apply(instance, EndAnchorData::new));

    public static EndAnchorData create(BlockPos pos) {
        return new EndAnchorData(
                true,
                pos.asLong()
        );
    }

    public BlockPos getBlockPos() {
        return BlockPos.of(pos);
    }
}
