package cyber.holesailmc.mixin;

import cyber.holesailmc.HolesailManager;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private static void parseString(String input, CallbackInfoReturnable<ServerAddress> cir) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (!Files.exists(holesailManager.GetBinaryFile()) || !isHolesailAddress(input)) {
            return;
        }

        ProcessHandle processHandle = holesailManager.FindProcessFor(input);

        if (processHandle == null) {
            Process process = holesailManager.StartProcessFor(input);
            CompletableFuture<Boolean> connected = new CompletableFuture<>();

            Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = process.inputReader()) {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null && !line.contains("Connected")) {
                            connected.complete(true);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            connected.get(15, TimeUnit.SECONDS);
            processHandle = process.toHandle();
        }

        cir.setReturnValue(new ServerAddress("127.0.0.1", holesailManager.ExtractPort(processHandle.info().commandLine().toString())));
    }

    @Unique
    private static boolean isHolesailAddress(String input) {
        return input.startsWith("hs://");
    }
}