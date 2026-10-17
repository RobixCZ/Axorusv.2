package cz.axorus;

/** Nastavení modulu: buď přepínač/výběr (klik), nebo slider (tažení myší / zadání čísla). */
public class Setting {
    public final String name;
    public final String[] options;
    public int index;               // u slideru je to přímo hodnota
    public final boolean slider;
    public final int min, max;
    public final String suffix;

    public Setting(String name, int defaultIndex, String... options) {
        this.name = name; this.options = options; this.index = defaultIndex;
        this.slider = false; this.min = 0; this.max = 0; this.suffix = "";
    }

    private Setting(String name, int def, int min, int max, String suffix) {
        this.name = name; this.options = new String[0]; this.index = def;
        this.slider = true; this.min = min; this.max = max; this.suffix = suffix;
    }

    public static Setting slider(String name, int def, int min, int max, String suffix) {
        return new Setting(name, def, min, max, suffix);
    }

    public static Setting toggle(String name, boolean on) {
        return new Setting(name, on ? 0 : 1, "On", "Off");
    }

    public boolean isOn() { return !slider && index == 0; }
    public String get() { return slider ? index + suffix : options[index]; }
    public void cycle() { if (!slider) index = (index + 1) % options.length; }
    public void set(int v) { index = Math.max(min, Math.min(max, v)); }
}
