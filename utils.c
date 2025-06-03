//
// Created by Admin on 01/06/2025.
//
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
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

unsigned char * calculate_pixels(unsigned char** randomized_image, int k, int n){
    unsigned char pixel;
    unsigned char * pixels = malloc(n * sizeof(unsigned char));
    for (int i = 0; i < k; i++){
        fread(&pixel, sizeof(unsigned char), 1, randomized_image);
        pixels[i] = pixel;
        fseek(randomized_image, sizeof(unsigned char), SEEK_CUR);
    }

}