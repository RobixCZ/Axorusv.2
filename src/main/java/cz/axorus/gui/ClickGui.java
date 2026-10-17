package cz.axorus.gui;

import cz.axorus.*;
import cz.axorus.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static class Panel {
        String title; int x, y; final int w;
        List<Module> modules = new ArrayList<>();
        Panel(String t, int x, int y, int w) { title = t; this.x = x; this.y = y; this.w = w; }
    }

    private static final List<Panel> PANELS = new ArrayList<>();
    private static int themePage = 0;
    private static final int PER_PAGE = 9;

    private boolean lmbDown, rmbDown, clicked, rclicked;
    private final boolean[] prevKeys = new boolean[350];
    private Panel dragging; private int dragDX, dragDY;
    private Module listening = null;                       // čeká na stisk klávesy
    private Setting editing = null; private String editBuf = "";   // psaní čísla do slideru
    private Setting dragSlider = null; private int sliderStartX, sliderStartVal; private boolean sliderMoved;

    public ClickGui() {
        super(Text.literal("Axorus"));
        if (PANELS.isEmpty()) {
            Panel visual = new Panel("Visual", 20, 20, 125);
            Panel colors = new Panel("Colors", 295, 20, 125);
            for (Module m : Modules.ALL) {
                if (m.category.equals("Visual")) visual.modules.add(m);
                else colors.modules.add(m);
            }
            PANELS.add(visual);
            PANELS.add(new Panel("Configs", 155, 20, 130));
            PANELS.add(colors);
            PANELS.add(new Panel("Menu", 430, 20, 125));
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return listening == null && editing == null; }
    @Override public void removed() { ConfigStore.save(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        clicked = l && !lmbDown; rclicked = r && !rmbDown;
        lmbDown = l; rmbDown = r;
        if (!l) dragging = null;
        if (clicked && editing != null) editing = null;    // klik mimo = zrušit psaní

        // tažení slideru myší doleva/doprava; krátký klik = psaní čísla
        if (dragSlider != null) {
            if (l) {
                int dx = mx - sliderStartX;
                if (Math.abs(dx) > 2) sliderMoved = true;
                if (sliderMoved) dragSlider.set(sliderStartVal + dx);
            } else {
                if (!sliderMoved) { editing = dragSlider; editBuf = ""; listening = null; snapshotKeys(); }
                dragSlider = null;
            }
        }

        pollKeys(win);

        ctx.fill(0, 0, width, height, 0x77000000);
        if (dragging != null) { dragging.x = mx - dragDX; dragging.y = my - dragDY; }

        for (Panel p : PANELS) {
            ctx.fill(p.x, p.y, p.x + p.w, p.y + 16, Themes.header());
            ctx.fill(p.x, p.y + 15, p.x + p.w, p.y + 16, Themes.edge());
            ctx.drawText(textRenderer, p.title, p.x + 6, p.y + 4, 0xFFFFFFFF, true);
            if (clicked && inside(mx, my, p.x, p.y, p.w, 16)) {
                dragging = p; dragDX = mx - p.x; dragDY = my - p.y; clicked = false;
            }
            int cy = p.y + 16;
            int end = switch (p.title) {
                case "Configs" -> drawConfigs(ctx, p, cy, mx, my);
                case "Colors" -> drawColors(ctx, p, cy, mx, my);
                case "Menu" -> drawMenu(ctx, p, cy);
                default -> drawModules(ctx, p, cy, mx, my);
            };
            Glow.box(ctx, p.x, p.y, p.x + p.w, end, Themes.edge(), Themes.border(), Themes.glow());
        }

        ctx.drawText(textRenderer, "AXORUS", 8, 6, Themes.edge(), true);

        if (listening != null) {
            String hint = "Stiskni klávesu pro " + listening.name + "  (ESC = zrušit, Backspace = smazat)";
            ctx.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, height - 20, 0xFFFFFFFF, true);
        } else if (editing != null) {
            String hint = "Piš číslo " + editing.min + "-" + editing.max + "  (Enter = potvrdit, ESC = zrušit)";
            ctx.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, height - 20, 0xFFFFFFFF, true);
        }
    }

    // ---------- moduly ----------
    private int drawModules(DrawContext ctx, Panel p, int cy, int mx, int my) {
        int main = Themes.main(), edge = Themes.edge();
        for (Module m : p.modules) {
            boolean hov = inside(mx, my, p.x, cy, p.w, 16);
            int bg = m.enabled ? (edge & 0xFFFFFF) | 0xB0000000 : (hov ? Themes.shade(main, 14) : main);
            ctx.fill(p.x, cy, p.x + p.w, cy + 16, bg);
            ctx.drawText(textRenderer, m.name, p.x + 6, cy + 4, m.enabled ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            String tag = m.key >= 0 ? "[" + keyName(m.key) + "]" : (m.expanded ? "-" : "+");
            ctx.drawText(textRenderer, tag, p.x + p.w - 6 - textRenderer.getWidth(tag), cy + 4, 0xFF888888, true);
            if (hov && clicked) { m.enabled = !m.enabled; clicked = false; }
            if (hov && rclicked) { m.expanded = !m.expanded; rclicked = false; }
            cy += 16;

            if (m.expanded) {
                if (row(ctx, p, cy, "Keybind", listening == m ? "..." : (m.key < 0 ? "None" : keyName(m.key)), mx, my)) {
                    listening = (listening == m) ? null : m;
                    editing = null;
                    snapshotKeys();
                }
                cy += 14;
                if (row(ctx, p, cy, "Mode", m.hold ? "Hold" : "Toggle", mx, my)) m.hold = !m.hold;
                cy += 14;
                for (Setting s : m.settings) {
                    if (s.slider) sliderRow(ctx, p, cy, s, mx, my);
                    else if (row(ctx, p, cy, s.name, s.get(), mx, my)) s.cycle();
                    cy += 14;
                }
            }
        }
        return cy;
    }

    private boolean row(DrawContext ctx, Panel p, int cy, String label, String value, int mx, int my) {
        int main = Themes.main(), edge = Themes.edge();
        boolean hov = inside(mx, my, p.x, cy, p.w, 14);
        ctx.fill(p.x, cy, p.x + p.w, cy + 14, hov ? Themes.shade(main, 14) : Themes.shade(main, -3));
        ctx.fill(p.x, cy, p.x + 2, cy + 14, edge);
        ctx.drawText(textRenderer, label, p.x + 8, cy + 3, 0xFFBBBBBB, true);
        ctx.drawText(textRenderer, value, p.x + p.w - 6 - textRenderer.getWidth(value), cy + 3, edge, true);
        if (hov && clicked) { clicked = false; return true; }
        return false;
    }

    /** Slider: táhni myší doleva/doprava, nebo klikni a napiš číslo. */
    private void sliderRow(DrawContext ctx, Panel p, int cy, Setting s, int mx, int my) {
        int main = Themes.main(), edge = Themes.edge();
        boolean hov = inside(mx, my, p.x, cy, p.w, 14);
        ctx.fill(p.x, cy, p.x + p.w, cy + 14, hov ? Themes.shade(main, 14) : Themes.shade(main, -3));
        float frac = (s.index - s.min) / (float) Math.max(1, s.max - s.min);
        ctx.fill(p.x + 2, cy + 12, p.x + 2 + (int) ((p.w - 2) * frac), cy + 14, edge);
        ctx.fill(p.x, cy, p.x + 2, cy + 14, edge);
        ctx.drawText(textRenderer, s.name, p.x + 8, cy + 2, 0xFFBBBBBB, true);
        String v = editing == s ? editBuf + "_" : s.get();
        ctx.drawText(textRenderer, v, p.x + p.w - 6 - textRenderer.getWidth(v), cy + 2, edge, true);
        if (hov && clicked) {
            dragSlider = s; sliderStartX = mx; sliderStartVal = s.index; sliderMoved = false;
            clicked = false;
        }
    }

    // ---------- configy / témata ----------
    private int drawConfigs(DrawContext ctx, Panel p, int cy, int mx, int my) {
        int main = Themes.main(), edge = Themes.edge();
        int pages = (Themes.ALL.length + PER_PAGE - 1) / PER_PAGE;
        int start = themePage * PER_PAGE;
        for (int i = start; i < Math.min(Themes.ALL.length, start + PER_PAGE); i++) {
            Themes.Theme t = Themes.ALL[i];
            boolean hov = inside(mx, my, p.x, cy, p.w, 16);
            boolean sel = i == Themes.current;
            ctx.fill(p.x, cy, p.x + p.w, cy + 16, hov ? Themes.shade(main, 14) : main);
            ctx.fill(p.x + 5, cy + 3, p.x + 15, cy + 13, t.accent);
            ctx.fill(p.x + 15, cy + 3, p.x + 25, cy + 13, t.bg);
            ctx.drawText(textRenderer, t.name, p.x + 30, cy + 4, sel ? 0xFFFFFFFF : 0xFFAAAAAA, true);
            if (sel) ctx.fill(p.x, cy, p.x + 2, cy + 16, edge);
            if (hov && clicked) { Themes.current = i; clicked = false; }
            cy += 16;
        }
        ctx.fill(p.x, cy, p.x + p.w, cy + 16, Themes.header());
        boolean hp = inside(mx, my, p.x, cy, p.w / 2, 16), hn = inside(mx, my, p.x + p.w / 2, cy, p.w / 2, 16);
        ctx.drawText(textRenderer, "< Prev", p.x + 6, cy + 4, hp ? 0xFFFFFFFF : 0xFF999999, true);
        String nx = "Next >";
        ctx.drawText(textRenderer, nx, p.x + p.w - 6 - textRenderer.getWidth(nx), cy + 4, hn ? 0xFFFFFFFF : 0xFF999999, true);
        String pg = (themePage + 1) + "/" + pages;
        ctx.drawText(textRenderer, pg, p.x + (p.w - textRenderer.getWidth(pg)) / 2, cy + 4, edge, true);
        if (clicked && hp) { themePage = (themePage - 1 + pages) % pages; clicked = false; }
        if (clicked && hn) { themePage = (themePage + 1) % pages; clicked = false; }
        return cy + 16;
    }

    /** Invert (s keybindem) + okraje a glow pro právě vybrané téma. */
    private int drawColors(DrawContext ctx, Panel p, int cy, int mx, int my) {
        cy = drawModules(ctx, p, cy, mx, my);
        int i = Math.max(0, Math.min(Themes.ALL.length - 1, Themes.current));
        if (row(ctx, p, cy, "Border", Themes.BORDER[i] ? "On" : "Off", mx, my)) Themes.BORDER[i] = !Themes.BORDER[i];
        cy += 14;
        if (row(ctx, p, cy, "Glow", Themes.GLOW[i] ? "On" : "Off", mx, my)) Themes.GLOW[i] = !Themes.GLOW[i];
        cy += 14;
        ctx.fill(p.x, cy, p.x + p.w, cy + 14, Themes.shade(Themes.main(), -3));
        ctx.drawText(textRenderer, Themes.get().name, p.x + 8, cy + 3, 0xFF777777, true);
        return cy + 14;
    }

    private int drawMenu(DrawContext ctx, Panel p, int cy) {
        ctx.fill(p.x, cy, p.x + p.w, cy + 62, Themes.main());
        ctx.drawText(textRenderer, "Menu Key: R-Shift", p.x + 6, cy + 5, 0xFFAAAAAA, true);
        ctx.drawText(textRenderer, "Theme: " + Themes.get().name, p.x + 6, cy + 18, Themes.edge(), true);
        ctx.drawText(textRenderer, "RMB na modul = config", p.x + 6, cy + 31, 0xFF777777, true);
        ctx.drawText(textRenderer, "Slider: tahej / klikni", p.x + 6, cy + 44, 0xFF777777, true);
        return cy + 62;
    }

    // ---------- klávesy ----------
    private void snapshotKeys() {
        long win = MinecraftClient.getInstance().getWindow().getHandle();
        for (int k = 32; k < 349; k++) prevKeys[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
    }

    private void pollKeys(long win) {
        if (listening == null && editing == null) return;
        for (int k = 32; k < 349; k++) {
            boolean down = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
            boolean edge = down && !prevKeys[k];
            prevKeys[k] = down;
            if (!edge || k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;

            if (editing != null) {
                if (k >= GLFW.GLFW_KEY_0 && k <= GLFW.GLFW_KEY_9) editBuf += (char) k;
                else if (k >= GLFW.GLFW_KEY_KP_0 && k <= GLFW.GLFW_KEY_KP_9) editBuf += (char) ('0' + k - GLFW.GLFW_KEY_KP_0);
                else if (k == GLFW.GLFW_KEY_BACKSPACE && !editBuf.isEmpty()) editBuf = editBuf.substring(0, editBuf.length() - 1);
                else if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                    if (!editBuf.isEmpty()) editing.set(Integer.parseInt(editBuf));
                    editing = null;
                } else if (k == GLFW.GLFW_KEY_ESCAPE) editing = null;
                if (editBuf.length() > 3) editBuf = editBuf.substring(0, 3);
            } else if (listening != null) {
                if (k == GLFW.GLFW_KEY_ESCAPE) listening = null;
                else if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) { listening.key = -1; listening = null; }
                else { listening.key = k; listening = null; }
            }
        }
    }

    static String keyName(int k) {
        switch (k) {
            case GLFW.GLFW_KEY_SPACE: return "Space";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RAlt";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps";
            case GLFW.GLFW_KEY_UP: return "Up";
            case GLFW.GLFW_KEY_DOWN: return "Down";
            case GLFW.GLFW_KEY_LEFT: return "Left";
            case GLFW.GLFW_KEY_RIGHT: return "Right";
            default:
        }
        if (k >= GLFW.GLFW_KEY_F1 && k <= GLFW.GLFW_KEY_F12) return "F" + (k - GLFW.GLFW_KEY_F1 + 1);
        String n = GLFW.glfwGetKeyName(k, 0);
        return n != null ? n.toUpperCase() : "K" + k;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
