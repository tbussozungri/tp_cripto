import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SecretSharing {

    // Configuration constants
    private static final int MIN_THRESHOLD = 2;
    private static final int MAX_THRESHOLD = 10;
    private static final int MIN_TOTAL_SHARES = 2;
    private static final int MAX_BYTE_VALUE = 256;
    private static final int SPECIAL_THRESHOLD_VALUE = 8;
    private static final int ZERO_VALUE = 0;
    
    // BMP header field positions
    private static final int RESERVED_FIELD_1_POSITION = 6;
    private static final int RESERVED_FIELD_2_POSITION = 8;
    private static final int EMBEDDED_BYTES_FIELD_POSITION = 34;
    
    // Instance variables for distribution
    private byte[] scrambledSecretData;
    private int thresholdValue;
    private int totalShares;
    private String sourceDirectory;
    private int secretImageWidth;
    private int secretImageHeight;
    private ImageProcessor originalSecretImage;
    
    // Instance variables for recovery
    private String shadowDirectory;
    
    // Constructor for distribution operations
    public SecretSharing(byte[] scrambledSecretData, int thresholdValue, int totalShares, 
                        int secretImageWidth, int secretImageHeight, 
                        ImageProcessor originalSecretImage, String sourceDirectory) {
        validateDistributionParameters(scrambledSecretData, thresholdValue, totalShares);
        
        this.scrambledSecretData = scrambledSecretData;
        this.thresholdValue = thresholdValue;
        this.totalShares = totalShares;
        this.secretImageWidth = secretImageWidth;
        this.secretImageHeight = secretImageHeight;
        this.originalSecretImage = originalSecretImage;
        this.sourceDirectory = sourceDirectory;
    }

    // Constructor for recovery operations
    public SecretSharing(int thresholdValue, int totalShares, String shadowDirectory) {
        validateRecoveryParameters(thresholdValue, totalShares);
        
        this.thresholdValue = thresholdValue;
        this.shadowDirectory = shadowDirectory;
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

    private void validateDistributionParameters(byte[] scrambledSecretData, int thresholdValue, int totalShares) {
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
        
        return carrierImage;
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
        byte[] modifiedPixels = SteganogarphyProcessor.hideSecretData(imagePixels, shareData,thresholdValue);
        
        processedImage.updatePixelData(modifiedPixels);
        processedImage.modifyReservedField(RESERVED_FIELD_1_POSITION, (short) randomSeed);
        processedImage.modifyReservedField(RESERVED_FIELD_2_POSITION, (short) (shareIndex + 1));
        processedImage.modifyEmbeddedBytesField(EMBEDDED_BYTES_FIELD_POSITION, polynomialCount);
        
        String outputFileName = String.format("resources/shadows/shadow%d.bmp", (shareIndex + 1));
        processedImage.writeToFile(outputFileName);
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

    private void validateRecoveryParameters(int thresholdValue, int totalShares) {
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