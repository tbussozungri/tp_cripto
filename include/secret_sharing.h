#ifndef SECRET_SHARING_H
#define SECRET_SHARING_H

#include <stdint.h>

// BMP file header structures
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

// Function prototypes
void distribute_secret(const char* secret_image, int k, int n, const char* directory);
void recover_secret(const char* output_image, int k, int n, const char* directory);
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