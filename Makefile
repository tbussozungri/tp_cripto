# Makefile for Visual Secret Sharing System

# Directories
BUILD_DIR = build
SRC_DIR = src
RESOURCES_DIR = resources
IMAGES_DIR = $(RESOURCES_DIR)/images
SHADOWS_DIR = $(RESOURCES_DIR)/shadows

# Java compiler and options
JAVAC = javac
JAVAC_OPTS = -d $(BUILD_DIR)
JAVA = java
JAVA_OPTS = -cp $(BUILD_DIR)

# Main class
MAIN_CLASS = VisualSSS

# Default target
.PHONY: all
all: build

# Build the project
.PHONY: build
build:
	@mkdir -p $(BUILD_DIR)
	@echo "Building Visual Secret Sharing System..."
	$(JAVAC) $(JAVAC_OPTS) $(SRC_DIR)/*.java
	@echo "Build successful! Compiled classes are in '$(BUILD_DIR)' directory."

# Create resources directories
.PHONY: dirs
dirs:
	@mkdir -p $(BUILD_DIR)
	@mkdir -p $(SHADOWS_DIR)
	@echo "Created necessary directories"

# Clean build artifacts
.PHONY: clean
clean:
	@echo "Cleaning build artifacts..."
	@rm -rf $(BUILD_DIR)
	@echo "Cleaned build directory"
	@echo "Cleaning shadow images and recovered files..."
	@rm -f $(SHADOWS_DIR)/shadow*.bmp
	@rm -f recovered_*.bmp
	@echo "Cleaned shadow images and recovered files"
	@echo "Cleaned everything"

# Clean shadows and recovered images
.PHONY: clean-shadows
clean-shadows:
	@echo "Cleaning shadow images and recovered files..."
	@rm -f $(SHADOWS_DIR)/shadow*.bmp
	@rm -f recovered_*.bmp
	@echo "Cleaned shadow images and recovered files"

# Recover directly from BMP images with k=8, n=8
.PHONY: recover-direct
recover-direct: build
	@echo "Running recovery directly from BMP images (k=8, n=8)..."
	$(JAVA) $(JAVA_OPTS) $(MAIN_CLASS) -r -secret recovered_direct.bmp -k 8 -n 8 -dir $(IMAGES_DIR)

# Show usage
.PHONY: help
help:
	@echo "Visual Secret Sharing System - Makefile"
	@echo ""
	@echo "Available targets:"
	@echo "  build          - Compile the project"
	@echo "  clean          - Remove compiled classes and shadow images"
	@echo "  clean-shadows  - Remove shadow images and recovered files"
	@echo "  dirs           - Create necessary directories"
	@echo "  recover-direct - Recover secret from BMP images (k=8, n=8)"
	@echo "  help           - Show this help message" 