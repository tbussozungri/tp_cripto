#ifndef SECRET_SHARING_H
#define SECRET_SHARING_H

#include <stdint.h>
#include <stdio.h>

// BMP file structures
typedef struct {
    uint16_t signature;
    uint32_t file_size;
    uint16_t reserved1;
    uint16_t reserved2;
    uint32_t data_offset;
} BMPHeader;

typedef struct {
    uint32_t size;
    int32_t width;
    int32_t height;
    uint16_t planes;
    uint16_t bits_per_pixel;
    uint32_t compression;
    uint32_t image_size;
    int32_t x_pixels_per_m;
    int32_t y_pixels_per_m;
    uint32_t colors_used;
    uint32_t colors_important;
} BMPInfoHeader;

// Image structure for steganography
typedef struct {
    BMPHeader header;
    BMPInfoHeader info_header;
    uint8_t* data;
    size_t size;
    uint8_t* palette;
    size_t palette_size;
} BMPImage;

// Function declarations
void distribute_secret(const char* secret_image, const char* carrier_images[], int k, int n, const char* directory);
void recover_secret(const char* carrier_images[], const char* output_image, int k, int n, const char* directory);

// New steganography functions
BMPImage* read_bmp_image(const char* filename);
void write_bmp_image(BMPImage* image, const char* filename);
void hide_shadow_in_carrier(BMPImage* carrier, uint8_t* shadow, size_t shadow_size, int k);
uint8_t* extract_shadow_from_carrier(BMPImage* carrier, size_t shadow_size, int k);
void free_bmp_image(BMPImage* image);

// Function prototypes
int read_bmp_header(FILE* file, BMPHeader* header, BMPInfoHeader* info_header);
int write_bmp_header(FILE* file, BMPHeader* header, BMPInfoHeader* info_header);
void print_bmp_info(BMPHeader* header, BMPInfoHeader* info_header);

// Modular arithmetic functions
uint8_t mod_257(int32_t x);
uint8_t evaluate_polynomial(uint8_t* coefficients, int k, uint8_t x);
void interpolate_polynomial(uint8_t* x_values, uint8_t* y_values, int k, uint8_t* coefficients);

// Permutation table functions
void setSeed(int64_t s);
uint8_t nextChar(void);
uint8_t* generate_permutation_table(int64_t seed_value, size_t size);

#endif // SECRET_SHARING_H 