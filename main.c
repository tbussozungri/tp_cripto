//
// Created by Admin on 01/06/2025.
//
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <dirent.h>
#include "include/secret_sharing.h"

#define MAX_CARRIERS 10

void print_usage(const char* program_name) {
    printf("Usage: %s [-d|-r] -secret <image> -k <number> [-n <number>] [-dir <directory>] [-carrier_dir <directory>]\n", program_name);
    printf("  -d: distribute secret image\n");
    printf("  -r: recover secret image\n");
    printf("  -secret: input/output image file (.bmp)\n");
    printf("  -k: minimum number of shares needed (2-10)\n");
    printf("  -n: total number of shares (optional)\n");
    printf("  -dir: directory for input/output files (optional, default: current directory)\n");
    printf("  -carrier_dir: directory containing carrier images (optional, default: current directory)\n");
}

// Function to get carrier images from directory
int get_carrier_images_from_dir(const char* dir_path, const char* carrier_images[], int max_carriers) {
    DIR* dir = opendir(dir_path);
    if (!dir) {
        printf("Error: Could not open carrier directory %s\n", dir_path);
        return 0;
    }

    int count = 0;
    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL && count < max_carriers) {
        // Check if file is a .bmp file
        char* ext = strrchr(entry->d_name, '.');
        if (ext && strcmp(ext, ".bmp") == 0) {
            char full_path[512];
            snprintf(full_path, sizeof(full_path), "%s/%s", dir_path, entry->d_name);
            carrier_images[count] = strdup(full_path);
            count++;
        }
    }
    closedir(dir);
    return count;
}

int main(int argc, char* argv[]) {
    if (argc < 4) {
        print_usage(argv[0]);
        return 1;
    }

    const char* secret_image = NULL;
    int k = 0;
    int n = 0;
    const char* directory = ".";
    const char* carrier_dir = ".";
    int mode = 0; // 0: undefined, 1: distribute, 2: recover
    const char* carrier_images[MAX_CARRIERS] = {NULL};
    int carrier_count = 0;

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
        } else if (strcmp(argv[i], "-carrier_dir") == 0 && i + 1 < argc) {
            carrier_dir = argv[++i];
        }
    }

    // Validate required parameters
    if (mode == 0 || secret_image == NULL || k < 2 || k > 10) {
        print_usage(argv[0]);
        return 1;
    }

    // Get carrier images from directory
    carrier_count = get_carrier_images_from_dir(carrier_dir, carrier_images, MAX_CARRIERS);
    if (carrier_count == 0) {
        printf("Error: No carrier images found in directory %s\n", carrier_dir);
        return 1;
    }

    // If n is not specified, use the number of carrier images found
    if (n == 0) {
        n = carrier_count;
    }

    // Validate k and n relationship
    if (k > n) {
        printf("Error: k (%d) cannot be greater than n (%d)\n", k, n);
        return 1;
    }

    // Validate that we have enough carrier images
    if (mode == 1 && carrier_count < n) {
        printf("Error: Not enough carrier images in directory (need %d, found %d)\n", n, carrier_count);
        return 1;
    }

    // Execute the appropriate mode
    if (mode == 1) {
        distribute_secret(secret_image, carrier_images, k, n, directory);
    } else {
        // For recovery, if no carrier images specified, use the ones in the directory
        if (carrier_count == 0) {
            for (int i = 0; i < k; i++) {
                char shadow_name[256];
                snprintf(shadow_name, sizeof(shadow_name), "%s/shadow_%d.bmp", directory, i + 1);
                carrier_images[i] = strdup(shadow_name);
            }
            carrier_count = k;
        }
        recover_secret(carrier_images, secret_image, k, n, directory);
    }

    // Free allocated memory
    for (int i = 0; i < carrier_count; i++) {
        free((void*)carrier_images[i]);
    }

    return 0;
}
