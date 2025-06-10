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
    unsigned char* pixels = malloc(k * sizeof(unsigned char));
    unsigned char* values = malloc(n * sizeof(unsigned char));
    unsigned char** result = malloc((rows * cols / k) * sizeof(unsigned char*));
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

        if (count == k) {
            count = 0;
            for (int i = 0; i < n; i++) {
                values[i] = polynomial_evaluation(pixels, k, i + 1);
            }
            result[index] = malloc(n * sizeof(unsigned char));
            memcpy(result[index], values, n * sizeof(unsigned char));
            index++;
        }
    }

    free(pixels);
    free(values);
    return result;
}

void ocultar_shadow_LSB(const char* portadora_path, const char* salida_path, unsigned char** shadow, int width, int height, uint16_t shadow_num, int own_seed) {
    FILE* portadora = fopen(portadora_path, "rb");
    FILE* salida = fopen(salida_path, "wb");
    if (!portadora || !salida) {
        printf("No se pudo abrir el archivo portadora o de salida.\n");
        if (portadora) fclose(portadora);
        if (salida) fclose(salida);
        return;
    }

    unsigned char header[1078];
    fread(header, 1, 1078, portadora);

    // Guardar seed en bytes 6 y 7 (little endian)
    header[6] = (unsigned char)(own_seed & 0xFF);
    header[7] = (unsigned char)((own_seed >> 8) & 0xFF);

    // Guardar número de shadow en bytes 8 y 9 (little endian)
    header[8] = (unsigned char)(shadow_num & 0xFF);
    header[9] = (unsigned char)((shadow_num >> 8) & 0xFF);

    fwrite(header, 1, 1078, salida);

    int row_padded = (width + 3) & (~3);
    unsigned char* buffer = malloc(row_padded);

    for (int y = height - 1; y >= 0; y--) {
        fread(buffer, 1, row_padded, portadora);
        for (int x = 0; x < width; x++) {
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

unsigned char** extraer_shadows_LSB(const char* portadora, int width, int height) {
    FILE* f = fopen(portadora, "rb");
    if (!f) return NULL;

    unsigned char header[1078];
    fread(header, 1, 1078, f);

    int row_padded = (width + 3) & (~3);
    unsigned char* buffer = malloc(row_padded);
    if (!buffer) {
        fclose(f);
        return NULL;
    }

    unsigned char** sombra = malloc(height * sizeof(unsigned char*));
    if (!sombra) {
        fclose(f);
        free(buffer);
        return NULL;
    }

    for (int y = height - 1; y >= 0; y--) {
        sombra[y] = malloc(width);
        if (!sombra[y]) {
            for (int k = y + 1; k < height; k++) free(sombra[k]);
            free(sombra);
            free(buffer);
            fclose(f);
            return NULL;
        }

        fread(buffer, 1, row_padded, f);
        for (int x = 0; x < width; x++) {
            sombra[y][x] = buffer[x] & 0x01 ? 255 : 0; // escalar a blanco o negro si lo vas a visualizar
        }
    }

    free(buffer);
    fclose(f);
    return sombra;
}

int inverso_modulo_257(int a) {
    int t = 0, newt = 1;
    int r = 257, newr = a;
    while (newr != 0) {
        int quotient = r / newr;
        int temp = newt;
        newt = t - quotient * newt;
        t = temp;
        temp = newr;
        newr = r - quotient * newr;
        r = temp;
    }
    if (r > 1) return -1;
    if (t < 0) t += 257;
    return t;
}

void gauss_jordan_gf257(int** matriz, int* resultado, int k) {
    for (int i = 0; i < k; i++) {
        // Buscar pivote
        int pivote = i;
        for (int j = i; j < k; j++) {
            if (matriz[j][i] != 0) {
                pivote = j;
                break;
            }
        }
        // Intercambiar filas si es necesario
        if (pivote != i) {
            int* tmp = matriz[i];
            matriz[i] = matriz[pivote];
            matriz[pivote] = tmp;
            int tmp_res = resultado[i];
            resultado[i] = resultado[pivote];
            resultado[pivote] = tmp_res;
        }
        // Hacer 1 el pivote
        int inv = inverso_modulo_257(matriz[i][i]);
        if (inv == -1) {
            printf("No hay inverso, sistema sin solución única.\n");
            return;
        }
        for (int k2 = 0; k2 < k; k2++)
            matriz[i][k2] = (matriz[i][k2] * inv) % 257;
        resultado[i] = (resultado[i] * inv) % 257;

        // Eliminar hacia arriba y abajo
        for (int j = 0; j < k; j++) {
            if (j != i && matriz[j][i] != 0) {
                int factor = matriz[j][i];
                for (int k2 = 0; k2 < k; k2++)
                    matriz[j][k2] = (matriz[j][k2] - factor * matriz[i][k2] + 257 * 257) % 257;
                resultado[j] = (resultado[j] - factor * resultado[i] + 257 * 257) % 257;
            }
        }
    }
}

void construir_vandermonde(int** matriz, int k) {
    for (int i = 0; i < k; i++) {
        int x = i + 1;
        int val = 1;
        for (int j = 0; j < k; j++) {
            matriz[i][j] = val;
            val = (val * x) % 257;
        }
    }
}

uint16_t leer_numero_sombra(const char* filename) {
    FILE* f = fopen(filename, "rb");
    if (!f) return 0;
    fseek(f, 8, SEEK_SET);
    uint16_t num = 0;
    fread(&num, sizeof(uint16_t), 1, f);
    fclose(f);
    return num;
}

void ordenar_portadoras_por_sombra(char** portadoras, int k) {
    for (int i = 0; i < k - 1; i++) {
        for (int j = i + 1; j < k; j++) {
            if (leer_numero_sombra(portadoras[i]) > leer_numero_sombra(portadoras[j])) {
                char* tmp = portadoras[i];
                portadoras[i] = portadoras[j];
                portadoras[j] = tmp;
            }
        }
    }
}


uint16_t leer_seed(const char* filename) {
    FILE* f = fopen(filename, "rb");
    if (!f) return 0;
    fseek(f, 6, SEEK_SET);
    uint16_t own_seed = 0;
    fread(&own_seed, sizeof(uint16_t), 1, f);
    fclose(f);
    return own_seed;
}

unsigned char** unrandomize_image(unsigned char** randomized_image, unsigned char** permutation_matrix, int height, int width) {
    unsigned char** original_image = malloc(height * sizeof(unsigned char*));
    for (int i = 0; i < height; i++)
        original_image[i] = malloc(width * sizeof(unsigned char));

    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            int permuted_pos = permutation_matrix[i][j];
            int orig_row = permuted_pos / width;
            int orig_col = permuted_pos % width;
            original_image[orig_row][orig_col] = randomized_image[i][j];
        }
    }
    return original_image;
}

// Explicación: Copia el encabezado, luego escribe cada fila de la imagen recuperada con padding, de abajo hacia arriba.
void guardar_bmp(const char* portadora_path, const char* filename, unsigned char** image, int width, int height) {
    FILE* portadora = fopen(portadora_path, "rb");
    FILE* salida = fopen(filename, "wb");
    if (!portadora || !salida) {
        printf("No se pudo abrir el archivo portadora o de salida.\n");
        if (portadora) fclose(portadora);
        if (salida) fclose(salida);
        return;
    }

    unsigned char header[1078];
    fread(header, 1, 1078, portadora);
    fwrite(header, 1, 1078, salida);

    int row_padded = (width + 3) & (~3);
    unsigned char* buffer = calloc(row_padded, 1);

    for (int y = height - 1; y >= 0; y--) {
        memcpy(buffer, image[y], width);
        // Asegúrate de limpiar el resto del buffer si width < row_padded
        memset(buffer + width, 0, row_padded - width);
        fwrite(buffer, 1, row_padded, salida);
    }

    free(buffer);
    fclose(portadora);
    fclose(salida);
}
