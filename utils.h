//
// Created by Admin on 01/06/2025.
//

#ifndef TP_CRIPTO_UTILS_H
#define TP_CRIPTO_UTILS_H

char** process_arguments(int argc, char *argv[]);
unsigned char** create_permutation_matrix(int rows,int cols);
unsigned char** randomize_image(FILE* original_image,unsigned char** permutation_matrix,int32_t offset,int rows, int cols);
void setSeed(int64_t s);
uint8_t nextChar(void);
#endif //TP_CRIPTO_UTILS_H
