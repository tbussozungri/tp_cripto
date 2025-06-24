import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SecretSharingDistributor {

    // Configuration constants
    private static final int MIN_THRESHOLD = 2;
    private static final int MAX_THRESHOLD = 10;
    private static final int MIN_TOTAL_SHARES = 2;
    private static final int MAX_BYTE_VALUE = 256;
    private static final int SPECIAL_THRESHOLD_VALUE = 8;
    
    // BMP header field positions
    private static final int RESERVED_FIELD_1_POSITION = 6;
    private static final int RESERVED_FIELD_2_POSITION = 8;
    private static final int EMBEDDED_BYTES_FIELD_POSITION = 34;
    
    // Instance variables
    private final byte[] scrambledSecretData;
    private final int thresholdValue;
    private final int totalShares;
    private final String sourceDirectory;
    private final int secretImageWidth;
    private final int secretImageHeight;
    private final ImageProcessor originalSecretImage;

    public SecretSharingDistributor(byte[] scrambledSecretData, int thresholdValue, int totalShares, 
                                   int secretImageWidth, int secretImageHeight, 
                                   ImageProcessor originalSecretImage, String sourceDirectory) {
        validateConstructorParameters(scrambledSecretData, thresholdValue, totalShares);
        
        this.scrambledSecretData = scrambledSecretData;
        this.thresholdValue = thresholdValue;
        this.totalShares = totalShares;
        this.secretImageWidth = secretImageWidth;
        this.secretImageHeight = secretImageHeight;
        this.originalSecretImage = originalSecretImage;
        this.sourceDirectory = sourceDirectory;
    }

    private void validateConstructorParameters(byte[] scrambledSecretData, int thresholdValue, int totalShares) {
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
    }

    public int calculatePolynomialCount() {
        return scrambledSecretData.length / thresholdValue;
    }

    public void createShares(int randomSeed) throws Exception {
        List<ImageProcessor> carrierImages = loadAndValidateCarrierImages();
        int polynomialCount = calculatePolynomialCount();
        byte[][] shareValues = generateShareValues(polynomialCount);
        embedSharesIntoImages(carrierImages, shareValues, polynomialCount, randomSeed);
    }

    private List<ImageProcessor> loadAndValidateCarrierImages() throws Exception {
        File[] imageFiles = findBmpFilesInDirectory();
        validateSufficientImages(imageFiles);
        
        List<ImageProcessor> carrierImages = new ArrayList<>();
        for (int imageIndex = 0; imageIndex < totalShares; imageIndex++) {
            ImageProcessor carrierImage = createCarrierImage(imageFiles[imageIndex], imageIndex);
            carrierImages.add(carrierImage);
        }
        return carrierImages;
    }

    private File[] findBmpFilesInDirectory() {
        File directoryPath = new File(sourceDirectory);
        return directoryPath.listFiles((fileFilter, fileName) -> fileName.toLowerCase().endsWith(".bmp"));
    }

    private void validateSufficientImages(File[] imageFiles) {
        if (imageFiles == null || imageFiles.length < totalShares) {
            throw new IllegalArgumentException("Insufficient BMP images available in directory: " + sourceDirectory);
        }
    }

    private ImageProcessor createCarrierImage(File imageFile, int imageIndex) throws Exception {
        String fileName = imageFile.getName();
        ImageProcessor carrierImage = new ImageProcessor(imageFile.getAbsolutePath());
        
        validateImageDimensions(carrierImage, fileName, imageIndex);
        
        if (shouldResizeImage(carrierImage)) {
            carrierImage = carrierImage.resizeImage(secretImageWidth, secretImageHeight);
        }
        
        return new ImageProcessor(originalSecretImage.retrieveHeader(), carrierImage.retrievePixelData());
    }

    private void validateImageDimensions(ImageProcessor carrierImage, String fileName, int imageIndex) {
        if (carrierImage.extractImageWidth() < secretImageWidth || 
            carrierImage.extractImageHeight() < secretImageHeight) {
            System.err.printf("Carrier image %d (%s) dimensions (%dx%d) are smaller than secret image (%dx%d).\n", 
                            imageIndex + 1, fileName, carrierImage.extractImageWidth(), 
                            carrierImage.extractImageHeight(), secretImageWidth, secretImageHeight);
            System.err.println("Operation aborted. All carrier images must have dimensions equal to or larger than the secret image.");
            System.exit(1);
        }
    }

    private boolean shouldResizeImage(ImageProcessor carrierImage) {
        return thresholdValue == SPECIAL_THRESHOLD_VALUE && 
               (carrierImage.extractImageWidth() != secretImageWidth || 
                carrierImage.extractImageHeight() != secretImageHeight);
    }

    private byte[][] generateShareValues(int polynomialCount) {
        byte[][] shareValues = new byte[totalShares][polynomialCount];
        
        for (int polynomialIndex = 0; polynomialIndex < polynomialCount; polynomialIndex++) {
            adjustPolynomialCoefficientsIfNeeded(polynomialIndex);
            computeShareValuesForPolynomial(polynomialIndex, shareValues);
        }
        
        return shareValues;
    }

    private void adjustPolynomialCoefficientsIfNeeded(int polynomialIndex) {
        boolean coefficientsAdjusted;
        do {
            coefficientsAdjusted = false;
            for (int shareId = 1; shareId <= totalShares; shareId++) {
                int evaluationResult = SecretSharingMath.computePolynomialValue(shareId, polynomialIndex, 
                                                                              scrambledSecretData, thresholdValue) % 
                                    SecretSharingMath.getModuloValue();
                
                if (evaluationResult == MAX_BYTE_VALUE) {
                    coefficientsAdjusted = reducePolynomialCoefficients(polynomialIndex) || coefficientsAdjusted;
                }
            }
        } while (coefficientsAdjusted);
    }

    private boolean reducePolynomialCoefficients(int polynomialIndex) {
        for (int coefficientIndex = 0; coefficientIndex < thresholdValue; coefficientIndex++) {
            int coefficientPosition = polynomialIndex * thresholdValue + coefficientIndex;
            int coefficientValue = Byte.toUnsignedInt(scrambledSecretData[coefficientPosition]);
            
            if (coefficientValue != 0) {
                scrambledSecretData[coefficientPosition]--;
                return true;
            }
        }
        throw new IllegalStateException("Unable to proceed: all polynomial coefficients are zero and cannot be reduced further");
    }

    private void computeShareValuesForPolynomial(int polynomialIndex, byte[][] shareValues) {
        for (int shareIndex = 0; shareIndex < totalShares; shareIndex++) {
            int shareId = shareIndex + 1;
            int polynomialValue = SecretSharingMath.computePolynomialValue(shareId, polynomialIndex, 
                                                                          scrambledSecretData, thresholdValue) % 
                                 SecretSharingMath.getModuloValue();
            shareValues[shareIndex][polynomialIndex] = (byte) polynomialValue;
        }
    }

    private void embedSharesIntoImages(List<ImageProcessor> carrierImages, byte[][] shareValues, 
                                     int polynomialCount, int randomSeed) throws Exception {
        for (int shareIndex = 0; shareIndex < totalShares; shareIndex++) {
            ImageProcessor processedImage = carrierImages.get(shareIndex);
            embedShareIntoImage(processedImage, shareValues[shareIndex], shareIndex, polynomialCount, randomSeed);
        }
    }

    private void embedShareIntoImage(ImageProcessor processedImage, byte[] shareData, 
                                   int shareIndex, int polynomialCount, int randomSeed) throws Exception {
        byte[] imagePixels = processedImage.retrievePixelData();
        byte[] modifiedPixels = SteganogarphyProcessor.hideSecretData(imagePixels, shareData);
        
        processedImage.updatePixelData(modifiedPixels);
        processedImage.modifyReservedField(RESERVED_FIELD_1_POSITION, (short) randomSeed);
        processedImage.modifyReservedField(RESERVED_FIELD_2_POSITION, (short) (shareIndex + 1));
        processedImage.modifyEmbeddedBytesField(EMBEDDED_BYTES_FIELD_POSITION, polynomialCount);
        
        String outputFileName = String.format("resources/shadows/shadow%d.bmp", (shareIndex + 1));
        processedImage.writeToFile(outputFileName);
    }
}
