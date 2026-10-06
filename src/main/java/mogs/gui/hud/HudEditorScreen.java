package mogs.gui.hud;

import mogs.MogsClient;
import mogs.config.ConfigManager;
import mogs.module.Module;
import mogs.module.ModuleManager;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag any enabled HUD element to a new position; scroll over it to change its scale.
 * Sample content is shown for elements that are normally hidden (no target, no effects, ...).
 */
public class HudEditorScreen extends Screen {
	private final Screen parent;
	private Module dragging;
	private float grabX;
	private float grabY;

	public HudEditorScreen(Screen parent) {
		super(Component.literal(MogsClient.NAME + " HUD Editor"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Module.setPreview(true);
	}

	@Override
	public void removed() {
		Module.setPreview(false);
		ConfigManager.save();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	private List<Module> elements() {
		List<Module> result = new ArrayList<>();
		for (Module module : ModuleManager.all()) {
			if (module.isEnabled() && module.hasHud() && module.hudVisible()) {
				result.add(module);
			}
		}
		return result;
	}

	private float widthOf(Module m) {
		return m.hudWidth() * m.hudScale();
	}

	private float heightOf(Module m) {
		return m.hudHeight() * m.hudScale();
	}

	private Module elementAt(double mx, double my) {
		List<Module> list = elements();
		for (int i = list.size() - 1; i >= 0; i--) {
			Module m = list.get(i);
			if (Render2D.inside(mx, my, m.hudX(), m.hudY(), widthOf(m), heightOf(m))) {
				return m;
			}
		}
		return null;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x80000000);
		Render2D.rect(g, width / 2f, 0, 1, height, Render2D.withAlpha(0xFFFFFF, 0x18));
		Render2D.rect(g, 0, height / 2f, width, 1, Render2D.withAlpha(0xFFFFFF, 0x18));

		Module hover = dragging != null ? dragging : elementAt(mouseX, mouseY);
		for (Module m : elements()) {
			ModuleManager.renderHudElement(g, m);
			boolean active = m == hover;
			Render2D.outline(g, m.hudX() - 1, m.hudY() - 1, widthOf(m) + 2, heightOf(m) + 2,
					active ? Theme.gold() : Render2D.alpha(Theme.red(), 0.7f), 1);
			if (active) {
				Render2D.textShadow(g, m.name(), m.hudX(), Math.max(1, m.hudY() - 11), Theme.gold());
			}
		}

		Render2D.centered(g, MogsClient.NAME + " HUD Editor", width / 2f, 14, Theme.gold());
		Render2D.centered(g, "Drag elements to move them. Scroll over one to resize it. Only enabled modules appear.",
				width / 2f, 27, Theme.MUTED);

		float bw = 90;
		float bx = width / 2f - bw / 2f;
		float by = height - 34;
		boolean over = Render2D.inside(mouseX, mouseY, bx, by, bw, 20);
		Render2D.rect(g, bx, by, bw, 20, over ? Theme.red() : Theme.FIELD);
		Render2D.outline(g, bx, by, bw, 20, over ? Theme.gold() : Theme.red(), 1);
		Render2D.centered(g, "Done", width / 2f, by + 6, Theme.TEXT);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		float bw = 90;
		if (Render2D.inside(mx, my, width / 2f - bw / 2f, height - 34, bw, 20)) {
			onClose();
			return true;
		}
		Module m = elementAt(mx, my);
		if (m != null) {
			dragging = m;
			grabX = (float) mx - m.hudX();
			grabY = (float) my - m.hudY();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging != null) {
			float x = (float) event.x() - grabX;
			float y = (float) event.y() - grabY;
			x = Math.max(0, Math.min(width - widthOf(dragging), x));
			y = Math.max(0, Math.min(height - heightOf(dragging), y));
			dragging.setHudPosition(Math.round(x), Math.round(y));
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			ConfigManager.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		Module m = elementAt(mouseX, mouseY);
		if (m != null) {
			m.hudScaleSetting().set(m.hudScaleSetting().get() + Math.signum(scrollY) * 0.1);
			ConfigManager.markDirty();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}
}
