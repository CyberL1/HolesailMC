package cyber.holesailmc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class HolesailMC implements ClientModInitializer {
    public static final String MOD_ID = "holesailmc";
    public static final Logger LOGGER = LoggerFactory.getLogger("HolesailMC");

    public static final Path modDirectory = FabricLoader.getInstance().getConfigDir().resolve(HolesailMC.MOD_ID);
    public static boolean skipDownloadScreen = false;

    @Override
    public void onInitializeClient() {
        try {
            Files.createDirectories(modDirectory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register(_ -> HolesailManager.getInstance().StopAll());
    }
}