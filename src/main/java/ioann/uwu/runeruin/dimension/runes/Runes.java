package ioann.uwu.runeruin.dimension.runes;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Runes {

    public static List<List<List<Boolean>>> list() {
        try {
            URL resource = Runes.class.getClassLoader().getResource("runes.txt");

            Path path = Path.of(Paths.get(resource.toURI()).toFile().getAbsolutePath());

            List<String> content = Files.readAllLines(path);

            List<List<List<Boolean>>> finalList = new ArrayList<>();

            for (List<String> record : Lists.partition(content, 17)) {
                String name = record.getFirst();

                List<List<Boolean>> recordDescription = new ArrayList<>();

                for (int i = 1; i < record.size(); i++) {
                    String line = record.get(i);

                    List<Boolean> list = line.chars()
                            .filter(ch -> !Character.isWhitespace(ch))
                            .limit(10)
                            .mapToObj(ch -> ch == '#')
                            .toList();
                    recordDescription.add(list);
                }

                finalList.add(recordDescription);
            }

            return finalList;

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
