/*
 * Copyright (c) 2026 CCNR
 * SPDX-License-Identifier: MIT
 */
package com.ccnrcom;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

public class Permissions {
    /** 管理通讯权限节点：ccnrcom.admin.chat（默认拒绝；OP 始终可用） */
    public static final PermissionNode<Boolean> ADMIN_CHAT =
            new PermissionNode<>("ccnrcom", "admin.chat", PermissionTypes.BOOLEAN, (player, uuid, context) -> false);

    @SubscribeEvent
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(ADMIN_CHAT);
    }

    /** 管理通讯资格：OP（>= 2 级）或拥有 ccnrcom.admin.chat 权限节点 */
    public static boolean canAdmin(ServerPlayer player) {
        return player.hasPermissions(2) || PermissionAPI.getPermission(player, ADMIN_CHAT);
    }
}
