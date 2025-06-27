import java.util.List;

public class SteganogarphyProcessor {

    private static final int BITS_PER_BYTE = 8;
    private static final int BIT_MASK = 1;
    private static final int LEFT_SHIFT_AMOUNT = 7;
    private static final int RIGHT_SHIFT_AMOUNT = 1;

    public static byte[] hideSecretData(byte[] hostImageData, byte[] secretInformation, int k) {
        byte[] alteredImageData = hostImageData.clone();
        int currentBitPosition = 0;

        if (k == 8) {
            // LSB Replacement
            int totalBitsSecret = secretInformation.length * 8;
            for (int i = 0; i < alteredImageData.length && currentBitPosition < totalBitsSecret; i++) {
                int byteIndex = currentBitPosition / 8;
                int bitIndex = 7 - (currentBitPosition % 8);
                int secretBit = (secretInformation[byteIndex] >> bitIndex) & 1;
                alteredImageData[i] = (byte) ((alteredImageData[i] & 0xFE) | secretBit);
                currentBitPosition++;
            }
            return alteredImageData;
        }
        // General case for hiding data using k bits per pixel
        int totalSecretBits = secretInformation.length * 8;
        int mask = (1 << k) - 1; // Máscara para k bits (por ejemplo, k=3 -> 0b111)
        for (int i = 0; i < alteredImageData.length && currentBitPosition < totalSecretBits; i++) {
            int secretBits = 0;
            for (int bit = 0; bit < k; bit++) {
                int bitPos = currentBitPosition + bit;
                int bitValue = 0;
                if (bitPos < totalSecretBits) {
                    int byteIndex = bitPos / 8;
                    int bitIndex = 7 - (bitPos % 8);
                    bitValue = (secretInformation[byteIndex] >> bitIndex) & 1;
                }
                secretBits = (secretBits << 1) | bitValue;
            }
            alteredImageData[i] = (byte) ((alteredImageData[i] & ~mask) | secretBits);
            currentBitPosition += k;
        }
        return alteredImageData;
    }

    public static byte[] retrieveHiddenData(byte[] hostImageData, int secretDataLength, int k) {
        byte[] extractedSecretData = new byte[secretDataLength];

        if (k == 8) {
            // LSB Replacement
            for (int secretByteIndex = 0; secretByteIndex < secretDataLength; secretByteIndex++) {
                int reconstructedByteValue = 0;
                for (int bitOffset = 0; bitOffset < 8; bitOffset++) {
                    int imageIndex = secretByteIndex * 8 + bitOffset;
                    if (imageIndex >= hostImageData.length) break;
                    int lsb = hostImageData[imageIndex] & 1;
                    reconstructedByteValue = (reconstructedByteValue << 1) | lsb;
                }
                extractedSecretData[secretByteIndex] = (byte) reconstructedByteValue;
            }
            return extractedSecretData;
        }

        // General: k bits per pixel
        int mask = (1 << k) - 1;
        int bitWritten = 0;
        for (int secretByteIndex = 0; secretByteIndex < secretDataLength; secretByteIndex++) {
            int reconstructedByteValue = 0;
            for (int bitOffset = 0; bitOffset < 8; bitOffset += k) {
                int imageIndex = bitWritten / k;
                if (imageIndex >= hostImageData.length) break;
                int secretBits = hostImageData[imageIndex] & mask;
                int bitsToShift = 8 - k - bitOffset;
                if (bitsToShift < 0) bitsToShift = 0;
                reconstructedByteValue |= (secretBits << bitsToShift);
                bitWritten += k;
            }
            extractedSecretData[secretByteIndex] = (byte) reconstructedByteValue;
        }
        return extractedSecretData;
    }

    public static byte[][] extractHiddenDataFromShadows(List<ImageProcessor> shadowImages, int polynomialCount, int thresholdValue) {
        byte[][] extractedData = new byte[thresholdValue][polynomialCount];
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            byte[] pixelData = shadowImages.get(shadowIndex).retrievePixelData();
            extractedData[shadowIndex] = retrieveHiddenData(pixelData, polynomialCount,thresholdValue);
        }
        return extractedData;
    }
}
