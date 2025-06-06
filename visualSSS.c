#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

// BMP file header structure
#pragma pack(push, 1)
typedef struct {
    uint16_t signature;      // 'BM'
    uint32_t file_size;      // Size of the BMP file
    uint16_t reserved1;      // Reserved
    uint16_t reserved2;      // Reserved
    uint32_t data_offset;    // Offset to image data
} BMPHeader;

typedef struct {
    uint32_t header_size;    // Size of this header
    int32_t width;          // Image width
    int32_t height;         // Image height
    uint16_t planes;        // Number of color planes
    uint16_t bits_per_pixel;// Bits per pixel
    uint32_t compression;   // Compression method
    uint32_t image_size;    // Size of raw bitmap data
    int32_t x_pixels_per_meter;
    int32_t y_pixels_per_meter;
    uint32_t colors_used;   // Number of colors in palette
    uint32_t colors_important;
} BMPInfoHeader;
#pragma pack(pop)

// Global seed for permutation
int64_t seed;

void setSeed(int64_t s) {
    seed = (s ^ 0x5DEECE66DL) & ((1LL << 48) - 1);
}

uint8_t nextChar(void) {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1LL << 48) - 1);
    return (uint8_t)(seed >> 40);
}

// Function to generate permutation table
uint8_t* generate_permutation_table(int64_t seed_value, size_t size) {
    uint8_t* table = (uint8_t*)malloc(size);
    if (!table) {
        return NULL;
    }

    setSeed(seed_value);
    for (size_t i = 0; i < size; i++) {
        table[i] = nextChar();
    }

    return table;
}

// Function prototypes
void distribute_secret(const char* secret_image, int k, int n, const char* directory);
void recover_secret(const char* output_image, int k, int n, const char* directory);
void print_usage(const char* program_name);
int read_bmp_header(FILE* file, BMPHeader* header, BMPInfoHeader* info_header);
int write_bmp_header(FILE* file, BMPHeader* header, BMPInfoHeader* info_header);
void print_bmp_info(BMPHeader* header, BMPInfoHeader* info_header);

// Modular arithmetic functions
uint8_t mod_257(int32_t x) {
    x = x % 257;
    if (x < 0) x += 257;
    return (uint8_t)x;
}

// Polynomial evaluation in GF(257)
uint8_t evaluate_polynomial(uint8_t* coefficients, int k, uint8_t x) {
    int32_t result = coefficients[0];
    for (int i = 1; i < k; i++) {
        result = mod_257(result * x + coefficients[i]);
    }
    return (uint8_t)result;
}

// Lagrange interpolation in GF(257)
void interpolate_polynomial(uint8_t* x_values, uint8_t* y_values, int k, uint8_t* coefficients) {
    for (int i = 0; i < k; i++) {
        coefficients[i] = 0;
        for (int j = 0; j < k; j++) {
            int32_t term = y_values[j];
            for (int m = 0; m < k; m++) {
                if (m != j) {
                    int32_t denominator = mod_257(x_values[j] - x_values[m]);
                    int32_t numerator = mod_257(x_values[i] - x_values[m]);
                    if (denominator == 0) continue;
                    // Find multiplicative inverse
                    int32_t inv = 1;
                    for (int n = 1; n < 257; n++) {
                        if (mod_257(n * denominator) == 1) {
                            inv = n;
                            break;
                        }
                    }
                    term = mod_257(term * numerator * inv);
                }
            }
            coefficients[i] = mod_257(coefficients[i] + term);
        }
    }
}

int main(int argc, char* argv[]) {
    if (argc < 4) {
        print_usage(argv[0]);
        return 1;
    }

    char* secret_image = NULL;
    int k = 0;
    int n = 0;
    char* directory = ".";
    int mode = 0; // 0: undefined, 1: distribute, 2: recover

    // Parse command line arguments
    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "-d") == 0) {
            mode = 1;
        } else if (strcmp(argv[i], "-r") == 0) {
            mode = 2;
        } else if (strcmp(argv[i], "-secret") == 0 && i + 1 < argc) {
            secret_image = argv[++i];
        } else if (strcmp(argv[i], "-k") == 0 && i + 1 < argc) {
            k = atoi(argv[++i]);
        } else if (strcmp(argv[i], "-n") == 0 && i + 1 < argc) {
            n = atoi(argv[++i]);
        } else if (strcmp(argv[i], "-dir") == 0 && i + 1 < argc) {
            directory = argv[++i];
        }
    }

    // Validate required parameters
    if (mode == 0 || secret_image == NULL || k < 2 || k > 10) {
        print_usage(argv[0]);
        return 1;
    }

    // Execute the appropriate mode
    if (mode == 1) {
        distribute_secret(secret_image, k, n, directory);
    } else {
        recover_secret(secret_image, k, n, directory);
    }

    return 0;
}

void print_usage(const char* program_name) {
    printf("Usage: %s [-d|-r] -secret <image> -k <number> [-n <number>] [-dir <directory>]\n", program_name);
    printf("  -d: distribute secret image\n");
    printf("  -r: recover secret image\n");
    printf("  -secret: input/output image file (.bmp)\n");
    printf("  -k: minimum number of shares needed (2-10)\n");
    printf("  -n: total number of shares (optional)\n");
    printf("  -dir: directory for input/output files (optional, default: current directory)\n");
}

void distribute_secret(const char* secret_image, int k, int n, const char* directory) {
    FILE* secret_file = fopen(secret_image, "rb");
    if (!secret_file) {
        printf("Error: Could not open secret image file\n");
        return;
    }

    BMPHeader header;
    BMPInfoHeader info_header;
    if (!read_bmp_header(secret_file, &header, &info_header)) {
        printf("Error: Invalid BMP file\n");
        fclose(secret_file);
        return;
    }

    // Verify 8-bit grayscale
    if (info_header.bits_per_pixel != 8) {
        printf("Error: Image must be 8-bit grayscale\n");
        fclose(secret_file);
        return;
    }

    // Calculate image size
    size_t image_size = info_header.width * info_header.height;
    uint8_t* image_data = (uint8_t*)malloc(image_size);
    if (!image_data) {
        printf("Error: Memory allocation failed\n");
        fclose(secret_file);
        return;
    }

    // Read image data
    fseek(secret_file, header.data_offset, SEEK_SET);
    if (fread(image_data, 1, image_size, secret_file) != image_size) {
        printf("Error: Failed to read image data\n");
        free(image_data);
        fclose(secret_file);
        return;
    }

    // Generate permutation table
    uint8_t* perm_table = generate_permutation_table(12345, image_size); // TODO: Use proper seed
    if (!perm_table) {
        printf("Error: Failed to generate permutation table\n");
        free(image_data);
        fclose(secret_file);
        return;
    }

    // Apply permutation
    for (size_t i = 0; i < image_size; i++) {
        image_data[i] ^= perm_table[i];
    }

    // Create shadow images
    for (int i = 0; i < n; i++) {
        char shadow_name[256];
        snprintf(shadow_name, sizeof(shadow_name), "%s/shadow_%d.bmp", directory, i + 1);
        
        FILE* shadow_file = fopen(shadow_name, "wb");
        if (!shadow_file) {
            printf("Error: Could not create shadow image %d\n", i + 1);
            continue;
        }

        // Write header
        BMPHeader shadow_header = header;
        BMPInfoHeader shadow_info = info_header;
        shadow_header.file_size = header.file_size;
        shadow_header.data_offset = header.data_offset;
        
        if (!write_bmp_header(shadow_file, &shadow_header, &shadow_info)) {
            printf("Error: Failed to write shadow image header %d\n", i + 1);
            fclose(shadow_file);
            continue;
        }

        // Write shadow data
        fseek(shadow_file, shadow_header.data_offset, SEEK_SET);
        
        // Process image in sections of k pixels
        for (size_t j = 0; j < image_size; j += k) {
            uint8_t section[k];
            for (int m = 0; m < k && j + m < image_size; m++) {
                section[m] = image_data[j + m];
            }
            
            // Evaluate polynomial for this section
            uint8_t shadow_value = evaluate_polynomial(section, k, i + 1);
            fwrite(&shadow_value, 1, 1, shadow_file);
        }

        fclose(shadow_file);
    }

    free(image_data);
    free(perm_table);
    fclose(secret_file);
}

void recover_secret(const char* output_image, int k, int n, const char* directory) {
    // First, find k shadow images
    char shadow_names[10][256];  // Assuming max 10 shadow images
    int found_shadows = 0;
    
    for (int i = 0; i < n && found_shadows < k; i++) {
        char shadow_name[256];
        snprintf(shadow_name, sizeof(shadow_name), "%s/shadow_%d.bmp", directory, i + 1);
        
        FILE* shadow_file = fopen(shadow_name, "rb");
        if (shadow_file) {
            strcpy(shadow_names[found_shadows], shadow_name);
            found_shadows++;
            fclose(shadow_file);
        }
    }

    if (found_shadows < k) {
        printf("Error: Could not find enough shadow images (found %d, need %d)\n", found_shadows, k);
        return;
    }

    // Read first shadow image to get dimensions
    FILE* first_shadow = fopen(shadow_names[0], "rb");
    if (!first_shadow) {
        printf("Error: Could not open first shadow image\n");
        return;
    }

    BMPHeader header;
    BMPInfoHeader info_header;
    if (!read_bmp_header(first_shadow, &header, &info_header)) {
        printf("Error: Invalid BMP file\n");
        fclose(first_shadow);
        return;
    }

    // Calculate image size
    size_t image_size = info_header.width * info_header.height;
    uint8_t* recovered_data = (uint8_t*)malloc(image_size);
    if (!recovered_data) {
        printf("Error: Memory allocation failed\n");
        fclose(first_shadow);
        return;
    }

    // Read shadow data
    uint8_t* shadow_data[10];  // Assuming max 10 shadow images
    for (int i = 0; i < k; i++) {
        shadow_data[i] = (uint8_t*)malloc(image_size);
        if (!shadow_data[i]) {
            printf("Error: Memory allocation failed\n");
            // Clean up
            for (int j = 0; j < i; j++) {
                free(shadow_data[j]);
            }
            free(recovered_data);
            fclose(first_shadow);
            return;
        }

        FILE* shadow_file = fopen(shadow_names[i], "rb");
        if (!shadow_file) {
            printf("Error: Could not open shadow image %d\n", i + 1);
            // Clean up
            for (int j = 0; j <= i; j++) {
                free(shadow_data[j]);
            }
            free(recovered_data);
            fclose(first_shadow);
            return;
        }

        fseek(shadow_file, header.data_offset, SEEK_SET);
        if (fread(shadow_data[i], 1, image_size, shadow_file) != image_size) {
            printf("Error: Failed to read shadow data %d\n", i + 1);
            // Clean up
            for (int j = 0; j <= i; j++) {
                free(shadow_data[j]);
            }
            free(recovered_data);
            fclose(shadow_file);
            fclose(first_shadow);
            return;
        }
        fclose(shadow_file);
    }

    // Recover the secret
    uint8_t x_values[10];  // Assuming max 10 shadow images
    uint8_t y_values[10];
    uint8_t coefficients[10];

    for (int i = 0; i < k; i++) {
        x_values[i] = i + 1;  // Shadow indices start at 1
    }

    // Process each pixel
    for (size_t i = 0; i < image_size; i++) {
        // Get y values from shadow images
        for (int j = 0; j < k; j++) {
            y_values[j] = shadow_data[j][i];
        }

        // Interpolate to get coefficients
        interpolate_polynomial(x_values, y_values, k, coefficients);

        // First coefficient is the recovered pixel
        recovered_data[i] = coefficients[0];
    }

    // Generate permutation table
    uint8_t* perm_table = generate_permutation_table(12345, image_size);  // TODO: Use proper seed
    if (!perm_table) {
        printf("Error: Failed to generate permutation table\n");
        // Clean up
        for (int i = 0; i < k; i++) {
            free(shadow_data[i]);
        }
        free(recovered_data);
        fclose(first_shadow);
        return;
    }

    // Apply inverse permutation
    for (size_t i = 0; i < image_size; i++) {
        recovered_data[i] ^= perm_table[i];
    }

    // Write recovered image
    FILE* output_file = fopen(output_image, "wb");
    if (!output_file) {
        printf("Error: Could not create output image\n");
        // Clean up
        for (int i = 0; i < k; i++) {
            free(shadow_data[i]);
        }
        free(recovered_data);
        free(perm_table);
        fclose(first_shadow);
        return;
    }

    // Write header
    if (!write_bmp_header(output_file, &header, &info_header)) {
        printf("Error: Failed to write output image header\n");
        // Clean up
        for (int i = 0; i < k; i++) {
            free(shadow_data[i]);
        }
        free(recovered_data);
        free(perm_table);
        fclose(output_file);
        fclose(first_shadow);
        return;
    }

    // Write image data
    fseek(output_file, header.data_offset, SEEK_SET);
    if (fwrite(recovered_data, 1, image_size, output_file) != image_size) {
        printf("Error: Failed to write output image data\n");
    }

    // Clean up
    for (int i = 0; i < k; i++) {
        free(shadow_data[i]);
    }
    free(recovered_data);
    free(perm_table);
    fclose(output_file);
    fclose(first_shadow);
}

int read_bmp_header(FILE* file, BMPHeader* header, BMPInfoHeader* info_header) {
    if (fread(header, sizeof(BMPHeader), 1, file) != 1) {
        return 0;
    }
    
    if (header->signature != 0x4D42) { // 'BM'
        return 0;
    }
    
    if (fread(info_header, sizeof(BMPInfoHeader), 1, file) != 1) {
        return 0;
    }
    
    return 1;
}

int write_bmp_header(FILE* file, BMPHeader* header, BMPInfoHeader* info_header) {
    if (fwrite(header, sizeof(BMPHeader), 1, file) != 1) {
        return 0;
    }
    
    if (fwrite(info_header, sizeof(BMPInfoHeader), 1, file) != 1) {
        return 0;
    }
    
    return 1;
}

void print_bmp_info(BMPHeader* header, BMPInfoHeader* info_header) {
    printf("BMP File Information:\n");
    printf("File Size: %u bytes\n", header->file_size);
    printf("Data Offset: %u bytes\n", header->data_offset);
    printf("Image Size: %dx%d pixels\n", info_header->width, info_header->height);
    printf("Bits per Pixel: %d\n", info_header->bits_per_pixel);
    printf("Compression: %u\n", info_header->compression);
} 