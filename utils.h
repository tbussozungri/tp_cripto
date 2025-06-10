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
void ocultar_shadow_LSB(const char* portadora_path, const char* salida_path, unsigned char** shadow, int width, int height, int shadow_num, int seed);
char** obtener_portadoras(const char* directory, int n);
void distribute_image(char* secret_image, int k, int n, char* directory);
void recovery_image(char* secret_image, int k, char* directory);
unsigned char** extraer_shadows_LSB(char* portadoras, int width, int height,int k);
int inverso_modulo_257(int a);
void gauss_jordan_gf257(int** matriz, int* resultado, int k);
void construir_vandermonde(int** matriz, int k);
uint16_t leer_numero_sombra(const char* filename);
void ordenar_portadoras_por_sombra(char** portadoras, int k);
uint16_t leer_seed(const char* filename);
unsigned char** unrandomize_image(unsigned char** randomized_image, unsigned char** permutation_matrix, int height, int width);
void guardar_bmp(const char* portadora_path, const char* filename, unsigned char** image, int width, int height);
#endif //TP_CRIPTO_UTILS_H
