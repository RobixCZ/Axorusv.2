package cz.axorus;

import java.util.List;

public class Modules {
    /** Vše kolem cílového HUD je v configu tohoto modulu (Skin/HP/Tier/Gear fungují jen když je zapnutý). */
    public static final Module TARGET_HUD = new Module("Target HUD", "Visual", true)
            .add(Setting.toggle("Skin", true))
            .add(Setting.toggle("HP", true))
            .add(Setting.toggle("Tier", true))
            .add(Setting.toggle("Gear", true))
            .add(Setting.slider("Scale", 100, 50, 200, "%"))
            .add(Setting.slider("Opacity", 75, 0, 100, "%"))
            .add(new Setting("Duration", 1, "3s", "6s", "10s", "15s"))
            .add(new Setting("Position", 0, "Right", "Left"));

    /** Prohodí barvu výplně a okrajů. Má vlastní keybind. */
    public static final Module INVERT = new Module("Invert", "Config", false);

    public static final List<Module> ALL = List.of(TARGET_HUD, INVERT);
}
