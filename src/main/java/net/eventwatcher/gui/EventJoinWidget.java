package net.eventwatcher.gui;

import net.eventwatcher.notify.AvatarCache;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Title-screen "join this event" button that also shows the poster's name and avatar. */
public class EventJoinWidget extends ClickableWidget {
   private final TextRenderer textRenderer;
   private final String serverName;
   private final String authorName;
   private final String authorId;
   private final String avatarHash;
   private final Runnable onPress;

   public EventJoinWidget(
      TextRenderer textRenderer, int x, int y, int width, int height, String serverName, String authorName, String authorId, String avatarHash, Runnable onPress
   ) {
      super(x, y, width, height, Text.literal("Join " + serverName));
      this.textRenderer = textRenderer;
      this.serverName = serverName;
      this.authorName = authorName == null ? "" : authorName;
      this.authorId = authorId;
      this.avatarHash = avatarHash;
      this.onPress = onPress;
   }

   protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      boolean hovered = this.isHovered();
      int bg = hovered ? -1607454603 : -1610612736;
      context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bg);
      int border = hovered ? -8323073 : -12961222;
      context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + 1, border);
      context.fill(this.getX(), this.getY() + this.getHeight() - 1, this.getX() + this.getWidth(), this.getY() + this.getHeight(), border);
      int textX = this.getX() + 6;
      int avatarSize = this.getHeight() - 6;
      Identifier avatar = AvatarCache.get(this.authorId, this.avatarHash);
      if (avatar != null) {
         try {
            int src = AvatarCache.size();
            context.drawTexture(
               RenderPipelines.GUI_TEXTURED, avatar, this.getX() + 3, this.getY() + 3, 0.0F, 0.0F, avatarSize, avatarSize, src, src, src, src
            );
            textX = this.getX() + 3 + avatarSize + 5;
         } catch (Exception var10) {
            textX = this.getX() + 6;
         }
      }

      String label = this.authorName.isBlank() ? "Join " + this.serverName : this.authorName + " — join " + this.serverName;
      int maxWidth = this.getX() + this.getWidth() - 6 - textX;
      String shown = this.textRenderer.getWidth(label) <= maxWidth
         ? label
         : this.textRenderer.trimToWidth(label, maxWidth - this.textRenderer.getWidth("...")) + "...";
      int textY = this.getY() + (this.getHeight() - 8) / 2;
      context.drawText(this.textRenderer, shown, textX, textY, -1, true);
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (click.button() == 0 && this.isMouseOver(click.x(), click.y())) {
         this.onPress.run();
         return true;
      }

      return false;
   }

   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      builder.put(NarrationPart.TITLE, Text.literal("Join event on " + this.serverName));
   }
}
