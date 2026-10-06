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
 * The MOGS ClickGUI: one panel per category, side by side. Panels can be dragged by their header, collapsed with a
 * click, and scrolled with the mouse wheel. Click a module to toggle it, right click (or the arrow) to open its
 * settings. All interaction regions ("hots") are rebuilt every frame while drawing, so hit-testing and rendering
 * can never get out of sync.
 */
public class ClickGuiScreen extends Screen {
	private static final int PW = 160;
	private static final int GAP = 12;
	private static final int HEAD = 22;
	private static final int ROW = 20;
	private static final int TOP = 48;
	private static final int BOTTOM = 44;
	private static final int INNER = PW - 16;
	private static final int RESET_H = 18;

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
		float clip0;
		float clip1;
		String tip;
		Click click;
		Drag drag;
	}

	private static final class Anim {
		float hover;
		float on;
		float expand;
	}

	private static final class Panel {
		final Category category;
		float x;
		float y;
		float scroll;
		float scrollTarget;
		float maxScroll;
		float viewH;
		boolean visible;

		Panel(Category category) {
			this.category = category;
		}
	}

	private final List<Hot> hots = new ArrayList<>();
	private final List<Panel> panels = new ArrayList<>();
	private final Map<Module, Anim> anims = new HashMap<>();
	private final Set<Module> expanded = new HashSet<>();

	private String search = "";
	private float scale = 1f;
	private float viewW;
	private float viewH;
	private float clip0 = -1.0E9f;
	private float clip1 = 1.0E9f;

	private Panel draggingPanel;
	private float dragOffX;
	private float dragOffY;
	private float dragStartX;
	private float dragStartY;
	private boolean dragMoved;
	private int dragButton;
	private Hot draggingHot;
	private KeybindSetting listening;

	private long lastNanos = System.nanoTime();
	private final long openedAt = System.currentTimeMillis();
	private Hot hoveredHot;
	private long hoverSince;
	private long resetConfirmUntil;

	public ClickGuiScreen() {
		super(Component.literal(MogsClient.FULL_NAME));
		for (Category category : Category.values()) {
			Panel panel = new Panel(category);
			int[] saved = ConfigManager.panelPos.get(category.name());
			panel.x = saved != null ? saved[0] : -1f;
			panel.y = saved != null ? saved[1] : TOP;
			panels.add(panel);
		}
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

		int count = Category.values().length;
		float totalW = count * PW + (count - 1) * GAP;
		scale = Math.max(0.5f, Math.min(ClickGuiModule.guiScale(), (width - 24f) / totalW));
		viewW = width / scale;
		viewH = height / scale;

		float startX = (viewW - totalW) / 2f;
		for (int i = 0; i < panels.size(); i++) {
			Panel panel = panels.get(i);
			if (panel.x < 0) {
				panel.x = startX + i * (PW + GAP);
				panel.y = TOP;
			}
			panel.x = Math.max(0f, Math.min(viewW - PW, panel.x));
			panel.y = Math.max(0f, Math.min(viewH - HEAD - 8f, panel.y));
		}

		hots.clear();
		g.fill(0, 0, width, height, 0x99000000);

		float lx = mouseX / scale;
		float ly = mouseY / scale;
		float animK = ClickGuiModule.animationsEnabled() ? Math.min(1f, dt * 14f * ClickGuiModule.animationSpeed()) : 1f;

		Render2D.setTransform(0, 0, scale);
		try {
			drawTop(g);
			for (Panel panel : panels) {
				drawPanel(g, panel, lx, ly, animK);
			}
			drawBottom(g, lx, ly);
			updateHover(lx, ly);
			drawTooltip(g, lx, ly);
		} finally {
			Render2D.resetTransform();
		}
	}

	// ---- top bar

	private void drawTop(GuiGraphics g) {
		Render2D.rect(g, 0, 0, viewW, 2, Theme.gold());
		Render2D.scaledText(g, MogsClient.NAME, 14, 10, 2f, Theme.gold(), true);
		Render2D.text(g, "Client", 14 + TextUtil.width(MogsClient.NAME) * 2 + 6, 17, Theme.MUTED);
		Render2D.text(g, MogsClient.SUBTITLE, 14, 30, Theme.MUTED);

		float sbW = 240f;
		float sbX = (viewW - sbW) / 2f;
		float sbY = 14f;
		boolean active = !search.isEmpty();
		Render2D.rect(g, sbX, sbY, sbW, 22, Theme.FIELD);
		Render2D.outline(g, sbX, sbY, sbW, 22, active ? Theme.red() : Theme.BORDER, 1);
		boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
		if (active) {
			Render2D.text(g, TextUtil.fit(search, (int) sbW - 20) + (caret ? "_" : ""), sbX + 8, sbY + 7, Theme.TEXT);
		} else {
			Render2D.text(g, "Search modules" + (caret ? "_" : ""), sbX + 8, sbY + 7, Theme.OFF);
		}
		addHot(sbX, sbY, sbW, 22, "Type to search every module by name or description", (btn, mx) -> {
		}, null);
	}

	// ---- bottom buttons

	private void drawBottom(GuiGraphics g, float lx, float ly) {
		float bw = 96f;
		float bh = 22f;
		float gap = 10f;
		float x = (viewW - (bw * 3 + gap * 2)) / 2f;
		float y = viewH - 32f;
		boolean confirming = System.currentTimeMillis() < resetConfirmUntil;

		button(g, x, y, bw, bh, "Edit HUD", lx, ly, Theme.TEXT);
		addHot(x, y, bw, bh, "Drag HUD elements to a new position", (btn, mx) ->
				Minecraft.getInstance().setScreen(new HudEditorScreen(this)), null);

		float x2 = x + bw + gap;
		button(g, x2, y, bw, bh, confirming ? "Click to confirm" : "Reset Config", lx, ly,
				confirming ? Theme.red() : Theme.MUTED);
		addHot(x2, y, bw, bh, "Reset every module, keybind, HUD position and panel position", (btn, mx) -> {
			if (System.currentTimeMillis() < resetConfirmUntil) {
				ConfigManager.resetAll();
				anims.clear();
				expanded.clear();
				for (Panel panel : panels) {
					panel.x = -1f;
					panel.y = TOP;
					panel.scroll = 0f;
					panel.scrollTarget = 0f;
				}
				resetConfirmUntil = 0;
				NotificationManager.push(MogsClient.NAME, "Configuration reset", 2500, 1);
			} else {
				resetConfirmUntil = System.currentTimeMillis() + 3000;
			}
		}, null);

		float x3 = x2 + bw + gap;
		button(g, x3, y, bw, bh, "Close", lx, ly, Theme.MUTED);
		addHot(x3, y, bw, bh, "Close the menu", (btn, mx) -> onClose(), null);
	}

	private void button(GuiGraphics g, float x, float y, float w, float h, String label, float lx, float ly, int textColor) {
		boolean hover = Render2D.inside(lx, ly, x, y, w, h);
		Render2D.rect(g, x, y, w, h, hover ? Theme.CARD_HOVER : Theme.FIELD);
		Render2D.outline(g, x, y, w, h, hover ? Theme.red() : Theme.BORDER, 1);
		Render2D.centered(g, label, x + w / 2f, y + (h - 8) / 2f, hover && textColor == Theme.MUTED ? Theme.TEXT : textColor);
	}

	// ---- panels

	private List<Module> modulesOf(Category category) {
		if (search.isEmpty()) {
			return ModuleManager.byCategory(category);
		}
		List<Module> result = new ArrayList<>();
		for (Module module : ModuleManager.search(search)) {
			if (module.category() == category) {
				result.add(module);
			}
		}
		return result;
	}

	private Anim anim(Module m) {
		return anims.computeIfAbsent(m, key -> {
			Anim a = new Anim();
			a.on = key.isEnabled() ? 1f : 0f;
			return a;
		});
	}

	private void drawPanel(GuiGraphics g, Panel p, float lx, float ly, float animK) {
		List<Module> mods = modulesOf(p.category);
		boolean searching = !search.isEmpty();
		p.visible = !(searching && mods.isEmpty());
		if (!p.visible) {
			return;
		}
		boolean closed = ConfigManager.panelClosed.contains(p.category.name()) && !searching;
		float x = p.x;
		float y = p.y;

		boolean headHover = Render2D.inside(lx, ly, x, y, PW, HEAD);
		Render2D.rect(g, x, y, PW, HEAD, headHover ? Theme.CARD_HOVER : Theme.CARD);
		Render2D.rect(g, x, y, PW, 2, Theme.red());
		Render2D.text(g, p.category.display(), x + 8, y + 8, Theme.gold());
		String count = Integer.toString(mods.size());
		Render2D.text(g, closed ? ">" : "v", x + PW - 12, y + 8, Theme.MUTED);
		Render2D.text(g, count, x + PW - 20 - TextUtil.width(count), y + 8, Theme.OFF);

		if (closed) {
			Render2D.outline(g, x, y, PW, HEAD, Theme.BORDER, 1);
			return;
		}

		float bodyTop = y + HEAD;
		float contentH = 0f;
		for (Module m : mods) {
			Anim a = anim(m);
			float target = expanded.contains(m) ? 1f : 0f;
			a.expand += (target - a.expand) * animK;
			if (Math.abs(target - a.expand) < 0.004f) {
				a.expand = target;
			}
			contentH += ROW + a.expand * settingsHeight(m);
		}

		float avail = viewH - BOTTOM - bodyTop;
		p.viewH = Math.max(ROW, Math.min(contentH, avail));
		p.maxScroll = Math.max(0f, contentH - p.viewH);
		p.scrollTarget = Math.max(0f, Math.min(p.maxScroll, p.scrollTarget));
		p.scroll += (p.scrollTarget - p.scroll) * (ClickGuiModule.animationsEnabled() ? Math.min(1f, animK * 1.2f) : 1f);
		if (Math.abs(p.scrollTarget - p.scroll) < 0.2f) {
			p.scroll = p.scrollTarget;
		}

		Render2D.rect(g, x, bodyTop, PW, p.viewH, Theme.BG);
		Render2D.clip(g, x, bodyTop, PW, p.viewH);
		clip0 = bodyTop;
		clip1 = bodyTop + p.viewH;
		float cy = bodyTop - p.scroll;
		for (Module m : mods) {
			cy += drawModule(g, m, x, cy, lx, ly, animK);
		}
		g.disableScissor();
		clip0 = -1.0E9f;
		clip1 = 1.0E9f;

		Render2D.outline(g, x, y, PW, HEAD + p.viewH, Theme.BORDER, 1);
		if (p.maxScroll > 0f) {
			float trackH = p.viewH - 4f;
			float thumbH = Math.max(18f, trackH * p.viewH / contentH);
			float thumbY = bodyTop + 2f + (trackH - thumbH) * (p.scroll / p.maxScroll);
			Render2D.rect(g, x + PW - 4, bodyTop + 2, 2, trackH, Theme.FIELD);
			Render2D.rect(g, x + PW - 4, thumbY, 2, thumbH, Theme.red());
		}
	}

	private float drawModule(GuiGraphics g, Module m, float x, float y, float lx, float ly, float animK) {
		Anim a = anim(m);
		float rowH = ROW + a.expand * settingsHeight(m);
		if (y + rowH < clip0 || y > clip1) {
			return rowH;
		}

		boolean hover = hoverIn(lx, ly, x, y, PW, ROW);
		a.hover += ((hover ? 1f : 0f) - a.hover) * animK;
		a.on += ((m.isEnabled() ? 1f : 0f) - a.on) * animK;

		final boolean plain = m.isSettingsOnly();
		Render2D.rect(g, x, y, PW, ROW, Render2D.lerp(Theme.CARD, Theme.CARD_HOVER, a.hover));
		if (!plain) {
			Render2D.rect(g, x, y, PW, ROW, Render2D.withAlpha(Theme.red() & 0xFFFFFF, Math.round(0x38 * a.on)));
		}
		Render2D.rect(g, x, y, 2, ROW, plain ? Theme.OFF : Render2D.lerp(Theme.OFF, Theme.red(), a.on));
		int nameColor = plain ? Theme.TEXT : Render2D.lerp(Theme.MUTED, Theme.TEXT, a.on);
		nameColor = Render2D.lerp(nameColor, Theme.gold(), a.hover * 0.6f);
		Render2D.text(g, TextUtil.fit(m.name(), PW - 34), x + 9, y + 6, nameColor);
		Render2D.text(g, a.expand > 0.5f ? "v" : ">", x + PW - 12, y + 6, Theme.MUTED);
		addHot(x, y, PW, ROW, m.description(), (btn, mx) -> {
			if (btn == 1 || plain || mx > x + PW - 20f) {
				toggleExpanded(m);
			} else {
				m.toggle();
			}
		}, null);

		if (a.expand > 0.005f) {
			float sTop = y + ROW;
			float sH = rowH - ROW;
			boolean clipping = a.expand < 0.999f;
			if (clipping) {
				Render2D.clip(g, x, sTop, PW, sH);
			}
			Render2D.rect(g, x, sTop, PW, sH, Theme.FIELD);
			boolean live = a.expand > 0.95f;
			float sy = sTop + 4f;
			if (!plain) {
				sy += drawSetting(g, m.keybind(), x, sy, live, lx, ly);
			}
			for (Setting<?> setting : m.visibleSettings()) {
				sy += drawSetting(g, setting, x, sy, live, lx, ly);
			}
			sy += 4f;
			boolean resetHover = live && hoverIn(lx, ly, x + 8, sy, INNER, RESET_H);
			Render2D.rect(g, x + 8, sy, INNER, RESET_H, resetHover ? Theme.CARD_HOVER : Theme.CARD);
			Render2D.outline(g, x + 8, sy, INNER, RESET_H, resetHover ? Theme.red() : Theme.BORDER, 1);
			Render2D.centered(g, "Reset settings", x + PW / 2f, sy + 5, Theme.red());
			if (live) {
				addHot(x + 8, sy, INNER, RESET_H, "Restore every setting of " + m.name() + " to its default",
						(btn, mx) -> m.resetSettings(), null);
			}
			if (clipping) {
				g.disableScissor();
			}
		}
		return rowH;
	}

	private void toggleExpanded(Module m) {
		if (!expanded.remove(m)) {
			expanded.add(m);
		}
	}

	// ---- settings rows

	private float rowHeight(Setting<?> s) {
		if (s instanceof NumberSetting) {
			return 28f;
		}
		if (s instanceof ColorSetting) {
			return 44f;
		}
		if (s instanceof BoolSetting) {
			return 20f;
		}
		return 22f;
	}

	private float settingsHeight(Module m) {
		float total = 4f;
		if (!m.isSettingsOnly()) {
			total += rowHeight(m.keybind());
		}
		for (Setting<?> setting : m.visibleSettings()) {
			total += rowHeight(setting);
		}
		return total + 4f + RESET_H + 6f;
	}

	private boolean hoverIn(float lx, float ly, float x, float y, float w, float h) {
		return ly >= clip0 && ly < clip1 && Render2D.inside(lx, ly, x, y, w, h);
	}

	private float drawSetting(GuiGraphics g, Setting<?> s, float panelX, float y, boolean live, float lx, float ly) {
		final float x = panelX + 8f;
		final float w = INNER;
		float rowH = rowHeight(s);
		boolean hover = live && hoverIn(lx, ly, x, y, w, rowH);
		int labelColor = hover ? Theme.gold() : Theme.TEXT;
		String tip = s.description;

		if (s instanceof BoolSetting bs) {
			Render2D.text(g, TextUtil.fit(bs.name, (int) w - 34), x, y + 6, labelColor);
			float sx = x + w - 26f;
			Render2D.rect(g, sx, y + 4, 26, 12, bs.on() ? Theme.red() : Theme.OFF);
			Render2D.rect(g, sx + (bs.on() ? 15 : 2), y + 6, 9, 8, bs.on() ? 0xFFFFFFFF : 0xFF8B8B95);
			if (live) {
				addHot(x, y, w, rowH, tip, (btn, mx) -> {
					bs.toggle();
					ConfigManager.markDirty();
				}, null);
			}
		} else if (s instanceof NumberSetting ns) {
			Render2D.text(g, TextUtil.fit(ns.name, (int) w - 40), x, y + 3, labelColor);
			String value = ns.display();
			Render2D.text(g, value, x + w - TextUtil.width(value), y + 3, Theme.gold());
			float trackY = y + 17f;
			Render2D.rect(g, x, trackY, w, 4, Theme.CARD);
			float fillW = (float) (w * ns.fraction());
			Render2D.rect(g, x, trackY, fillW, 4, Theme.red());
			Render2D.rect(g, x + Math.max(0f, Math.min(w - 4f, fillW - 2f)), trackY - 3, 4, 10, 0xFFFFFFFF);
			if (live) {
				Drag drag = mx -> {
					ns.setFraction((mx - x) / w);
					ConfigManager.markDirty();
				};
				addHot(x, y, w, rowH, tip, (btn, mx) -> drag.run(mx), drag);
			}
		} else if (s instanceof ModeSetting ms) {
			Render2D.text(g, TextUtil.fit(ms.name, 56), x, y + 7, labelColor);
			String text = TextUtil.fit(ms.get(), (int) (w - 58f - 12f));
			float bw = TextUtil.width(text) + 12f;
			float bx = x + w - bw;
			Render2D.rect(g, bx, y + 2, bw, 18, Theme.CARD);
			Render2D.outline(g, bx, y + 2, bw, 18, hover ? Theme.red() : Theme.BORDER, 1);
			Render2D.centered(g, text, bx + bw / 2f, y + 7, Theme.TEXT);
			if (live) {
				addHot(x, y, w, rowH, tip + " (left click: next, right click: previous)", (btn, mx) -> {
					if (btn == 1) {
						ms.previous();
					} else {
						ms.next();
					}
					ConfigManager.markDirty();
				}, null);
			}
		} else if (s instanceof ColorSetting cs) {
			Render2D.text(g, cs.name, x, y + 3, labelColor);
			Render2D.rect(g, x + w - 14, y + 2, 14, 10, cs.argb());
			float swatch = (w - 7 * 3f) / 8f;
			for (int i = 0; i < ColorSetting.PRESETS.length; i++) {
				final int rgb = ColorSetting.PRESETS[i];
				float sx = x + i * (swatch + 3f);
				Render2D.rect(g, sx, y + 16, swatch, 10, 0xFF000000 | rgb);
				if (cs.get() == rgb) {
					Render2D.outline(g, sx - 1, y + 15, swatch + 2, 12, Theme.gold(), 1);
				}
				if (live) {
					addHot(sx, y + 16, swatch, 10, tip, (btn, mx) -> {
						cs.set(rgb);
						ConfigManager.markDirty();
					}, null);
				}
			}
			float step = w / 36f;
			for (int i = 0; i < 36; i++) {
				int rgb = java.awt.Color.HSBtoRGB(i / 36f, 0.85f, 1f);
				Render2D.rect(g, x + i * step, y + 31, step + 0.5f, 8, 0xFF000000 | (rgb & 0xFFFFFF));
			}
			Render2D.rect(g, x + w * cs.hue() - 1, y + 29, 2, 12, 0xFFFFFFFF);
			if (live) {
				Drag drag = mx -> {
					cs.setHue((mx - x) / w);
					ConfigManager.markDirty();
				};
				addHot(x, y + 29, w, 12, tip + " (drag the hue strip)", (btn, mx) -> drag.run(mx), drag);
			}
		} else if (s instanceof KeybindSetting ks) {
			Render2D.text(g, TextUtil.fit(ks.name, 70), x, y + 7, labelColor);
			boolean isListening = listening == ks;
			String text = isListening ? "..." : Compat.keyName(ks.key());
			float bw = Math.max(44f, TextUtil.width(text) + 14f);
			float bx = x + w - bw;
			Render2D.rect(g, bx, y + 2, bw, 18, Theme.CARD);
			Render2D.outline(g, bx, y + 2, bw, 18, isListening ? Theme.gold() : Theme.BORDER, 1);
			Render2D.centered(g, text, bx + bw / 2f, y + 7,
					isListening ? Theme.gold() : ks.key() < 0 ? Theme.OFF : Theme.TEXT);
			if (live) {
				addHot(bx, y + 2, bw, 18, "Click, then press a key. Backspace clears, Esc cancels.", (btn, mx) -> {
					listening = ks;
				}, null);
			}
		} else if (s instanceof ActionSetting action) {
			Render2D.rect(g, x, y + 2, w, 18, hover ? Theme.CARD_HOVER : Theme.CARD);
			Render2D.outline(g, x, y + 2, w, 18, hover ? Theme.red() : Theme.BORDER, 1);
			Render2D.centered(g, action.name, x + w / 2f, y + 7, Theme.TEXT);
			if (live) {
				addHot(x, y + 2, w, 18, tip, (btn, mx) -> action.run(), null);
			}
		}
		return rowH;
	}

	// ---- tooltips and hot regions

	private void addHot(float x, float y, float w, float h, String tip, Click click, Drag drag) {
		Hot hot = new Hot();
		hot.x = x;
		hot.y = y;
		hot.w = w;
		hot.h = h;
		hot.clip0 = clip0;
		hot.clip1 = clip1;
		hot.tip = tip;
		hot.click = click;
		hot.drag = drag;
		hots.add(hot);
	}

	private Hot hotAt(float lx, float ly) {
		for (int i = hots.size() - 1; i >= 0; i--) {
			Hot hot = hots.get(i);
			if (ly < hot.clip0 || ly >= hot.clip1) {
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
				|| draggingHot != null || draggingPanel != null || System.currentTimeMillis() - hoverSince < 350) {
			return;
		}
		String text = TextUtil.fit(hoveredHot.tip, 300);
		float w = TextUtil.width(text) + 14f;
		float h = 18f;
		float x = Math.min(viewW - w - 4f, lx + 12f);
		float y = Math.min(viewH - h - 4f, ly + 14f);
		Render2D.rect(g, x, y, w, h, 0xF2060608);
		Render2D.outline(g, x, y, w, h, Theme.red(), 1);
		Render2D.text(g, text, x + 7, y + 5, Theme.TEXT);
	}

	// ------------------------------------------------------------------ input

	private float localX(double mouseX) {
		return (float) (mouseX / scale);
	}

	private float localY(double mouseY) {
		return (float) (mouseY / scale);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		float lx = localX(event.x());
		float ly = localY(event.y());
		int button = event.button();

		if (listening != null) {
			listening = null;
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
		for (Panel panel : panels) {
			if (panel.visible && Render2D.inside(lx, ly, panel.x, panel.y, PW, HEAD)) {
				draggingPanel = panel;
				dragOffX = lx - panel.x;
				dragOffY = ly - panel.y;
				dragStartX = lx;
				dragStartY = ly;
				dragMoved = false;
				dragButton = button;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingHot != null && draggingHot.drag != null) {
			draggingHot.drag.run(localX(event.x()));
			return true;
		}
		if (draggingPanel != null) {
			float lx = localX(event.x());
			float ly = localY(event.y());
			if (!dragMoved && Math.abs(lx - dragStartX) + Math.abs(ly - dragStartY) > 4f) {
				dragMoved = true;
			}
			if (dragMoved) {
				draggingPanel.x = lx - dragOffX;
				draggingPanel.y = ly - dragOffY;
			}
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = draggingHot != null || draggingPanel != null;
		if (draggingPanel != null) {
			Panel panel = draggingPanel;
			String key = panel.category.name();
			if (dragMoved) {
				ConfigManager.panelPos.put(key, new int[]{Math.round(panel.x), Math.round(panel.y)});
			} else if (dragButton == 0 || dragButton == 1) {
				if (!ConfigManager.panelClosed.remove(key)) {
					ConfigManager.panelClosed.add(key);
				}
			}
			ConfigManager.markDirty();
		}
		draggingHot = null;
		draggingPanel = null;
		if (handled) {
			ConfigManager.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		float lx = localX(mouseX);
		float ly = localY(mouseY);
		for (Panel panel : panels) {
			if (panel.visible && Render2D.inside(lx, ly, panel.x, panel.y + HEAD, PW, panel.viewH)) {
				panel.scrollTarget = Math.max(0f, Math.min(panel.maxScroll, panel.scrollTarget - (float) scrollY * 30f));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (handleKey(event.key())) {
			return true;
		}
		return super.keyPressed(event);
	}

	private boolean handleKey(int key) {
		if (listening != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				// cancel, keep the old binding
			} else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
				listening.set(KeybindSetting.NONE);
				ConfigManager.markDirty();
			} else {
				listening.set(key);
				ConfigManager.markDirty();
			}
			listening = null;
			return true;
		}
		if (key == ClickGuiModule.openKey()) {
			if (System.currentTimeMillis() - openedAt > 300) {
				onClose();
			}
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			if (System.currentTimeMillis() - openedAt < 400) {
				return true; // Esc is still held from the Esc + Right Shift chord
			}
			if (!search.isEmpty()) {
				search = "";
			} else {
				onClose();
			}
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!search.isEmpty()) {
				search = search.substring(0, search.length() - 1);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (listening != null) {
			return true;
		}
		int codepoint = event.codepoint();
		if (codepoint >= 32 && codepoint != 127 && Character.isValidCodePoint(codepoint) && search.length() < 32) {
			search += new String(Character.toChars(codepoint));
			return true;
		}
		return super.charTyped(event);
	}
}
