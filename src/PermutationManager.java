import java.util.Random;

public class PermutationManager {
    private static final int MAX_BYTE_VALUE = 256;
    private final byte[] permutationArray;

    public PermutationManager(int randomSeed, int arrayLength) {
        if (arrayLength < 0) {
            throw new IllegalArgumentException("Invalid array size: length cannot be negative");
        }
        
        permutationArray = new byte[arrayLength];
        Random randomGenerator = new Random(randomSeed);

        for (int position = 0; position < arrayLength; position++) {
            permutationArray[position] = (byte) randomGenerator.nextInt(MAX_BYTE_VALUE);
        }
    }

    public byte[] retrievePermutationArray() {
        return permutationArray;
    }

    public byte retrieveValueAt(int arrayIndex) {
        if (arrayIndex < 0 || arrayIndex >= permutationArray.length) {
            throw new IndexOutOfBoundsException("Index " + arrayIndex + " is out of bounds for array size " + permutationArray.length);
        }
        return permutationArray[arrayIndex];
    }

    public int getArrayLength() {
        return permutationArray.length;
    }
}
