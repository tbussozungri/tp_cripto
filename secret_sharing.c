#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "secret_sharing.h"
#include <time.h>
#include <math.h>

// Global seed for permutation
int64_t seed;

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

// Modular inverse in GF(257)
int modinv(int a, int p) {
    for (int i = 1; i < p; i++) {
        if ((a * i) % p == 1) return i;
    }
    return 1; // Should not happen for valid input
}

// Interpolate polynomial coefficients in GF(257)
void interpolate_polynomial(uint8_t* x_values, uint8_t* y_values, int k, uint8_t* coefficients) {
    int p = 257;
    if (k == 2) {
        int x1 = x_values[0], x2 = x_values[1];
        int y1 = y_values[0], y2 = y_values[1];
        int denom = (x1 - x2 + p) % p;
        int denom_inv = modinv(denom, p);
        int a1 = ((y1 - y2 + p) % p) * denom_inv % p;
        int a0 = (y1 - a1 * x1 + p * p) % p;
        coefficients[0] = a0;
        coefficients[1] = a1;
        return;
    }
    // For k > 2, use Gaussian elimination
    int mat[10][10]; // max k=10
    int vec[10];
    for (int i = 0; i < k; i++) {
        int x = 1;
        for (int j = 0; j < k; j++) {
            mat[i][j] = x;
            x = (x * x_values[i]) % p;
        }
        vec[i] = y_values[i];
    }
    // Gaussian elimination
    for (int i = 0; i < k; i++) {
        int inv = modinv(mat[i][i], p);
        for (int j = i; j < k; j++) mat[i][j] = (mat[i][j] * inv) % p;
        vec[i] = (vec[i] * inv) % p;
        for (int j = 0; j < k; j++) {
            if (j != i) {
                int factor = mat[j][i];
                for (int l = i; l < k; l++) {
                    mat[j][l] = (mat[j][l] - factor * mat[i][l] + p * p) % p;
                }
                vec[j] = (vec[j] - factor * vec[i] + p * p) % p;
            }
        }
    }
    for (int i = 0; i < k; i++) coefficients[i] = (vec[i] + p) % p;
}

// Permutation table functions
void setSeed(int64_t s) {
    seed = (s ^ 0x5DEECE66DL) & ((1LL << 48) - 1);
}

uint8_t nextChar(void) {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1LL << 48) - 1);
    return (uint8_t)(seed >> 40);
}

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

// BMP file handling functions
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

void write_seed_to_header(BMPHeader* header, uint16_t seed) {
    // BMP reserved1 is at offset 6, reserved2 at 8
    header->reserved1 = seed & 0xFFFF;
}

uint16_t read_seed_from_header(BMPHeader* header) {
    return header->reserved1;
}

// Secret sharing functions
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

    if (info_header.bits_per_pixel != 8) {
        printf("Error: Image must be 8-bit grayscale\n");
        fclose(secret_file);
        return;
    }

    int original_width = info_header.width;
    int original_height = info_header.height;
    size_t image_size = original_width * original_height;
    size_t section_count = (image_size + k - 1) / k;
    int shadow_height = (int)ceil((double)original_height / k);
    size_t shadow_size = original_width * shadow_height;

    uint8_t* image_data = (uint8_t*)malloc(image_size);
    if (!image_data) {
        printf("Error: Memory allocation failed\n");
        fclose(secret_file);
        return;
    }

    fseek(secret_file, header.data_offset, SEEK_SET);
    if (fread(image_data, 1, image_size, secret_file) != image_size) {
        printf("Error: Failed to read image data\n");
        free(image_data);
        fclose(secret_file);
        return;
    }

    // Read the palette (assume 8-bit BMP, palette is 1024 bytes after header)
    size_t palette_size = header.data_offset - sizeof(BMPHeader) - sizeof(BMPInfoHeader);
    uint8_t* palette = malloc(palette_size);
    fseek(secret_file, sizeof(BMPHeader) + sizeof(BMPInfoHeader), SEEK_SET);
    fread(palette, 1, palette_size, secret_file);

    srand(time(NULL));
    uint16_t seed_value = (uint16_t)(rand() & 0xFFFF);
    uint8_t* perm_table = generate_permutation_table(seed_value, image_size);
    if (!perm_table) {
        printf("Error: Failed to generate permutation table\n");
        free(image_data);
        free(palette);
        fclose(secret_file);
        return;
    }

    for (size_t i = 0; i < image_size; i++) {
        image_data[i] ^= perm_table[i];
    }

    // Debug: Print first 16 bytes of image data after permutation
    printf("Permuted image data (first 16 bytes): ");
    for (int b = 0; b < 16 && b < image_size; b++) {
        printf("%02X ", image_data[b]);
    }
    printf("\n");

    uint8_t** shadows = malloc(n * sizeof(uint8_t*));
    for (int i = 0; i < n; i++) {
        shadows[i] = calloc(shadow_size, 1);
    }

    // Fill shadow data row by row
    size_t sec = 0;
    int debug_sections = 0;
    for (int row = 0; row < shadow_height; row++) {
        for (int col = 0; col < original_width; col++) {
            if (sec >= section_count) break;
            uint8_t section[k];
            for (int m = 0; m < k; m++) {
                size_t idx = sec * k + m;
                section[m] = (idx < image_size) ? image_data[idx] : 0;
            }
            // Debug: Print first section
            if (sec == 0) {
                printf("First section: ");
                for (int m = 0; m < k; m++) printf("%02X ", section[m]);
                printf("\n");
            }
            for (int s = 0; s < n; s++) {
                uint8_t val = evaluate_polynomial(section, k, s + 1);
                shadows[s][row * original_width + col] = val;
                // Debug: Print polynomial result for first 3 sections
                if (sec < 3) {
                    printf("Section %zu, Shadow %d: poly = %02X\n", sec, s+1, val);
                }
            }
            sec++;
        }
    }

    for (int i = 0; i < n; i++) {
        char shadow_name[256];
        snprintf(shadow_name, sizeof(shadow_name), "%s/shadow_%d.bmp", directory, i + 1);
        FILE* shadow_file = fopen(shadow_name, "wb");
        if (!shadow_file) {
            printf("Error: Could not create shadow image %d\n", i + 1);
            continue;
        }
        BMPHeader shadow_header = header;
        BMPInfoHeader shadow_info = info_header;
        shadow_info.height = shadow_height;
        shadow_info.image_size = shadow_size;
        shadow_header.file_size = header.data_offset + shadow_size;
        write_seed_to_header(&shadow_header, seed_value);
        if (!write_bmp_header(shadow_file, &shadow_header, &shadow_info)) {
            printf("Error: Failed to write shadow image header %d\n", i + 1);
            fclose(shadow_file);
            continue;
        }
        // Write the palette
        fwrite(palette, 1, palette_size, shadow_file);
        fseek(shadow_file, shadow_header.data_offset, SEEK_SET);
        fwrite(shadows[i], 1, shadow_size, shadow_file);
        fclose(shadow_file);
        printf("Shadow %d BMP Header: file_size=%u, data_offset=%u, width=%d, height=%d, bpp=%d\n",
            i+1, shadow_header.file_size, shadow_header.data_offset, shadow_info.width, shadow_info.height, shadow_info.bits_per_pixel);
        printf("Shadow %d first 16 bytes: ", i+1);
        for (int b = 0; b < 16 && b < shadow_size; b++) {
            printf("%02X ", shadows[i][b]);
        }
        printf("\n");
    }
    for (int i = 0; i < n; i++) free(shadows[i]);
    free(shadows);
    free(image_data);
    free(perm_table);
    free(palette);
    fclose(secret_file);
}

void recover_secret(const char* output_image, int k, int n, const char* directory) {
    char shadow_names[10][256];
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
    int shadow_width = info_header.width;
    int shadow_height = info_header.height;
    size_t shadow_size = shadow_width * shadow_height;
    // Recover original image size
    int original_height = shadow_height * k;
    int original_width = shadow_width;
    size_t image_size = original_width * original_height;
    uint16_t seed_value = read_seed_from_header(&header);
    printf("[DEBUG] Recovery seed value: %u\n", seed_value);
    uint8_t* recovered_data = (uint8_t*)malloc(image_size);
    if (!recovered_data) {
        printf("Error: Memory allocation failed\n");
        fclose(first_shadow);
        return;
    }
    uint8_t* shadow_data[10];
    for (int i = 0; i < k; i++) {
        shadow_data[i] = (uint8_t*)malloc(shadow_size);
        if (!shadow_data[i]) {
            printf("Error: Memory allocation failed\n");
            for (int j = 0; j < i; j++) free(shadow_data[j]);
            free(recovered_data);
            fclose(first_shadow);
            return;
        }
        FILE* shadow_file = fopen(shadow_names[i], "rb");
        if (!shadow_file) {
            printf("Error: Could not open shadow image %d\n", i + 1);
            for (int j = 0; j <= i; j++) free(shadow_data[j]);
            free(recovered_data);
            fclose(first_shadow);
            return;
        }
        fseek(shadow_file, header.data_offset, SEEK_SET);
        fread(shadow_data[i], 1, shadow_size, shadow_file);
        fclose(shadow_file);
    }
    uint8_t x_values[10];
    uint8_t y_values[10];
    uint8_t coefficients[10];
    for (int i = 0; i < k; i++) x_values[i] = i + 1;
    size_t sec = 0;
    size_t out_idx = 0;
    printf("Recovered coefficients[0] (first 16): ");
    int coeff_printed = 0;
    for (int row = 0; row < shadow_height; row++) {
        for (int col = 0; col < shadow_width; col++) {
            if (sec * k >= image_size) break;
            for (int j = 0; j < k; j++) y_values[j] = shadow_data[j][row * shadow_width + col];
            interpolate_polynomial(x_values, y_values, k, coefficients);
            // Debug: Print coefficients for first 3 sections
            if (sec < 3) {
                printf("Section %zu, coefficients: ", sec);
                for (int m = 0; m < k; m++) printf("%02X ", coefficients[m]);
                printf("\n");
            }
            for (int m = 0; m < k && out_idx < image_size; m++, out_idx++) {
                recovered_data[out_idx] = coefficients[k - 1 - m];
                if (coeff_printed < 16) {
                    printf("%02X ", coefficients[k - 1 - m]);
                    coeff_printed++;
                }
            }
            sec++;
        }
    }
    printf("\n");
    // Debug: Print first 16 bytes before permutation
    printf("Recovered data before permutation (first 16 bytes): ");
    for (int b = 0; b < 16 && b < image_size; b++) {
        printf("%02X ", recovered_data[b]);
    }
    printf("\n");
    uint8_t* perm_table = generate_permutation_table(seed_value, image_size);
    if (!perm_table) {
        printf("Error: Failed to generate permutation table\n");
        for (int i = 0; i < k; i++) free(shadow_data[i]);
        free(recovered_data);
        fclose(first_shadow);
        return;
    }
    for (size_t i = 0; i < image_size; i++) {
        recovered_data[i] ^= perm_table[i];
    }
    // Debug: Print first 16 bytes after permutation
    printf("Recovered data after permutation (first 16 bytes): ");
    for (int b = 0; b < 16 && b < image_size; b++) {
        printf("%02X ", recovered_data[b]);
    }
    printf("\n");
    // Read the palette from the first shadow image
    size_t palette_size = header.data_offset - sizeof(BMPHeader) - sizeof(BMPInfoHeader);
    uint8_t* palette = malloc(palette_size);
    fseek(first_shadow, sizeof(BMPHeader) + sizeof(BMPInfoHeader), SEEK_SET);
    fread(palette, 1, palette_size, first_shadow);
    // Write recovered image with original header
    FILE* output_file = fopen(output_image, "wb");
    if (!output_file) {
        printf("Error: Could not create output image\n");
        for (int i = 0; i < k; i++) free(shadow_data[i]);
        free(recovered_data);
        free(perm_table);
        free(palette);
        fclose(first_shadow);
        return;
    }
    BMPHeader orig_header = header;
    BMPInfoHeader orig_info = info_header;
    orig_info.height = original_height;
    orig_info.image_size = image_size;
    orig_header.file_size = orig_header.data_offset + image_size;
    if (!write_bmp_header(output_file, &orig_header, &orig_info)) {
        printf("Error: Failed to write output image header\n");
        for (int i = 0; i < k; i++) free(shadow_data[i]);
        free(recovered_data);
        free(perm_table);
        free(palette);
        fclose(output_file);
        fclose(first_shadow);
        return;
    }
    // Write the palette
    fwrite(palette, 1, palette_size, output_file);
    fseek(output_file, orig_header.data_offset, SEEK_SET);
    fwrite(recovered_data, 1, image_size, output_file);
    for (int i = 0; i < k; i++) free(shadow_data[i]);
    free(recovered_data);
    free(perm_table);
    free(palette);
    fclose(output_file);
    fclose(first_shadow);
} 