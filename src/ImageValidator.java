public class ImageValidator {
    
    private static final int SPECIAL_THRESHOLD_VALUE = 8;
    
    public static void validateImageDimensions(ImageProcessor carrierImage, String fileName, 
                                             int imageIndex, int secretImageWidth, int secretImageHeight) {
        if (carrierImage.extractImageWidth() < secretImageWidth || 
            carrierImage.extractImageHeight() < secretImageHeight) {
            System.err.printf("Carrier image %d (%s) dimensions (%dx%d) are smaller than secret image (%dx%d).\n", 
                            imageIndex + 1, fileName, carrierImage.extractImageWidth(), 
                            carrierImage.extractImageHeight(), secretImageWidth, secretImageHeight);
            System.err.println("Operation aborted. All carrier images must have dimensions equal to or larger than the secret image.");
            System.exit(1);
        }
    }

    public static boolean shouldResizeImage(ImageProcessor carrierImage, int thresholdValue, 
                                          int secretImageWidth, int secretImageHeight) {
        return thresholdValue == SPECIAL_THRESHOLD_VALUE && 
               (carrierImage.extractImageWidth() != secretImageWidth || 
                carrierImage.extractImageHeight() != secretImageHeight);
    }
} 