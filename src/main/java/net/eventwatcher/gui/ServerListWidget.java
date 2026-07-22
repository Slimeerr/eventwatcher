package net.eventwatcher.gui;

import java.util.List;
import java.util.function.IntConsumer;
import net.eventwatcher.config.EventWatcherConfig.ServerEntry;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class ServerListWidget extends ClickableWidget {
   private static final int ITEM_HEIGHT = 24;
   private final TextRenderer textRenderer;
   private final List<ServerEntry> servers;
   private final IntConsumer onEdit;
   private final IntConsumer onRemove;
   private int scroll = 0;

   public ServerListWidget(
      TextRenderer textRenderer, int x, int y, int width, int height, List<ServerEntry> servers, IntConsumer onEdit, IntConsumer onRemove
   ) {
      super(x, y, width, height, Text.literal("Watched servers"));
      this.textRenderer = textRenderer;
      this.servers = servers;
      this.onEdit = onEdit;
      this.onRemove = onRemove;
   }

   private int contentHeight() {
      return this.servers.size() * ITEM_HEIGHT;
   }

   private int maxScroll() {
      return Math.max(0, this.contentHeight() - this.getHeight());
   }

   private void clampScroll() {
      if (this.scroll > this.maxScroll()) {
         this.scroll = this.maxScroll();
      }

      if (this.scroll < 0) {
         this.scroll = 0;
      }
   }

   private int rowY(int i) {
      return this.getY() + 2 - this.scroll + i * ITEM_HEIGHT;
   }

   private boolean isOverRemove(double mouseX, double mouseY, int rowY) {
      int rx = this.getX() + this.getWidth() - 16;
      return mouseX >= rx - 2 && mouseX <= rx + 12 && mouseY >= rowY + 4 && mouseY <= rowY + 18;
   }

   protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      this.clampScroll();
      context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), Integer.MIN_VALUE);
      context.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());

      for (int i = 0; i < this.servers.size(); i++) {
         int iy = this.rowY(i);
         if (iy + ITEM_HEIGHT >= this.getY() && iy <= this.getY() + this.getHeight()) {
            ServerEntry entry = this.servers.get(i);
            boolean overRow = mouseX >= this.getX()
               && mouseX <= this.getX() + this.getWidth() - 18
               && mouseY >= iy
               && mouseY <= iy + ITEM_HEIGHT;
            if (overRow) {
               context.fill(this.getX(), iy, this.getX() + this.getWidth(), iy + ITEM_HEIGHT, 553648127);
            }

            int kwCount = entry.keywords == null ? 0 : entry.keywords.size();
            int chCount = entry.channels().size();
            String sub = (entry.autoConnect ? "Auto-Connect" : "Notify Me")
               + " · " + kwCount + (kwCount == 1 ? " keyword" : " keywords")
               + " · " + chCount + (chCount == 1 ? " channel" : " channels");
            context.drawText(this.textRenderer, this.trim(entry.displayName(), this.getWidth() - 24), this.getX() + 4, iy + 3, -1, false);
            context.drawText(this.textRenderer, this.trim(sub, this.getWidth() - 24), this.getX() + 4, iy + 13, -7303024, false);
            boolean overRemove = this.isOverRemove(mouseX, mouseY, iy);
            int rx = this.getX() + this.getWidth() - 16;
            context.fill(rx - 2, iy + 4, rx + 12, iy + 18, overRemove ? -2130750123 : 1073741824);
            context.drawText(this.textRenderer, "X", rx + 1, iy + 8, overRemove ? -43691 : -21846, false);
         }
      }

      context.disableScissor();
      if (this.maxScroll() > 0) {
         int trackX = this.getX() + this.getWidth() - 2;
         context.fill(trackX, this.getY(), trackX + 2, this.getY() + this.getHeight(), 1090519039);
         int barH = Math.max(10, this.getHeight() * this.getHeight() / this.contentHeight());
         int barY = this.getY() + (int)((this.getHeight() - barH) * ((float)this.scroll / this.maxScroll()));
         context.fill(trackX, barY, trackX + 2, barY + barH, -5592406);
      }
   }

   private String trim(String text, int maxWidth) {
      if (this.textRenderer.getWidth(text) <= maxWidth) {
         return text;
      }

      return this.textRenderer.trimToWidth(text, maxWidth - this.textRenderer.getWidth("...")) + "...";
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      double mouseX = click.x();
      double mouseY = click.y();
      if (click.button() == 0 && this.isMouseOver(mouseX, mouseY)) {
         for (int i = 0; i < this.servers.size(); i++) {
            int iy = this.rowY(i);
            if (mouseY >= iy && mouseY <= iy + ITEM_HEIGHT) {
               if (this.isOverRemove(mouseX, mouseY, iy)) {
                  this.onRemove.accept(i);
                  this.clampScroll();
               } else {
                  this.onEdit.accept(i);
               }

               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (!this.isMouseOver(mouseX, mouseY)) {
         return false;
      } else {
         this.scroll -= (int)(verticalAmount * ITEM_HEIGHT);
         this.clampScroll();
         return true;
      }
   }

   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      builder.put(NarrationPart.TITLE, Text.literal("Watched servers list"));
   }
}
