package me.leafs.fakename.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.leafs.fakename.FakeName;
import me.leafs.fakename.utils.ChatUtils;
import me.leafs.fakename.utils.SkinSpoofStorage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Util;

import java.net.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class SpoofSkin {
    private static final Object profileRepositoryLock = new Object();

    private static GameProfileRepository profileRepository;

    private SpoofSkin() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("spoofskin")
            .executes(context -> showHelp())
            .then(ClientCommandManager.literal("help")
                .executes(context -> showHelp()))
            .then(ClientCommandManager.literal("reset")
                .executes(context -> {
                    SkinSpoofStorage.clear();
                    MinecraftClient client = context.getSource().getClient();
                    if (client != null) {
                        client.execute(() -> ChatUtils.printChat("&7All spoofed skins have been &dreset&7."));
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .then(ClientCommandManager.argument("target", StringArgumentType.word())
                    .executes(context -> {
                        String target = StringArgumentType.getString(context, "target");
                        FabricClientCommandSource source = context.getSource();
                        resetTarget(source, target);
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(ClientCommandManager.argument("target", StringArgumentType.word())
                .then(ClientCommandManager.argument("skin", StringArgumentType.word())
                    .executes(context -> {
                        String target = StringArgumentType.getString(context, "target");
                        String skin = StringArgumentType.getString(context, "skin");
                        FabricClientCommandSource source = context.getSource();
                        applySkin(source, target, skin);
                        return Command.SINGLE_SUCCESS;
                    }))));
    }

    private static int showHelp() {
        ChatUtils.printChat("&7Usage: &b/spoofskin <target> <skin>&7.");
        ChatUtils.printChat("&7Use &b/spoofskin reset <target>&7 to clear a single override, or &b/spoofskin reset&7 to clear all.");
        ChatUtils.printChat("&7Spoofed skins persist between sessions until they are reset.");
        return Command.SINGLE_SUCCESS;
    }

    private static void applySkin(FabricClientCommandSource source, String targetInput, String skinInput) {
        MinecraftClient client = source.getClient();
        if (client == null) {
            return;
        }

        CompletableFuture<GameProfile> targetFuture = resolveProfile(client, targetInput, false);
        CompletableFuture<GameProfile> skinFuture = resolveProfile(client, skinInput, true);

        targetFuture.thenCombine(skinFuture, ProfilePair::new)
            .thenCompose(pair -> fetchTextures(client, pair.skin())
                .thenApply(textures -> new SkinResult(pair.target(), pair.skin(), textures)))
            .thenAccept(result -> client.execute(() -> {
                SkinTextures textures = result.textures();
                if (textures == null) {
                    ChatUtils.printChat("&cFailed to download skin data for &d" + describeProfile(result.skin()) + "&c.");
                    return;
                }

                boolean stored = SkinSpoofStorage.setOverride(result.target(), result.skin(), textures);
                if (!stored) {
                    ChatUtils.printChat("&cUnable to store spoofed skin for &d" + describeProfile(result.target()) + "&c.");
                    return;
                }

                ChatUtils.printChat("&7Spoofed skin for &d" + describeProfile(result.target()) + " &7using &d" + describeProfile(result.skin()) + "&7.");
            }))
            .exceptionally(error -> {
                Throwable cause = unwrap(error);
                FakeName.LOGGER.error("Failed to spoof skin for {} using {}", targetInput, skinInput, cause);
                client.execute(() -> ChatUtils.printChat("&cUnable to spoof skin for &d" + targetInput + " &7using &d" + skinInput + "&c: &f" + formatError(cause)));
                return null;
            });
    }

    private static void resetTarget(FabricClientCommandSource source, String targetInput) {
        MinecraftClient client = source.getClient();
        if (client == null) {
            return;
        }

        resolveProfile(client, targetInput, false)
            .thenAccept(profile -> client.execute(() -> {
                boolean removed = SkinSpoofStorage.remove(profile);
                if (removed) {
                    ChatUtils.printChat("&7Cleared spoofed skin for &d" + describeProfile(profile) + "&7.");
                } else {
                    ChatUtils.printChat("&cNo spoofed skin stored for &d" + describeProfile(profile) + "&c.");
                }
            }))
            .exceptionally(error -> {
                boolean removed = SkinSpoofStorage.remove(targetInput);
                client.execute(() -> {
                    if (removed) {
                        ChatUtils.printChat("&7Cleared spoofed skin for &d" + targetInput + "&7.");
                    } else {
                        ChatUtils.printChat("&cUnable to find spoofed skin entry for &d" + targetInput + "&c.");
                    }
                });
                return null;
            });
    }

    private static CompletableFuture<SkinTextures> fetchTextures(MinecraftClient client, GameProfile skinProfile) {
        return client.getSkinProvider().fetchSkinTextures(skinProfile);
    }

    private static CompletableFuture<GameProfile> resolveProfile(MinecraftClient client, String input, boolean requireUuid) {
        if (input == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Missing name"));
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Name cannot be empty"));
        }

        Proxy proxy = client == null ? Proxy.NO_PROXY : client.getNetworkProxy();

        GameProfile online = findOnlineProfile(client, trimmed);
        if (online != null) {
            if (requireUuid && online.getId() == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Unable to resolve UUID for " + trimmed));
            }

            return CompletableFuture.completedFuture(online);
        }

        UUID uuid = parseUuid(trimmed);
        if (uuid != null) {
            return CompletableFuture.supplyAsync(() -> resolveByUuid(client, uuid, trimmed), Util.getDownloadWorkerExecutor());
        }

        return resolveByNameAsync(client, trimmed, requireUuid, proxy);
    }

    private static GameProfile resolveByUuid(MinecraftClient client, UUID uuid, String fallbackName) {
        MinecraftSessionService sessionService = client.getSessionService();
        if (sessionService != null) {
            try {
                ProfileResult result = sessionService.fetchProfile(uuid, true);
                if (result != null && result.profile() != null) {
                    GameProfile profile = result.profile();
                    if (profile.getName() == null || profile.getName().isEmpty()) {
                        return new GameProfile(profile.getId(), fallbackName);
                    }

                    return profile;
                }
            } catch (Exception e) {
                FakeName.LOGGER.warn("Failed to fetch profile for UUID {}", uuid, e);
            }
        }

        return new GameProfile(uuid, fallbackName.isEmpty() ? null : fallbackName);
    }

    private static CompletableFuture<GameProfile> resolveByNameAsync(MinecraftClient client, String name, boolean requireUuid, Proxy proxy) {
        GameProfileRepository repository = getProfileRepository(proxy);
        if (repository == null) {
            if (requireUuid) {
                return CompletableFuture.failedFuture(new IllegalStateException("Unable to resolve profile for " + name));
            }

            return CompletableFuture.completedFuture(new GameProfile(null, name));
        }

        CompletableFuture<GameProfile> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> repository.findProfilesByNames(new String[] { name }, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                if (profile != null) {
                    future.complete(profile);
                } else {
                    future.completeExceptionally(new IllegalStateException("No profile data returned for " + name));
                }
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception e) {
                if (e != null) {
                    future.completeExceptionally(e);
                } else {
                    future.completeExceptionally(new IllegalStateException("Profile lookup failed for " + profileName));
                }
            }
        }), Util.getDownloadWorkerExecutor());

        return future.thenApply(profile -> {
            GameProfile resolved = profile;
            if (resolved.getName() == null || resolved.getName().isEmpty()) {
                resolved = new GameProfile(resolved.getId(), name);
            }

            if (requireUuid && resolved.getId() == null) {
                throw new CompletionException(new IllegalStateException("Unable to resolve UUID for " + name));
            }

            return resolved;
        }).exceptionally(error -> {
            Throwable cause = unwrap(error);
            if (requireUuid) {
                throw cause instanceof RuntimeException ? (RuntimeException) cause : new CompletionException(new IllegalStateException("Failed to resolve profile for " + name, cause));
            }

            FakeName.LOGGER.warn("Falling back to name-only spoof for {}", name, cause);
            return new GameProfile(null, name);
        });
    }

    private static GameProfile findOnlineProfile(MinecraftClient client, String input) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return null;
        }

        UUID uuid = parseUuid(input);
        PlayerListEntry entry = uuid != null ? handler.getPlayerListEntry(uuid) : handler.getPlayerListEntry(input);
        return entry == null ? null : entry.getProfile();
    }

    private static UUID parseUuid(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ignored) {
            if (trimmed.length() == 32 && trimmed.matches("(?i)[0-9a-f]{32}")) {
                String dashed = trimmed.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5");
                try {
                    return UUID.fromString(dashed);
                } catch (IllegalArgumentException ignored2) {
                    return null;
                }
            }

            return null;
        }
    }

    private static GameProfileRepository getProfileRepository(Proxy proxy) {
        synchronized (profileRepositoryLock) {
            if (profileRepository == null) {
                profileRepository = new YggdrasilAuthenticationService(proxy == null ? Proxy.NO_PROXY : proxy).createProfileRepository();
            }
        }

        return profileRepository;
    }

    private static String describeProfile(GameProfile profile) {
        if (profile == null) {
            return "?";
        }

        String name = profile.getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }

        UUID uuid = profile.getId();
        return uuid == null ? "?" : uuid.toString();
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause;
    }

    private static String formatError(Throwable error) {
        if (error == null) {
            return "unknown error";
        }

        String message = error.getMessage();
        if (message == null || message.isEmpty()) {
            return error.getClass().getSimpleName();
        }

        return message;
    }

    private record ProfilePair(GameProfile target, GameProfile skin) {
    }

    private record SkinResult(GameProfile target, GameProfile skin, SkinTextures textures) {
    }
}
