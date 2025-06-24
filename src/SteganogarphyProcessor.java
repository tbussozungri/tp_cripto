public class SteganogarphyProcessor {

    private static final int BITS_PER_BYTE = 8;
    private static final int BIT_MASK = 1;
    private static final int LEFT_SHIFT_AMOUNT = 7;
    private static final int RIGHT_SHIFT_AMOUNT = 1;

    public static byte[] hideSecretData(byte[] hostImageData, byte[] secretInformation) {
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

    public static byte[] retrieveHiddenData(byte[] hostImageData, int secretDataLength) {

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
}
