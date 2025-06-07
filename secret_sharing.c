#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "include/secret_sharing.h"
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

// Función para calcular el inverso multiplicativo en módulo 257
static uint8_t mod_inverse(uint8_t a) {
    a = a % 257;
    for (uint8_t x = 1; x < 257; x++) {
        if (((a * x) % 257) == 1) {
            return x;
        }
    }
    return 1; // Nunca debería llegar aquí si a es válido
}

// Función para calcular (a * b) mod 257
static uint8_t mod_multiply(uint8_t a, uint8_t b) {
    return ((uint16_t)a * (uint16_t)b) % 257;
}

// Función para calcular (a + b) mod 257
static uint8_t mod_add(uint8_t a, uint8_t b) {
    return (a + b) % 257;
}

// Función para calcular (a - b) mod 257
static uint8_t mod_subtract(uint8_t a, uint8_t b) {
    return (a + 257 - b) % 257;
}

void interpolate_polynomial(const uint8_t* x_values, const uint8_t* y_values, int k, uint8_t* coefficients) {
    // Inicializar coeficientes a cero
    for (int i = 0; i < k; i++) {
        coefficients[i] = 0;
    }

    // Para cada punto
    for (int i = 0; i < k; i++) {
        uint8_t numerator = 1;
        uint8_t denominator = 1;
        
        // Calcular el término base del polinomio de Lagrange
        for (int j = 0; j < k; j++) {
            if (i != j) {
                numerator = mod_multiply(numerator, mod_subtract(x_values[j], 0));
                denominator = mod_multiply(denominator, mod_subtract(x_values[i], x_values[j]));
            }
        }

        // Calcular el inverso del denominador
        uint8_t inv_denominator = mod_inverse(denominator);
        
        // Calcular el coeficiente de Lagrange
        uint8_t lagrange_coef = mod_multiply(numerator, inv_denominator);
        lagrange_coef = mod_multiply(lagrange_coef, y_values[i]);

        // Acumular el resultado en el coeficiente constante
        coefficients[0] = mod_add(coefficients[0], lagrange_coef);
    }
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

// New steganography functions
BMPImage* read_bmp_image(const char* filename) {
    FILE* file = fopen(filename, "rb");
    if (!file) return NULL;

    BMPImage* image = (BMPImage*)malloc(sizeof(BMPImage));
    if (!image) {
        fclose(file);
        return NULL;
    }

    if (!read_bmp_header(file, &image->header, &image->info_header)) {
        free(image);
        fclose(file);
        return NULL;
    }

    // Read palette for 8-bit images
    image->palette_size = image->header.data_offset - sizeof(BMPHeader) - sizeof(BMPInfoHeader);
    image->palette = (uint8_t*)malloc(image->palette_size);
    if (!image->palette) {
        free(image);
        fclose(file);
        return NULL;
    }
    fread(image->palette, 1, image->palette_size, file);

    // Read image data
    image->size = image->info_header.width * image->info_header.height;
    image->data = (uint8_t*)malloc(image->size);
    if (!image->data) {
        free(image->palette);
        free(image);
        fclose(file);
        return NULL;
    }
    fread(image->data, 1, image->size, file);

    fclose(file);
    return image;
}

void write_bmp_image(BMPImage* image, const char* filename) {
    FILE* file = fopen(filename, "wb");
    if (!file) return;

    write_bmp_header(file, &image->header, &image->info_header);
    fwrite(image->palette, 1, image->palette_size, file);
    fwrite(image->data, 1, image->size, file);

    fclose(file);
}

void hide_shadow_in_carrier(BMPImage* carrier, uint8_t* shadow, size_t shadow_size, int k) {
    if (k == 8) {
        // For k=8, use LSB replacement
        // Each shadow bit is hidden in the LSB of each carrier byte
        for (size_t i = 0; i < shadow_size; i++) {
            carrier->data[i] = (carrier->data[i] & 0xFE) | (shadow[i] & 0x01);
        }
    } else {
        // For k!=8, use a custom method
        // In this case, we'll use the 2 least significant bits
        // This allows us to hide more data but with slightly more visible changes
        for (size_t i = 0; i < shadow_size; i++) {
            carrier->data[i] = (carrier->data[i] & 0xFC) | (shadow[i] & 0x03);
        }
    }
}

uint8_t* extract_shadow_from_carrier(BMPImage* carrier, size_t shadow_size, int k) {
    uint8_t* shadow = (uint8_t*)malloc(shadow_size);
    if (!shadow) return NULL;

    if (k == 8) {
        // For k=8, extract from LSB
        for (size_t i = 0; i < shadow_size; i++) {
            shadow[i] = carrier->data[i] & 0x01;
        }
    } else {
        // For k!=8, extract from 2 LSBs
        for (size_t i = 0; i < shadow_size; i++) {
            shadow[i] = carrier->data[i] & 0x03;
        }
    }

    return shadow;
}

void free_bmp_image(BMPImage* image) {
    if (image) {
        free(image->data);
        free(image->palette);
        free(image);
    }
}

// Modify distribute_secret to use steganography
void distribute_secret(const char* secret_image, const char* carrier_images[], int k, int n, const char* directory) {
    // Read secret image
    BMPImage* secret = read_bmp_image(secret_image);
    if (!secret) {
        printf("Error: Could not read secret image\n");
        return;
    }

    // Generate shadows
    size_t shadow_size = secret->size;
    uint8_t** shadows = (uint8_t**)malloc(n * sizeof(uint8_t*));
    for (int i = 0; i < n; i++) {
        shadows[i] = (uint8_t*)malloc(shadow_size);
    }

    // Generate permutation table
    uint16_t seed_value = (uint16_t)(rand() & 0xFFFF);
    uint8_t* perm_table = generate_permutation_table(seed_value, shadow_size);

    // Apply permutation to secret image
    for (size_t i = 0; i < shadow_size; i++) {
        secret->data[i] ^= perm_table[i];
    }

    // Generate shadows using polynomial evaluation
    for (size_t i = 0; i < shadow_size; i++) {
        uint8_t section[k];
        for (int j = 0; j < k; j++) {
            section[j] = secret->data[i];
        }
        for (int j = 0; j < n; j++) {
            shadows[j][i] = evaluate_polynomial(section, k, j + 1);
        }
    }

    // Hide shadows in carrier images
    for (int i = 0; i < n; i++) {
        BMPImage* carrier = read_bmp_image(carrier_images[i]);
        if (!carrier) {
            printf("Error: Could not read carrier image %d\n", i + 1);
            continue;
        }

        // Verify carrier image size for k=8
        if (k == 8 && (carrier->info_header.width != secret->info_header.width ||
                      carrier->info_header.height != secret->info_header.height)) {
            printf("Error: Carrier image %d must have same size as secret image for k=8\n", i + 1);
            free_bmp_image(carrier);
            continue;
        }

        // Hide shadow in carrier
        hide_shadow_in_carrier(carrier, shadows[i], shadow_size, k);

        // Save carrier with hidden shadow
        char output_name[256];
        snprintf(output_name, sizeof(output_name), "%s/shadow_%d.bmp", directory, i + 1);
        write_bmp_image(carrier, output_name);

        free_bmp_image(carrier);
    }

    // Cleanup
    for (int i = 0; i < n; i++) {
        free(shadows[i]);
    }
    free(shadows);
    free(perm_table);
    free_bmp_image(secret);
}

// Modify recover_secret to use steganography
void recover_secret(const char* carrier_images[], const char* output_image, int k, int n, const char* directory) {
    // Validate input parameters
    if (!carrier_images || !output_image || !directory) {
        printf("Error: Invalid input parameters\n");
        return;
    }

    // Read first carrier to get image size
    BMPImage* first_carrier = read_bmp_image(carrier_images[0]);
    if (!first_carrier) {
        printf("Error: Could not read first carrier image: %s\n", carrier_images[0]);
        return;
    }

    size_t shadow_size = first_carrier->size;
    uint8_t** shadows = (uint8_t**)malloc(k * sizeof(uint8_t*));
    if (!shadows) {
        printf("Error: Memory allocation failed for shadows array\n");
        free_bmp_image(first_carrier);
        return;
    }

    // Initialize shadows array
    for (int i = 0; i < k; i++) {
        shadows[i] = NULL;
    }

    // Extract shadows from carriers
    for (int i = 0; i < k; i++) {
        if (!carrier_images[i]) {
            printf("Error: Missing carrier image %d\n", i + 1);
            goto cleanup;
        }

        BMPImage* carrier = read_bmp_image(carrier_images[i]);
        if (!carrier) {
            printf("Error: Could not read carrier image %d: %s\n", i + 1, carrier_images[i]);
            goto cleanup;
        }

        // Validate carrier image size
        if (carrier->size != shadow_size) {
            printf("Error: Carrier image %d has different size than first carrier\n", i + 1);
            free_bmp_image(carrier);
            goto cleanup;
        }

        shadows[i] = extract_shadow_from_carrier(carrier, shadow_size, k);
        if (!shadows[i]) {
            printf("Error: Failed to extract shadow from carrier %d\n", i + 1);
            free_bmp_image(carrier);
            goto cleanup;
        }

        free_bmp_image(carrier);
    }

    // Recover secret image
    uint8_t* recovered_data = (uint8_t*)malloc(shadow_size);
    if (!recovered_data) {
        printf("Error: Memory allocation failed for recovered data\n");
        goto cleanup;
    }

    for (size_t i = 0; i < shadow_size; i++) {
        uint8_t x_values[10];
        uint8_t y_values[10];
        for (int j = 0; j < k; j++) {
            x_values[j] = j + 1;
            y_values[j] = shadows[j][i];
        }
        uint8_t coefficients[10];
        interpolate_polynomial(x_values, y_values, k, coefficients);
        recovered_data[i] = coefficients[0];
    }

    // Apply inverse permutation
    uint16_t seed_value = first_carrier->header.reserved1;
    uint8_t* perm_table = generate_permutation_table(seed_value, shadow_size);
    if (!perm_table) {
        printf("Error: Failed to generate permutation table\n");
        free(recovered_data);
        goto cleanup;
    }

    for (size_t i = 0; i < shadow_size; i++) {
        recovered_data[i] ^= perm_table[i];
    }

    // Create output image
    BMPImage* output = (BMPImage*)malloc(sizeof(BMPImage));
    if (!output) {
        printf("Error: Memory allocation failed for output image\n");
        free(perm_table);
        free(recovered_data);
        goto cleanup;
    }

    // Copy header and info header
    memcpy(&output->header, &first_carrier->header, sizeof(BMPHeader));
    memcpy(&output->info_header, &first_carrier->info_header, sizeof(BMPInfoHeader));
    
    // Copy palette if it exists
    if (first_carrier->palette) {
        output->palette = (uint8_t*)malloc(first_carrier->palette_size);
        if (!output->palette) {
            printf("Error: Memory allocation failed for palette\n");
            free(perm_table);
            free(recovered_data);
            free(output);
            goto cleanup;
        }
        memcpy(output->palette, first_carrier->palette, first_carrier->palette_size);
        output->palette_size = first_carrier->palette_size;
    } else {
        output->palette = NULL;
        output->palette_size = 0;
    }

    output->data = recovered_data;
    output->size = shadow_size;

    // Save recovered image
    write_bmp_image(output, output_image);
    printf("Successfully recovered secret image to: %s\n", output_image);

    // Cleanup
    free(perm_table);
    free_bmp_image(output);
    free_bmp_image(first_carrier);
    for (int i = 0; i < k; i++) {
        free(shadows[i]);
    }
    free(shadows);
    return;

cleanup:
    // Cleanup in case of error
    if (shadows) {
        for (int i = 0; i < k; i++) {
            free(shadows[i]);
        }
        free(shadows);
    }
    free_bmp_image(first_carrier);
} 