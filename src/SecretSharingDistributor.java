import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SecretSharingDistributor {

    private static final int MIN_THRESHOLD = 2;
    private static final int MAX_THRESHOLD = 10;
    private static final int MIN_TOTAL_SHARES = 2;
    private static final int MAX_BYTE_VALUE = 256;
    private static final int RESERVED_FIELD_POSITION_1 = 6;
    private static final int RESERVED_FIELD_POSITION_2 = 8;
    private static final int EMBEDDED_BYTES_FIELD_POSITION = 34;
    private static final int SPECIAL_THRESHOLD_VALUE = 8;

    private final byte[] scrambledSecretData;
    private final int thresholdValue;
    private final int totalShares;
    private final String sourceDirectory;
    private final int secretImageWidth;
    private final int secretImageHeight;
    private final ImageProcessor originalSecretImage;

    public SecretSharingDistributor(byte[] scrambledSecretData, int thresholdValue, int totalShares, int secretImageWidth, int secretImageHeight, ImageProcessor originalSecretImage, String sourceDirectory) {
        if (scrambledSecretData.length % thresholdValue != 0) {
            throw new IllegalArgumentException("Secret data size is not compatible with threshold k. " +
                    "Data length must be divisible by k to form complete polynomials.");
        }
        if (thresholdValue < MIN_THRESHOLD || thresholdValue > MAX_THRESHOLD) {
            throw new IllegalArgumentException("Threshold k must be within range " + MIN_THRESHOLD + " to " + MAX_THRESHOLD);
        }
        if (totalShares < MIN_TOTAL_SHARES) {
            throw new IllegalArgumentException("Total shares n must be at least " + MIN_TOTAL_SHARES);
        }
        if (thresholdValue > totalShares) {
            throw new IllegalArgumentException("Threshold k cannot exceed total shares n");
        }
        this.scrambledSecretData = scrambledSecretData;
        this.thresholdValue = thresholdValue;
        this.totalShares = totalShares;
        this.secretImageWidth = secretImageWidth;
        this.secretImageHeight = secretImageHeight;
        this.originalSecretImage = originalSecretImage;
        this.sourceDirectory = sourceDirectory;
    }

    public int calculatePolynomialCount() {
        return scrambledSecretData.length / thresholdValue;
    }

    public void createShares(int randomSeed) throws Exception {
        File directoryPath = new File(sourceDirectory);
        File[] imageFiles = directoryPath.listFiles((fileFilter, fileName) -> fileName.toLowerCase().endsWith(".bmp"));
        if (imageFiles == null || imageFiles.length < totalShares) {
            throw new IllegalArgumentException("Insufficient BMP images available in directory: " + sourceDirectory);
        }

        List<ImageProcessor> carrierImages = new ArrayList<>();
        for (int carrierIndex = 0; carrierIndex < totalShares; carrierIndex++) {
            String currentFileName = imageFiles[carrierIndex].getName();
            ImageProcessor currentCarrier = new ImageProcessor(imageFiles[carrierIndex].getAbsolutePath());
            if (currentCarrier.extractImageWidth() < secretImageWidth || currentCarrier.extractImageHeight() < secretImageHeight) {
                System.err.printf("Carrier image %d (%s) dimensions (%dx%d) are smaller than secret image (%dx%d).\n", carrierIndex + 1, currentFileName, currentCarrier.extractImageWidth(), currentCarrier.extractImageHeight(), secretImageWidth, secretImageHeight);
                System.err.println("Operation aborted. All carrier images must have dimensions equal to or larger than the secret image.");
                System.exit(1);
            }
            if (thresholdValue == SPECIAL_THRESHOLD_VALUE) {
                if (currentCarrier.extractImageWidth() != secretImageWidth || currentCarrier.extractImageHeight() != secretImageHeight) {
                    currentCarrier = currentCarrier.resizeImage(secretImageWidth, secretImageHeight);
                }
            }
            ImageProcessor processedImage = new ImageProcessor(originalSecretImage.retrieveHeader(), currentCarrier.retrievePixelData());
            carrierImages.add(processedImage);
        }

        int polynomialCount = calculatePolynomialCount();

        byte[][] valuesToHide = new byte[totalShares][polynomialCount];

        for (int polynomialIndex = 0; polynomialIndex < polynomialCount; polynomialIndex++) {
            boolean coefficientsModified;
            do {
                coefficientsModified = false;
                for (int shareIdentifier = 1; shareIdentifier <= totalShares; shareIdentifier++) {
                    int evaluationResult = SecretSharingMath.computePolynomialValue(shareIdentifier, polynomialIndex, scrambledSecretData, thresholdValue) % SecretSharingMath.getModuloValue();
                    if (evaluationResult == MAX_BYTE_VALUE) {
                        boolean coefficientReduced = false;
                        for (int coefficientIndex = 0; coefficientIndex < thresholdValue; coefficientIndex++) {
                            int coefficientValue = Byte.toUnsignedInt(scrambledSecretData[polynomialIndex * thresholdValue + coefficientIndex]);
                            if (coefficientValue != 0) {
                                scrambledSecretData[polynomialIndex * thresholdValue + coefficientIndex]--;
                                coefficientReduced = true;
                                coefficientsModified = true;
                                break;
                            }
                        }
                        if (!coefficientReduced) {
                            throw new IllegalStateException("Unable to proceed: all polynomial coefficients are zero and cannot be reduced further");
                        }
                    }
                }
            } while (coefficientsModified);

            for (int shareIndex = 0; shareIndex < totalShares; shareIndex++) {
                int shareIdentifier = shareIndex + 1;
                valuesToHide[shareIndex][polynomialIndex] = (byte) (SecretSharingMath.computePolynomialValue(shareIdentifier, polynomialIndex, scrambledSecretData, thresholdValue) % SecretSharingMath.getModuloValue());
            }
        }

        for(int shareIndex = 0; shareIndex < totalShares; shareIndex++) {
            ImageProcessor processedImage = carrierImages.get(shareIndex);
            byte[] imagePixels = processedImage.retrievePixelData();
            byte[] modifiedPixelData = SteganogarphyProcessor.hideSecretData(imagePixels, valuesToHide[shareIndex]);
            processedImage.updatePixelData(modifiedPixelData);
            processedImage.modifyReservedField(RESERVED_FIELD_POSITION_1, (short) randomSeed);
            processedImage.modifyReservedField(RESERVED_FIELD_POSITION_2, (short) (shareIndex+1));
            processedImage.modifyEmbeddedBytesField(EMBEDDED_BYTES_FIELD_POSITION, polynomialCount);
            String outputFileName = String.format("resources/shadows/shadow%d.bmp", (shareIndex+1));
            processedImage.writeToFile(outputFileName);
        }

    }

}
