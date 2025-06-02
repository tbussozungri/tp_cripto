//
// Created by Admin on 01/06/2025.
//
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "utils.h"

#define SEED 43

int main (int argc, char *argv[]) {

    // Indicates the operation mode (distribution or recovery)
    int distribution_mode = 0;

    if (strcmp(argv[1],"-d") == 0)
        distribution_mode = 1;
    else if (strcmp(argv[1],"-r") == 0)
        distribution_mode = 0;
    else{
        printf("Error: Invalid operation mode specified. Use -d for distribution or -r for recovery.\n");
        return 1;
    }

    char** arguments = process_arguments(argc, argv);

    FILE *original_image = NULL;
    if (distribution_mode){
        original_image = fopen(arguments[0], "rb");
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

    unsigned char** permutation_matrix = create_permutation_matrix(height, width);

    unsigned char** randomized_image = randomize_image(original_image,permutation_matrix,offset,height, width);

    return 0;

}
