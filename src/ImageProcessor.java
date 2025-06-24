import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

public class ImageProcessor {

    private static final int GRAYSCALE_BITS_PER_PIXEL = 8;
    private static final int PADDING_BYTES_PER_ROW = 4;
    private static final int PADDING_ALIGNMENT = 3;
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
    private static final int FILE_SIZE_OFFSET_2 = 2;
    private static final int FILE_SIZE_OFFSET_3 = 3;
    private static final int FILE_SIZE_OFFSET_4 = 4;
    private static final int FILE_SIZE_OFFSET_5 = 5;
    private static final int IMAGE_SIZE_OFFSET_34 = 34;
    private static final int IMAGE_SIZE_OFFSET_35 = 35;
    private static final int IMAGE_SIZE_OFFSET_36 = 36;
    private static final int IMAGE_SIZE_OFFSET_37 = 37;

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

    public int retrieveHeaderSize() {
        return headerSize;
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

    public boolean dimensionsMatch(ImageProcessor otherImage) {
        return this.extractImageWidth() == otherImage.extractImageWidth() && 
               this.extractImageHeight() == otherImage.extractImageHeight();
    }

    public void writeToFile(String destinationPath) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(destinationPath)) {
            outputStream.write(fileHeader);
            outputStream.write(imagePixels);
        }
    }

    public ImageProcessor resizeImage(int desiredWidth, int desiredHeight) {
        int originalWidth = extractImageWidth();
        int originalHeight = extractImageHeight();

        int cropStartX = (originalWidth - desiredWidth) / 2;
        int cropStartY = (originalHeight - desiredHeight) / 2;

        int originalRowBytes = ((originalWidth + PADDING_ALIGNMENT) / PADDING_BYTES_PER_ROW) * PADDING_BYTES_PER_ROW;
        int newRowBytes = ((desiredWidth + PADDING_ALIGNMENT) / PADDING_BYTES_PER_ROW) * PADDING_BYTES_PER_ROW;

        byte[] resizedPixels = new byte[newRowBytes * desiredHeight];

        for (int rowIndex = 0; rowIndex < desiredHeight; rowIndex++) {
            int sourceRowIndex = cropStartY + rowIndex;
            int sourceRowPosition = originalHeight - 1 - sourceRowIndex;
            int targetRowPosition = desiredHeight - 1 - rowIndex;

            int sourceRowOffset = sourceRowPosition * originalRowBytes;
            int targetRowOffset = targetRowPosition * newRowBytes;

            System.arraycopy(
                imagePixels,
                sourceRowOffset + cropStartX,
                resizedPixels,
                targetRowOffset,
                desiredWidth
            );
        }

        byte[] updatedHeader = fileHeader.clone();      
        updatedHeader[WIDTH_OFFSET_18] = (byte) (desiredWidth & BYTE_MASK);
        updatedHeader[WIDTH_OFFSET_19] = (byte) ((desiredWidth >> 8) & BYTE_MASK);
        updatedHeader[WIDTH_OFFSET_20] = (byte) ((desiredWidth >> 16) & BYTE_MASK);
        updatedHeader[WIDTH_OFFSET_21] = (byte) ((desiredWidth >> 24) & BYTE_MASK);

        updatedHeader[HEIGHT_OFFSET_22] = (byte) (desiredHeight & BYTE_MASK);
        updatedHeader[HEIGHT_OFFSET_23] = (byte) ((desiredHeight >> 8) & BYTE_MASK);
        updatedHeader[HEIGHT_OFFSET_24] = (byte) ((desiredHeight >> 16) & BYTE_MASK);
        updatedHeader[HEIGHT_OFFSET_25] = (byte) ((desiredHeight >> 24) & BYTE_MASK);

        int totalFileSize = updatedHeader.length + resizedPixels.length;
        updatedHeader[FILE_SIZE_OFFSET_2] = (byte) (totalFileSize & BYTE_MASK);
        updatedHeader[FILE_SIZE_OFFSET_3] = (byte) ((totalFileSize >> 8) & BYTE_MASK);
        updatedHeader[FILE_SIZE_OFFSET_4] = (byte) ((totalFileSize >> 16) & BYTE_MASK);
        updatedHeader[FILE_SIZE_OFFSET_5] = (byte) ((totalFileSize >> 24) & BYTE_MASK);

        updatedHeader[IMAGE_SIZE_OFFSET_34] = (byte) (resizedPixels.length & BYTE_MASK);
        updatedHeader[IMAGE_SIZE_OFFSET_35] = (byte) ((resizedPixels.length >> 8) & BYTE_MASK);
        updatedHeader[IMAGE_SIZE_OFFSET_36] = (byte) ((resizedPixels.length >> 16) & BYTE_MASK);
        updatedHeader[IMAGE_SIZE_OFFSET_37] = (byte) ((resizedPixels.length >> 24) & BYTE_MASK);

        return new ImageProcessor(updatedHeader, resizedPixels);
    }

    public int extractIntegerFromHeader(int bytePosition) {
        return ((fileHeader[bytePosition + 3] & BYTE_MASK) << 24) |
               ((fileHeader[bytePosition + 2] & BYTE_MASK) << 16) |
               ((fileHeader[bytePosition + 1] & BYTE_MASK) << 8) |
               (fileHeader[bytePosition] & BYTE_MASK);
    }
}
