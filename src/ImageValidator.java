import java.util.List;

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

    public static int calculatePolynomialCount(ImageProcessor referenceShadow, int thresholdValue) {
        int polynomialCount;
        if (thresholdValue != 8) { // SPECIAL_THRESHOLD_VALUE
            polynomialCount = referenceShadow.extractIntegerFromHeader(34); // EMBEDDED_BYTES_FIELD_POSITION
        } else {
            polynomialCount = referenceShadow.retrievePixelData().length / thresholdValue;
        }
        
        if (polynomialCount <= 0) {
            throw new IllegalArgumentException("Invalid polynomial count: " + polynomialCount);
        }
        return polynomialCount;
    }

    public static int[] extractShareIdentifiers(List<ImageProcessor> shadowImages, int thresholdValue) {
        int[] shareIdentifiers = new int[thresholdValue];
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            shareIdentifiers[shadowIndex] = shadowImages.get(shadowIndex).extractReservedField(8); // RESERVED_FIELD_2_POSITION
        }
        return shareIdentifiers;
    }
} 