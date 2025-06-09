#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "utils.h"
#define SEED 43


void distribute_image(char* secret_image, int k, int n, char* directory){
    FILE *original_image = NULL;
    original_image = fopen(secret_image, "rb");
    char** portadoras = obtener_portadoras(directory, n);
    if (!portadoras) {
        printf("Error: No se encontraron suficientes imágenes.\n");
        return;
    }

    // Obtenemos el alto y ancho de la imagen, para conocer la cantidad total de píxeles
    fseek(original_image, 18, SEEK_SET);
    int32_t width = 0;
    int32_t height = 0;

    fread(&width, sizeof(int32_t), 1, original_image);
    fread(&height, sizeof(int32_t), 1, original_image);

    //Defino la seed para luego poder reconstruir la matriz
    setSeed(SEED);

    //Obtengo el offset para saber donde comienza la imagen
    fseek(original_image,10,SEEK_SET);
    int32_t offset = 0;
    fread(&offset, sizeof (int32_t),1,original_image);

    unsigned char ***shadows;
    shadows = malloc(n * sizeof(unsigned char **));
    for (int i = 0; i < n; i++) {
        shadows[i] = malloc(height * sizeof(unsigned char *));
        for (int j = 0; j < height; j++) {
            shadows[i][j] = malloc(width * sizeof(unsigned char));
        }
    }

    unsigned char** permutation_matrix = create_permutation_matrix(height, width);

    unsigned char** randomized_image = randomize_image(original_image,permutation_matrix,offset,height, width);

    unsigned char** pixels = calculate_pixels(randomized_image, k, n,height,width);

    int row = 0;
    int col = 0;
    int pixels_rows = height * width / k;
    for (int i = 0; i < pixels_rows ; ++i) {
        for (int j= 0; j < n ; j++){
            shadows[j][row][col] = pixels[i][j];
        }
        col++;
        if (col == width) {
            col = 0;
            row++;
        }
    }

    for (int i = 0; i < k; i++) {
        char nombre[128];
        snprintf(nombre, sizeof(nombre), "sombrasG/sombra_portadora_oculta%d.bmp", i + 1);
        guardar_sombra_bmp(portadoras[i], nombre, shadows[i], width, height);
    }


    for (int i = 0; i < n; i++) {
        char salida[128];
        snprintf(salida, sizeof(salida), "%s/portadora_oculta_%d.bmp", "portadoras", i+1);
        ocultar_shadow_LSB(portadoras[i], salida, shadows[i], width, height, i+1, SEED);
    }

    // Liberar memoria de shadows
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < height; j++) {
            free(shadows[i][j]);
        }
        free(shadows[i]);
    }
    free(shadows);
}


void recovery_image(char* output_image, int k, char* directory) {
    char** portadoras = obtener_portadoras(directory, k);
    if (!portadoras) {
        printf("Error: No se encontraron suficientes imágenes.\n");
        return;
    }

    // Ordenar portadoras según el número de sombra
    ordenar_portadoras_por_sombra(portadoras, k);

    // Leer la seed de la primera portadora
    uint16_t seed = leer_seed(portadoras[0]);
    setSeed(seed);

    // Obtener dimensiones
    FILE* file = fopen(portadoras[0], "rb");
    fseek(file, 18, SEEK_SET);
    int32_t width = 0, height = 0;
    fread(&width, sizeof(int32_t), 1, file);
    fread(&height, sizeof(int32_t), 1, file);
    fclose(file);

    // Extraer sombras
    unsigned char*** shadows = extraer_shadows_LSB(portadoras, width, height);

    for (int i = 0; i < k; i++) {
        char nombre[128];
        snprintf(nombre, sizeof(nombre), "sombrasR/sombra_portadora_oculta%d.bmp", i + 1);
        guardar_sombra_bmp(portadoras[i], nombre, shadows[i], width, height);
    }

    // Matriz de Vandermonde
    int** vandermonde = malloc(k * sizeof(int*));
    for (int i = 0; i < k; i++) vandermonde[i] = malloc(k * sizeof(int));
    construir_vandermonde(vandermonde, k);

    // Recuperar imagen randomizada
    unsigned char** randomized_image = malloc(height * sizeof(unsigned char*));
    for (int i = 0; i < height; i++)
        randomized_image[i] = malloc(width * sizeof(unsigned char));

    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col += k) {
            int* resultado = malloc(k * sizeof(int));
            for (int i = 0; i < k; i++)
                resultado[i] = shadows[i][row][col / k];

            int** vander_copia = malloc(k * sizeof(int*));
            for (int i = 0; i < k; i++) {
                vander_copia[i] = malloc(k * sizeof(int));
                memcpy(vander_copia[i], vandermonde[i], k * sizeof(int));
            }

            gauss_jordan_gf257(vander_copia, resultado, k);

            for (int i = 0; i < k && (col + i) < width; i++)
                randomized_image[row][col + i] = (unsigned char)resultado[i];

            for (int i = 0; i < k; i++) free(vander_copia[i]);
            free(vander_copia);
            free(resultado);
        }
    }

    // Deshacer permutación usando la seed leída
    unsigned char** permutation_matrix = create_permutation_matrix(height, width);
    unsigned char** original_image = unrandomize_image(randomized_image, permutation_matrix, height, width);

    guardar_bmp(portadoras[0], output_image, original_image, width, height);

    // Liberar memoria
    for (int i = 0; i < height; i++) {
        free(randomized_image[i]);
        free(original_image[i]);
    }
    free(randomized_image);
    free(original_image);
    for (int i = 0; i < k; i++) {
        for (int j = 0; j < height; j++) free(shadows[i][j]);
        free(shadows[i]);
    }
    free(shadows);
    for (int i = 0; i < k; i++) free(vandermonde[i]);
    free(vandermonde);
}
