package mogs.gui.clickgui;

import mogs.MogsClient;
import mogs.config.ConfigManager;
import mogs.gui.hud.HudEditorScreen;
import mogs.gui.hud.NotificationManager;
import mogs.module.Category;
import mogs.module.Module;
import mogs.module.ModuleManager;
import mogs.module.client.ClickGuiModule;
import mogs.setting.ActionSetting;
import mogs.setting.BoolSetting;
import mogs.setting.ColorSetting;
import mogs.setting.KeybindSetting;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;
import mogs.setting.Setting;
import mogs.util.Compat;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The MOGS ClickGUI. Everything is drawn in a fixed 640x400 "design" space that is scaled and moved as a whole,
 * so the layout stays identical at every GUI scale. All interaction regions ("hots") are rebuilt every frame while
 * drawing, which keeps hit-testing and rendering impossible to get out of sync.
 */
public class ClickGuiScreen extends Screen {
	private static final int W = 640;
	private static final int H = 400;
	private static final int SIDE = 160;
	private static final int HEADER = 46;
	private static final int CARD_H = 34;
	private static final int PAD = 14;

	private interface Click {
		void run(int button, float localX);
	}

	private interface Drag {
		void run(float localX);
	}

	private static final class Hot {
		float x;
		float y;
		float w;
		float h;
		boolean inList;
		String tip;
		Click click;
		Drag drag;
	}

	private static final class Anim {
		float hover;
		float on;
		float expand;
	}

	private final List<Hot> hots = new ArrayList<>();
	private final Map<Module, Anim> anims = new HashMap<>();
	private final Set<Module> expanded = new HashSet<>();
	private List<Module> shown = new ArrayList<>();

	private Category category = ConfigManager.lastCategory;
	private String search = "";
	private float scroll;
	private float scrollTarget;
	private float maxScroll;
	private int selected = -1;
	private boolean ensureSelectedVisible;

	private float px;
	private float py;
	private float scale = 1f;

	private boolean draggingWindow;
	private Hot draggingHot;
	private Module listeningModule;
	private KeybindSetting listeningSetting;

	private long lastNanos = System.nanoTime();
	private final long openedAt = System.currentTimeMillis();
	private Hot hoveredHot;
	private long hoverSince;
	private long resetConfirmUntil;

	public ClickGuiScreen() {
		super(Component.literal(MogsClient.FULL_NAME));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		ConfigManager.save();
	}

	// ------------------------------------------------------------------ rendering

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
		lastNanos = now;

		scale = Math.min(ClickGuiModule.guiScale(), Math.min((width - 16f) / W, (height - 16f) / H));
		if (ConfigManager.guiX < 0 || ConfigManager.guiY < 0) {
			px = (width - W * scale) / 2f;
			py = (height - H * scale) / 2f;
		} else {
			px = Math.max(0, Math.min(width - W * scale, ConfigManager.guiX));
			py = Math.max(0, Math.min(height - H * scale, ConfigManager.guiY));
		}

		hots.clear();
		g.fill(0, 0, width, height, 0x99000000);

		float lx = (mouseX - px) / scale;
		float ly = (mouseY - py) / scale;

		Render2D.setTransform(px, py, scale);
		try {
			drawWindow(g, lx, ly, dt);
		} finally {
			Render2D.resetTransform();
		}
	}

	private void drawWindow(GuiGraphics g, float lx, float ly, float dt) {
		Render2D.rect(g, 0, 0, W, H, Theme.BG);
		Render2D.rect(g, 0, 0, SIDE, H, Theme.SIDEBAR);
		Render2D.rect(g, SIDE, 0, 1, H, Theme.BORDER);
		drawList(g, lx, ly, dt);
		drawHeader(g, lx, ly);
		drawSidebar(g, lx, ly);
		Render2D.outline(g, 0, 0, W, H, Theme.BORDER, 1);
		Render2D.rect(g, 0, 0, W, 1, Theme.gold());

		updateHover(lx, ly);
		drawTooltip(g, lx, ly);
	}

	// ---- sidebar

	private void drawSidebar(GuiGraphics g, float lx, float ly) {
		Render2D.scaledText(g, MogsClient.NAME, 16, 14, 2f, Theme.gold(), true);
		Render2D.text(g, "Client", 16 + TextUtil.width(MogsClient.NAME) * 2 + 6, 22, Theme.MUTED);
		Render2D.text(g, MogsClient.SUBTITLE, 16, 35, Theme.MUTED);
		Render2D.rect(g, 16, 52, SIDE - 32, 1, Theme.BORDER);

		float y = 64;
		for (Category c : Category.values()) {
			final Category target = c;
			boolean selectedCategory = c == category && search.isEmpty();
			boolean hover = Render2D.inside(lx, ly, 8, y, SIDE - 16, 26);
			int bg = selectedCategory ? Render2D.withAlpha(Theme.red() & 0xFFFFFF, 0x38)
					: hover ? Theme.CARD : 0;
			if (bg != 0) {
				Render2D.rect(g, 8, y, SIDE - 16, 26, bg);
			}
			if (selectedCategory) {
				Render2D.rect(g, 8, y, 3, 26, Theme.red());
			}
			Render2D.text(g, c.display(), 22, y + 9, selectedCategory ? Theme.gold() : hover ? Theme.TEXT : Theme.MUTED);
			String count = Integer.toString(ModuleManager.byCategory(c).size());
			Render2D.text(g, count, SIDE - 20 - TextUtil.width(count), y + 9, Theme.OFF);
			addHot(8, y, SIDE - 16, 26, false, c.display() + " modules", (button, x) -> selectCategory(target), null);
			y += 30;
		}

		// bottom buttons
		float by = H - 62;
		boolean editHover = button(g, 16, by, SIDE - 32, 22, "Edit HUD", lx, ly, Theme.TEXT);
		addHot(16, by, SIDE - 32, 22, false, "Drag HUD elements to a new position", (b, x) ->
				Minecraft.getInstance().setScreen(new HudEditorScreen(this)), null);

		boolean confirming = System.currentTimeMillis() < resetConfirmUntil;
		button(g, 16, by + 28, SIDE - 32, 22, confirming ? "Click again to confirm" : "Reset Config", lx, ly,
				confirming ? Theme.red() : Theme.MUTED);
		addHot(16, by + 28, SIDE - 32, 22, false, "Reset every module, keybind, HUD position and GUI position", (b, x) -> {
			if (System.currentTimeMillis() < resetConfirmUntil) {
				ConfigManager.resetAll();
				anims.clear();
				expanded.clear();
				category = Category.COMBAT;
				resetConfirmUntil = 0;
				NotificationManager.push(MogsClient.NAME, "Configuration reset", 2500, 1);
			} else {
				resetConfirmUntil = System.currentTimeMillis() + 3000;
			}
		}, null);
		if (editHover) {
			// hover feedback is drawn by button(); nothing else needed
			return;
		}
	}

	private boolean button(GuiGraphics g, float x, float y, float w, float h, String label, float lx, float ly, int textColor) {
		boolean hover = Render2D.inside(lx, ly, x, y, w, h);
		Render2D.rect(g, x, y, w, h, hover ? Theme.CARD_HOVER : Theme.FIELD);
		Render2D.outline(g, x, y, w, h, hover ? Theme.red() : Theme.BORDER, 1);
		Render2D.centered(g, label, x + w / 2f, y + (h - 8) / 2f, hover && textColor == Theme.MUTED ? Theme.TEXT : textColor);
		return hover;
	}

	private void selectCategory(Category target) {
		category = target;
		ConfigManager.lastCategory = target;
		ConfigManager.markDirty();
		search = "";
		scroll = 0;
		scrollTarget = 0;
		selected = -1;
	}

	// ---- header

	private void drawHeader(GuiGraphics g, float lx, float ly) {
		Render2D.rect(g, SIDE + 1, 1, W - SIDE - 2, HEADER - 1, Theme.BG);
		Render2D.rect(g, SIDE + 1, HEADER, W - SIDE - 2, 1, Theme.BORDER);

		String title = search.isEmpty() ? category.display() : "Search results";
		Render2D.scaledText(g, title, SIDE + 16, 15, 1.5f, Theme.TEXT, false);

		float bx = W - 246;
		float by = 11;
		boolean active = !search.isEmpty();
		Render2D.rect(g, bx, by, 230, 24, Theme.FIELD);
		Render2D.outline(g, bx, by, 230, 24, active ? Theme.red() : Theme.BORDER, 1);
		boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
		if (active) {
			Render2D.text(g, TextUtil.fit(search, 210) + (caret ? "_" : ""), bx + 8, by + 8, Theme.TEXT);
		} else {
			Render2D.text(g, "Type to search modules" + (caret ? "_" : ""), bx + 8, by + 8, Theme.OFF);
		}
		addHot(bx, by, 230, 24, false, "Search every module by name or description", (b, x) -> {
		}, null);
	}

	// ---- module list

	private void drawList(GuiGraphics g, float lx, float ly, float dt) {
		shown = search.isEmpty() ? ModuleManager.byCategory(category) : ModuleManager.search(search);
		if (selected >= shown.size()) {
			selected = shown.size() - 1;
		}

		float viewTop = HEADER + 1;
		float viewH = H - viewTop;
		float cardX = SIDE + PAD;
		float cardW = W - SIDE - PAD * 2 - 6;

		float animK = ClickGuiModule.animationsEnabled() ? Math.min(1f, dt * 14f * ClickGuiModule.animationSpeed()) : 1f;

		// layout pass
		float[] tops = new float[shown.size()];
		float[] heights = new float[shown.size()];
		float cursor = 10;
		for (int i = 0; i < shown.size(); i++) {
			Module m = shown.get(i);
			Anim a = anim(m);
			float expandTarget = expanded.contains(m) ? 1f : 0f;
			a.expand += (expandTarget - a.expand) * animK;
			if (Math.abs(expandTarget - a.expand) < 0.004f) {
				a.expand = expandTarget;
			}
			tops[i] = cursor;
			heights[i] = CARD_H + a.expand * settingsHeight(m);
			cursor += heights[i] + 8;
		}
		float contentH = cursor + 6;
		maxScroll = Math.max(0, contentH - viewH);

		if (ensureSelectedVisible && selected >= 0 && selected < tops.length) {
			if (tops[selected] - 8 < scrollTarget) {
				scrollTarget = tops[selected] - 8;
			} else if (tops[selected] + heights[selected] + 8 > scrollTarget + viewH) {
				scrollTarget = tops[selected] + heights[selected] + 8 - viewH;
			}
			ensureSelectedVisible = false;
		}
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
		scroll += (scrollTarget - scroll) * (ClickGuiModule.animationsEnabled() ? Math.min(1f, dt * 16f) : 1f);
		if (Math.abs(scrollTarget - scroll) < 0.2f) {
			scroll = scrollTarget;
		}

		Render2D.clip(g, SIDE + 1, viewTop, W - SIDE - 1, H - viewTop);
		for (int i = 0; i < shown.size(); i++) {
			Module m = shown.get(i);
			float y = viewTop + tops[i] - scroll;
			float h = heights[i];
			if (y + h < viewTop || y > H) {
				continue;
			}
			drawCard(g, m, i == selected, cardX, y, cardW, h, lx, ly, animK, viewTop);
		}
		g.disableScissor();

		if (shown.isEmpty()) {
			Render2D.centered(g, "No modules match \"" + search + "\"", SIDE + (W - SIDE) / 2f, HEADER + 60, Theme.MUTED);
		}

		// scrollbar
		if (maxScroll > 0) {
			float trackH = viewH - 8;
			float thumbH = Math.max(24, trackH * viewH / contentH);
			float thumbY = viewTop + 4 + (trackH - thumbH) * (scroll / maxScroll);
			Render2D.rect(g, W - 8, viewTop + 4, 3, trackH, Theme.FIELD);
			Render2D.rect(g, W - 8, thumbY, 3, thumbH, Theme.red());
		}
	}

	private Anim anim(Module m) {
		return anims.computeIfAbsent(m, key -> {
			Anim a = new Anim();
			a.on = key.isEnabled() ? 1f : 0f;
			return a;
		});
	}

	private void drawCard(GuiGraphics g, Module m, boolean isSelected, float x, float y, float w, float h,
						  float lx, float ly, float animK, float viewTop) {
		Anim a = anim(m);
		boolean headerHover = ly >= viewTop && Render2D.inside(lx, ly, x, y, w, CARD_H);
		a.hover += ((headerHover ? 1f : 0f) - a.hover) * animK;
		a.on += ((m.isEnabled() ? 1f : 0f) - a.on) * animK;

		Render2D.rect(g, x, y, w, h, Render2D.lerp(Theme.CARD, Theme.CARD_HOVER, a.hover));
		Render2D.rect(g, x, y, 3, h, m.isSettingsOnly() ? Theme.OFF : Render2D.lerp(Theme.OFF, Theme.red(), a.on));
		if (isSelected) {
			Render2D.outline(g, x, y, w, h, Render2D.alpha(Theme.gold(), 0.8f), 1);
		}

		// right side: switch + key badge
		float switchW = 34;
		float rightEdge = x + w - 12;
		float contentRight = rightEdge;
		if (m.isSettingsOnly()) {
			String label = "Settings";
			Render2D.text(g, label, rightEdge - TextUtil.width(label), y + 13, Theme.MUTED);
			contentRight = rightEdge - TextUtil.width(label) - 10;
		} else {
			float sx = rightEdge - switchW;
			float sy = y + 9;
			Render2D.rect(g, sx, sy, switchW, 16, Render2D.lerp(Theme.OFF, Theme.red(), a.on));
			Render2D.rect(g, sx + 2 + (switchW - 16) * a.on, sy + 2, 12, 12, Render2D.lerp(0xFF8B8B95, 0xFFFFFFFF, a.on));

			boolean listening = listeningModule == m;
			String key = listening ? "..." : Compat.keyName(m.keybind().key());
			float badgeW = Math.max(40, TextUtil.width(key) + 12);
			float bx = sx - 10 - badgeW;
			Render2D.rect(g, bx, y + 9, badgeW, 16, Theme.FIELD);
			Render2D.outline(g, bx, y + 9, badgeW, 16, listening ? Theme.gold() : Theme.BORDER, 1);
			Render2D.centered(g, key, bx + badgeW / 2f, y + 13, listening ? Theme.gold()
					: m.keybind().key() < 0 ? Theme.OFF : Theme.TEXT);
			contentRight = bx - 10;

			// header first (lowest priority), then badge and switch on top
			addHot(x, y, w, CARD_H, true, m.description(), (b, px2) -> toggleExpanded(m), null);
			addHot(bx, y + 9, badgeW, 16, true, "Click, then press a key to bind. Backspace clears, Esc cancels.",
					(b, px2) -> {
						listeningModule = m;
						listeningSetting = null;
					}, null);
			addHot(sx, sy, switchW, 16, true, m.isEnabled() ? "Disable " + m.name() : "Enable " + m.name(),
					(b, px2) -> m.toggle(), null);
		}
		if (m.isSettingsOnly()) {
			addHot(x, y, w, CARD_H, true, m.description(), (b, px2) -> toggleExpanded(m), null);
		}

		Render2D.text(g, TextUtil.fit(m.name(), (int) (contentRight - x - 14)), x + 12, y + 7,
				Render2D.lerp(Theme.TEXT, Theme.gold(), a.hover));
		Render2D.text(g, TextUtil.fit(m.description(), (int) (contentRight - x - 14)), x + 12, y + 19, Theme.MUTED);

		// settings panel
		if (a.expand > 0.005f) {
			Render2D.clip(g, x, y + CARD_H, w, h - CARD_H);
			Render2D.rect(g, x + 12, y + CARD_H - 1, w - 24, 1, Theme.BORDER);
			boolean live = a.expand > 0.95f && !(ly < viewTop);
			float sy = y + CARD_H + 6;
			for (Setting<?> setting : m.visibleSettings()) {
				sy += drawSetting(g, setting, x, sy, w, live, lx, ly);
			}
			float resetY = sy + 2;
			boolean hover = live && Render2D.inside(lx, ly, x + 12, resetY, w - 24, 18);
			Render2D.rect(g, x + 12, resetY, w - 24, 18, hover ? Theme.CARD_HOVER : Theme.FIELD);
			Render2D.outline(g, x + 12, resetY, w - 24, 18, hover ? Theme.red() : Theme.BORDER, 1);
			Render2D.centered(g, "Reset settings", x + w / 2f, resetY + 5, Theme.red());
			if (live) {
				addHot(x + 12, resetY, w - 24, 18, true, "Restore every setting of " + m.name() + " to its default",
						(b, px2) -> m.resetSettings(), null);
			}
			g.disableScissor();
		}
	}

	private void toggleExpanded(Module m) {
		if (!expanded.remove(m)) {
			expanded.add(m);
		}
	}

	// ---- settings rows

	private float settingRowHeight(Setting<?> s) {
		if (s instanceof NumberSetting) {
			return 28;
		}
		if (s instanceof ColorSetting || s instanceof ActionSetting) {
			return 24;
		}
		return 22;
	}

	private float settingsHeight(Module m) {
		float total = 6;
		for (Setting<?> setting : m.visibleSettings()) {
			total += settingRowHeight(setting);
		}
		return total + 2 + 18 + 10;
	}

	private float drawSetting(GuiGraphics g, Setting<?> s, float cardX, float y, float cardW, boolean live, float lx, float ly) {
		float x = cardX + 12;
		float w = cardW - 24;
		float rowH = settingRowHeight(s);
		boolean hover = live && Render2D.inside(lx, ly, x, y, w, rowH);
		int labelColor = hover ? Theme.gold() : Theme.TEXT;
		String tip = s.description;

		if (s instanceof BoolSetting b) {
			Render2D.text(g, b.name, x, y + 7, labelColor);
			float sx = x + w - 28;
			Render2D.rect(g, sx, y + 5, 28, 12, b.on() ? Theme.red() : Theme.OFF);
			Render2D.rect(g, sx + (b.on() ? 16 : 2), y + 7, 10, 8, b.on() ? 0xFFFFFFFF : 0xFF8B8B95);
			if (live) {
				addHot(x, y, w, rowH, true, tip, (btn, px2) -> {
					b.toggle();
					ConfigManager.markDirty();
				}, null);
			}
		} else if (s instanceof NumberSetting n) {
			Render2D.text(g, n.name, x, y + 4, labelColor);
			String value = n.display();
			Render2D.text(g, value, x + w - TextUtil.width(value), y + 4, Theme.gold());
			float trackY = y + 18;
			Render2D.rect(g, x, trackY, w, 4, Theme.FIELD);
			float fillW = (float) (w * n.fraction());
			Render2D.rect(g, x, trackY, fillW, 4, Theme.red());
			Render2D.rect(g, x + fillW - 2, trackY - 3, 4, 10, 0xFFFFFFFF);
			if (live) {
				Drag drag = lxx -> {
					n.setFraction((lxx - x) / w);
					ConfigManager.markDirty();
				};
				addHot(x, y, w, rowH, true, tip, (btn, px2) -> drag.run(px2), drag);
			}
		} else if (s instanceof ModeSetting mode) {
			Render2D.text(g, mode.name, x, y + 7, labelColor);
			String text = "< " + mode.get() + " >";
			float bw = TextUtil.width(text) + 14;
			float bx = x + w - bw;
			Render2D.rect(g, bx, y + 2, bw, 18, Theme.FIELD);
			Render2D.outline(g, bx, y + 2, bw, 18, hover ? Theme.red() : Theme.BORDER, 1);
			Render2D.centered(g, text, bx + bw / 2f, y + 7, Theme.TEXT);
			if (live) {
				addHot(x, y, w, rowH, true, tip + " (left click: next, right click: previous)", (btn, px2) -> {
					if (btn == 1) {
						mode.previous();
					} else {
						mode.next();
					}
					ConfigManager.markDirty();
				}, null);
			}
		} else if (s instanceof ColorSetting color) {
			Render2D.text(g, color.name, x, y + 8, labelColor);
			float swatchesX = x + w - ColorSetting.PRESETS.length * 15f + 3f;
			for (int i = 0; i < ColorSetting.PRESETS.length; i++) {
				final int rgb = ColorSetting.PRESETS[i];
				float sx = swatchesX + i * 15f;
				Render2D.rect(g, sx, y + 6, 12, 12, 0xFF000000 | rgb);
				if (color.get() == rgb) {
					Render2D.outline(g, sx - 1, y + 5, 14, 14, Theme.gold(), 1);
				}
				if (live) {
					addHot(sx, y + 6, 12, 12, true, tip, (btn, px2) -> {
						color.set(rgb);
						ConfigManager.markDirty();
					}, null);
				}
			}
			float stripW = 64;
			float stripX = swatchesX - 12 - stripW;
			for (int i = 0; i < 32; i++) {
				int rgb = java.awt.Color.HSBtoRGB(i / 32f, 0.85f, 1f);
				Render2D.rect(g, stripX + i * 2f, y + 8, 2, 8, 0xFF000000 | (rgb & 0xFFFFFF));
			}
			Render2D.rect(g, stripX + stripW * color.hue() - 1, y + 5, 2, 14, 0xFFFFFFFF);
			if (live) {
				Drag drag = lxx -> {
					color.setHue((lxx - stripX) / stripW);
					ConfigManager.markDirty();
				};
				addHot(stripX, y + 4, stripW, 16, true, tip + " (drag the hue strip)", (btn, px2) -> drag.run(px2), drag);
			}
		} else if (s instanceof KeybindSetting key) {
			Render2D.text(g, key.name, x, y + 7, labelColor);
			boolean listening = listeningSetting == key;
			String text = listening ? "..." : Compat.keyName(key.key());
			float bw = Math.max(44, TextUtil.width(text) + 14);
			float bx = x + w - bw;
			Render2D.rect(g, bx, y + 2, bw, 18, Theme.FIELD);
			Render2D.outline(g, bx, y + 2, bw, 18, listening ? Theme.gold() : Theme.BORDER, 1);
			Render2D.centered(g, text, bx + bw / 2f, y + 7, listening ? Theme.gold() : Theme.TEXT);
			if (live) {
				addHot(bx, y + 2, bw, 18, true, tip, (btn, px2) -> {
					listeningSetting = key;
					listeningModule = null;
				}, null);
			}
		} else if (s instanceof ActionSetting action) {
			Render2D.rect(g, x, y + 2, w, 18, hover ? Theme.CARD_HOVER : Theme.FIELD);
			Render2D.outline(g, x, y + 2, w, 18, hover ? Theme.red() : Theme.BORDER, 1);
			Render2D.centered(g, action.name, x + w / 2f, y + 7, Theme.TEXT);
			if (live) {
				addHot(x, y + 2, w, 18, true, tip, (btn, px2) -> action.run(), null);
			}
		}
		return rowH;
	}

	// ---- tooltips and hot regions

	private void addHot(float x, float y, float w, float h, boolean inList, String tip, Click click, Drag drag) {
		Hot hot = new Hot();
		hot.x = x;
		hot.y = y;
		hot.w = w;
		hot.h = h;
		hot.inList = inList;
		hot.tip = tip;
		hot.click = click;
		hot.drag = drag;
		hots.add(hot);
	}

	private Hot hotAt(float lx, float ly) {
		for (int i = hots.size() - 1; i >= 0; i--) {
			Hot hot = hots.get(i);
			if (hot.inList && (ly < HEADER + 1 || lx < SIDE + 1)) {
				continue;
			}
			if (Render2D.inside(lx, ly, hot.x, hot.y, hot.w, hot.h)) {
				return hot;
			}
		}
		return null;
	}

	private void updateHover(float lx, float ly) {
		Hot now = hotAt(lx, ly);
		boolean same = now != null && hoveredHot != null && now.tip != null && now.tip.equals(hoveredHot.tip)
				&& now.x == hoveredHot.x && now.y == hoveredHot.y;
		if (!same) {
			hoverSince = System.currentTimeMillis();
		}
		hoveredHot = now;
	}

	private void drawTooltip(GuiGraphics g, float lx, float ly) {
		if (!ClickGuiModule.tooltipsEnabled() || hoveredHot == null || hoveredHot.tip == null || hoveredHot.tip.isEmpty()
				|| draggingHot != null || draggingWindow || System.currentTimeMillis() - hoverSince < 350) {
			return;
		}
		String text = TextUtil.fit(hoveredHot.tip, 300);
		float w = TextUtil.width(text) + 14;
		float h = 18;
		float x = Math.min(W - w - 4, lx + 12);
		float y = Math.min(H - h - 4, ly + 14);
		Render2D.rect(g, x, y, w, h, 0xF2060608);
		Render2D.outline(g, x, y, w, h, Theme.red(), 1);
		Render2D.text(g, text, x + 7, y + 5, Theme.TEXT);
	}

	// ------------------------------------------------------------------ input

	private float localX(double mouseX) {
		return (float) ((mouseX - px) / scale);
	}

	private float localY(double mouseY) {
		return (float) ((mouseY - py) / scale);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float lx = localX(event.x());
		float ly = localY(event.y());
		int button = event.button();

		if (listeningModule != null || listeningSetting != null) {
			listeningModule = null;
			listeningSetting = null;
			return true;
		}
		Hot hot = hotAt(lx, ly);
		if (hot != null) {
			if (hot.click != null) {
				hot.click.run(button, lx);
			}
			ConfigManager.markDirty();
			if (hot.drag != null) {
				draggingHot = hot;
			}
			return true;
		}
		if (ly >= 0 && ly < HEADER && lx >= 0 && lx < W) {
			draggingWindow = true;
			if (ConfigManager.guiX < 0 || ConfigManager.guiY < 0) {
				ConfigManager.guiX = Math.round(px);
				ConfigManager.guiY = Math.round(py);
			}
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingHot != null && draggingHot.drag != null) {
			draggingHot.drag.run(localX(event.x()));
			return true;
		}
		if (draggingWindow) {
			ConfigManager.guiX = (int) Math.max(0, Math.min(width - W * scale, ConfigManager.guiX + dragX));
			ConfigManager.guiY = (int) Math.max(0, Math.min(height - H * scale, ConfigManager.guiY + dragY));
			ConfigManager.markDirty();
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean was = draggingHot != null || draggingWindow;
		draggingHot = null;
		draggingWindow = false;
		if (was) {
			ConfigManager.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) scrollY * 34f));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (handleKey(event.key(), event.modifiers())) {
			return true;
		}
		return super.keyPressed(event);
	}

	private boolean handleKey(int key, int modifiers) {
		if (listeningModule != null || listeningSetting != null) {
			int bound;
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				bound = Integer.MIN_VALUE; // cancel
			} else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
				bound = KeybindSetting.NONE;
			} else {
				bound = key;
			}
			if (bound != Integer.MIN_VALUE) {
				if (listeningModule != null) {
					listeningModule.keybind().set(bound);
				} else {
					listeningSetting.set(bound);
				}
				ConfigManager.markDirty();
			}
			listeningModule = null;
			listeningSetting = null;
			return true;
		}

		if (key == ClickGuiModule.openKey()) {
			if (System.currentTimeMillis() - openedAt > 300) {
				onClose();
			}
			return true;
		}

		boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
		switch (key) {
			case GLFW.GLFW_KEY_ESCAPE -> {
				if (System.currentTimeMillis() - openedAt < 400) {
					return true; // Esc is still held from the Esc + Right Shift chord
				}
				if (!search.isEmpty()) {
					search = "";
					selected = -1;
				} else {
					onClose();
				}
				return true;
			}
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (!search.isEmpty()) {
					search = search.substring(0, search.length() - 1);
					selected = -1;
					scrollTarget = 0;
				}
				return true;
			}
			case GLFW.GLFW_KEY_TAB -> {
				selectCategory(shift ? category.previous() : category.next());
				return true;
			}
			case GLFW.GLFW_KEY_DOWN -> {
				moveSelection(1);
				return true;
			}
			case GLFW.GLFW_KEY_UP -> {
				moveSelection(-1);
				return true;
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				Module m = selectedModule();
				if (m != null && !m.isSettingsOnly()) {
					m.toggle();
				} else if (m != null) {
					toggleExpanded(m);
				}
				return true;
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				Module m = selectedModule();
				if (m != null) {
					expanded.add(m);
				}
				return true;
			}
			case GLFW.GLFW_KEY_LEFT -> {
				Module m = selectedModule();
				if (m != null) {
					expanded.remove(m);
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private Module selectedModule() {
		return selected >= 0 && selected < shown.size() ? shown.get(selected) : null;
	}

	private void moveSelection(int delta) {
		if (shown.isEmpty()) {
			return;
		}
		selected = Math.max(0, Math.min(shown.size() - 1, selected + delta));
		ensureSelectedVisible = true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (listeningModule != null || listeningSetting != null) {
			return true;
		}
		int codepoint = event.codepoint();
		if (codepoint >= 32 && codepoint != 127 && Character.isValidCodePoint(codepoint) && search.length() < 32) {
			search += new String(Character.toChars(codepoint));
			selected = -1;
			scrollTarget = 0;
			return true;
		}
		return super.charTyped(event);
	}
}
