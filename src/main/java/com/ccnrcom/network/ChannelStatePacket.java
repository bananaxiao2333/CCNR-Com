package com.ccnrcom.network;

import com.ccnrcom.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** S2C：我频道上的成员列表（供 HUD 区分对讲机/近场语音） */
public class ChannelStatePacket {
    private final List<UUID> members;

    public ChannelStatePacket(List<UUID> members) {
        this.members = members;
    }

    public static void encode(ChannelStatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.members.size());
        for (UUID id : msg.members) {
            buf.writeUUID(id);
        }
    }

    public static ChannelStatePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<UUID> members = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            members.add(buf.readUUID());
        }
        return new ChannelStatePacket(members);
    }

    public static void handle(ChannelStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientPacketHandler.handleChannelState(msg)));
        ctx.get().setPacketHandled(true);
    }

    public List<UUID> getMembers() { return members; }
}
