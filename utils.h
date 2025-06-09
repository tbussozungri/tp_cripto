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
unsigned char** calculate_pixels(unsigned char** randomized_image, int k, int n, int rows, int cols);
unsigned char polynomial_evaluation(unsigned char* coefficients, int degree, unsigned char x);
void ocultar_shadow_LSB(const char* portadora_path, const char* salida_path, unsigned char** shadow, int width, int height);
char** obtener_portadoras(const char* directory, int n);
#endif //TP_CRIPTO_UTILS_H
