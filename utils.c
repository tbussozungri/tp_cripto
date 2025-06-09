//
// Created by Admin on 01/06/2025.
//
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <dirent.h>
#include <string.h>
int64_t seed;
void setSeed(int64_t s){
    seed = (s ^ 0x5DEECE66DL) & ((1LL << 48) - 1);
}

uint8_t nextChar(void){
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1LL << 48) - 1);
    return (uint8_t)(seed>>40);
}

char** process_arguments(int argc, char *argv[]){
    char **values = malloc(sizeof(char*) * (argc / 2));
    int count = 0;

    for (int i = 1; i < argc - 1; i++) {
        if (argv[i][0] == '-' && argv[i + 1][0] != '-') {
            values[count] = argv[i + 1];
            count++;
            i++;
        }
    }
    return values;
}

unsigned char** create_permutation_matrix(int rows,int cols){
    unsigned char** permutation_matrix = malloc(rows * sizeof (unsigned char*));

    for (int i = 0; i < rows ; ++i) {
        permutation_matrix[i] = malloc(cols * sizeof(unsigned char));
        for (int j = 0; j < cols ; ++j) {
            uint8_t value = nextChar();
            permutation_matrix[i][j] = value;
        }
    }
    return permutation_matrix;
}

unsigned char** randomize_image(FILE* original_image,unsigned char** permutation_matrix,int32_t offset,int rows, int cols){
    // Avanzo hasta donde comienzan los bytes de la imagen
    fseek(original_image,offset,SEEK_SET);

    unsigned char pixel ;
    unsigned char ** randomized_image = malloc(rows * sizeof (unsigned char *));
    for (int i = 0; i < rows ; ++i) {
        randomized_image[i] = malloc(cols * sizeof (unsigned char));
        for (int j = 0; j < cols ; ++j) {
            fread(&pixel,sizeof (unsigned char),1,original_image);
            randomized_image[i][j] = pixel ^ permutation_matrix[i][j];
            // Avanzo al siguiente pixel
            fseek(original_image,sizeof (unsigned char),SEEK_CUR);
        }
    }
    return randomized_image;
}

unsigned char polynomial_evaluation(unsigned char* coefficients, int degree, unsigned char x) {
    int result = 0;
    int x_pow = 1;
    for (int i = 0; i < degree; i++) {
        result = (result + (coefficients[i] * x_pow) % 257) % 257;
        x_pow = (x_pow * x) % 257;
    }
    return (unsigned char)result;
}

unsigned char** calculate_pixels(unsigned char** randomized_image, int k, int n, int rows, int cols) {
    unsigned char* pixels = malloc( k * sizeof(unsigned char));
    unsigned char* values = malloc(n * sizeof(unsigned char));
    unsigned char ** result = malloc(rows * cols / k * sizeof (unsigned char *));
    int total_pixels = rows * cols;
    int count = 0;
    int idx = 0;
    int index = 0;
    while (idx < total_pixels) {
        int row = idx / cols;
        int col = idx % cols;
        pixels[count] = randomized_image[row][col];
        count++;
        idx++;
        // Si ya tomaste k píxeles, puedes hacer aquí lo que necesites con el bloque
        if (count % k == 0) {
            count = 0;
            for (int i = 1; i <= n; ++i) {
                values[i] = polynomial_evaluation(pixels, k, i);
            }
            result[index] = values;
            index++;
        }

    }
    return result;
}

void ocultar_shadow_LSB(const char* portadora_path, const char* salida_path, unsigned char** shadow, int width, int height) {
    FILE* portadora = fopen(portadora_path, "rb");
    FILE* salida = fopen(salida_path, "wb");
    if (!portadora || !salida) {
        printf("No se pudo abrir el archivo portadora o de salida.\n");
        if (portadora) fclose(portadora);
        if (salida) fclose(salida);
        return;
    }

    // Leer y escribir encabezado BMP (asume 8 bits por píxel, 54+1024 bytes)
    unsigned char header[1078];
    fread(header, 1, 1078, portadora);
    fwrite(header, 1, 1078, salida);

    int row_padded = (width + 3) & (~3);
    unsigned char* buffer = malloc(row_padded);

    for (int y = height - 1; y >= 0; y--) {
        fread(buffer, 1, row_padded, portadora);
        for (int x = 0; x < width; x++) {
            // Oculta el bit menos significativo
            buffer[x] = (buffer[x] & 0xFE) | (shadow[y][x] & 0x01);
        }
        fwrite(buffer, 1, row_padded, salida);
    }

    free(buffer);
    fclose(portadora);
    fclose(salida);
}

char** obtener_portadoras(const char* directory, int n) {
    DIR* dir = opendir(directory);
    if (!dir) return NULL;

    char** portadoras = malloc(n * sizeof(char*));
    int count = 0;
    struct dirent* entry;
    while ((entry = readdir(dir)) && count < n) {
        if (strstr(entry->d_name, ".bmp")) {
            portadoras[count] = malloc(512);
            snprintf(portadoras[count], 512, "%s/%s", directory, entry->d_name);
            count++;
        }
    }
    closedir(dir);

    if (count < n) {
        // No hay suficientes imágenes
        for (int i = 0; i < count; i++) free(portadoras[i]);
        free(portadoras);
        return NULL;
    }
    return portadoras;
}