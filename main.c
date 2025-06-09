//
// Created by Admin on 01/06/2025.
//
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "utils.h"

#define SEED 43

int main (int argc, char *argv[]) {



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

    FILE *original_image = NULL;
    if (mode == 1){
        original_image = fopen(secret_image, "rb");
    }

    char** portadoras = obtener_portadoras(directory, n);
    if (!portadoras) {
        printf("Error: No se encontraron suficientes imágenes portadoras.\n");
        return 1;
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

    for (int i = 0; i < n; i++) {
        char salida[64];
        sprintf(salida, "portadora_oculta_%d.bmp", i+1);
        ocultar_shadow_LSB(portadoras[i], salida, shadows[i], width, height);
    }


    // Liberar memoria de shadows
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < height; j++) {
            free(shadows[i][j]);
        }
        free(shadows[i]);
    }
    free(shadows);


    return 0;

}
