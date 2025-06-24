import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecretSharingRecoverer {

    // Configuration constants
    private static final int MIN_THRESHOLD = 2;
    private static final int MAX_THRESHOLD = 10;
    private static final int MIN_TOTAL_SHARES = 2;
    private static final int SPECIAL_THRESHOLD_VALUE = 8;
    private static final int ZERO_VALUE = 0;
    
    // BMP header field positions
    private static final int RESERVED_FIELD_1_POSITION = 6;
    private static final int RESERVED_FIELD_2_POSITION = 8;
    private static final int EMBEDDED_BYTES_FIELD_POSITION = 34;
    
    // Instance variables
    private final int thresholdValue;
    private final String shadowDirectory;

    public SecretSharingRecoverer(int thresholdValue, int totalShares, String shadowDirectory) {
        validateConstructorParameters(thresholdValue, totalShares);
        
        this.thresholdValue = thresholdValue;
        this.shadowDirectory = shadowDirectory;
    }

    public short extractRandomSeed() {
        try {
            File[] shadowFiles = FileManager.findBmpFilesInDirectory(shadowDirectory);
            FileManager.validateShadowFilesExist(shadowFiles, shadowDirectory);
            
            ImageProcessor firstShadow = new ImageProcessor(shadowFiles[0].getAbsolutePath());
            return firstShadow.extractReservedField(RESERVED_FIELD_1_POSITION);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shadow file to extract seed value", e);
        }
    }

    public byte[] reconstructSecret() throws IOException {
        File[] shadowFiles = FileManager.findBmpFilesInDirectory(shadowDirectory);
        FileManager.validateSufficientShadowFiles(shadowFiles, thresholdValue);
        
        List<ImageProcessor> shadowImages = loadRandomShadowImages(shadowFiles);
        int[] shareIdentifiers = ImageValidator.extractShareIdentifiers(shadowImages, thresholdValue);
        int polynomialCount = ImageValidator.calculatePolynomialCount(shadowImages.get(0), thresholdValue);
        
        byte[][] extractedData = SteganogarphyProcessor.extractHiddenDataFromShadows(shadowImages, polynomialCount, thresholdValue);
        return reconstructScrambledData(extractedData, shareIdentifiers, polynomialCount);
    }

    private void validateConstructorParameters(int thresholdValue, int totalShares) {
        if (thresholdValue < MIN_THRESHOLD || thresholdValue > MAX_THRESHOLD) {
            throw new IllegalArgumentException("Threshold k must be within range " + MIN_THRESHOLD + " to " + MAX_THRESHOLD);
        }
        if (thresholdValue > totalShares) {
            throw new IllegalArgumentException("Threshold k cannot exceed total shares n");
        }
        if (totalShares < MIN_TOTAL_SHARES) {
            throw new IllegalArgumentException("Total shares n must be at least " + MIN_TOTAL_SHARES);
        }
    }

    private List<ImageProcessor> loadRandomShadowImages(File[] shadowFiles) throws IOException {
        List<File> fileList = new ArrayList<>();
        Collections.addAll(fileList, shadowFiles);
        Collections.shuffle(fileList);
        
        List<ImageProcessor> shadowImages = new ArrayList<>();
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            ImageProcessor shadowImage = new ImageProcessor(fileList.get(shadowIndex).getAbsolutePath());
            shadowImages.add(shadowImage);
        }
        return shadowImages;
    }

    private byte[] reconstructScrambledData(byte[][] extractedData, int[] shareIdentifiers, int polynomialCount) {
        byte[] reconstructedScrambledData = new byte[polynomialCount * thresholdValue];
        
        for (int polynomialIndex = 0; polynomialIndex < polynomialCount; polynomialIndex++) {
            int[] yValues = SecretSharingMath.extractYValuesForPolynomial(extractedData, polynomialIndex, thresholdValue);
            int[] coefficients = SecretSharingMath.solvePolynomialCoefficients(shareIdentifiers, yValues, thresholdValue);
            SecretSharingMath.storePolynomialCoefficients(reconstructedScrambledData, coefficients, polynomialIndex, thresholdValue);
        }
        
        return reconstructedScrambledData;
    }
}
