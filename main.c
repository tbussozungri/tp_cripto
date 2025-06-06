//
// Created by Admin on 01/06/2025.
//
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "utils.h"
#include "secret_sharing.h"

#define SEED 43

void print_usage(const char* program_name) {
    printf("Usage: %s [-d|-r] -secret <image> -k <number> [-n <number>] [-dir <directory>]\n", program_name);
    printf("  -d: distribute secret image\n");
    printf("  -r: recover secret image\n");
    printf("  -secret: input/output image file (.bmp)\n");
    printf("  -k: minimum number of shares needed (2-10)\n");
    printf("  -n: total number of shares (optional)\n");
    printf("  -dir: directory for input/output files (optional, default: current directory)\n");
}

int main(int argc, char* argv[]) {
    if (argc < 4) {
        print_usage(argv[0]);
        return 1;
    }

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

    // Validate required parameters
    if (mode == 0 || secret_image == NULL || k < 2 || k > 10) {
        print_usage(argv[0]);
        return 1;
    }

    // Execute the appropriate mode
    if (mode == 1) {
        distribute_secret(secret_image, k, n, directory);
    } else {
        recover_secret(secret_image, k, n, directory);
    }

    return 0;
}
