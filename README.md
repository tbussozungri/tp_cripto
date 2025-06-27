# Visual Secret Sharing System

Trabajo Práctico Criptografía y Seguridad 2025 1Q

## Grupo 5

- Axel Castro Benza
- Lautaro Farias
- Thomás Busso

## Generalidades

El programa debe recibir como parámetros obligatorios:

- `-d` o bien `-r`
- `-secret imagen`
- `-k número`

Y los siguientes parámetros opcionales:

- `-n número`
- `-dir directorio`

## Significado de cada uno de los parámetros obligatorios

- **`-d`**: indica que se va a distribuir una imagen secreta en otras imágenes.
- **`-r`**: indica que se va a recuperar una imagen secreta a partir de otras imágenes.
- **`-secret imagen`**: El nombre imagen corresponde al nombre de un archivo de extensión `.bmp`. En el caso de que se haya elegido la opción `-d` éste archivo debe existir ya que es la imagen a ocultar. Si se eligió la opción `-r` éste archivo será el archivo de salida, con la imagen secreta revelada al finalizar el programa.
- **`-k número`**: El número corresponde a la cantidad mínima de sombras necesarias para recuperar el secreto en un esquema (k, n).

## Significado de cada uno de los parámetros opcionales

- **`-n número`**: El número corresponde a la cantidad total de sombras en las que se distribuirá el secreto en un esquema (k, n). Sólo puede usarse en el caso de que se haya elegido la opción `-d`. Si no se usa, el programa elegirá como valor de n la cantidad total de imágenes del directorio.
- **`-dir directorio`**: El directorio donde se encuentran las imágenes en las que se distribuirá el secreto (en el caso de que se haya elegido la opción `-d`), o donde están las imágenes que contienen oculto el secreto (en el caso de que se haya elegido la opción `-r`). Si no se usa, el programa buscará las imágenes en el directorio actual.

## Ejemplos

### Distribución (Ocultar imagen secreta)

```bash
# Ocultar la imagen "clave.bmp", en un esquema (2, 4) buscando imágenes en el directorio "varias"
java -cp build VisualSSS -d -secret clave.bmp -k 2 -n 4 -dir varias

# Ocultar la imagen "clave.bmp", en un esquema que use k = 3 buscando imágenes en el directorio actual
java -cp build VisualSSS -d -secret clave.bmp -k 3
```

### Recuperación (Revelar imagen secreta)

```bash
# Recuperar la imagen "secreta.bmp", en un esquema (2, 4) buscando imágenes en el directorio "varias"
java -cp build VisualSSS -r -secret secreta.bmp -k 2 -n 4 -dir varias

# Recuperar la imagen "secreta.bmp", en un esquema que use k = 3 buscando imágenes en el directorio actual
java -cp build VisualSSS -r -secret secreta.bmp -k 3
```

## Uso con Makefile

Para facilitar el uso del proyecto, se incluye un Makefile con comandos predefinidos:

```bash
# Compilar el proyecto
make build

# Limpiar archivos generados (build y sombras)
make clean

# Limpiar solo sombras y archivos recuperados
make clean-shadows

# Crear directorios necesarios
make dirs

# Recuperar secreto directamente desde imágenes BMP (k=8, n=8)
make recover-direct

# Ver todos los comandos disponibles
make help
```

## Estructura del Proyecto

```text
tp_cripto/
├── build/                    # Archivos compilados (.class)
├── src/                      # Código fuente Java
├── resources/
│   ├── images/              # Imágenes de prueba
│   └── shadows/             # Sombras generadas
├── Makefile                 # Scripts de construcción
└── README.md               # Este archivo
```

## Requisitos

- Java 8 o superior
- Make (opcional, para usar el Makefile)

## Notas

- Las imágenes deben estar en formato BMP
- El valor de k debe estar entre 2 y 10
- El valor de n debe ser mayor o igual a k
- Las imágenes portadoras deben ser al menos del mismo tamaño que la imagen secreta
