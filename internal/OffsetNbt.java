package internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class OffsetNbt {
public static void main(String[] args) {
    if (args.length != 3) {
        System.out.println("Usage: java ./OffsetNbt.java <x> <y> <z>");
        return;
    }

    int[] coords = new int[3];
    boolean allNumbers = true;
    for (int i = 0; i < args.length; i++) {
        try {
            coords[i] = Integer.parseInt(args[i]);
        } catch (NumberFormatException e) {
            allNumbers = false;
            break;
        }
    }
    int x, y, z;
    if (allNumbers) {
        x = coords[0];
        y = coords[1];
        z = coords[2];
    } else {
        System.out.println("All arguments must be numbers");
        System.exit(1);
        return;
    }

    File dir = new File(".");
    File[] files = dir.listFiles();

    for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".txt")) {

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
                System.exit(1);
            }
            String fileContent = sb.toString();

            StringBuilder output = new StringBuilder();
            String[] lines = fileContent.split(System.lineSeparator());
            for (String line : lines) {
                if (line.contains("pos")) {
                    int pos = line.indexOf(":");

                    String array = line.substring(pos + 1).trim();
                    array = array.substring(1, array.length() - 1);

                    String[] arr = array.split(", ");

                    int blockX = Integer.parseInt(arr[0].trim());
                    int blockY = Integer.parseInt(arr[1].trim());
                    int blockZ = Integer.parseInt(arr[2].trim());

                    int newX = blockX + x;
                    int newY = blockY + y;
                    int newZ = blockZ + z;

                    String newPos = "			pos: [ " + newX + ", " + newY + ", " + newZ + ", ]";

                    output.append(newPos + "\n");
                } else {
                    output.append(line + "\n");
                }
            }

            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".offset";
            try (PrintWriter pw = new PrintWriter(new File(fileName))) {
                pw.write(output.toString());
            } catch (IOException e) {
                System.out.println("Error writing to file: " + e.getMessage());
                System.exit(1);
            }

            System.out.println(output.toString());
        }
    }
}
}