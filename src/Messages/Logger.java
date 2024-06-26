package Messages;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    private static BufferedWriter writer;

    public static void openLogFile() {
        System.out.println("Opening log file");
        String fileName = "log.txt";
        try {
            writer = new BufferedWriter(new FileWriter(fileName, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeLogFile() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeLog(String input) {
        try {
            if (writer != null) {
                String[] lines = input.split("\\r?\\n");
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
                 writer.flush();
            } else {
                System.out.println("Il file non è aperto. Apri il file prima di scrivere.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
