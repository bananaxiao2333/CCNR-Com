package com.ccnrcom.network;

import com.ccnrcom.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C：管理通讯 / 场外通讯消息 */
public class CommsChatPacket {
    public static final int TYPE_ADMIN = 1;
    public static final int TYPE_OOC = 2;

    private final int type;
    private final String sender;
    private final String message;

    public CommsChatPacket(int type, String sender, String message) {
        this.type = type;
        this.sender = sender;
        this.message = message;
    }

    public static void encode(CommsChatPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.type);
        buf.writeUtf(msg.sender);
        buf.writeUtf(msg.message);
    }

    public static CommsChatPacket decode(FriendlyByteBuf buf) {
        return new CommsChatPacket(buf.readVarInt(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(CommsChatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientPacketHandler.handleCommsChat(msg)));
        ctx.get().setPacketHandled(true);
    }

    public int getType() { return type; }
    public String getSender() { return sender; }
    public String getMessage() { return message; }
}
