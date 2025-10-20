package me.leafs.fakename.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.leafs.fakename.utils.ChatUtils;
import me.leafs.fakename.utils.SpoofStorage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class Remove {
    private Remove() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("remove")
            .executes(context -> {
                ChatUtils.printChat("&cUsage: /remove <target>");
                return 0;
            })
            .then(ClientCommandManager.argument("target", StringArgumentType.greedyString())
                .executes(context -> {
                    String target = StringArgumentType.getString(context, "target").trim();
                    if (target.isEmpty()) {
                        return 0;
                    }

                    SpoofStorage.setTarget(target, "");
                    ChatUtils.printChat("&d" + target + "&7 has been hidden.");
                    return 1;
                }))
        );
    }
}
