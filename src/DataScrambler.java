public class DataScrambler {

    private static final int MAX_RANDOM_SEED = 65536;
    private static final int SEED_MASK = 0xFFFF;

    public static byte[] scrambleDataWithSeed(short randomSeed, byte[] inputData) {
        PermutationManager permutationTable = new PermutationManager(randomSeed & SEED_MASK, inputData.length);
        byte[] scrambledResult = new byte[inputData.length];

        for (int dataIndex = 0; dataIndex < inputData.length; dataIndex++) {
            scrambledResult[dataIndex] = (byte) (inputData[dataIndex] ^ permutationTable.retrieveValueAt(dataIndex));
        }

        return scrambledResult;
    }

    public static byte[] unscrambleDataWithSeed(short randomSeed, byte[] scrambledData) {
        // XOR operation is symmetric, so unscrambling is the same as scrambling
        return scrambleDataWithSeed(randomSeed, scrambledData);
    }

    public static short createRandomSeed() {
        return (short) (Math.random() * MAX_RANDOM_SEED);
    }
} 
