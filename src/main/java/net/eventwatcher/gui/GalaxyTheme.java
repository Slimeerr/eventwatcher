package net.eventwatcher.gui;

import java.util.Random;
import net.eventwatcher.util.GradientText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Shared "galaxy" look for EventWatcher screens: gradient panels, a twinkling starfield, gradient titles. */
public final class GalaxyTheme {
   private static final int PANEL_TOP = 0xEE0A0620;
   private static final int PANEL_BOTTOM = 0xEE1E0B44;
   private static final int BORDER = 0xFF7C5CFF;
   private static final int SEPARATOR = 0x66B49CFF;
   private static final int STAR_COUNT = 46;
   private static final int[] TITLE_STOPS = {0x74F0FF, 0x6C8CFF, 0xB06CFF, 0xF060E0};

   private GalaxyTheme() {
   }

   /** Draws the galaxy panel background, border, and twinkling stars. */
   public static void panel(DrawContext context, int x, int y, int w, int h) {
      context.fillGradient(x, y, x + w, y + h, PANEL_TOP, PANEL_BOTTOM);
      stars(context, x, y, w, h);
      context.fill(x, y, x + w, y + 1, BORDER);
      context.fill(x, y + h - 1, x + w, y + h, BORDER);
      context.fill(x, y, x + 1, y + h, BORDER);
      context.fill(x + w - 1, y, x + w, y + h, BORDER);
   }

   /** A faint vertical divider that matches the theme. */
   public static void separator(DrawContext context, int x, int top, int bottom) {
      context.fill(x, top, x + 1, bottom, SEPARATOR);
   }

   /** Draws a title using the galaxy gradient, centered on centerX. */
   public static void title(DrawContext context, TextRenderer textRenderer, String text, int centerX, int y) {
      Text gradient = GradientText.of(text, TITLE_STOPS);
      int width = textRenderer.getWidth(text);
      context.drawText(textRenderer, gradient, centerX - width / 2, y, 0xFFFFFFFF, true);
   }

   private static void stars(DrawContext context, int x, int y, int w, int h) {
      if (w <= 6 || h <= 6) {
         return;
      }

      long time = System.currentTimeMillis();
      Random rng = new Random(0x5747L);
      for (int i = 0; i < STAR_COUNT; i++) {
         int sx = x + 3 + rng.nextInt(w - 6);
         int sy = y + 3 + rng.nextInt(h - 6);
         float phase = rng.nextFloat() * 6.2832F;
         float twinkle = 0.5F + 0.5F * (float)Math.sin(time / 480.0 + phase);
         int alpha = (int)(70 + 165 * twinkle);
         int rgb = i % 6 == 0 ? 0x9BE0FF : i % 9 == 0 ? 0xE0A6FF : 0xFFFFFF;
         int color = alpha << 24 | rgb;
         context.fill(sx, sy, sx + 1, sy + 1, color);
         if (twinkle > 0.82F) {
            int faint = alpha / 2 << 24 | rgb;
            context.fill(sx - 1, sy, sx + 2, sy + 1, faint);
            context.fill(sx, sy - 1, sx + 1, sy + 2, faint);
         }
      }
   }
}
