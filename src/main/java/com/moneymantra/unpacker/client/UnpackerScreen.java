package com.moneymantra.unpacker.client;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.blockentity.UnpackerBlockEntity;
import com.moneymantra.unpacker.menu.UnpackerMenu;
import com.moneymantra.unpacker.util.ContainerItemUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class UnpackerScreen extends AbstractContainerScreen<UnpackerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Unpacker.MOD_ID, "textures/gui/unpacker.png");

    private static final int TEXTURE_WIDTH = 384;
    private static final int TEXTURE_HEIGHT = 256;

    private static final int STATUS_TEXT_X = 137;
    private static final int STATUS_TEXT_Y = 84;
    private static final int STATUS_TEXT_WIDTH = 132;
    private static final int STATUS_PRIMARY_Y = STATUS_TEXT_Y + 10;
    private static final int STATUS_SECONDARY_Y = STATUS_TEXT_Y + 20;

    private static final int PROGRESS_BAR_X = 137;
    private static final int PROGRESS_BAR_Y = 115;
    private static final int PROGRESS_BAR_WIDTH = 104;
    private static final int PROGRESS_BAR_HEIGHT = 5;
    private static final int PROGRESS_PERCENT_X = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH + 7;
    private static final int PROGRESS_PERCENT_Y = PROGRESS_BAR_Y - 2;

    private static final int COLOR_TITLE = 0xFFE7E2D4;
    private static final int COLOR_LABEL = 0xFFD9D2BE;
    private static final int COLOR_LABEL_DIM = 0xFFB0A996;
    private static final int COLOR_PANEL_TEXT = 0xFFECEFF4;
    private static final int COLOR_SECONDARY = 0xFFB6BEC8;
    private static final int COLOR_MUTED = 0xFF69737F;
    private static final int COLOR_ORANGE = 0xFFFFA334;
    private static final int COLOR_CYAN = 0xFF54DDEB;
    private static final int COLOR_WARNING = 0xFFFFC36B;
    private static final int COLOR_BLOCKED = 0xFFFF967A;
    private static final int COLOR_DARK_EDGE = 0xFF06080B;
    private static final int COLOR_PROGRESS_BACKGROUND = 0xFF20262D;
    private static final int COLOR_PROGRESS_GRID = 0xFF0E1217;
    private static final int COLOR_PROGRESS_FILL = 0xFFD58522;
    private static final int COLOR_PROGRESS_HIGHLIGHT = 0xFFFFB34E;
    private static final int COLOR_PROGRESS_SHADOW = 0xFF8B4817;

    public UnpackerScreen(UnpackerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderStaticLabels(guiGraphics);
        renderStatusPanel(guiGraphics);
    }

    private void renderStaticLabels(GuiGraphics guiGraphics) {
        drawCenteredText(guiGraphics, "UNPACKER", this.imageWidth / 2, 5, COLOR_TITLE);

        drawCenteredText(guiGraphics, "INPUT", 36, 44, COLOR_LABEL);
        drawCenteredText(guiGraphics, "TOP SIDE", 36, 54, COLOR_LABEL_DIM);
        drawUpArrow(guiGraphics, 36, 29, COLOR_LABEL);

        drawCenteredText(guiGraphics, "EXTRACT", 36, 107, COLOR_LABEL);
        drawCenteredText(guiGraphics, "BOTTOM", 36, 117, COLOR_LABEL_DIM);
        drawCenteredText(guiGraphics, "SIDE", 36, 127, COLOR_LABEL_DIM);
        drawDownArrow(guiGraphics, 36, 140, COLOR_LABEL);

        drawCenteredText(guiGraphics, "OUTPUT", 328, 82, COLOR_LABEL);
        drawCenteredText(guiGraphics, "FRONT SIDE", 328, 113, COLOR_LABEL_DIM);

        guiGraphics.drawString(this.font, "INPUT", 88, 13, COLOR_LABEL, false);
        guiGraphics.drawString(this.font, "OUTPUT", 88, 118, COLOR_LABEL, false);
        guiGraphics.drawString(this.font, "INVENTORY", 88, 168, COLOR_LABEL, false);
    }

    private void renderStatusPanel(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, "STATUS", STATUS_TEXT_X, STATUS_TEXT_Y, getStatusAccentColor(), false);

        String primary = fitText(getPrimaryStatusLine(), STATUS_TEXT_WIDTH);
        String secondary = fitText(getSecondaryStatusLine(), STATUS_TEXT_WIDTH);

        guiGraphics.drawString(this.font, primary, STATUS_TEXT_X, STATUS_PRIMARY_Y, getPrimaryStatusColor(), false);
        guiGraphics.drawString(this.font, secondary, STATUS_TEXT_X, STATUS_SECONDARY_Y, getSecondaryStatusColor(), false);

        if(shouldRenderProgressBar()) {
            drawProgressBar(guiGraphics);
        } else {
            drawIdleProgressTrack(guiGraphics);
        }
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        int barRight = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH;
        int barBottom = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT;

        guiGraphics.fill(PROGRESS_BAR_X - 1, PROGRESS_BAR_Y - 1, barRight + 1, barBottom + 1, COLOR_DARK_EDGE);
        guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, barRight, barBottom, COLOR_PROGRESS_BACKGROUND);

        for(int x = PROGRESS_BAR_X + 6; x < barRight; x += 7) {
            guiGraphics.fill(x, PROGRESS_BAR_Y, x + 1, barBottom, COLOR_PROGRESS_GRID);
        }

        int fillWidth = Math.round(PROGRESS_BAR_WIDTH * getProgress());
        if(fillWidth > 0) {
            int fillRight = Math.min(barRight, PROGRESS_BAR_X + fillWidth);
            guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, fillRight, barBottom, COLOR_PROGRESS_FILL);
            guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, fillRight, PROGRESS_BAR_Y + 1, COLOR_PROGRESS_HIGHLIGHT);
            guiGraphics.fill(PROGRESS_BAR_X, barBottom - 1, fillRight, barBottom, COLOR_PROGRESS_SHADOW);

            for(int x = PROGRESS_BAR_X + 6; x < fillRight; x += 8) {
                guiGraphics.fill(x, PROGRESS_BAR_Y + 1, x + 1, barBottom - 1, 0x55FFE0A0);
            }
        }

        guiGraphics.drawString(this.font, getProgressPercentText(), PROGRESS_PERCENT_X, PROGRESS_PERCENT_Y, COLOR_ORANGE, false);
    }

    private void drawIdleProgressTrack(GuiGraphics guiGraphics) {
        int barRight = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH;
        int barBottom = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT;
        guiGraphics.fill(PROGRESS_BAR_X - 1, PROGRESS_BAR_Y - 1, barRight + 1, barBottom + 1, COLOR_DARK_EDGE);
        guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, barRight, barBottom, COLOR_PROGRESS_BACKGROUND);
    }

    private String getPrimaryStatusLine() {
        int status = this.menu.getStatus();
        return switch(status) {
            case UnpackerBlockEntity.STATUS_WORKING -> "Unpacking " + getActiveContainerName();
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> "Waiting for hopper";
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL -> "Output full";
            case UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> "Front output blocked";
            default -> "Idle";
        };
    }

    private String getSecondaryStatusLine() {
        int status = this.menu.getStatus();
        int activeSlot = this.menu.getActiveInputSlot();
        int remaining = Math.max(0, this.menu.getRemainingItemCount());

        return switch(status) {
            case UnpackerBlockEntity.STATUS_WORKING -> activeSlot >= 0
                    ? "Slot " + (activeSlot + 1) + "/18 - " + remaining + " left"
                    : remaining + " items remaining";
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> activeSlot >= 0
                    ? "Slot " + (activeSlot + 1) + "/18 - hopper blocked"
                    : "Check bottom hopper/chest";
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL -> "Clear bottom output space";
            case UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> "Clear empty-container output";
            default -> "Insert containers from top";
        };
    }

    private int getStatusAccentColor() {
        int status = this.menu.getStatus();
        return switch(status) {
            case UnpackerBlockEntity.STATUS_WORKING -> COLOR_ORANGE;
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> COLOR_WARNING;
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL, UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> COLOR_BLOCKED;
            default -> COLOR_CYAN;
        };
    }

    private int getPrimaryStatusColor() {
        int status = this.menu.getStatus();
        return switch(status) {
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> COLOR_WARNING;
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL, UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> COLOR_BLOCKED;
            case UnpackerBlockEntity.STATUS_IDLE -> COLOR_MUTED;
            default -> COLOR_PANEL_TEXT;
        };
    }

    private int getSecondaryStatusColor() {
        int status = this.menu.getStatus();
        return status == UnpackerBlockEntity.STATUS_IDLE ? COLOR_MUTED : COLOR_SECONDARY;
    }

    private String getActiveContainerName() {
        ItemStack stack = this.menu.getActiveInputStack();
        return switch(ContainerItemUtil.getKind(stack)) {
            case TRAVELERS_BACKPACK -> "Backpack";
            case SHULKER_BOX -> "Shulker Box";
            case BUNDLE -> "Bundle";
            case UNSUPPORTED -> "Container";
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

    private void drawCenteredText(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        guiGraphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }

    private void drawUpArrow(GuiGraphics guiGraphics, int centerX, int y, int color) {
        guiGraphics.fill(centerX - 1, y + 2, centerX + 2, y + 10, color);
        guiGraphics.fill(centerX - 4, y + 5, centerX + 5, y + 8, color);
        guiGraphics.fill(centerX - 3, y + 4, centerX + 4, y + 5, color);
        guiGraphics.fill(centerX - 2, y + 3, centerX + 3, y + 4, color);
        guiGraphics.fill(centerX - 1, y + 2, centerX + 2, y + 3, color);
    }

    private void drawDownArrow(GuiGraphics guiGraphics, int centerX, int y, int color) {
        guiGraphics.fill(centerX - 1, y, centerX + 2, y + 8, color);
        guiGraphics.fill(centerX - 4, y + 4, centerX + 5, y + 7, color);
        guiGraphics.fill(centerX - 3, y + 7, centerX + 4, y + 8, color);
        guiGraphics.fill(centerX - 2, y + 8, centerX + 3, y + 9, color);
        guiGraphics.fill(centerX - 1, y + 9, centerX + 2, y + 10, color);
    }

    private String fitText(String text, int maxWidth) {
        if(this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        String result = text;
        while(!result.isEmpty() && this.font.width(result + ellipsis) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
