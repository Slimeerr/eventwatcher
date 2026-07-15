package net.eventwatcher;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.eventwatcher.gui.EventWatcherSettingsScreen;

public class EventWatcherModMenu implements ModMenuApi {
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return EventWatcherSettingsScreen::new;
   }
}
