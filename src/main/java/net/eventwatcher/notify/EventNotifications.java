package net.eventwatcher.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.Nullable;

public final class EventNotifications {
   private static final int MAX_PENDING = 6;
   private static final CopyOnWriteArrayList<EventNotifications.PendingEvent> pending = new CopyOnWriteArrayList<>();

   private EventNotifications() {
   }

   /** Adds (or refreshes) a pending event for a server, newest first, deduped by server. */
   public static void add(String serverName, String server, String authorName, String authorId, String avatarHash) {
      pending.removeIf(e -> e.server().equalsIgnoreCase(server));
      pending.add(0, new EventNotifications.PendingEvent(serverName, server, authorName, authorId, avatarHash));

      while (pending.size() > MAX_PENDING) {
         pending.remove(pending.size() - 1);
      }
   }

   /** All pending events, newest first. */
   public static List<EventNotifications.PendingEvent> all() {
      return new ArrayList<>(pending);
   }

   /** The most recent pending event, or null if none. */
   @Nullable
   public static EventNotifications.PendingEvent latest() {
      return pending.isEmpty() ? null : pending.get(0);
   }

   /** Drops the pending event(s) for one server (e.g. after joining it). */
   public static void remove(String server) {
      pending.removeIf(e -> e.server().equalsIgnoreCase(server));
   }

   public static void clear() {
      pending.clear();
   }

   public record PendingEvent(String serverName, String server, String authorName, String authorId, String avatarHash) {
   }
}
