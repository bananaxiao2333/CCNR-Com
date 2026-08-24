/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import com.ccnrcom.network.CommsChatPacket;
import com.ccnrcom.network.RadioReceivePacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.network.PacketDistributor;

public class ModCommands {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /channel [set|clear|get] [<玩家>] [<频道>]
        dispatcher.register(Commands.literal("channel")
                .executes(ctx -> showChannel(ctx.getSource()))
                .then(Commands.literal("set")
                        .then(Commands.argument("channel", StringArgumentType.word())
                                .executes(ctx ->
                                        setChannel(ctx.getSource(), StringArgumentType.getString(ctx, "channel"))))
                        .then(Commands.argument("player", EntityArgument.players())
                                .then(Commands.argument("channel", StringArgumentType.word())
                                        .executes(ctx -> setChannelOther(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "player"),
                                                StringArgumentType.getString(ctx, "channel"))))))
                .then(Commands.literal("clear")
                        .executes(ctx -> clearChannel(ctx.getSource()))
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ctx ->
                                        clearChannelOther(ctx.getSource(), EntityArgument.getPlayers(ctx, "player")))))
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ctx ->
                                        getChannelOther(ctx.getSource(), EntityArgument.getPlayers(ctx, "player")))))
                .then(Commands.argument("channel", StringArgumentType.word())
                        .executes(ctx -> setChannel(ctx.getSource(), StringArgumentType.getString(ctx, "channel")))));

        // /r <消息>
        dispatcher.register(Commands.literal("r")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> sendRadio(ctx.getSource(), StringArgumentType.getString(ctx, "message"))))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.translatable("ccnrcom.radio.usage"));
                    return 0;
                }));

        // /a <消息> 管理通讯（OP 或 ccnrcom.admin.chat 权限）
        LiteralArgumentBuilder<CommandSourceStack> adminCmd = Commands.literal("a")
                .requires(src -> {
                    try {
                        return Permissions.canAdmin(src.getPlayerOrException());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> sendAdmin(ctx.getSource(), StringArgumentType.getString(ctx, "message"))))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.translatable("ccnrcom.admin.usage"));
                    return 0;
                });
        dispatcher.register(adminCmd);
        dispatcher.register(Commands.literal("admin").redirect(adminCmd.build()));

        // /ccnrsend <channel|admin|ooc> <消息> 服务端广播（OP）
        dispatcher.register(Commands.literal("ccnrsend")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> serverBroadcast(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "message"))))));

        // /o <消息> 场外通讯（所有人都能收到，无视距离）
        LiteralArgumentBuilder<CommandSourceStack> oocCmd = Commands.literal("o")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> sendOoc(ctx.getSource(), StringArgumentType.getString(ctx, "message"))))
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.translatable("ccnrcom.ooc.usage"));
                    return 0;
                });
        dispatcher.register(oocCmd);
        dispatcher.register(Commands.literal("ooc").redirect(oocCmd.build()));
    }

    private static int showChannel(CommandSourceStack source) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return 0;
        ChannelManager.get(player.getUUID())
                .ifPresentOrElse(
                        channel -> source.sendSuccess(
                                () -> Component.translatable("ccnrcom.channel.current", channel)
                                        .withStyle(ChatFormatting.GREEN),
                                false),
                        () -> source.sendFailure(Component.translatable("ccnrcom.channel.not_set")));
        return 1;
    }

    private static int clearChannel(CommandSourceStack source) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return 0;
        ChannelManager.clear(player.getUUID());
        source.sendSuccess(
                () -> Component.translatable("ccnrcom.channel.cleared").withStyle(ChatFormatting.YELLOW), false);
        ChannelSync.syncAll(source.getServer());
        return 1;
    }

    private static int setChannel(CommandSourceStack source, String channel) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return 0;
        if (!ChannelManager.isValid(channel)) {
            source.sendFailure(Component.translatable("ccnrcom.channel.invalid"));
            return 0;
        }
        ChannelManager.set(player.getUUID(), channel);
        source.sendSuccess(
                () -> Component.translatable("ccnrcom.channel.set", channel).withStyle(ChatFormatting.GREEN), false);
        ChannelSync.syncAll(source.getServer());
        return 1;
    }

    private static int sendRadio(CommandSourceStack source, String message) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return 0;
        UUID playerId = player.getUUID();

        String channel = ChannelManager.get(playerId).orElse(null);
        if (channel == null) {
            source.sendFailure(Component.translatable("ccnrcom.channel.not_set"));
            return 0;
        }
        String trimmed = validateMessage(source, message);
        if (trimmed == null) return 0;

        RadioReceivePacket packet =
                new RadioReceivePacket(channel, player.getGameProfile().getName(), trimmed);

        int range = Config.CHAT_RANGE.get();
        boolean crossDim = Config.CROSS_DIMENSION.get();
        boolean echo = Config.ECHO_TO_SENDER.get();

        // 仅向满足条件的同频道在线玩家发送（服务端强制校验）
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (!ChannelManager.isOnChannel(p.getUUID(), channel)) continue;
            if (!echo && p.getUUID().equals(playerId)) continue;
            if (!crossDim && p.level().dimension() != player.level().dimension()) continue;
            if (range >= 0
                    && p.level().dimension() == player.level().dimension()
                    && p.distanceToSqr(player) > (double) range * (double) range) continue;
            CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
        }
        return 1;
    }

    /** 服务端广播：/ccnrsend <channel|admin|ooc> <message> */
    private static int serverBroadcast(CommandSourceStack source, String target, String message) {
        String trimmed = validateMessage(source, message);
        if (trimmed == null) return 0;
        int sent;
        if (target.equalsIgnoreCase("admin")) {
            sent = CommsBroadcast.admin(source.getServer(), trimmed);
        } else if (target.equalsIgnoreCase("ooc")) {
            sent = CommsBroadcast.ooc(source.getServer(), trimmed);
        } else {
            if (!ChannelManager.isValid(target)) {
                source.sendFailure(Component.translatable("ccnrcom.channel.invalid"));
                return 0;
            }
            sent = CommsBroadcast.radio(source.getServer(), target, trimmed);
        }
        source.sendSuccess(
                () -> Component.translatable("ccnrcom.broadcast.sent", target, sent)
                        .withStyle(ChatFormatting.GREEN),
                false);
        return 1;
    }

    /** 管理员：把一批玩家的频道设为指定频道（支持 @a 等选择器） */
    private static int setChannelOther(CommandSourceStack source, Collection<ServerPlayer> targets, String channel) {
        ServerPlayer admin = adminOrFail(source);
        if (admin == null) return 0;
        if (!ChannelManager.isValid(channel)) {
            source.sendFailure(Component.translatable("ccnrcom.channel.invalid"));
            return 0;
        }
        String adminName = admin.getGameProfile().getName();
        for (ServerPlayer target : targets) {
            ChannelManager.set(target.getUUID(), channel);
            target.sendSystemMessage(Component.translatable("ccnrcom.channel.changed_by_admin", adminName, channel)
                    .withStyle(ChatFormatting.YELLOW));
        }
        ChannelSync.syncAll(source.getServer());
        source.sendSuccess(
                () -> Component.translatable("ccnrcom.channel.set_other_count", targets.size(), channel)
                        .withStyle(ChatFormatting.GREEN),
                false);
        return 1;
    }

    /** 管理员：清除一批玩家的频道（支持 @a 等选择器） */
    private static int clearChannelOther(CommandSourceStack source, Collection<ServerPlayer> targets) {
        ServerPlayer admin = adminOrFail(source);
        if (admin == null) return 0;
        String adminName = admin.getGameProfile().getName();
        for (ServerPlayer target : targets) {
            ChannelManager.clear(target.getUUID());
            target.sendSystemMessage(Component.translatable("ccnrcom.channel.cleared_by_admin", adminName)
                    .withStyle(ChatFormatting.YELLOW));
        }
        ChannelSync.syncAll(source.getServer());
        source.sendSuccess(
                () -> Component.translatable("ccnrcom.channel.cleared_other_count", targets.size())
                        .withStyle(ChatFormatting.YELLOW),
                false);
        return 1;
    }

    /** 管理员：查看一批玩家的频道（支持 @a 等选择器） */
    private static int getChannelOther(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (adminOrFail(source) == null) return 0;
        for (ServerPlayer target : targets) {
            String name = target.getGameProfile().getName();
            ChannelManager.get(target.getUUID())
                    .ifPresentOrElse(
                            channel -> source.sendSuccess(
                                    () -> Component.translatable("ccnrcom.channel.get_other", name, channel)
                                            .withStyle(ChatFormatting.GREEN),
                                    false),
                            () -> source.sendFailure(
                                    Component.translatable("ccnrcom.channel.get_other_not_set", name)));
        }
        return 1;
    }

    private static ServerPlayer adminOrFail(CommandSourceStack source) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return null;
        if (!Permissions.canAdmin(player)) {
            source.sendFailure(Component.translatable("ccnrcom.admin.no_permission"));
            return null;
        }
        return player;
    }

    /** 管理通讯：仅发送给有 OP 或权限节点的玩家 */
    private static int sendAdmin(CommandSourceStack source, String message) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return 0;
        if (!Permissions.canAdmin(player)) {
            source.sendFailure(Component.translatable("ccnrcom.admin.no_permission"));
            return 0;
        }
        String trimmed = validateMessage(source, message);
        if (trimmed == null) return 0;

        CommsChatPacket packet = new CommsChatPacket(
                CommsChatPacket.TYPE_ADMIN, player.getGameProfile().getName(), trimmed);
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (Permissions.canAdmin(p)) {
                CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
            }
        }
        return 1;
    }

    /** 场外通讯：无视距离，广播给所有在线玩家 */
    private static int sendOoc(CommandSourceStack source, String message) {
        ServerPlayer player = playerOrFail(source);
        if (player == null) return 0;
        String trimmed = validateMessage(source, message);
        if (trimmed == null) return 0;

        CommsChatPacket packet = new CommsChatPacket(
                CommsChatPacket.TYPE_OOC, player.getGameProfile().getName(), trimmed);
        for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            CCNRComMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), packet);
        }
        return 1;
    }

    /** 校验并裁剪消息；非法时发送错误提示并返回 null */
    private static String validateMessage(CommandSourceStack source, String message) {
        if (message.isBlank()) {
            source.sendFailure(Component.translatable("ccnrcom.radio.empty"));
            return null;
        }
        int max = Config.MAX_MESSAGE_LENGTH.get();
        if (message.length() > max) {
            source.sendFailure(Component.translatable("ccnrcom.radio.too_long", max));
            return null;
        }
        return message.strip();
    }

    private static ServerPlayer playerOrFail(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.translatable("ccnrcom.command.player_only"));
            return null;
        }
    }
}
