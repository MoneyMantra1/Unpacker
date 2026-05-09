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

    private static final int TEXTURE_WIDTH = 220;
    private static final int TEXTURE_HEIGHT = 240;

    private static final int STATUS_PANEL_X = 11;
    private static final int STATUS_PANEL_Y = 63;
    private static final int STATUS_PANEL_WIDTH = 198;
    private static final int STATUS_PANEL_HEIGHT = 43;

    private static final int STATUS_TEXT_X = 58;
    private static final int STATUS_TEXT_Y = STATUS_PANEL_Y + 6;
    private static final int STATUS_TEXT_WIDTH = 126;
    private static final int STATUS_PRIMARY_Y = STATUS_TEXT_Y + 10;
    private static final int STATUS_SECONDARY_Y = STATUS_TEXT_Y + 20;

    private static final int PROGRESS_BAR_X = STATUS_TEXT_X;
    private static final int PROGRESS_BAR_Y = STATUS_PANEL_Y + 35;
    private static final int PROGRESS_BAR_WIDTH = 120;
    private static final int PROGRESS_BAR_HEIGHT = 5;
    private static final int PROGRESS_PERCENT_X = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH + 7;
    private static final int PROGRESS_PERCENT_Y = PROGRESS_BAR_Y - 2;

    private static final int COLOR_TITLE = 0xFFE7E2D4;
    private static final int COLOR_LABEL = 0xFFD8D0BD;
    private static final int COLOR_LABEL_DIM = 0xFF8E98A2;
    private static final int COLOR_PANEL_TEXT = 0xFFECEFF4;
    private static final int COLOR_SECONDARY = 0xFFABB5C0;
    private static final int COLOR_MUTED = 0xFF68737E;
    private static final int COLOR_ORANGE = 0xFFFFA334;
    private static final int COLOR_CYAN = 0xFF56DCEB;
    private static final int COLOR_WARNING = 0xFFFFC36B;
    private static final int COLOR_BLOCKED = 0xFFFF967A;
    private static final int COLOR_DARK_EDGE = 0xFF05070A;
    private static final int COLOR_PROGRESS_BACKGROUND = 0xFF20262D;
    private static final int COLOR_PROGRESS_GRID = 0xFF0D1116;
    private static final int COLOR_PROGRESS_FILL = 0xFFD58522;
    private static final int COLOR_PROGRESS_HIGHLIGHT = 0xFFFFB34E;
    private static final int COLOR_PROGRESS_SHADOW = 0xFF8B4817;

    public UnpackerScreen(UnpackerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.inventoryLabelX = 29;
        this.inventoryLabelY = 157;
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
        drawCenteredText(guiGraphics, "UNPACKER", this.imageWidth / 2, 8, COLOR_TITLE);

        guiGraphics.drawString(this.font, "INPUT", 29, 13, COLOR_LABEL, false);
        guiGraphics.drawString(this.font, "TOP INSERT", 142, 13, COLOR_LABEL_DIM, false);

        guiGraphics.drawString(this.font, "OUTPUT", 29, 112, COLOR_LABEL, false);
        guiGraphics.drawString(this.font, "BOTTOM EXTRACT", 124, 112, COLOR_LABEL_DIM, false);

        guiGraphics.drawString(this.font, "INVENTORY", this.inventoryLabelX, this.inventoryLabelY, COLOR_LABEL, false);
    }

    private void renderStatusPanel(GuiGraphics guiGraphics) {
        renderStatusIcon(guiGraphics);

        guiGraphics.drawString(this.font, "STATUS", STATUS_TEXT_X, STATUS_TEXT_Y, getStatusAccentColor(), false);

        String primary = fitText(getPrimaryStatusLine(), STATUS_TEXT_WIDTH);
        String secondary = fitText(getSecondaryStatusLine(), STATUS_TEXT_WIDTH);

        guiGraphics.drawString(this.font, primary, STATUS_TEXT_X, STATUS_PRIMARY_Y, getPrimaryStatusColor(), false);
        guiGraphics.drawString(this.font, secondary, STATUS_TEXT_X, STATUS_SECONDARY_Y, getSecondaryStatusColor(), false);

        drawProgressBar(guiGraphics);
    }

    private void renderStatusIcon(GuiGraphics guiGraphics) {
        int x = 22;
        int y = STATUS_PANEL_Y + 10;
        int accent = getStatusAccentColor();

        guiGraphics.fill(x, y + 4, x + 25, y + 23, 0xFF0A0D11);
        guiGraphics.fill(x + 1, y + 5, x + 24, y + 22, 0xFF1B222A);
        guiGraphics.fill(x + 4, y + 8, x + 21, y + 19, 0xFF2A3139);
        guiGraphics.fill(x + 6, y + 10, x + 19, y + 17, 0xFF12171D);
        guiGraphics.fill(x + 8, y + 11, x + 17, y + 16, accent);
        guiGraphics.fill(x + 9, y + 11, x + 16, y + 12, 0x55FFFFFF);
        guiGraphics.fill(x + 8, y + 16, x + 17, y + 17, 0x66000000);
        guiGraphics.fill(x + 2, y + 2, x + 8, y + 5, 0xFF333B44);
        guiGraphics.fill(x + 17, y + 2, x + 23, y + 5, 0xFF333B44);
        guiGraphics.fill(x + 3, y + 23, x + 22, y + 25, accent);
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        int barRight = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH;
        int barBottom = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT;

        guiGraphics.fill(PROGRESS_BAR_X - 1, PROGRESS_BAR_Y - 1, barRight + 1, barBottom + 1, COLOR_DARK_EDGE);
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

            guiGraphics.drawString(this.font, getProgressPercentText(), PROGRESS_PERCENT_X, PROGRESS_PERCENT_Y, COLOR_ORANGE, false);
        }
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
                    : "Check bottom output";
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL -> "Clear bottom output";
            case UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> "Clear front output";
            default -> "Insert containers above";
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
