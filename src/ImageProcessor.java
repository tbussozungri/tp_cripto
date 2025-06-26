import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

public class ImageProcessor {

    private static final int GRAYSCALE_BITS_PER_PIXEL = 8;
    private static final int BYTE_MASK = 0xFF;
    private static final int HEADER_SIZE_OFFSET_10 = 10;
    private static final int HEADER_SIZE_OFFSET_11 = 11;
    private static final int HEADER_SIZE_OFFSET_12 = 12;
    private static final int HEADER_SIZE_OFFSET_13 = 13;
    private static final int BITS_PER_PIXEL_OFFSET = 28;
    private static final int WIDTH_OFFSET_18 = 18;
    private static final int WIDTH_OFFSET_19 = 19;
    private static final int WIDTH_OFFSET_20 = 20;
    private static final int WIDTH_OFFSET_21 = 21;
    private static final int HEIGHT_OFFSET_22 = 22;
    private static final int HEIGHT_OFFSET_23 = 23;
    private static final int HEIGHT_OFFSET_24 = 24;
    private static final int HEIGHT_OFFSET_25 = 25;

    private byte[] fileHeader;
    private byte[] imagePixels;
    private int headerSize;

    public ImageProcessor(String filePath) throws IOException {
        byte[] completeFileContent = Files.readAllBytes(new File(filePath).toPath());

        headerSize = ((completeFileContent[HEADER_SIZE_OFFSET_13] & BYTE_MASK) << 24) | ((completeFileContent[HEADER_SIZE_OFFSET_12] & BYTE_MASK) << 16) |
                    ((completeFileContent[HEADER_SIZE_OFFSET_11] & BYTE_MASK) << 8) | (completeFileContent[HEADER_SIZE_OFFSET_10] & BYTE_MASK);

        fileHeader = Arrays.copyOfRange(completeFileContent, 0, headerSize);

        if (fileHeader[BITS_PER_PIXEL_OFFSET] != GRAYSCALE_BITS_PER_PIXEL) {
            throw new IOException("Image format error: only grayscale images are supported");
        }

        imagePixels = Arrays.copyOfRange(completeFileContent, headerSize, completeFileContent.length);
    }

    public ImageProcessor(byte[] fileHeader, byte[] imagePixels) {
        this.fileHeader = fileHeader;
        this.imagePixels = imagePixels;
        this.headerSize = fileHeader.length;
    }

    public byte[] retrievePixelData() {
        return imagePixels;
    }

    public byte[] retrieveHeader() {
        return fileHeader;
    }

    public int extractImageWidth() {
        return extractIntegerFromHeader(WIDTH_OFFSET_18);
    }

    public int extractImageHeight() {
        return extractIntegerFromHeader(HEIGHT_OFFSET_22);
    }

    public short extractReservedField(int bytePosition) {
        return (short)(((fileHeader[bytePosition + 1] & BYTE_MASK) << 8) | (fileHeader[bytePosition] & BYTE_MASK));
    }

    public void updatePixelData(byte[] newPixels) {
        this.imagePixels = newPixels;
    }

    public void modifyReservedField(int bytePosition, short fieldValue) {
        fileHeader[bytePosition] = (byte) (fieldValue & BYTE_MASK);        
        fileHeader[bytePosition + 1] = (byte) ((fieldValue >> 8) & BYTE_MASK);
    }

    public void modifyEmbeddedBytesField(int bytePosition, int fieldValue) {
        fileHeader[bytePosition] = (byte) (fieldValue & BYTE_MASK);        
        fileHeader[bytePosition + 1] = (byte) ((fieldValue >> 8) & BYTE_MASK);
        fileHeader[bytePosition + 2] = (byte) ((fieldValue >> 16) & BYTE_MASK);
    }

    public void writeToFile(String destinationPath) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
            outputStream.write(fileHeader);
            outputStream.write(imagePixels);
        }
    }

    public int extractIntegerFromHeader(int bytePosition) {
        return ((fileHeader[bytePosition + 3] & BYTE_MASK) << 24) |
               ((fileHeader[bytePosition + 2] & BYTE_MASK) << 16) |
               ((fileHeader[bytePosition + 1] & BYTE_MASK) << 8) |
               (fileHeader[bytePosition] & BYTE_MASK);
    }
}
