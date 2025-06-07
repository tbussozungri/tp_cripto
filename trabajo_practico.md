1 Objetivos
▪ Introducirlos en el campo de la criptografía visual y sus aplicaciones, a través de la
implementación de un algoritmo de Secreto Compartido en Imágenes.
▪ Introducirlos en el campo de la esteganografía y sus aplicaciones.
▪ Implementar y analizar un algoritmo descripto en un documento científico.
2 Consigna
Realizar un programa en lenguaje C o en Java que implemente el algoritmo de Secreto
Compartido en Imágenes descripto en el documento “An Efficient Secret Image Sharing Scheme” cuyos
autores son Luang-Shyr Wu y Tsung-Ming Lo de la Universidad de Tecnología China de Taiwan.
El programa permitirá:
1) Distribuir una imagen secreta de extensión “.bmp” en otras imágenes también de extensión
“.bmp” que serán las sombras en un esquema (k, n) de secreto compartido.
2) Recuperar una imagen secreta de extensión “.bmp” a partir de k imágenes, también de
extensión “.bmp”

4 Detalles del sistema
4.1 Generalidades
El programa debe recibir como parámetros obligatorios:1
➢ -d o bien –r
➢ -secret imagen
➢ -k número
Y los siguientes parámetros opcionales:
➢ <-n número >
➢ <-dir directorio>
Significado de cada uno de los parámetros obligatorios:
➢ -d: indica que se va a distribuir una imagen secreta en otras imágenes.
➢ –r: indica que se va a recuperar una imagen secreta a partir de otras imágenes.
➢ -secret imagen: El nombre imagen corresponde al nombre de un archivo de extensión
.bmp. En el caso de que se haya elegido la opción (-d) éste archivo debe existir ya que es la
imagen a ocultar. Si se eligió la opción (-r) éste archivo será el archivo de salida, con la
imagen secreta revelada al finalizar el programa.
➢ -k número: El número corresponde a la cantidad mínima de sombras necesarias para
recuperar el secreto en un esquema (k, n).
Significado de cada uno de los parámetros opcionales:
➢ <-n número >: El número corresponde a la cantidad total de sombras en las que se
distribuirá el secreto en un esquema (k, n). Sólo puede usarse en el caso de que se haya
elegido la opción (-d). Si no se usa, el programa elegirá como valor de n la cantidad total de
imágenes del directorio.
➢ <-dir directorio> El directorio donde se encuentran las imágenes en las que se distribuirá
el secreto (en el caso de que se haya elegido la opción (-d)), o donde están las imágenes que
contienen oculto el secreto ( en el caso de que se haya elegido la opción (-r)). Si no se usa, el
programa buscará las imágenes en el directorio actual.
Ejemplos:
➢ Ocultar la imagen “clave.bmp”, en un esquema (2, 4) buscando imágenes en el directorio
“varias”
$visualSSS –d –secret clave.bmp –k 2 –n 4 –dir varias
➢ Ocultar la imagen “clave.bmp”, en un esquema que use k = 3 buscando imágenes en el
directorio actual.
$visualSSS –d –secret clave.bmp –k 3
➢ Recuperar la imagen “secreta.bmp”, en un esquema (2, 4) buscando imágenes en el
directorio “varias”
$visualSSS –r –secret secreta.bmp –k 2 –n 4 –dir varias
➢ Recuperar la imagen “secreta.bmp”, en un esquema que use k = 3 buscando imágenes en
el directorio actual.
$visualSSS –r –secret secreta.bmp –k 3

4.2 Algoritmo de Distribución
En la distribución hay que tener en cuenta los siguientes aspectos:

4.2.1 Valor de k
El valor de k debe ser mayor o igual que 2 y menor o igual que 10. (Y también menor o igual que
n).
4.2.2 Valor de n
El valor de n será de, mínimo 2.
4.2.3 Imagen secreta
La imagen secreta debe ser de formato BMP, de 8 bits por píxel. (1 byte = 1 pixel)
El formato BMP es un formato de archivos binario de imagen bastante simple. Consta de dos
partes:

i. encabezado → de 54 bytes
ii. Cuerpo → de tamaño variable.

El encabezado contiene información acerca del archivo: tamaño de archivo, ancho de imagen, alto
de imagen, bits por píxel, si está comprimido, etc.

IMPORTANTE: Leer bien el valor que indica en qué offset empieza la matriz de píxeles, ya que
puede comenzar inmediatamente después de los 54 bytes del encabezado, o bien empezar más adelante.

En el cuerpo del archivo bmp, están los bits que definen la imagen propiamente dicha. La imagen
se lee de abajo hacia arriba y de izquierda a derecha. Si la imagen es de 8 bits por píxel, es una imagen en
tonos de grises: el píxel de valor 0x00 es de color negro y el píxel 0xFF es de color blanco.

Tener cuidado al elegir la imagen: revisarla con algún editor hexadecimal para asegurarse que no
tenga información extra al final (metadata) y que se ajuste al formato que se pide.

4.2.4 Tabla de Permutación
La tabla de permutación R que usa el algoritmo propuesto por el paper, se generará a partir de
una semilla ocultada en el archivo bmp (en la sección siguiente se indica dónde).
Para una imagen de, por ejemplo, m píxeles, se generará una secuencia de m valores en el rango
[0,255]
4.2.4.1 Implementación en lenguaje C.
Ver documento anexo Tabla de Permutación de Implementación.

4.2.5 Imágenes Portadoras y ocultamiento por esteganografía
Las imágenes portadoras deben ser de formato BMP, de 8 bits por píxel.
Esquema (8,n)
En el caso de que el valor de k sea igual a 8, las imágenes deberán tener igual tamaño (ancho y
alto) que la imagen secreta. Si no tienen n imágenes que cumplan esa condición, se muestra mensaje de
error y no se realiza nada.
El ocultamiento de la información se hará mediante el método de LSB replacement (Least
Significant Bit – Reemplazo del bit menos significativo). Esto se hará en el orden en que se tengan los
bytes a partir del primer píxel (tener en cuenta el offset) y considerando los bits de mayor a menor.
Así, suponiendo que el primer valor a ocultar fuera el 0xD1 (1101 0001)
Y suponiendo que el primer píxel comienza en el offset 1078:
          Valor actual Ultimos 4 bits Valor después Ultimos 4 bits
Byte 1078 ED 1101 ED 1101
Byte 1079 A4 0100 A5 0101
Byte 1080 45 0101 44 0100
Byte 1081 36 0110 37 0111
Byte 1082 3A 1010 3A 1010
Byte 1083 3A 1010 3A 1010
Byte 1084 3A 1010 3A 1010
Byte 1085 39 1001 39 1001

La semilla de generación de números aleatorios será un número entero de 2 bytes y debe ocultarse
en los bytes 6 y 7 del archivo bmp (sección de bytes reservados).

Así, si el número es 641 (0000 0010 1000 0001) se guardará

       Valor actual Valor después
Byte 6 00 81
Byte 7 00 02

El número de orden correspondiente a la sombra (es decir, si la sombra es la número 1, 2, 3, …k)
deberá ocultarse en los bytes 8 y 9 del archivo bmp (sección de bytes reservados).
Así, si la sombra es la tercera (0000 0000 0000 0011) se guardará

       Valor actual Valor después
Byte 8 00 03
Byte 9 00 00

Esquema (k,n) con k distinto de 8.
En el caso de que el valor de k sea distinto de 8, queda a criterio del grupo definir
(justificadamente) el tamaño de las imágenes portadoras y el método de ocultamiento.

4.3 Algoritmo de Recuperación
4.3.1 Valor de k
El valor de k debe ser mayor o igual que 2 y menor o igual que 10.
4.3.2 Imagen Secreta
Esquema (8,n)
La imagen secreta se tendrá que generar del mismo tamaño que las imágenes portadoras. Para
armar su encabezado, se puede tomar el encabezado de cualquiera de las imágenes portadoras.
Esquema (k,n) con k distinto de 8
Cada grupo deberá determinar cómo se regenera la imagen secreta (en correspondencia a lo
establecido en el item 4.2.5)
4.3.3 Imágenes portadoras
Esquema (8,n)
Las imágenes portadoras deben ser de formato BMP, de 8 bits por píxel y todas del mismo tamaño
(ancho y alto) entre sí. Si no se tienen 8 imágenes que cumplan esta condición, se muestra mensaje de
error y no se realiza nada.
Esquema (k,n) con k distinto de 8
Cada grupo deberá determinar cómo validar las imágenes portadoras (en correspondencia a lo
establecido en el item 4.2.5)

4.3.4 Permutación
Se verifica de manera inversa a la distribución.
4.3.5 Recuperación del secreto.
Se recomienda usar el método de Gauss o el de Lagrange reducido (ver “Clase de Shamir.ppt”) y
no el de determinantes para resolver el sistema de ecuaciones. Tener en cuenta que se está trabajando en
aritmética entera módulo 257.