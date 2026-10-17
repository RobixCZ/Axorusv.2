package cz.axorus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stahuje FlowPvP tier asynchronně a cachuje ho.
 * !!! ENDPOINT je zatím odhad - flowpvp.gg nemá zveřejněné API. Správnou adresu najdeš v prohlížeči:
 * otevři profil hráče na flowpvp.gg, F12 -> Network -> Fetch/XHR a zkopíruj URL, která vrací JSON.
 * Použij v ní "%s" tam, kde je nick hráče.
 */
public class TierFetcher {
    private static final String ENDPOINT = "https://flowpvp.gg/api/player/%s";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static String get(String name) {
        String key = name.toLowerCase();
        String cached = CACHE.get(key);
        if (cached != null) return cached;
        CACHE.put(key, "...");
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(String.format(ENDPOINT, name)))
                    .timeout(Duration.ofSeconds(6)).header("Accept", "application/json").GET().build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
                if (res.statusCode() == 200) CACHE.put(key, parse(res.body()));
                else CACHE.put(key, res.statusCode() == 404 ? "Unranked" : "N/A");
            }).exceptionally(e -> { CACHE.put(key, "N/A"); return null; });
        } catch (Exception e) {
            CACHE.put(key, "N/A");
        }
        return "...";
    }

    /** Odolný parser: hledá pole tier a rating (tr/sr/elo) kdekoliv v JSONu. */
    private static String parse(String body) {
        try {
            JsonElement root = JsonParser.parseString(body);
            String tier = find(root, "tier", "highesttier", "rank");
            String rating = find(root, "tr", "sr", "elo", "rating");
            if (tier == null && rating == null) return "Unranked";
            return (tier == null ? "?" : tier) + (rating == null ? "" : " (" + rating + " TR)");
        } catch (Exception ex) {
            return "N/A";
        }
    }

    private static String find(JsonElement e, String... keys) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            for (String k : keys)
                for (var entry : o.entrySet())
                    if (entry.getKey().equalsIgnoreCase(k) && entry.getValue().isJsonPrimitive())
                        return entry.getValue().getAsString();
            for (var entry : o.entrySet()) {
                String r = find(entry.getValue(), keys);
                if (r != null) return r;
            }
        } else if (e.isJsonArray()) {
            for (JsonElement c : e.getAsJsonArray()) {
                String r = find(c, keys);
                if (r != null) return r;
            }
        }
        return null;
    }
}
