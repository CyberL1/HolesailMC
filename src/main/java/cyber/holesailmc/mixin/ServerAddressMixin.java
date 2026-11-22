package cyber.holesailmc.mixin;

import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerAddress.class)
public class ServerAddressMixin {
    @Inject(method = "isValidAddress", at = @At("HEAD"), cancellable = true)
    private static void isValidAddress(String input, CallbackInfoReturnable<Boolean> cir) {
        if (isHolesailAddress(input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "parseString", at = @At("HEAD"), cancellable = true)
    private static void parseString(String input, CallbackInfoReturnable<ServerAddress> cir) {
        if (isHolesailAddress(input)) {
            cir.setReturnValue(new ServerAddress("127.0.0.1", 25565));
        }
    }

    @Unique
    private static boolean isHolesailAddress(String input) {
        return input.startsWith("hs://");
    }
}