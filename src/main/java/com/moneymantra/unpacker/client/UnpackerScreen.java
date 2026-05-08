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

    private static final int STATUS_TEXT_X = 8;
    private static final int STATUS_TEXT_Y = 60;
    private static final int STATUS_TEXT_WIDTH = 160;
    private static final int PROGRESS_BAR_X = 8;
    private static final int PROGRESS_BAR_Y = 74;
    private static final int PROGRESS_BAR_WIDTH = 160;
    private static final int PROGRESS_BAR_HEIGHT = 7;

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
        String statusText = getStatusText();
        String fittedStatusText = fitText(statusText, STATUS_TEXT_WIDTH);
        int textX = STATUS_TEXT_X + Math.max(0, (STATUS_TEXT_WIDTH - this.font.width(fittedStatusText)) / 2);
        guiGraphics.drawString(this.font, fittedStatusText, textX, STATUS_TEXT_Y, 0xFFD8DEE9, false);

        int barX = PROGRESS_BAR_X;
        int barY = PROGRESS_BAR_Y;
        int barRight = barX + PROGRESS_BAR_WIDTH;
        int barBottom = barY + PROGRESS_BAR_HEIGHT;

        guiGraphics.fill(barX - 1, barY - 1, barRight + 1, barBottom + 1, 0xFF101216);
        guiGraphics.fill(barX, barY, barRight, barBottom, 0xFF252A31);

        int fillWidth = Math.round(PROGRESS_BAR_WIDTH * getProgress());
        if(fillWidth > 0) {
            guiGraphics.fill(barX, barY, barX + fillWidth, barBottom, 0xFFC98D3A);
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + 1, 0xFFE0B15C);
        }
    }

    private String getStatusText() {
        int status = this.menu.getStatus();
        int activeSlot = this.menu.getActiveInputSlot();
        int remaining = Math.max(0, this.menu.getRemainingItemCount());

        return switch(status) {
            case UnpackerBlockEntity.STATUS_WORKING -> activeSlot >= 0
                    ? "Unpacking slot " + (activeSlot + 1) + " / 18 | " + remaining + " items remaining"
                    : "Unpacking...";
            case UnpackerBlockEntity.STATUS_WAITING_FOR_HOPPER -> remaining > 0
                    ? "Waiting for hopper... | " + remaining + " items remaining"
                    : "Waiting for hopper...";
            case UnpackerBlockEntity.STATUS_OUTPUT_FULL -> "Output full — remove empty containers";
            case UnpackerBlockEntity.STATUS_FRONT_OUTPUT_BLOCKED -> "Front output blocked";
            default -> "Idle";
        };
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
