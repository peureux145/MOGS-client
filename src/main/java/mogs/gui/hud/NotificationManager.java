package mogs.gui.hud;

import mogs.MogsClient;
import mogs.module.Module;
import mogs.module.ModuleManager;
import mogs.module.client.NotificationsModule;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Small toast notifications in one of the four screen corners. */
public final class NotificationManager {
	public static final String[] ANCHORS = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};

	private record Note(String title, String body, long start, long duration, int anchor) {
	}

	private static final List<Note> NOTES = new ArrayList<>();
	private static final int MAX_NOTES = 8;

	private NotificationManager() {
	}

	public static void push(String title, String body, long durationMs, int anchor) {
		if (NOTES.size() >= MAX_NOTES) {
			NOTES.remove(0);
		}
		NOTES.add(new Note(title, body, System.currentTimeMillis(), Math.max(500L, durationMs),
				Math.max(0, Math.min(3, anchor))));
	}

	/** Called by Module.setEnabled; honours the Client > Notifications settings. */
	public static void moduleToggled(Module module) {
		NotificationsModule settings = ModuleManager.get(NotificationsModule.class);
		if (settings == null || !settings.isEnabled() || !settings.toggleAlerts()) {
			return;
		}
		push(MogsClient.NAME, module.name() + (module.isEnabled() ? " enabled" : " disabled"),
				settings.durationMs(), settings.anchor());
	}

	public static void render(GuiGraphics graphics) {
		long now = System.currentTimeMillis();
		NOTES.removeIf(note -> now - note.start() > note.duration());
		if (NOTES.isEmpty()) {
			return;
		}
		int screenW = graphics.guiWidth();
		int screenH = graphics.guiHeight();
		int margin = 10;
		int height = 28;
		int[] stacked = new int[4];

		for (Note note : NOTES) {
			long age = now - note.start();
			long left = note.duration() - age;
			float fade = Math.min(1f, Math.min(age / 150f, left / 250f));
			fade = Math.max(0f, fade);

			int width = Math.max(150, Math.max(TextUtil.width(note.title()), TextUtil.width(note.body())) + 22);
			boolean leftSide = note.anchor() == 0 || note.anchor() == 2;
			boolean top = note.anchor() == 0 || note.anchor() == 1;
			float slide = (1f - fade) * 14f * (leftSide ? -1f : 1f);

			float x = (leftSide ? margin : screenW - width - margin) + slide;
			float y = top ? margin + stacked[note.anchor()] : screenH - margin - height - stacked[note.anchor()];
			stacked[note.anchor()] += height + 4;

			Render2D.rect(graphics, x, y, width, height, Render2D.alpha(0xF0101014, fade));
			Render2D.rect(graphics, x, y, 2, height, Render2D.alpha(Theme.red(), fade));
			Render2D.rect(graphics, x, y + height - 1, width, 1, Render2D.alpha(Theme.BORDER, fade));
			Render2D.text(graphics, note.title(), x + 9, y + 5, Render2D.alpha(Theme.gold(), fade));
			Render2D.text(graphics, note.body(), x + 9, y + 16, Render2D.alpha(Theme.TEXT, fade));
		}
	}
}
