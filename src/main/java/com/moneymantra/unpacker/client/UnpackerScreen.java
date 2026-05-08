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

    private static final int PANEL_X = 6;
    private static final int PANEL_Y = 56;
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 33;

    private static final int PRIMARY_TEXT_X = PANEL_X + 5;
    private static final int PRIMARY_TEXT_Y = PANEL_Y + 4;
    private static final int SECONDARY_TEXT_X = PANEL_X + 5;
    private static final int SECONDARY_TEXT_Y = PANEL_Y + 14;
    private static final int TEXT_WIDTH = PANEL_WIDTH - 10;

    private static final int PROGRESS_BAR_X = PANEL_X + 5;
    private static final int PROGRESS_BAR_Y = PANEL_Y + 25;
    private static final int PROGRESS_BAR_WIDTH = 132;
    private static final int PROGRESS_BAR_HEIGHT = 5;
    private static final int PROGRESS_PERCENT_X = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH + 6;
    private static final int PROGRESS_PERCENT_Y = PANEL_Y + 22;

    private static final int COLOR_PANEL_BORDER_DARK = 0xFF07090C;
    private static final int COLOR_PANEL_BORDER_LIGHT = 0xFF3C444F;
    private static final int COLOR_PANEL_BACKGROUND = 0xFF171C23;
    private static final int COLOR_PRIMARY = 0xFFECEFF4;
    private static final int COLOR_SECONDARY = 0xFF9AA3AE;
    private static final int COLOR_MUTED = 0xFF68717D;
    private static final int COLOR_WARNING = 0xFFE0B15C;
    private static final int COLOR_BLOCKED = 0xFFFF9B7A;
    private static final int COLOR_PROGRESS_BACKGROUND = 0xFF252A31;
    private static final int COLOR_PROGRESS_FILL = 0xFFC98D3A;
    private static final int COLOR_PROGRESS_HIGHLIGHT = 0xFFE0B15C;

    public UnpackerScreen(UnpackerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.inventoryLabelY = 136;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        renderStatusPanel(guiGraphics);
    }

    private void renderStatusPanel(GuiGraphics guiGraphics) {
        drawPanelBackground(guiGraphics);

        String primary = fitText(getPrimaryStatusLine(), TEXT_WIDTH);
        String secondary = fitText(getSecondaryStatusLine(), TEXT_WIDTH);

        guiGraphics.drawString(this.font, primary, PRIMARY_TEXT_X, PRIMARY_TEXT_Y, getPrimaryStatusColor(), false);
        guiGraphics.drawString(this.font, secondary, SECONDARY_TEXT_X, SECONDARY_TEXT_Y, getSecondaryStatusColor(), false);

        if(shouldRenderProgressBar()) {
            drawProgressBar(guiGraphics);
        }
    }

    private void drawPanelBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_WIDTH, PANEL_Y + PANEL_HEIGHT, COLOR_PANEL_BORDER_DARK);
        guiGraphics.fill(PANEL_X + 1, PANEL_Y + 1, PANEL_X + PANEL_WIDTH - 1, PANEL_Y + PANEL_HEIGHT - 1, COLOR_PANEL_BACKGROUND);
        guiGraphics.fill(PANEL_X + 1, PANEL_Y + 1, PANEL_X + PANEL_WIDTH - 1, PANEL_Y + 2, COLOR_PANEL_BORDER_LIGHT);
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        int barRight = PROGRESS_BAR_X + PROGRESS_BAR_WIDTH;
        int barBottom = PROGRESS_BAR_Y + PROGRESS_BAR_HEIGHT;

        guiGraphics.fill(PROGRESS_BAR_X - 1, PROGRESS_BAR_Y - 1, barRight + 1, barBottom + 1, COLOR_PANEL_BORDER_DARK);
        guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, barRight, barBottom, COLOR_PROGRESS_BACKGROUND);

        int fillWidth = Math.round(PROGRESS_BAR_WIDTH * getProgress());
        if(fillWidth > 0) {
            guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_X + fillWidth, barBottom, COLOR_PROGRESS_FILL);
            guiGraphics.fill(PROGRESS_BAR_X, PROGRESS_BAR_Y, PROGRESS_BAR_X + fillWidth, PROGRESS_BAR_Y + 1, COLOR_PROGRESS_HIGHLIGHT);
        }

        String percent = getProgressPercentText();
        guiGraphics.drawString(this.font, percent, PROGRESS_PERCENT_X, PROGRESS_PERCENT_Y, COLOR_SECONDARY, false);
    }

    private String getPrimaryStatusLine() {
        int status = this.menu.getStatus();
        return switch(status) {
            case UnpackerBlockEntity.STATUS_WORKING -> "Unpacking " + getActiveContainerName();
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> "Waiting for hopper...";
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
                    ? "Slot " + (activeSlot + 1) + "/18 • " + remaining + " left"
                    : remaining + " items remaining";
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> activeSlot >= 0
                    ? "Slot " + (activeSlot + 1) + "/18 • " + remaining + " left"
                    : "Check bottom hopper/chest";
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL -> "Remove empty containers";
            case UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> "Clear the front inventory";
            default -> "Insert containers to unpack";
        };
    }

    private int getPrimaryStatusColor() {
        int status = this.menu.getStatus();
        return switch(status) {
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> COLOR_WARNING;
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL, UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> COLOR_BLOCKED;
            case UnpackerBlockEntity.STATUS_IDLE -> COLOR_MUTED;
            default -> COLOR_PRIMARY;
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
