package me.leafs.fakename.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.leafs.fakename.utils.ChatUtils;
import me.leafs.fakename.utils.SpoofStorage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class SpoofFirst {
    private SpoofFirst() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("spooffirst")
            .executes(context -> {
                ChatUtils.printChat("&cUsage: /spooffirst <target>");
                return 0;
            })
            .then(ClientCommandManager.argument("target", StringArgumentType.greedyString())
                .executes(context -> {
                    String target = StringArgumentType.getString(context, "target").trim();
                    if (target.isEmpty()) {
                        return 0;
                    }

                    SpoofStorage.setPendingTarget(target);
                    ChatUtils.printChat("&7Set target to &d" + target + "&7. Do &b/spoofsecond&7 to change the target to what you want.");
                    return 1;
                }))
        );
    }
}
