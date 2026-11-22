package cyber.holesailmc.mixin;

import cyber.holesailmc.HolesailMC;
import cyber.holesailmc.HolesailManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.11
import net.minecraft.server.network.EventLoopGroupHolder;

@Mixin(ServerStatusPinger.class)
public class ServerStatusPingerMixin {
    @Unique
    private static final HolesailManager holesailManager = HolesailManager.getInstance();

    //? if >=1.21.11 {
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

    //?} else {
    /*@Inject(method = "pingServer", at = @At(("HEAD")))
    private static void onPingServer(ServerData data, Runnable runnable, Runnable runnable2, CallbackInfo ci) {
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
    *///?}
}
