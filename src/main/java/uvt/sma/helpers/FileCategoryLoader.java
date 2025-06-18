package uvt.sma.helpers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class FileCategoryLoader {

    public static HashMap<String, String> loadExtensionCategoryMap(String csvFilePath) {
        HashMap<String, String> extensionToCategory = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains(",")) {
                    continue; // skip empty lines, comments, malformed lines
                }

                String[] parts = line.split(",", 2);
                String category = parts[0].trim();
                String extension = parts[1].trim().toLowerCase();

                extensionToCategory.put(extension, category);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Successfully loaded a number of" + extensionToCategory.size() + " file extensions and their categories.");
        return extensionToCategory;
    }
}

