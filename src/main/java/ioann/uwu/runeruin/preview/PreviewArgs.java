package ioann.uwu.runeruin.preview;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Seed, output name, and free-form params ({@code height}, {@code radius}, …).
 * Headless: {@code -Pseed=1 -Pheight=75} or {@code -Parg.bluntness=1.2}.
 * In-game: {@code /rrpreview <job> [seed] [name] [k=v]…}.
 */
public final class PreviewArgs {
    public static final String PROPERTY_PREFIX = "runeruin.preview.";

    private final Path exportDir;
    private final Map<String, String> values;

    public PreviewArgs(Path exportDir, Map<String, String> values) {
        this.exportDir = exportDir;
        this.values = new LinkedHashMap<>(values);
    }

    public static PreviewArgs fromSystem(Path exportDir) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (!key.startsWith(PROPERTY_PREFIX)) {
                continue;
            }
            String shortKey = key.substring(PROPERTY_PREFIX.length());
            if (shortKey.isEmpty()) {
                continue;
            }
            values.put(shortKey, String.valueOf(entry.getValue()));
        }
        return new PreviewArgs(exportDir, values);
    }

    public PreviewArgs with(String key, String value) {
        values.put(key, value);
        return this;
    }

    public PreviewArgs merge(Map<String, String> extra) {
        values.putAll(extra);
        return this;
    }

    public Path exportDir() {
        return exportDir;
    }

    public long seed() {
        return getLong("seed", PreviewJobs.DEFAULT_SEED);
    }

    public String name(String defaultName) {
        String raw = values.get("name");
        if (raw == null || raw.isBlank()) {
            return defaultName;
        }
        return raw.trim();
    }

    public boolean has(String key) {
        return values.containsKey(key) && !values.get(key).isBlank();
    }

    public String get(String key, String fallback) {
        String raw = values.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw.trim();
    }

    public int getInt(String key, int fallback) {
        String raw = values.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(raw.trim());
    }

    public long getLong(String key, long fallback) {
        String raw = values.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Long.parseLong(raw.trim());
    }

    /**
     * Parses {@code name height=80 radius=40} or {@code height=80}.
     * A token without {@code =} is the export name.
     */
    public static Map<String, String> parseTrailing(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String token : raw.trim().split("\\s+")) {
            int eq = token.indexOf('=');
            if (eq <= 0) {
                out.put("name", token);
            } else {
                out.put(token.substring(0, eq).toLowerCase(Locale.ROOT), token.substring(eq + 1));
            }
        }
        return out;
    }
}
