import java.util.List;

public class SteganogarphyProcessor {

    private static final int BITS_PER_BYTE = 8;
    private static final int BIT_MASK = 1;
    private static final int LEFT_SHIFT_AMOUNT = 7;
    private static final int RIGHT_SHIFT_AMOUNT = 1;
    private static final int SPECIAL_THRESHOLD_VALUE = 8;

    public static byte[] hideSecretData(byte[] hostImageData, byte[] secretInformation, int thresholdValue) {
        if (thresholdValue == SPECIAL_THRESHOLD_VALUE) {
            return hideSecretDataTraditionalLSB(hostImageData, secretInformation);
        } else {
            return hideSecretDataModifiedLSB(hostImageData, secretInformation);
        }
    }

    // Traditional LSB: 1 bit per pixel
    private static byte[] hideSecretDataTraditionalLSB(byte[] hostImageData, byte[] secretInformation) {
        byte[] alteredImageData = hostImageData.clone();
        int currentBitPosition = 0;

        for (int secretByteIndex = 0; secretByteIndex < secretInformation.length; secretByteIndex++) {
            int secretByteValue = Byte.toUnsignedInt(secretInformation[secretByteIndex]);

            for (int bitOffset = 0; bitOffset < BITS_PER_BYTE; bitOffset++) {
                if (currentBitPosition >= hostImageData.length) {
                    throw new IllegalArgumentException("Not enough pixels for traditional LSB hiding");
                }

                int secretBit = (secretByteValue >> (LEFT_SHIFT_AMOUNT - bitOffset)) & BIT_MASK;

                // Traditional LSB: only modify the least significant bit
                alteredImageData[currentBitPosition] &= (byte) ~BIT_MASK;  // Clear LSB
                alteredImageData[currentBitPosition] |= (byte) secretBit;   // Set LSB

                currentBitPosition++;
            }
        }

        return alteredImageData;
    }

    // Modified LSB: multiple bits per pixel (original implementation)
    private static byte[] hideSecretDataModifiedLSB(byte[] hostImageData, byte[] secretInformation) {
        byte[] alteredImageData = hostImageData.clone();
        int imageDataSize = hostImageData.length;
        int currentBitPosition = 0;

        for (int secretByteIndex = 0; secretByteIndex < secretInformation.length; secretByteIndex++) {
            int secretByteValue = Byte.toUnsignedInt(secretInformation[secretByteIndex]);

            for (int bitOffset = 0; bitOffset < BITS_PER_BYTE; bitOffset++) {
                int pixelIndex = currentBitPosition % imageDataSize;
                int bitLocation = (currentBitPosition / imageDataSize) % BITS_PER_BYTE;

                int secretBit = (secretByteValue >> (LEFT_SHIFT_AMOUNT - bitOffset)) & BIT_MASK;

                alteredImageData[pixelIndex] &= (byte) ~(BIT_MASK << bitLocation);
                alteredImageData[pixelIndex] |= (byte) (secretBit << bitLocation);

                currentBitPosition++;
            }
        }

        return alteredImageData;
    }

    public static byte[] retrieveHiddenData(byte[] hostImageData, int secretDataLength, int thresholdValue) {
        if (thresholdValue == SPECIAL_THRESHOLD_VALUE) {
            return retrieveHiddenDataTraditionalLSB(hostImageData, secretDataLength);
        } else {
            return retrieveHiddenDataModifiedLSB(hostImageData, secretDataLength);
        }
    }

    // Traditional LSB: 1 bit per pixel
    private static byte[] retrieveHiddenDataTraditionalLSB(byte[] hostImageData, int secretDataLength) {
        byte[] extractedSecretData = new byte[secretDataLength];
        int currentBitPosition = 0;

        for (int secretByteIndex = 0; secretByteIndex < secretDataLength; secretByteIndex++) {
            int reconstructedByteValue = 0;

            for (int bitOffset = 0; bitOffset < BITS_PER_BYTE; bitOffset++) {
                if (currentBitPosition >= hostImageData.length) {
                    throw new IllegalArgumentException("Not enough pixels for traditional LSB extraction");
                }

                // Traditional LSB: extract only the least significant bit
                int extractedBit = hostImageData[currentBitPosition] & BIT_MASK;
                reconstructedByteValue = (reconstructedByteValue << RIGHT_SHIFT_AMOUNT) | extractedBit;

                currentBitPosition++;
            }

            extractedSecretData[secretByteIndex] = (byte) reconstructedByteValue;
        }

        return extractedSecretData;
    }

    // Modified LSB: multiple bits per pixel (original implementation)
    private static byte[] retrieveHiddenDataModifiedLSB(byte[] hostImageData, int secretDataLength) {
        byte[] extractedSecretData = new byte[secretDataLength];
        int imageDataSize = hostImageData.length;
        int currentBitPosition = 0;

        for (int secretByteIndex = 0; secretByteIndex < secretDataLength; secretByteIndex++) {
            int reconstructedByteValue = 0;

            for (int bitOffset = 0; bitOffset < BITS_PER_BYTE; bitOffset++) {
                int pixelIndex = currentBitPosition % imageDataSize;
                int bitLocation = (currentBitPosition / imageDataSize) % BITS_PER_BYTE;

                int extractedBit = (hostImageData[pixelIndex] >> bitLocation) & BIT_MASK;
                reconstructedByteValue = (reconstructedByteValue << RIGHT_SHIFT_AMOUNT) | extractedBit;

                currentBitPosition++;
            }

            extractedSecretData[secretByteIndex] = (byte) reconstructedByteValue;
        }

        return extractedSecretData;
    }

    public static byte[][] extractHiddenDataFromShadows(List<ImageProcessor> shadowImages, int polynomialCount, int thresholdValue) {
        byte[][] extractedData = new byte[thresholdValue][polynomialCount];
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            byte[] pixelData = shadowImages.get(shadowIndex).retrievePixelData();
            extractedData[shadowIndex] = retrieveHiddenData(pixelData, polynomialCount, thresholdValue);
        }
        return extractedData;
    }

    // Backward compatibility methods (for existing code that doesn't pass threshold)
    public static byte[] hideSecretData(byte[] hostImageData, byte[] secretInformation) {
        return hideSecretDataModifiedLSB(hostImageData, secretInformation);
    }

    public static byte[] retrieveHiddenData(byte[] hostImageData, int secretDataLength) {
        return retrieveHiddenDataModifiedLSB(hostImageData, secretDataLength);
    }
}
