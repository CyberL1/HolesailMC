package cyber.holesailmc.mixin;

import cyber.holesailmc.HolesailMC;
import cyber.holesailmc.HolesailManager;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;

@Mixin(ServerAddress.class)
public class ServerAddressMixin {
    @Unique
    private static final HolesailManager holesailManager = HolesailManager.getInstance();

    @Inject(method = "isValidAddress", at = @At("HEAD"), cancellable = true)
    private static void isValidAddress(String input, CallbackInfoReturnable<Boolean> cir) {
        if (!Files.exists(holesailManager.GetBinaryFile()) || !isHolesailAddress(input)) {
            return;
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "parseString", at = @At("HEAD"), cancellable = true)
    private static void parseString(String input, CallbackInfoReturnable<ServerAddress> cir) {
        if (!Files.exists(holesailManager.GetBinaryFile()) || !isHolesailAddress(input)) {
            return;
        }

        ProcessHandle process = holesailManager.FindProcessFor(input);

        if (process != null) {
            HolesailMC.LOGGER.info("Process already exists for key {}", input);
        } else {
            try {
                process = holesailManager.StartProcessFor(input);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        cir.setReturnValue(new ServerAddress("127.0.0.1", holesailManager.ExtractPort(process.info().commandLine().toString())));
    }

    @Unique
    private static boolean isHolesailAddress(String input) {
        return input.startsWith("hs://");
    }
}