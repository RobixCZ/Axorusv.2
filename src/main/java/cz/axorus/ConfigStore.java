package cz.axorus;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int VERSION = 2;
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("axorus.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("v", VERSION);
            root.addProperty("theme", Themes.current);
            JsonArray borders = new JsonArray(), glows = new JsonArray();
            for (int i = 0; i < Themes.ALL.length; i++) { borders.add(Themes.BORDER[i]); glows.add(Themes.GLOW[i]); }
            root.add("borders", borders);
            root.add("glows", glows);
            JsonObject mods = new JsonObject();
            for (Module m : Modules.ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("key", m.key);
                o.addProperty("hold", m.hold);
                JsonObject s = new JsonObject();
                for (Setting st : m.settings) s.addProperty(st.name, st.index);
                o.add("settings", s);
                mods.add(m.name, o);
            }
            root.add("modules", mods);
            Files.writeString(path(), GSON.toJson(root));
        } catch (Exception ignored) {}
    }

    public static void load() {
        try {
            if (!Files.exists(path())) return;
            JsonObject root = JsonParser.parseString(Files.readString(path())).getAsJsonObject();
            if (!root.has("v") || root.get("v").getAsInt() < VERSION) return; // starý formát -> výchozí hodnoty
            if (root.has("theme")) Themes.current = root.get("theme").getAsInt();
            if (root.has("borders")) {
                JsonArray a = root.getAsJsonArray("borders");
                for (int i = 0; i < Math.min(a.size(), Themes.BORDER.length); i++) Themes.BORDER[i] = a.get(i).getAsBoolean();
            }
            if (root.has("glows")) {
                JsonArray a = root.getAsJsonArray("glows");
                for (int i = 0; i < Math.min(a.size(), Themes.GLOW.length); i++) Themes.GLOW[i] = a.get(i).getAsBoolean();
            }
            JsonObject mods = root.getAsJsonObject("modules");
            for (Module m : Modules.ALL) {
                if (mods == null || !mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("hold")) m.hold = o.get("hold").getAsBoolean();
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting st : m.settings) {
                        if (!s.has(st.name)) continue;
                        int v = s.get(st.name).getAsInt();
                        st.index = st.slider ? Math.max(st.min, Math.min(st.max, v))
                                             : Math.max(0, Math.min(st.options.length - 1, v));
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
