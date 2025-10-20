package me.leafs.fakename;

import me.leafs.fakename.commands.NameHandler;
import me.leafs.fakename.commands.Remove;
import me.leafs.fakename.commands.SpoofFirst;
import me.leafs.fakename.commands.SpoofSecond;
import me.leafs.fakename.utils.SpoofStorage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeName implements ClientModInitializer {
    public static final String MOD_ID = "name_spoofer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SpoofStorage.init(FabricLoader.getInstance().getConfigDir());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            NameHandler.register(dispatcher);
            Remove.register(dispatcher);
            SpoofFirst.register(dispatcher);
            SpoofSecond.register(dispatcher);
        });
    }
}
