package cyber.holesailmc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HolesailMC implements ClientModInitializer {
    public static final String MOD_ID = "holesailmc";
    public static final Logger LOGGER = LoggerFactory.getLogger("HolesailMC");

    public static final Path modDirectory = FabricLoader.getInstance().getConfigDir().resolve(HolesailMC.MOD_ID);
    public static boolean skipDownloadScreen = false;

    private static final HolesailManager holesailManager = HolesailManager.getInstance();

    private static final ScheduledExecutorService holesailProcessCleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Holesail process cleaner");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void onInitializeClient() {
        try {
            Files.createDirectories(modDirectory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        holesailProcessCleaner.scheduleAtFixedRate(() -> {
            List<ProcessHandle> processHandles = holesailManager.GetProcesses();

            for (ProcessHandle processHandle : processHandles) {
                ServerData currentServer = Minecraft.getInstance().getCurrentServer();
                boolean isPlayingOnConnector = currentServer != null && currentServer.ip.equals(holesailManager.ExtractConnector(processHandle.info().commandLine().toString()));

                if (isPlayingOnConnector) {
                    return;
                }

                processHandle.destroy();
            }
        }, 30, 30, TimeUnit.SECONDS);

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
            holesailProcessCleaner.shutdownNow();

            for (ProcessHandle processHandle : holesailManager.GetProcesses()) {
                processHandle.destroy();
            }
        });
    }
}