package cyber.holesailmc.mixin;

import cyber.holesailmc.HolesailMC;
import cyber.holesailmc.HolesailManager;
import cyber.holesailmc.gui.screens.DownloadHolesailScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Unique
    private static final HolesailManager holesailManager = HolesailManager.getInstance();

    @Inject(method = "init", at = @At("TAIL"))
    private static void onInit(CallbackInfo ci) {
        if (Files.exists(holesailManager.GetBinaryFile())) {
            HolesailMC.skipDownloadScreen = true;
        }

        if (!HolesailMC.skipDownloadScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            HolesailMC.skipDownloadScreen = true;

            //? if >=26.1 {
            minecraft.setScreenAndShow(new DownloadHolesailScreen());
            //?} else {
            /*minecraft.setScreen(new DownloadHolesailScreen());
             *///?}
        }
    }
}
