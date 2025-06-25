import java.io.File;
import java.util.Map;

public class VisualSSS {

    private static final int MINIMUM_ARGUMENTS = 4;
    private static final int DEFAULT_TOTAL_SHARES = -1;
    private static final String DEFAULT_WORKING_DIRECTORY = ".";

    public static void main(String[] commandLineArgs) throws Exception {
        try {
            if (commandLineArgs.length < MINIMUM_ARGUMENTS) {
                CommandLineParser.displayUsageAndTerminate("Insufficient command line arguments provided");
            }
            
            Map<String, String> parsedParameters = CommandLineParser.parseArguments(commandLineArgs);
            CommandLineParser.validateArguments(parsedParameters);
            
            String operationMode = parsedParameters.get("mode");
            String secretFilePath = parsedParameters.get("secret");
            int thresholdParameter = Integer.parseInt(parsedParameters.get("k"));
            int totalSharesParameter = parsedParameters.containsKey("n") ? 
                Integer.parseInt(parsedParameters.get("n")) : DEFAULT_TOTAL_SHARES;
            String workingDirectory = parsedParameters.getOrDefault("dir", DEFAULT_WORKING_DIRECTORY);
            
            if (operationMode.equals("d")) {
                executeDistribution(secretFilePath, thresholdParameter, totalSharesParameter, workingDirectory);
            } else if (operationMode.equals("r")) {
                executeRecovery(secretFilePath, thresholdParameter, totalSharesParameter, workingDirectory);
            } else {
                CommandLineParser.displayUsageAndTerminate("Invalid operation mode specified: must be -d or -r");
            }
            
        } catch (IllegalArgumentException e) {
            CommandLineParser.displayUsageAndTerminate(e.getMessage());
        }
    }

    private static void executeDistribution(String secretFilePath, int thresholdParameter, 
                                          int totalSharesParameter, String workingDirectory) throws Exception {
        FileManager.validateFileExists(secretFilePath, "secret file");
        FileManager.cleanShadowDirectory();
        
        if (totalSharesParameter == DEFAULT_TOTAL_SHARES) {
            totalSharesParameter = FileManager.determineTotalSharesFromDirectory(workingDirectory);
        }
        
        short randomSeed = DataScrambler.createRandomSeed();
        ImageProcessor secretImageProcessor = new ImageProcessor(secretFilePath);
        byte[] originalSecretData = secretImageProcessor.retrievePixelData();
        byte[] scrambledSecretData = DataScrambler.scrambleDataWithSeed(randomSeed, originalSecretData);
        
        SecretSharing secretSharing = new SecretSharing(scrambledSecretData, 
        thresholdParameter, totalSharesParameter, secretImageProcessor.extractImageWidth(),
        secretImageProcessor.extractImageHeight(), secretImageProcessor, workingDirectory);
        secretSharing.createShares(randomSeed);
    }

    private static void executeRecovery(String secretFilePath, int thresholdParameter, 
                                       int totalSharesParameter, String workingDirectory) throws Exception {
        if (totalSharesParameter == DEFAULT_TOTAL_SHARES) {
            totalSharesParameter = FileManager.determineTotalSharesFromDirectory(workingDirectory);
        }
        
        FileManager.validateEnoughBmpFiles(workingDirectory, totalSharesParameter, "working directory");
        
        SecretSharing secretSharing = new SecretSharing(thresholdParameter, totalSharesParameter, workingDirectory);
        byte[] scrambledSecretData = secretSharing.reconstructSecret();
        short randomSeed = secretSharing.extractRandomSeed();
        byte[] originalSecretData = DataScrambler.unscrambleDataWithSeed(randomSeed, scrambledSecretData);
        
        File[] shadowFiles = FileManager.getBmpFilesInDirectory(workingDirectory);
        ImageProcessor referenceShadow = new ImageProcessor(shadowFiles[0].getAbsolutePath());
        byte[] imageHeader = referenceShadow.retrieveHeader();
        ImageProcessor reconstructedImage = new ImageProcessor(imageHeader, originalSecretData);
        reconstructedImage.writeToFile(secretFilePath);
    }
}
