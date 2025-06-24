public class SecretSharingMath {

    private static final int MODULO_VALUE = 257;
    private static final int MIN_MODULAR_INVERSE = 1;
    private static final int ZERO_VALUE = 0;

    public static int computePolynomialValue(int shareIdentifier, int polynomialIndex, 
                                           byte[] scrambledSecretData, int thresholdValue) {
        int coefficientStart = polynomialIndex * thresholdValue;
        int polynomialResult = 0;
        for (int coefficientIndex = 0; coefficientIndex < thresholdValue; coefficientIndex++) {
            int coefficientValue = Byte.toUnsignedInt(scrambledSecretData[coefficientStart + coefficientIndex]);
            polynomialResult += coefficientValue * (int) Math.pow(shareIdentifier, coefficientIndex);
        }
        return polynomialResult;
    }

    public static int[] solveLinearSystemModulo(int[][] coefficientMatrix, int[] constantVector, int modulus) {
        int systemSize = coefficientMatrix.length;
        int[][] augmentedMatrix = new int[systemSize][systemSize + 1];
        
        // Create augmented matrix
        for (int rowIndex = 0; rowIndex < systemSize; rowIndex++) {
            System.arraycopy(coefficientMatrix[rowIndex], 0, augmentedMatrix[rowIndex], 0, systemSize);
            augmentedMatrix[rowIndex][systemSize] = constantVector[rowIndex];
        }
        
        // Gaussian elimination with modular arithmetic
        for (int pivotRow = 0; pivotRow < systemSize; pivotRow++) {
            // Find pivot with non-zero element
            int maxRow = pivotRow;
            for (int row = pivotRow + 1; row < systemSize; row++) {
                if (Math.abs(augmentedMatrix[row][pivotRow]) > Math.abs(augmentedMatrix[maxRow][pivotRow])) {
                    maxRow = row;
                }
            }
            
            // Swap rows if necessary
            if (maxRow != pivotRow) {
                int[] temp = augmentedMatrix[pivotRow];
                augmentedMatrix[pivotRow] = augmentedMatrix[maxRow];
                augmentedMatrix[maxRow] = temp;
            }
            
            // Normalize pivot row
            int pivotElement = augmentedMatrix[pivotRow][pivotRow];
            int inversePivot = calculateModularInverse(pivotElement, modulus);
            
            for (int col = pivotRow; col <= systemSize; col++) {
                augmentedMatrix[pivotRow][col] = (augmentedMatrix[pivotRow][col] * inversePivot) % modulus;
            }
            
            // Eliminate pivot column in other rows
            for (int row = 0; row < systemSize; row++) {
                if (row != pivotRow) {
                    int factor = augmentedMatrix[row][pivotRow];
                    for (int col = pivotRow; col <= systemSize; col++) {
                        augmentedMatrix[row][col] = (augmentedMatrix[row][col] - 
                                                   (factor * augmentedMatrix[pivotRow][col]) % modulus + modulus) % modulus;
                    }
                }
            }
        }
        
        // Extract solution
        int[] solution = new int[systemSize];
        for (int i = 0; i < systemSize; i++) {
            solution[i] = augmentedMatrix[i][systemSize];
        }
        
        return solution;
    }

    public static int calculateModularInverse(int number, int modulus) {
        number = ((number % modulus) + modulus) % modulus;
        int originalModulus = modulus, temporaryValue, quotient;
        int previousX = 0, currentX = 1;
        
        if (modulus == MIN_MODULAR_INVERSE) return ZERO_VALUE;
        
        while (number > 1) {
            quotient = number / modulus;
            temporaryValue = modulus;
            modulus = number % modulus;
            number = temporaryValue;
            temporaryValue = previousX;
            previousX = currentX - quotient * previousX;
            currentX = temporaryValue;
        }
        
        return currentX < 0 ? currentX + originalModulus : currentX;
    }

    public static int[][] createCoefficientMatrix(int[] xValues, int thresholdValue, int modulus) {
        int[][] coefficientMatrix = new int[thresholdValue][thresholdValue];
        for (int matrixRow = 0; matrixRow < thresholdValue; matrixRow++) {
            int xValue = xValues[matrixRow];
            int currentValue = 1;
            for (int matrixCol = 0; matrixCol < thresholdValue; matrixCol++) {
                coefficientMatrix[matrixRow][matrixCol] = currentValue;
                currentValue = (currentValue * xValue) % modulus;
            }
        }
        return coefficientMatrix;
    }

    public static int getModuloValue() {
        return MODULO_VALUE;
    }
} 