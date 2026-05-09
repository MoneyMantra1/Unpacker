package com.moneymantra.unpacker.client;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.blockentity.UnpackerBlockEntity;
import com.moneymantra.unpacker.menu.UnpackerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class UnpackerScreen extends AbstractContainerScreen<UnpackerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Unpacker.MOD_ID, "textures/gui/unpacker.png");

    private static final int TEXTURE_WIDTH = 220;
    private static final int TEXTURE_HEIGHT = 248;

    private static final int PROGRESS_BAR_X = 34;
    private static final int PROGRESS_BAR_Y = 78;
    private static final int PROGRESS_BAR_WIDTH = 146;
    private static final int PROGRESS_BAR_HEIGHT = 9;
    private static final int PROGRESS_PERCENT_X = 184;
    private static final int PROGRESS_PERCENT_Y = 79;

    private static final int COLOR_ORANGE = 0xFFD78828;
    private static final int COLOR_WARNING = 0xFFE4B55D;
    private static final int COLOR_BLOCKED = 0xFFE08667;
    private static final int COLOR_PROGRESS_BORDER = 0xFF1E232A;
    private static final int COLOR_PROGRESS_BACKGROUND = 0xFF2E3844;
    private static final int COLOR_PROGRESS_GRID = 0xFF435060;
    private static final int COLOR_PROGRESS_FILL = 0xFFD58522;
    private static final int COLOR_PROGRESS_HIGHLIGHT = 0xFFF1B458;
    private static final int COLOR_PROGRESS_SHADOW = 0xFF99501A;

    public UnpackerScreen(UnpackerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.inventoryLabelX = 0;
        this.inventoryLabelY = 0;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawProgressBar(guiGraphics);
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        int barRight = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH;
        int barBottom = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT;

        guiGraphics.fill(PROGRESS_BAR_X - 1, PROGRESS_BAR_Y - 1, barRight + 1, barBottom + 1, COLOR_PROGRESS_BORDER);
        guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, barRight, barBottom, COLOR_PROGRESS_BACKGROUND);

        for(int x = PROGRESS_BAR_X + 7; x < barRight; x += 8) {
            guiGraphics.fill(x, PROGRESS_BAR_Y, x + 1, barBottom, COLOR_PROGRESS_GRID);
        }

        if(shouldRenderProgressBar()) {
            int fillWidth = Math.round(PROGRESS_BAR_WIDTH * getProgress());
            if(fillWidth > 0) {
                int fillRight = Math.min(barRight, PROGRESS_BAR_X + fillWidth);
                guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, fillRight, barBottom, COLOR_PROGRESS_FILL);
                guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, fillRight, PROGRESS_BAR_Y + 1, COLOR_PROGRESS_HIGHLIGHT);
                guiGraphics.fill(PROGRESS_BAR_X, barBottom - 1, fillRight, barBottom, COLOR_PROGRESS_SHADOW);
            }
            guiGraphics.drawString(this.font, getProgressPercentText(), PROGRESS_PERCENT_X, PROGRESS_PERCENT_Y, getProgressTextColor(), false);
        }
    }

    private int getProgressTextColor() {
        int status = this.menu.getStatus();
        return switch(status) {
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> COLOR_WARNING;
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL, UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> COLOR_BLOCKED;
            default -> COLOR_ORANGE;
        };
    }

    private boolean shouldRenderProgressBar() {
        int status = this.menu.getStatus();
        return status == UnpackerBlockEntity.STATUS_WORKING
                || status == UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER
                || status == UnpackerBlockEntity.STATUS_OUTPUT_FULL;
    }

    private String getProgressPercentText() {
        return Math.round(getProgress() * 100.0F) + "%";
    }

    private float getProgress() {
        int status = this.menu.getStatus();
        int starting = this.menu.getStartingItemCount();
        int remaining = this.menu.getRemainingItemCount();

        if(status == UnpackerBlockEntity.STATUS_OUTPUT_FULL && starting > 0) {
            return 1.0F;
        }
        if(starting <= 0) {
            return 0.0F;
        }

        float progress = 1.0F - (remaining / (float)starting);
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
