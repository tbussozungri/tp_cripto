//
// Created by Admin on 01/06/2025.
//
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "utils.h"


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


    if (mode == 1){
        distribute_image(secret_image, k, n, directory);
    }
    else if (mode == 2) {
        recovery_image(secret_image, k, directory);
    } else {
        printf("Modo no definido. Use -d para distribuir o -r para recuperar.\n");
    }
    return 0;

}
