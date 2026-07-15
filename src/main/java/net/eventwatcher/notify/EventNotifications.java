package net.eventwatcher.notify;

import org.jetbrains.annotations.Nullable;

public final class EventNotifications {
   @Nullable
   private static volatile EventNotifications.PendingEvent current;

   private EventNotifications() {
   }

   public static void set(String message, String server) {
      current = new EventNotifications.PendingEvent(message, server);
   }

   @Nullable
   public static EventNotifications.PendingEvent get() {
      return current;
   }

   public static void clear() {
      current = null;
   }

   public record PendingEvent(String message, String server) {
   }
}
