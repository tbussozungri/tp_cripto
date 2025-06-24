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
        File[] imageFiles = FileManager.findBmpFilesInDirectory(sourceDirectory);
        FileManager.validateSufficientImages(imageFiles, totalShares, sourceDirectory);
        
        List<ImageProcessor> carrierImages = new ArrayList<>();
        for (int imageIndex = 0; imageIndex < totalShares; imageIndex++) {
            ImageProcessor carrierImage = createCarrierImage(imageFiles[imageIndex], imageIndex);
            carrierImages.add(carrierImage);
        }
        return carrierImages;
    }

    private ImageProcessor createCarrierImage(File imageFile, int imageIndex) throws Exception {
        String fileName = imageFile.getName();
        ImageProcessor carrierImage = new ImageProcessor(imageFile.getAbsolutePath());
        
        ImageValidator.validateImageDimensions(carrierImage, fileName, imageIndex, secretImageWidth, secretImageHeight);
        
        if (ImageValidator.shouldResizeImage(carrierImage, thresholdValue, secretImageWidth, secretImageHeight)) {
            carrierImage = carrierImage.resizeImage(secretImageWidth, secretImageHeight);
        }
        
        return new ImageProcessor(originalSecretImage.retrieveHeader(), carrierImage.retrievePixelData());
    }

    private byte[][] generateShareValues(int polynomialCount) {
        byte[][] shareValues = new byte[totalShares][polynomialCount];
        
        for (int polynomialIndex = 0; polynomialIndex < polynomialCount; polynomialIndex++) {
            SecretSharingMath.adjustPolynomialCoefficientsIfNeeded(polynomialIndex, scrambledSecretData, 
                                                                 thresholdValue, totalShares);
            SecretSharingMath.computeShareValuesForPolynomial(polynomialIndex, shareValues, 
                                                            scrambledSecretData, thresholdValue, totalShares);
        }
        
        return shareValues;
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
