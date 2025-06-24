import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommandLineParser {
    
    private static final int MIN_K = 2;
    private static final int MAX_K = 10;
    private static final int MIN_N = 2;
    
    public static Map<String, String> parseArguments(String[] commandLineArgs) {
        if (commandLineArgs == null || commandLineArgs.length == 0) {
            throw new IllegalArgumentException("No command line arguments provided");
        }
        
        Map<String, String> parameterMap = new HashMap<>();
        
        for (int argumentIndex = 0; argumentIndex < commandLineArgs.length; argumentIndex++) {
            String currentArg = commandLineArgs[argumentIndex];
            
            if (!currentArg.startsWith("-")) {
                throw new IllegalArgumentException("Invalid argument format: " + currentArg + ". All arguments must start with '-'");
            }
            
            switch (currentArg) {
                case "-d":
                    if (parameterMap.containsKey("mode")) {
                        throw new IllegalArgumentException("Operation mode already specified. Cannot use both -d and -r");
                    }
                    parameterMap.put("mode", "d");
                    break;
                    
                case "-r":
                    if (parameterMap.containsKey("mode")) {
                        throw new IllegalArgumentException("Operation mode already specified. Cannot use both -d and -r");
                    }
                    parameterMap.put("mode", "r");
                    break;
                    
                case "-secret":
                    if (parameterMap.containsKey("secret")) {
                        throw new IllegalArgumentException("Secret file already specified");
                    }
                    if (argumentIndex + 1 >= commandLineArgs.length) {
                        throw new IllegalArgumentException("No filename provided after -secret parameter");
                    }
                    String secretFile = commandLineArgs[++argumentIndex];
                    if (!secretFile.endsWith(".bmp")) {
                        throw new IllegalArgumentException("Secret file must have .bmp extension: " + secretFile);
                    }
                    parameterMap.put("secret", secretFile);
                    break;
                    
                case "-k":
                    if (parameterMap.containsKey("k")) {
                        throw new IllegalArgumentException("Threshold value k already specified");
                    }
                    if (argumentIndex + 1 >= commandLineArgs.length) {
                        throw new IllegalArgumentException("No value provided after -k parameter");
                    }
                    String kValue = commandLineArgs[++argumentIndex];
                    validateNumericParameter(kValue, "k", MIN_K, MAX_K);
                    parameterMap.put("k", kValue);
                    break;
                    
                case "-n":
                    if (parameterMap.containsKey("n")) {
                        throw new IllegalArgumentException("Total shares value n already specified");
                    }
                    if (argumentIndex + 1 >= commandLineArgs.length) {
                        throw new IllegalArgumentException("No value provided after -n parameter");
                    }
                    String nValue = commandLineArgs[++argumentIndex];
                    validateNumericParameter(nValue, "n", MIN_N, Integer.MAX_VALUE);
                    parameterMap.put("n", nValue);
                    break;
                    
                case "-dir":
                    if (parameterMap.containsKey("dir")) {
                        throw new IllegalArgumentException("Directory already specified");
                    }
                    if (argumentIndex + 1 >= commandLineArgs.length) {
                        throw new IllegalArgumentException("No directory path provided after -dir parameter");
                    }
                    String directory = commandLineArgs[++argumentIndex];
                    validateDirectory(directory);
                    parameterMap.put("dir", directory);
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown parameter: " + currentArg);
            }
        }
        
        return parameterMap;
    }

    private static void validateNumericParameter(String value, String paramName, int minValue, int maxValue) {
        try {
            int numValue = Integer.parseInt(value);
            if (numValue < minValue || numValue > maxValue) {
                throw new IllegalArgumentException("Parameter -" + paramName + " must be between " + minValue + " and " + maxValue + ": " + numValue);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter -" + paramName + " requires a valid integer value: " + value);
        }
    }

    private static void validateDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            throw new IllegalArgumentException("Directory does not exist: " + directoryPath);
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + directoryPath);
        }
        if (!directory.canRead()) {
            throw new IllegalArgumentException("Directory is not readable: " + directoryPath);
        }
    }

    public static void validateArguments(Map<String, String> parameters) {
        // Check required parameters
        if (!parameters.containsKey("mode")) {
            throw new IllegalArgumentException("Operation mode (-d or -r) is mandatory");
        }
        if (!parameters.containsKey("secret")) {
            throw new IllegalArgumentException("Secret file path (-secret) is mandatory");
        }
        if (!parameters.containsKey("k")) {
            throw new IllegalArgumentException("Threshold value (-k) is mandatory");
        }
        
        // Validate k and n relationship
        int k = Integer.parseInt(parameters.get("k"));
        if (parameters.containsKey("n")) {
            int n = Integer.parseInt(parameters.get("n"));
            if (k > n) {
                throw new IllegalArgumentException("Threshold k (" + k + ") cannot be greater than total shares n (" + n + ")");
            }
        }
        
        // Validate secret file existence for distribution mode
        if ("d".equals(parameters.get("mode"))) {
            String secretFile = parameters.get("secret");
            File file = new File(secretFile);
            if (!file.exists()) {
                throw new IllegalArgumentException("Secret file does not exist: " + secretFile);
            }
            if (!file.canRead()) {
                throw new IllegalArgumentException("Secret file is not readable: " + secretFile);
            }
        }
    }

    public static void displayUsageAndTerminate(String errorMessage) {
        System.err.println("Error: " + errorMessage);
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  Distribution mode:");
        System.err.println("    java -cp build VisualSSS -d -secret <file.bmp> -k <num> [-n <num>] [-dir <directory>]");
        System.err.println();
        System.err.println("  Recovery mode:");
        System.err.println("    java -cp build VisualSSS -r -secret <file.bmp> -k <num> [-n <num>] [-dir <directory>]");
        System.err.println();
        System.err.println("Parameters:");
        System.err.println("  -d                    Distribution mode (hide secret in shadows)");
        System.err.println("  -r                    Recovery mode (reconstruct secret from shadows)");
        System.err.println("  -secret <file.bmp>    Secret image file (input for distribution, output for recovery)");
        System.err.println("  -k <num>              Minimum shares required (2-10)");
        System.err.println("  -n <num>              Total number of shares (optional, defaults to available images)");
        System.err.println("  -dir <directory>      Working directory (optional, defaults to current directory)");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java -cp build VisualSSS -d -secret secret.bmp -k 2 -n 4 -dir images/");
        System.err.println("  java -cp build VisualSSS -r -secret recovered.bmp -k 2 -dir shadows/");
        System.exit(1);
    }
} 
