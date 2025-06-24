import java.io.File;

public class FileManager {
    
    public static void cleanShadowDirectory() {
        File shadowDirectory = new File("resources/shadows");
        if (shadowDirectory.exists() && shadowDirectory.isDirectory()) {
            File[] existingShadows = shadowDirectory.listFiles((directoryFilter, fileName) -> 
                fileName.startsWith("shadow") && fileName.endsWith(".bmp"));
            if (existingShadows != null) {
                for (File shadowFile : existingShadows) {
                    shadowFile.delete();
                }
            }
        }
    }

    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    public static void validateFileExists(String filePath, String fileDescription) {
        if (!new File(filePath).exists()) {
            throw new IllegalArgumentException("File not found: " + fileDescription + " at path " + filePath);
        }
    }

    public static File[] getBmpFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        File[] bmpFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".bmp"));
        if (bmpFiles == null) {
            bmpFiles = new File[0];
        }
        return bmpFiles;
    }

    public static void validateEnoughBmpFiles(String directoryPath, int requiredCount, String description) {
        File[] bmpFiles = getBmpFilesInDirectory(directoryPath);
        if (bmpFiles.length < requiredCount) {
            throw new IllegalArgumentException("Insufficient BMP files in " + description + ": need " +
                    requiredCount + " but found " + bmpFiles.length);
        }
    }

    public static void validateFileExtension(String filePath, String expectedExtension, String fileDescription) {
        if (!filePath.toLowerCase().endsWith(expectedExtension.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type: " + fileDescription + " must be a " + expectedExtension + " file");
        }
    }

    public static int determineTotalSharesFromDirectory(String directoryPath) {
        File[] bmpFiles = getBmpFilesInDirectory(directoryPath);
        if (bmpFiles.length == 0) {
            throw new IllegalArgumentException("No BMP images found in directory: " + directoryPath);
        }
        return bmpFiles.length;
    }
} 
