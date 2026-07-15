package net.eventwatcher.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class GradientText {
   public static final int[] EVENT_GRADIENT = new int[]{16731491, 16751421, 14834640, 10181631};

   private GradientText() {
   }

   public static MutableText of(String text) {
      return of(text, EVENT_GRADIENT);
   }

   public static MutableText of(String text, int[] stops) {
      MutableText result = Text.empty();
      if (text != null && !text.isEmpty() && stops.length != 0) {
         int len = text.length();

         for (int i = 0; i < len; i++) {
            float t = len == 1 ? 0.0F : (float)i / (len - 1);
            int color = sample(stops, t);
            result.append(Text.literal(String.valueOf(text.charAt(i))).styled(s -> s.withColor(color)));
         }

         return result;
      } else {
         return result;
      }
   }

   private static int sample(int[] stops, float t) {
      if (stops.length == 1) {
         return stops[0];
      } else {
         float scaled = t * (stops.length - 1);
         int idx = (int)Math.floor(scaled);
         return idx >= stops.length - 1 ? stops[stops.length - 1] : lerp(stops[idx], stops[idx + 1], scaled - idx);
      }
   }

   private static int lerp(int a, int b, float f) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int r = Math.round(ar + (br - ar) * f);
      int g = Math.round(ag + (bg - ag) * f);
      int bl = Math.round(ab + (bb - ab) * f);
      return r << 16 | g << 8 | bl;
   }
}
