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

    public static int getModuloValue() {
        return MODULO_VALUE;
    }

    public static void adjustPolynomialCoefficientsIfNeeded(int polynomialIndex, byte[] scrambledSecretData, 
                                                          int thresholdValue, int totalShares) {
        boolean coefficientsAdjusted;
        do {
            coefficientsAdjusted = false;
            for (int shareId = 1; shareId <= totalShares; shareId++) {
                int evaluationResult = computePolynomialValue(shareId, polynomialIndex, 
                                                            scrambledSecretData, thresholdValue) % MODULO_VALUE;
                
                if (evaluationResult == 256) { // MAX_BYTE_VALUE
                    coefficientsAdjusted = reducePolynomialCoefficients(polynomialIndex, scrambledSecretData, thresholdValue) || coefficientsAdjusted;
                }
            }
        } while (coefficientsAdjusted);
    }

    public static boolean reducePolynomialCoefficients(int polynomialIndex, byte[] scrambledSecretData, int thresholdValue) {
        for (int coefficientIndex = 0; coefficientIndex < thresholdValue; coefficientIndex++) {
            int coefficientPosition = polynomialIndex * thresholdValue + coefficientIndex;
            int coefficientValue = Byte.toUnsignedInt(scrambledSecretData[coefficientPosition]);
            
            if (coefficientValue != 0) {
                scrambledSecretData[coefficientPosition]--;
                return true;
            }
        }
        throw new IllegalStateException("Unable to proceed: all polynomial coefficients are zero and cannot be reduced further");
    }

    public static void computeShareValuesForPolynomial(int polynomialIndex, byte[][] shareValues, 
                                                     byte[] scrambledSecretData, int thresholdValue, int totalShares) {
        for (int shareIndex = 0; shareIndex < totalShares; shareIndex++) {
            int shareId = shareIndex + 1;
            int polynomialValue = computePolynomialValue(shareId, polynomialIndex, 
                                                       scrambledSecretData, thresholdValue) % MODULO_VALUE;
            shareValues[shareIndex][polynomialIndex] = (byte) polynomialValue;
        }
    }

    public static int[] extractYValuesForPolynomial(byte[][] extractedData, int polynomialIndex, int thresholdValue) {
        int[] yValues = new int[thresholdValue];
        for (int shadowIndex = 0; shadowIndex < thresholdValue; shadowIndex++) {
            int extractedValue = Byte.toUnsignedInt(extractedData[shadowIndex][polynomialIndex]);
            yValues[shadowIndex] = extractedValue;
        }
        return yValues;
    }

    public static int[] solvePolynomialCoefficients(int[] shareIdentifiers, int[] yValues, int thresholdValue) {
        return solvePolynomialCoefficientsLagrange(shareIdentifiers, yValues, thresholdValue, MODULO_VALUE);
    }

    //Solves polynomial coefficients using Lagrange interpolation
    public static int[] solvePolynomialCoefficientsLagrange(int[] xValues, int[] yValues, int thresholdValue, int modulus) {
        int[] coefficients = new int[thresholdValue];
        
        // For each coefficient position (a0, a1, a2, ...)
        for (int coeffIndex = 0; coeffIndex < thresholdValue; coeffIndex++) {
            int coefficient = 0;
            
            // For each Lagrange basis polynomial L_i
            for (int i = 0; i < thresholdValue; i++) {
                // Calculate L_i(x) coefficient at position coeffIndex
                int lagrangeCoeff = calculateLagrangeCoefficient(xValues, i, coeffIndex, modulus);
                
                // Multiply by y_i and add to the result
                coefficient = (coefficient + (lagrangeCoeff * yValues[i]) % modulus) % modulus;
            }
            
            coefficients[coeffIndex] = coefficient;
        }
        
        return coefficients;
    }
    
    //Calculates the coefficient of x^coeffIndex in the Lagrange basis polynomial L_i(x)
    private static int calculateLagrangeCoefficient(int[] xValues, int i, int coeffIndex, int modulus) {
        int n = xValues.length;
        
        // Calculate denominator: Π(j≠i) (x_i - x_j)
        int denominator = 1;
        for (int j = 0; j < n; j++) {
            if (j != i) {
                denominator = (denominator * ((xValues[i] - xValues[j] + modulus) % modulus)) % modulus;
            }
        }
        int denominatorInverse = calculateModularInverse(denominator, modulus);
        
        // Calculate numerator coefficient of x^coeffIndex in Π(j≠i) (x - x_j)
        int[] numeratorCoeffs = new int[n];
        numeratorCoeffs[0] = 1; // Start with 1 (constant term)
        
        for (int j = 0; j < n; j++) {
            if (j != i) {
                // Multiply by (x - x_j)
                multiplyByLinearFactor(numeratorCoeffs, (-xValues[j] + modulus) % modulus, modulus);
            }
        }
        
        // Return the coefficient at the desired position
        int result = (numeratorCoeffs[coeffIndex] * denominatorInverse) % modulus;
        return result < 0 ? result + modulus : result;
    }
    
    //Multiplies a polynomial (stored as coefficients) by (x + constant)
    //This is equivalent to shifting and adding the constant multiple
    private static void multiplyByLinearFactor(int[] coeffs, int constant, int modulus) {
        int n = coeffs.length;
        int[] result = new int[n];
        
        // Multiply by x (shift right)
        for (int i = 1; i < n; i++) {
            result[i] = coeffs[i - 1];
        }
        
        // Add constant multiple
        for (int i = 0; i < n; i++) {
            result[i] = (result[i] + (coeffs[i] * constant) % modulus) % modulus;
        }
        
        // Copy back to original array
        System.arraycopy(result, 0, coeffs, 0, n);
    }

    public static void storePolynomialCoefficients(byte[] reconstructedData, int[] coefficients, int polynomialIndex, int thresholdValue) {
        for (int coefficientIndex = 0; coefficientIndex < thresholdValue; coefficientIndex++) {
            int dataIndex = polynomialIndex * thresholdValue + coefficientIndex;
            reconstructedData[dataIndex] = (byte) coefficients[coefficientIndex];
        }
    }
} 