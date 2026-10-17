package ioann.uwu.runeruin.dimension.runes;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Runes {

    /** The patterns of {@code runes.txt}: a name line and 16 rows of 10 cells each, {@code #} where the rune is cut. */
    public static List<List<List<Boolean>>> list() {
        List<String> content;
        // Read as a stream: inside the mod jar the resource is no file.
        try (InputStream in = Runes.class.getClassLoader().getResourceAsStream("runes.txt")) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<List<List<Boolean>>> finalList = new ArrayList<>();
        for (List<String> record : Lists.partition(content, 17)) {
            List<List<Boolean>> recordDescription = new ArrayList<>();
            for (String line : record.subList(1, record.size())) {
                recordDescription.add(line.chars()
                        .filter(ch -> !Character.isWhitespace(ch))
                        .limit(10)
                        .mapToObj(ch -> ch == '#')
                        .toList());
            }
            // The empty "Template" record is there to copy, not a rune.
            if (recordDescription.stream().anyMatch(row -> row.contains(true))) {
                finalList.add(recordDescription);
            }
        }
        return finalList;
    }
}
