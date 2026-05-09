package com.moneymantra.unpacker.client;

import com.moneymantra.unpacker.Unpacker;
import com.moneymantra.unpacker.blockentity.PackerBlockEntity;
import com.moneymantra.unpacker.menu.PackerMenu;
import com.moneymantra.unpacker.util.ContainerItemUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PackerScreen extends AbstractContainerScreen<PackerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Unpacker.MOD_ID, "textures/gui/packer.png");

    private static final int TEXTURE_WIDTH = 220;
    private static final int TEXTURE_HEIGHT = 248;

    private static final int PROGRESS_BAR_X = 30;
    private static final int PROGRESS_BAR_Y = 91;
    private static final int PROGRESS_BAR_WIDTH = 160;
    private static final int PROGRESS_BAR_HEIGHT = 8;

    private static final int COLOR_PROGRESS_BORDER = 0xFF383838;
    private static final int COLOR_PROGRESS_BACKGROUND = 0xFF445568;
    private static final int COLOR_PROGRESS_GRID = 0xFF5D6E82;
    private static final int COLOR_PROGRESS_FILL = 0xFF4F9E5A;
    private static final int COLOR_PROGRESS_HIGHLIGHT = 0xFF7FD789;
    private static final int COLOR_PROGRESS_SHADOW = 0xFF2F6C38;

    public PackerScreen(PackerMenu menu, Inventory playerInventory, Component title) {
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
        drawTitle(guiGraphics);
        drawSectionLabels(guiGraphics);
        drawStatusText(guiGraphics);
        drawProgressBar(guiGraphics);
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        String text = this.title.getString();
        int x = (this.imageWidth - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, x, 10, 0xFFECEFF4, false);
    }

    private void drawSectionLabels(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, "Items", 29, 62, 0xFF7F8996, false);
        guiGraphics.drawString(this.font, "Containers", 38, 105, 0xFF7F8996, false);
        guiGraphics.drawString(this.font, "Packed Output", 126, 105, 0xFF7F8996, false);
    }

    private void drawStatusText(GuiGraphics guiGraphics) {
        String primary = getPrimaryStatusText();
        String secondary = getSecondaryStatusText();
        int primaryColor = getPrimaryStatusColor();

        guiGraphics.drawString(this.font, primary, 30, 70, primaryColor, false);
        guiGraphics.drawString(this.font, secondary, 30, 80, 0xFFB9C0C8, false);

        if(shouldRenderProgressBar()) {
            String percent = Math.max(0, Math.min(100, this.menu.getFillPercent())) + "%";
            guiGraphics.drawString(this.font, percent, 192 - this.font.width(percent), 90, 0xFFECEFF4, false);
        }
    }

    private String getPrimaryStatusText() {
        int status = this.menu.getStatus();
        ItemStack activeStack = this.menu.getActiveContainerStack();

        return switch(status) {
            case PackerBlockEntity.STATUS_WORKING -> "Packing " + ContainerItemUtil.getKindDisplayName(activeStack);
            case PackerBlockEntity.STATUS_WAITING_FOR_ITEMS -> "Waiting for items...";
            case PackerBlockEntity.STATUS_WAITING_FOR_CONTAINER -> "Waiting for container...";
            case PackerBlockEntity.STATUS_OUTPUT_FULL -> "Output full";
            case PackerBlockEntity.STATUS_NO_ITEM_FITS -> "No input item fits";
            default -> "Idle";
        };
    }

    private String getSecondaryStatusText() {
        int status = this.menu.getStatus();
        int slot = this.menu.getActiveContainerSlot() - PackerBlockEntity.CONTAINER_START + 1;
        int packed = this.menu.getPackedItemCount();

        return switch(status) {
            case PackerBlockEntity.STATUS_WORKING -> "Slot " + slot + "/6 • " + packed + " packed";
            case PackerBlockEntity.STATUS_WAITING_FOR_ITEMS -> "Add loose items from the top";
            case PackerBlockEntity.STATUS_WAITING_FOR_CONTAINER -> "Insert backpacks, shulkers, or bundles";
            case PackerBlockEntity.STATUS_OUTPUT_FULL -> "Remove packed containers below";
            case PackerBlockEntity.STATUS_NO_ITEM_FITS -> "Moving container to output";
            default -> "Insert items and containers";
        };
    }

    private int getPrimaryStatusColor() {
        int status = this.menu.getStatus();
        return switch(status) {
            case PackerBlockEntity.STATUS_OUTPUT_FULL -> 0xFFFF8E7A;
            case PackerBlockEntity.STATUS_WAITING_FOR_ITEMS, PackerBlockEntity.STATUS_WAITING_FOR_CONTAINER, PackerBlockEntity.STATUS_NO_ITEM_FITS -> 0xFFFFD28A;
            default -> 0xFFECEFF4;
        };
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
        }
    }

    private boolean shouldRenderProgressBar() {
        int status = this.menu.getStatus();
        return status == PackerBlockEntity.STATUS_WORKING
                || status == PackerBlockEntity.STATUS_WAITING_FOR_ITEMS
                || status == PackerBlockEntity.STATUS_OUTPUT_FULL
                || status == PackerBlockEntity.STATUS_NO_ITEM_FITS;
    }

    private float getProgress() {
        return Math.max(0.0F, Math.min(1.0F, this.menu.getFillPercent() / 100.0F));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
