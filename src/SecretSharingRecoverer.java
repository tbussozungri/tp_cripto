import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecretSharingRecoverer {

    private static final int MIN_THRESHOLD = 2;
    private static final int MAX_THRESHOLD = 10;
    private static final int MIN_TOTAL_SHARES = 2;
    private static final int SPECIAL_THRESHOLD_VALUE = 8;
    private static final int RESERVED_FIELD_POSITION_1 = 6;
    private static final int RESERVED_FIELD_POSITION_2 = 8;
    private static final int EMBEDDED_BYTES_FIELD_POSITION = 34;
    private static final int ZERO_VALUE = 0;

    private final int thresholdValue;
    private final String shadowDirectory;

    public SecretSharingRecoverer(int thresholdValue, int totalShares, String shadowDirectory) {
        if (thresholdValue < MIN_THRESHOLD || thresholdValue > MAX_THRESHOLD) {
            throw new IllegalArgumentException("Threshold k must be within range " + MIN_THRESHOLD + " to " + MAX_THRESHOLD);
        }
        if (thresholdValue > totalShares) {
            throw new IllegalArgumentException("Threshold k cannot exceed total shares n");
        }
        if (totalShares < MIN_TOTAL_SHARES) {
            throw new IllegalArgumentException("Total shares n must be at least " + MIN_TOTAL_SHARES);
        }
        this.thresholdValue = thresholdValue;
        this.shadowDirectory = shadowDirectory;
    }

    public short extractRandomSeed() {
        try {
            File directoryPath = new File(shadowDirectory);
            File[] shadowFiles = directoryPath.listFiles((directoryFilter, fileName) -> fileName.endsWith(".bmp"));
            if (shadowFiles == null || shadowFiles.length == 0) {
                throw new IOException("No shadow files detected in directory: " + shadowDirectory);
            }
            ImageProcessor firstShadow = new ImageProcessor(shadowFiles[0].getAbsolutePath());
            return firstShadow.extractReservedField(RESERVED_FIELD_POSITION_1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shadow file to extract seed value", e);
        }
    }

    public byte[] reconstructSecret() throws IOException {
        File directoryPath = new File(shadowDirectory);
        File[] shadowFiles = directoryPath.listFiles((directoryFilter, fileName) -> fileName.endsWith(".bmp"));
        if (shadowFiles == null || shadowFiles.length < thresholdValue) {
            throw new IllegalArgumentException("Insufficient shadow files found: need " + thresholdValue + " but only " + (shadowFiles != null ? shadowFiles.length : 0) + " available");
        }
        List<File> fileList = new ArrayList<>();
        Collections.addAll(fileList, shadowFiles);
        Collections.shuffle(fileList);
        List<ImageProcessor> shadowImages = new ArrayList<>();
        int[] shareIdentifiers = new int[thresholdValue];
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            ImageProcessor shadowImage = new ImageProcessor(fileList.get(shadowIndex).getAbsolutePath());
            shadowImages.add(shadowImage);
            shareIdentifiers[shadowIndex] = shadowImage.extractReservedField(RESERVED_FIELD_POSITION_2);
        }
        ImageProcessor referenceShadow = shadowImages.getFirst();
        int polynomialCount;
        if(thresholdValue != SPECIAL_THRESHOLD_VALUE) {
            polynomialCount = referenceShadow.extractIntegerFromHeader(EMBEDDED_BYTES_FIELD_POSITION);
        } else {
            polynomialCount = referenceShadow.retrievePixelData().length / thresholdValue;
        }
        if (polynomialCount <= ZERO_VALUE) {
            throw new IllegalArgumentException("Invalid polynomial count: " + polynomialCount);
        }
        byte[][] extractedData = new byte[thresholdValue][polynomialCount];
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            extractedData[shadowIndex] = SteganogarphyProcessor.retrieveHiddenData(shadowImages.get(shadowIndex).retrievePixelData(), polynomialCount);
        }
        byte[] reconstructedScrambledData = new byte[polynomialCount * thresholdValue];
        for (int polynomialIndex = 0; polynomialIndex < polynomialCount; polynomialIndex++) {
            int[] yValues = new int[thresholdValue];
            for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
                int extractedValue = Byte.toUnsignedInt(extractedData[shadowIndex][polynomialIndex]);
                yValues[shadowIndex] = extractedValue;
            }
            int[] xValues = shareIdentifiers;
            int[][] coefficientMatrix = SecretSharingMath.createCoefficientMatrix(xValues, thresholdValue, SecretSharingMath.getModuloValue());
            int[] coefficients = SecretSharingMath.solveLinearSystemModulo(coefficientMatrix, yValues, SecretSharingMath.getModuloValue());
            for (int coefficientIndex = 0; coefficientIndex < thresholdValue; coefficientIndex++) {
                int dataIndex = polynomialIndex * thresholdValue + coefficientIndex;
                reconstructedScrambledData[dataIndex] = (byte) coefficients[coefficientIndex];
            }
        }
        return reconstructedScrambledData;
    }
}
