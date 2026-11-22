package cyber.holesailmc.gui.screens;

import cyber.holesailmc.HolesailMC;
import cyber.holesailmc.HolesailManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.NonNull;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

public class DownloadHolesailScreen extends Screen {
    private Button downloadButton;
    private Button skipButton;

    private boolean downloading = false;

    public DownloadHolesailScreen() {
        super(Component.literal("Download Holesail"));
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 150;
        int buttonHeight = 20;

        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 20;

        downloadButton = Button.builder(Component.literal("Download"), button -> startDownload())
                .bounds(centerX - buttonWidth - 6, buttonY, buttonWidth, buttonHeight).build();

        skipButton = Button.builder(Component.literal("Skip"), button -> this.onClose())
                .bounds(centerX + 6, buttonY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(downloadButton);
        this.addRenderableWidget(skipButton);
    }

    private void startDownload() {
        downloading = true;

        downloadButton.active = false;
        skipButton.active = false;

        HolesailManager holesailManager = HolesailManager.getInstance();

        holesailManager.DownloadHolesail().thenRunAsync(this::onClose, Minecraft.getInstance()).exceptionally(e -> {
            HolesailMC.LOGGER.error("Failed to download Holesail: {}.\n{}", e, e.getCause());
            downloading = false;

            Minecraft.getInstance().execute(() -> {
                downloadButton.active = true;
                skipButton.active = true;
            });

            return null;
        });
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);


        graphics.centeredText(
                this.font,
                this.title,
                this.width / 2,
                this.height / 2 - 20,
                0xFFFFFFFF
        );

        graphics.centeredText(this.font, "HolesailMC is powered by Holesail. Do you want to download it?", this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);


        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.height / 2 - 20,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(this.font, "HolesailMC is powered by Holesail. Do you want to download it?", this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
    }
    *///?}

    @Override
    public void onClose() {
        Minecraft minecraft = Minecraft.getInstance();

        //? if >=26.1 {
        minecraft.gui.setScreen(new TitleScreen());
        //?} else {
        /*minecraft.setScreen(new TitleScreen());
         *///?}
    }

    public boolean shouldCloseOnEsc() {
        return !downloading;
    }
}
