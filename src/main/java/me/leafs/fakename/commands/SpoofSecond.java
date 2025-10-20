package me.leafs.fakename.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.leafs.fakename.utils.ChatUtils;
import me.leafs.fakename.utils.SpoofStorage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class SpoofSecond {
    private SpoofSecond() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("spoofsecond")
            .executes(context -> {
                ChatUtils.printChat("&cUsage: /spoofsecond <replacement>");
                return 0;
            })
            .then(ClientCommandManager.argument("replacement", StringArgumentType.greedyString())
                .executes(context -> SpoofStorage.getPendingTarget()
                    .map(target -> {
                        String replacement = StringArgumentType.getString(context, "replacement").trim();
                        if (replacement.isEmpty()) {
                            SpoofStorage.setTarget(target, "");
                            SpoofStorage.setPendingTarget(null);
                            ChatUtils.printChat("&d" + target + "&7 has been hidden.");
                            return Command.SINGLE_SUCCESS;
                        }

                        SpoofStorage.setTarget(target, replacement);
                        SpoofStorage.setPendingTarget(null);
                        ChatUtils.printChat("&d" + target + "&7 has been set to &d" + replacement + "&7.");
                        return Command.SINGLE_SUCCESS;
                    })
                    .orElseGet(() -> {
                        ChatUtils.printChat("&7spooffirst has not been set. Do &b/spooffirst <string>&7 before running this command.");
                        return 0;
                    })))
        );
    }
}
