package cyber.holesailmc.mixin;

import cyber.holesailmc.HolesailMC;
import cyber.holesailmc.HolesailManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerStatusPinger.class)
public class ServerStatusPingerMixin {
    @Unique
    private static final HolesailManager holesailManager = HolesailManager.getInstance();

    @Inject(method = "pingServer", at = @At(("HEAD")))
    private static void onPingServer(ServerData data, Runnable onPersistentDataChange, Runnable onPongResponse, EventLoopGroupHolder eventLoopGroupHolder, CallbackInfo ci) {
        if (data == null || !data.ip.startsWith("hs://")) {
            return;
        }

        ProcessHandle process = holesailManager.FindProcessFor(data.ip);

        if (process != null) {
            HolesailMC.LOGGER.info("Process already exists for key {}", data.ip);
            return;
        }

        holesailManager.StartProcessFor(data.ip);
    }
}
