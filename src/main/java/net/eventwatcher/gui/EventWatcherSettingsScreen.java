package net.eventwatcher.gui;

import java.util.List;
import net.eventwatcher.EventWatcherClient;
import net.eventwatcher.config.EventWatcherConfig;
import net.eventwatcher.config.WatchConfig;
import net.eventwatcher.discord.DiscordSource;
import net.eventwatcher.util.GradientText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class EventWatcherSettingsScreen extends Screen {
   @Nullable
   private final Screen parent;
   private final EventWatcherConfig working;
   private int selectedWatch;
   private TextFieldWidget nameField;
   private TextFieldWidget tokenField;
   private TextFieldWidget channelField;
   private TextFieldWidget targetField;
   private TextFieldWidget addKeywordField;
   private KeywordListWidget keywordList;
   private ButtonWidget notifyButton;
   private ButtonWidget autoButton;
   private ButtonWidget tokenTypeButton;
   private int panelLeft;
   private int panelTop;
   private int panelWidth;
   private int panelHeight;
   private int col1;
   private int col2;
   private int col3;
   private int contentTop;
   private int statusY;
   private int navCountCenterX;

   public EventWatcherSettingsScreen(@Nullable Screen parent) {
      super(Text.literal("EventWatcher Settings"));
      this.parent = parent;
      this.working = EventWatcherClient.getConfig().copy();
      this.working.sanitize();
   }

   private WatchConfig watch() {
      return this.working.watches.get(this.selectedWatch);
   }

   protected void init() {
      this.panelWidth = Math.min(this.width - 20, 470);
      this.panelHeight = Math.min(this.height - 20, 252);
      this.panelLeft = (this.width - this.panelWidth) / 2;
      this.panelTop = (this.height - this.panelHeight) / 2;
      int pad = 10;
      int colWidth = (this.panelWidth - pad * 4) / 3;
      this.col1 = this.panelLeft + pad;
      this.col2 = this.col1 + colWidth + pad;
      this.col3 = this.col2 + colWidth + pad;
      this.contentTop = this.panelTop + 40;
      this.selectedWatch = Math.max(0, Math.min(this.selectedWatch, this.working.watches.size() - 1));
      WatchConfig w = this.watch();
      int watchCount = this.working.watches.size();
      int navY = this.panelTop + 8;
      int panelRight = this.panelLeft + this.panelWidth;
      ButtonWidget prevButton = ButtonWidget.builder(Text.literal("<"), b -> this.cycleWatch(-1))
         .dimensions(panelRight - 124, navY, 18, 18)
         .tooltip(Tooltip.of(Text.literal("Previous watch")))
         .build();
      prevButton.active = watchCount > 1;
      this.addDrawableChild(prevButton);
      this.navCountCenterX = panelRight - 89;
      ButtonWidget nextButton = ButtonWidget.builder(Text.literal(">"), b -> this.cycleWatch(1))
         .dimensions(panelRight - 72, navY, 18, 18)
         .tooltip(Tooltip.of(Text.literal("Next watch")))
         .build();
      nextButton.active = watchCount > 1;
      this.addDrawableChild(nextButton);
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("+"), b -> this.addWatch())
            .dimensions(panelRight - 48, navY, 18, 18)
            .tooltip(Tooltip.of(Text.literal("Add a new watch")))
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("-"), b -> this.deleteWatch())
            .dimensions(panelRight - 28, navY, 18, 18)
            .tooltip(Tooltip.of(Text.literal("Delete this watch")))
            .build()
      );
      this.nameField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 10, colWidth, 18, Text.literal("Watch Name"));
      this.nameField.setMaxLength(32);
      this.nameField.setText(w.label);
      this.nameField.setChangedListener(s -> w.label = s.trim());
      this.addDrawableChild(this.nameField);
      this.tokenField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 48, colWidth, 18, Text.literal("Discord Token"));
      this.tokenField.setMaxLength(120);
      this.tokenField.setText(w.discordToken);
      this.tokenField.setChangedListener(s -> w.discordToken = s.trim());
      this.tokenField.addFormatter((displayed, offset) -> OrderedText.styledForwardsVisitedString("*".repeat(displayed.length()), Style.EMPTY));
      this.addDrawableChild(this.tokenField);
      this.channelField = new TextFieldWidget(this.textRenderer, this.col1, this.contentTop + 86, colWidth, 18, Text.literal("Channel ID"));
      this.channelField.setMaxLength(40);
      this.channelField.setText(w.channelId);
      this.channelField.setChangedListener(s -> w.channelId = s.trim());
      this.addDrawableChild(this.channelField);
      this.tokenTypeButton = ButtonWidget.builder(this.tokenTypeLabel(), b -> {
         this.watch().useUserToken = !this.watch().useUserToken;
         this.tokenTypeButton.setMessage(this.tokenTypeLabel());
      }).dimensions(this.col1, this.contentTop + 114, colWidth, 18).build();
      this.addDrawableChild(this.tokenTypeButton);
      int listTop = this.contentTop + 10;
      int listBottom = this.panelTop + this.panelHeight - 56;
      this.keywordList = new KeywordListWidget(this.textRenderer, this.col2, listTop, colWidth, listBottom - listTop, w.keywords, this::removeKeyword);
      this.addDrawableChild(this.keywordList);
      this.addKeywordField = new TextFieldWidget(this.textRenderer, this.col2, listBottom + 4, colWidth - 44, 18, Text.literal("New keyword"));
      this.addKeywordField.setMaxLength(32);
      this.addDrawableChild(this.addKeywordField);
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Add"), b -> this.addKeyword())
            .dimensions(this.col2 + colWidth - 40, listBottom + 4, 40, 18)
            .build()
      );
      this.notifyButton = ButtonWidget.builder(Text.literal("Notify Me"), b -> {
         this.working.autoConnect = false;
         this.updateModeButtons();
      }).dimensions(this.col3, this.contentTop + 10, colWidth, 18).build();
      this.addDrawableChild(this.notifyButton);
      this.autoButton = ButtonWidget.builder(Text.literal("Auto-Connect"), b -> {
         this.working.autoConnect = true;
         this.updateModeButtons();
      }).dimensions(this.col3, this.contentTop + 32, colWidth, 18).build();
      this.addDrawableChild(this.autoButton);
      this.updateModeButtons();
      this.targetField = new TextFieldWidget(this.textRenderer, this.col3, this.contentTop + 78, colWidth, 18, Text.literal("Target Server"));
      this.targetField.setMaxLength(80);
      this.targetField.setText(w.targetServer);
      this.targetField.setChangedListener(s -> w.targetServer = s.trim());
      this.addDrawableChild(this.targetField);
      this.statusY = this.contentTop + 108;
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Sound..."), b -> this.client.setScreen(new SoundSettingsScreen(this, this.working)))
            .dimensions(this.col3, this.statusY + 16, colWidth, 18)
            .build()
      );
      int btnY = this.panelTop + this.panelHeight - 26;
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Save"), b -> this.save()).dimensions(this.width / 2 - 104, btnY, 100, 20).build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Cancel"), b -> this.backToParent()).dimensions(this.width / 2 + 4, btnY, 100, 20).build()
      );
   }

   private void cycleWatch(int direction) {
      int count = this.working.watches.size();
      this.selectedWatch = ((this.selectedWatch + direction) % count + count) % count;
      this.clearAndInit();
   }

   private void addWatch() {
      this.working.watches.add(new WatchConfig());
      this.selectedWatch = this.working.watches.size() - 1;
      this.clearAndInit();
   }

   private void deleteWatch() {
      if (this.working.watches.size() <= 1) {
         this.working.watches.set(0, new WatchConfig());
         this.selectedWatch = 0;
      } else {
         this.working.watches.remove(this.selectedWatch);
         this.selectedWatch = Math.min(this.selectedWatch, this.working.watches.size() - 1);
      }

      this.clearAndInit();
   }

   private Text tokenTypeLabel() {
      return Text.literal(this.watch().useUserToken ? "Type: User token" : "Type: Bot token");
   }

   private void updateModeButtons() {
      this.notifyButton.setMessage(Text.literal((this.working.autoConnect ? "( ) " : "(*) ") + "Notify Me"));
      this.autoButton.setMessage(Text.literal((this.working.autoConnect ? "(*) " : "( ) ") + "Auto-Connect"));
   }

   private void addKeyword() {
      String kw = this.addKeywordField.getText().trim();
      if (!kw.isEmpty() && !this.watch().keywords.contains(kw)) {
         this.watch().keywords.add(kw);
         this.addKeywordField.setText("");
      }
   }

   private void removeKeyword(String keyword) {
      this.watch().keywords.remove(keyword);
   }

   private void save() {
      this.working.sanitize();
      EventWatcherClient.setConfig(this.working);
      this.working.save();
      EventWatcherClient.restartDiscord();
      this.backToParent();
   }

   private void backToParent() {
      this.client.setScreen(this.parent);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, -1072689136);
      context.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 1, -12961222);
      context.fill(this.panelLeft, this.panelTop + this.panelHeight - 1, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, -12961222);
      int sepTop = this.contentTop - 6;
      int sepBottom = this.panelTop + this.panelHeight - 32;
      context.fill(this.col2 - 6, sepTop, this.col2 - 5, sepBottom, 822083583);
      context.fill(this.col3 - 6, sepTop, this.col3 - 5, sepBottom, 822083583);
      super.render(context, mouseX, mouseY, delta);
      context.drawTextWithShadow(this.textRenderer, GradientText.of("EventWatcher Settings"), this.col1, this.panelTop + 13, -1);
      int labelColor = -5197648;
      context.drawCenteredTextWithShadow(
         this.textRenderer, this.selectedWatch + 1 + "/" + this.working.watches.size(), this.navCountCenterX, this.panelTop + 13, labelColor
      );
      context.drawTextWithShadow(this.textRenderer, "Watch Name", this.col1, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Discord Token", this.col1, this.contentTop + 38, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Channel ID", this.col1, this.contentTop + 76, labelColor);
      if (this.watch().useUserToken) {
         context.drawTextWithShadow(this.textRenderer, "selfbot — use alt acct", this.col1, this.contentTop + 136, -2056144);
      }

      context.drawTextWithShadow(this.textRenderer, "Monitored Keywords", this.col2, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "On Keyword Detected:", this.col3, this.contentTop, labelColor);
      context.drawTextWithShadow(this.textRenderer, "Target Server", this.col3, this.contentTop + 68, labelColor);
      List<DiscordSource> sources = EventWatcherClient.getSources();
      int total = sources.size();
      int up = 0;

      for (DiscordSource source : sources) {
         if (source.isConnected()) {
            up++;
         }
      }

      boolean allUp = total > 0 && up == total;
      String statusText = total == 0 ? "Disconnected" : up + "/" + total + " connected";
      int dotColor = allUp ? -12264124 : -2276284;
      context.drawTextWithShadow(this.textRenderer, "Status:", this.col3, this.statusY, labelColor);
      int dotX = this.col3 + 46;
      context.fill(dotX, this.statusY - 1, dotX + 8, this.statusY + 7, dotColor);
      context.drawTextWithShadow(this.textRenderer, statusText, dotX + 12, this.statusY, dotColor);
   }

   public void close() {
      this.backToParent();
   }
}
