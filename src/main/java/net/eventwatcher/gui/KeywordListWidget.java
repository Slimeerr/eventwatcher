package net.eventwatcher.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class KeywordListWidget extends ClickableWidget {
   private static final int ITEM_HEIGHT = 18;
   private final TextRenderer textRenderer;
   private final List<String> keywords;
   private final Consumer<String> onRemove;
   private int scroll = 0;

   public KeywordListWidget(TextRenderer textRenderer, int x, int y, int width, int height, List<String> keywords, Consumer<String> onRemove) {
      super(x, y, width, height, Text.literal("Monitored keywords"));
      this.textRenderer = textRenderer;
      this.keywords = keywords;
      this.onRemove = onRemove;
   }

   private int contentHeight() {
      return this.keywords.size() * 18;
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
      return this.getY() + 2 - this.scroll + i * 18;
   }

   private boolean isOverRemove(double mouseX, double mouseY, int rowY) {
      int rx = this.getX() + this.getWidth() - 16;
      return mouseX >= rx - 2 && mouseX <= rx + 12 && mouseY >= rowY + 1 && mouseY <= rowY + 15;
   }

   protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      this.clampScroll();
      context.fill(
         this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), Integer.MIN_VALUE
      );
      context.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());

      for (int i = 0; i < this.keywords.size(); i++) {
         int iy = this.rowY(i);
         if (iy + 18 >= this.getY() && iy <= this.getY() + this.getHeight()) {
            String kw = this.keywords.get(i);
            context.drawText(this.textRenderer, kw, this.getX() + 4, iy + 5, -1, false);
            boolean overRemove = this.isOverRemove(mouseX, mouseY, iy);
            int rx = this.getX() + this.getWidth() - 16;
            context.fill(rx - 2, iy + 1, rx + 12, iy + 15, overRemove ? -2130750123 : 1073741824);
            context.drawText(this.textRenderer, "X", rx + 1, iy + 5, overRemove ? -43691 : -21846, false);
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

   public boolean mouseClicked(Click click, boolean doubled) {
      double mouseX = click.x();
      double mouseY = click.y();
      if (click.button() == 0 && this.isMouseOver(mouseX, mouseY)) {
         for (int i = 0; i < this.keywords.size(); i++) {
            if (this.isOverRemove(mouseX, mouseY, this.rowY(i))) {
               this.onRemove.accept(this.keywords.get(i));
               this.clampScroll();
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
         this.scroll -= (int)(verticalAmount * 18.0);
         this.clampScroll();
         return true;
      }
   }

   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      builder.put(NarrationPart.TITLE, Text.literal("Monitored keywords list"));
   }
}
