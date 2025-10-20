package me.leafs.fakename.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.leafs.fakename.utils.ChatUtils;
import me.leafs.fakename.utils.SpoofStorage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class NameHandler {
    private NameHandler() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("spoof")
            .executes(context -> {
                return clearTargets();
            })
            .then(ClientCommandManager.argument("input", StringArgumentType.greedyString())
                .executes(context -> {
                    String input = StringArgumentType.getString(context, "input").trim();
                    if (input.isEmpty()) {
                        return clearTargets();
                    }

                    int spaceIndex = input.indexOf(' ');
                    if (spaceIndex < 0) {
                        String realName = context.getSource().getClient().getSession().getUsername();
                        SpoofStorage.setTarget(realName, input);
                        ChatUtils.printChat("&7Your name has been set to &d" + input + "&7.");
                    } else {
                        String target = input.substring(0, spaceIndex).trim();
                        if (target.isEmpty()) {
                            return 0;
                        }

                        String fakeName = input.substring(spaceIndex + 1).trim();
                        if (fakeName.isEmpty()) {
                            SpoofStorage.setTarget(target, "");
                            ChatUtils.printChat("&d" + target + "&7 has been hidden.");
                            return 1;
                        }

                        SpoofStorage.setTarget(target, fakeName);
                        ChatUtils.printChat("&d" + target + "&7 has been set to &d" + fakeName + "&7.");
                    }

                    return 1;
                }))
        );
    }

    private static int clearTargets() {
        SpoofStorage.clearTargets();
        ChatUtils.printChat("&7All names have been&d reset&7. To use, type &b/spoof <name> or <target> <name>");
        return 1;
    }
}
