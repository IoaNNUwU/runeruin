package internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class RemoveAir {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ./RemoveAir.java <idx>");
            return;
        }

        int idx;
        try {
            idx = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Argument must be a number: " + args[0]);
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

                String[] lines = fileContent.split(System.lineSeparator());

                ArrayList<Integer> toRemove = new ArrayList<>();

                for(int i = 0; i < lines.length; i++) {

                    String line = lines[i];

                    if (line.contains("state")) {
                        int pos = line.indexOf(":");

                        String num = line.substring(pos + 1).trim();

                        int state = Integer.parseInt(num.trim());

                        if (state == idx) {
                            toRemove.add(i - 2);
                            toRemove.add(i - 1);
                            toRemove.add(i);
                            toRemove.add(i + 1);
                        }
                    }
                }

                StringBuilder output = new StringBuilder();

                for(int i = 0; i < lines.length; i++) {
                    if (toRemove.contains(i)) {
                        continue;
                    }

                    String line = lines[i];
                    output.append(line + "\n");
                }


                String fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".airless";
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