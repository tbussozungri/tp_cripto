import java.io.File;
import java.io.IOException;

public class FileManager {
    
    public static File[] getBmpFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        File[] bmpFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".bmp"));
        if (bmpFiles == null) {
            bmpFiles = new File[0];
        }
        return bmpFiles;
    }

    public static File[] findBmpFilesInDirectory(String directoryPath) {
        File directoryPathFile = new File(directoryPath);
        return directoryPathFile.listFiles((fileFilter, fileName) -> fileName.toLowerCase().endsWith(".bmp"));
    }

    public static int determineTotalSharesFromDirectory(String directoryPath) {
        File[] bmpFiles = getBmpFilesInDirectory(directoryPath);
        if (bmpFiles.length == 0) {
            throw new IllegalArgumentException("No BMP images found in directory: " + directoryPath);
        }
        return bmpFiles.length;
    }

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

    public static void validateFileExists(String filePath, String fileDescription) {
        if (!new File(filePath).exists()) {
            throw new IllegalArgumentException("File not found: " + fileDescription + " at path " + filePath);
        }
    }

    public static void validateEnoughBmpFiles(String directoryPath, int requiredCount, String description) {
        File[] bmpFiles = getBmpFilesInDirectory(directoryPath);
        if (bmpFiles.length < requiredCount) {
            throw new IllegalArgumentException("Insufficient BMP files in " + description + ": need " +
                    requiredCount + " but found " + bmpFiles.length);
        }
    }

    public static void validateSufficientImages(File[] imageFiles, int totalShares, String sourceDirectory) {
        if (imageFiles == null || imageFiles.length < totalShares) {
            throw new IllegalArgumentException("Insufficient BMP images available in directory: " + sourceDirectory);
        }
    }

    public static void validateShadowFilesExist(File[] shadowFiles, String shadowDirectory) throws IOException {
        if (shadowFiles == null || shadowFiles.length == 0) {
            throw new IOException("No shadow files detected in directory: " + shadowDirectory);
        }
    }

    public static void validateSufficientShadowFiles(File[] shadowFiles, int thresholdValue) {
        if (shadowFiles == null || shadowFiles.length < thresholdValue) {
            throw new IllegalArgumentException("Insufficient shadow files found: need " + thresholdValue + 
                                             " but only " + (shadowFiles != null ? shadowFiles.length : 0) + " available");
        }
    }
} 
