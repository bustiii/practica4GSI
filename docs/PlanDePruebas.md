# Práctica 4 · Plan de pruebas entre máquinas y guion de la exposición

Grupo 03 · Gestión de Sistemas de Información · UPNA, curso 2026/2027

Este documento es el guion para probar el acceso remoto con RMI usando máquinas diferentes (Ejercicio 3) y para preparar la exposición del 25 de noviembre. Las pruebas en una sola máquina ya están automatizadas en `test/Pruebas` (ver el apartado 5).

## 1. Preparación

### Máquinas y papeles

| Papel | Máquina | Programa |
|---|---|---|
| Servidor | A | `GSILabs.connect.BusinessServer` |
| Cliente | B | `GSILabs.connect.ClientHub` |
| Administrador | C (o la misma B, en otra terminal) | `GSILabs.connect.AdminHub` |

Con dos máquinas basta: B ejecuta los dos hubs. Lo importante es que el servidor esté en una máquina distinta de los clientes.

### Requisitos

1. Todas las máquinas en la misma red (la del laboratorio o un punto de acceso del móvil si la del laboratorio bloquea el tráfico entre equipos).
2. JDK 21 en todas (`java -version`): el código está compilado para Java 21.
3. La misma versión del proyecto en todas (las clases de `BModel` llevan `serialVersionUID`; si una máquina tiene otra versión, la deserialización falla).
4. En la máquina A, el puerto 1099/TCP abierto en el cortafuegos. El objeto remoto se exporta en el mismo puerto que el registro, así que no hace falta abrir ningún otro.
   - Linux con ufw: `sudo ufw allow 1099/tcp` (al terminar, `sudo ufw delete allow 1099/tcp`).
   - Windows: aceptar el aviso del cortafuegos la primera vez que se lanza el servidor, o crear una regla de entrada para el puerto 1099/TCP.
5. Anotar la IP de la máquina A en la red común: `ip -4 addr` (Linux) o `ipconfig` (Windows).

### Compilación y ejecución

**Con NetBeans:** *Clean and Build* genera `dist/practica4GSI.jar` y copia jOpenDocument en `dist/lib`. La clase principal es `BusinessServer`, así que *Run* lanza el servidor.

**Desde la terminal**, en la carpeta del proyecto:

```bash
# Servidor (máquina A)
java -jar dist/practica4GSI.jar --host IP_DE_A

# Clientes (máquina B o C), en Linux
java -cp "dist/practica4GSI.jar:dist/lib/*" GSILabs.connect.ClientHub
java -cp "dist/practica4GSI.jar:dist/lib/*" GSILabs.connect.AdminHub
```

En Windows el separador del classpath es `;`: `java -cp "dist\practica4GSI.jar;dist\lib\*" GSILabs.connect.ClientHub`.

**Sin NetBeans** (compilando a mano):

```bash
mkdir -p build/classes
javac -encoding UTF-8 -cp jOpenDocument-1.5.jar -d build/classes $(find src -name '*.java')
java -cp "jOpenDocument-1.5.jar:build/classes" GSILabs.connect.BusinessServer --host IP_DE_A
```

**Opciones útiles:**

- Servidor: `--host IP` (dirección que llevan los stubs), `--puerto N` (1099 por defecto) y `--xml fichero` (cargar datos de la Práctica 3 en vez de los de ejemplo). En su consola: `estado` (locales, número de reseñas y media), `ayuda` y `salir`.
- Clientes: al pedir dirección, puerto y tag, Enter acepta el valor entre corchetes. `-Dgsi.reintento=N` cambia los segundos entre reintentos (5 por defecto). Ctrl+C termina el cliente.

### Datos de ejemplo

El servidor arranca con los datos de `DatosIniciales`. Con ellos, la primera ronda de `ClientHub` da: `mejorBar("Pamplona")` = Txoko Berri; `mejoresRestaurantes("Pamplona", 5)` = La Cocina de Marta, Asador Ribera, Casa Pepe, Restaurante Ciudadela y una posición vacía; `insertaReview` true y después false (repetida); `quitaReview` true; y `quitaReview` de una reseña contestada, false.

`AdminHub` modifica los datos. Su primera ronda da los resultados que indica cada paso («con los datos iniciales: …»). Para volver a empezar basta con escribir `salir` en el servidor y lanzarlo otra vez.

## 2. Pruebas

En cada prueba, guardad una captura de la terminal correspondiente con el nombre indicado, por ejemplo en `docs/capturas/`. Anotad en la columna «Resultado» si se cumple lo esperado y cualquier observación.

### P1. Conexión de ClientHub desde otra máquina

- **Pasos:** lanzar el servidor en A con `--host IP_DE_A`. En B, lanzar `ClientHub` e indicar `IP_DE_A`, `1099` y `ClientGateway`. Responder `n` al final.
- **Esperado:** «Conectado a 'ClientGateway' en IP_DE_A:1099» y las nueve operaciones con los resultados del apartado 1.
- **Captura:** `P1-clienthub.png`.

### P2. Conexión de AdminHub desde otra máquina

- **Pasos:** con el servidor recién arrancado, lanzar `AdminHub` en C (o en B) con `IP_DE_A`, `1099` y `AdminGateway`. Después, en A, escribir `estado`.
- **Esperado:** las diez operaciones con los resultados indicados («con los datos iniciales: …»). En A, `estado` muestra 9 locales: ha desaparecido Pub Luna y Txoko Berri tiene 0 reseñas.
- **Captura:** `P2-adminhub.png` y `P2-estado-servidor.png`.

### P3. Los cambios del administrador los ve el cliente

- **Pasos:** justo después de P2, lanzar `ClientHub` en B.
- **Esperado:** `mejorBar("Pamplona")` ahora es Bar El Rincon (media 3,5 con la reseña falsa) y el paso 9 se omite porque el administrador borró la reseña contestada de ana.
- **Captura:** `P3-cambios-visibles.png`.

### P4. Servidor no disponible al arrancar el cliente

- **Pasos:** con el servidor parado, lanzar `ClientHub` en B. Esperar dos o tres avisos y lanzar el servidor en A.
- **Esperado:** el cliente muestra «AVISO: no se puede contactar con el servidor…» cada 5 segundos, sin terminar; cuando el servidor arranca, se conecta y ejecuta las operaciones.
- **Captura:** `P4-servidor-tardio.png`.

### P5. Caída del servidor durante la ejecución

- **Pasos:** con `ClientHub` conectado y esperando en «Repetir las operaciones?», detener el servidor (`salir` o Ctrl+C). Responder `s` en el cliente. Esperar varios avisos y volver a lanzar el servidor.
- **Esperado:** «AVISO: fallo la operacion 'getLocal'…» seguido de reintentos; cuando el servidor vuelve, el cliente obtiene un stub nuevo y completa la ronda.
- **Variante:** repetir con `AdminHub`.
- **Captura:** `P5-caida.png`.

### P6. Tag incorrecto

- **Pasos:** lanzar `ClientHub` y escribir como tag `Gateway`. Después pulsar Enter.
- **Esperado:** «El servidor no tiene ningun objeto publicado como 'Gateway'», la lista de tags publicados (`[ClientGateway, AdminGateway]`) y una nueva petición del tag; con Enter se conecta.
- **Captura:** `P6-tag.png`.

### P7. Puerto incorrecto

- **Pasos:** lanzar `ClientHub` con el puerto `1100`.
- **Esperado:** avisos de reintento con «Connection refused to host: IP_DE_A» (nadie escucha en ese puerto; la causa final puede salir traducida, por ejemplo «Conexión rehusada»). Terminar con Ctrl+C y volver a lanzarlo con 1099.
- **Captura:** `P7-puerto.png`.

### P8. Dirección inalcanzable

- **Pasos:** lanzar `ClientHub` con una IP de la red que no corresponda a ningún equipo.
- **Esperado:** cada intento termina en unos 5 segundos con «Exception creating connection to: … / SocketTimeoutException: Connect timed out» y se reintenta; el cliente no se queda bloqueado.
- **Captura:** `P8-ip.png`.

### P9. Servidor con una dirección no alcanzable en el stub

- **Pasos:** lanzar el servidor en A con `--host 127.0.0.1` y conectar `ClientHub` desde B.
- **Esperado:** el `lookup` funciona (el registro sí es accesible) y aparece «Conectado», pero la primera operación falla con «Connection refused to host: 127.0.0.1» y el cliente queda reintentando: el stub indica al cliente que llame a su propia máquina. Sirve para explicar por qué hay que fijar `java.rmi.server.hostname`. Después, relanzar el servidor con `--host IP_DE_A`.
- **Captura:** `P9-hostname.png`.

### P10. Cortafuegos cerrado

- **Pasos:** cerrar el puerto 1099 en A (`sudo ufw delete allow 1099/tcp` con ufw activo) e intentar conectar desde B. Volver a abrirlo.
- **Esperado:** avisos de reintento («Connect timed out» o «Connection refused», según el cortafuegos); al abrir el puerto, el cliente se conecta solo.
- **Captura:** `P10-cortafuegos.png`.

### P11. Varios clientes a la vez

- **Pasos:** lanzar dos `ClientHub` (en B y en C) y un `AdminHub` casi a la vez.
- **Esperado:** todos terminan sin avisos. Los métodos remotos son `synchronized`, así que las llamadas concurrentes se atienden de una en una.
- **Captura:** `P11-concurrencia.png`.

### P12. Datos cargados desde XML (opcional)

- **Pasos:** lanzar el servidor con `--xml fichero.xml` (por ejemplo, un fichero guardado con `saveToXML` en la Práctica 3) y conectar `AdminHub`.
- **Esperado:** el paso 1 lista los locales del fichero. Las operaciones que dependen de los nombres de `DatosIniciales` muestran null o false.
- **Captura:** `P12-xml.png`.

### Tabla de resultados

| Prueba | Fecha | Máquinas (SO e IP) | Resultado | Observaciones |
|---|---|---|---|---|
| P1 | | | | |
| P2 | | | | |
| P3 | | | | |
| P4 | | | | |
| P5 | | | | |
| P6 | | | | |
| P7 | | | | |
| P8 | | | | |
| P9 | | | | |
| P10 | | | | |
| P11 | | | | |
| P12 | | | | |

## 3. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| El cliente se conecta pero las operaciones fallan con «Connection refused to host: 127.0.0.1» o con una IP 172.x | El stub lleva una dirección que el cliente no alcanza | Lanzar el servidor con `--host IP_DE_A` |
| «Connect timed out» desde otra máquina, pero en local funciona | Cortafuegos de A o red que aísla los equipos | Abrir el puerto 1099/TCP o usar otra red |
| El servidor no arranca: «La dirección ya se está usando» | Hay otro servidor o un `rmiregistry` en el puerto | Cerrarlo o usar `--puerto` |
| `InvalidClassException` o `ClassNotFoundException` | Versiones distintas del proyecto en cada máquina | Usar el mismo jar en todas |
| `UnsupportedClassVersionError` | Una máquina tiene un JDK anterior a 21 | Instalar JDK 21 |

## 4. Guion de la exposición

Propuesta de unos 10 minutos, repartida entre los miembros del grupo:

1. **Objetivo (1 min).** Dos accesos remotos al sistema LeisureOffice: clientes (consultar locales y publicar o retirar reseñas) y administradores (consultar y borrar locales y reseñas).
2. **Ejercicio 1 (2 min).** Opción b): `PublicBusinessSystem extends BusinessSystem` e implementa las tres interfaces. Motivos: separación de responsabilidades, reutilización sin tocar la lógica de las prácticas anteriores y exposición explícita. El conflicto de `eliminaReview` (`boolean` en `LeisureOffice` frente a `Boolean` en `AdminGateway`) y cómo se resolvió.
3. **Cambios en BModel (1 min).** Serializable y `serialVersionUID`; `readObject` para que los generadores de identificadores no repitan ids; por qué no hay ningún atributo `transient`.
4. **Ejercicio 2 (2 min).** `BusinessServer`: los cuatro pasos del enunciado (instancia poblada, `exportObject`, `createRegistry`, `rebind` con los dos tags). Objeto y registro en el mismo puerto; `java.rmi.server.hostname` y la detección de la IP descartando interfaces virtuales; métodos `synchronized`. Nota: el mismo stub se publica con los dos tags, así que la separación es por interfaz, no de seguridad.
5. **Ejercicio 3 (2 min).** `RemoteConnector` común: petición de datos, `getRegistry` y `lookup`, reintentos ante `RemoteException`, nueva petición del tag ante `NotBoundException` y tiempos máximos de conexión. `ClientHub` y `AdminHub` con operaciones predefinidas.
6. **Demostración en vivo (2 min).** P1, P2 y P3 seguidas, y después P5 (caída y recuperación del servidor).
7. **Pruebas y conclusiones (1 min).** Pruebas automáticas y pruebas entre máquinas (tabla del apartado 2). Lo aprendido: transparencia de RMI «hasta cierto punto» (paso por valor, excepciones remotas, configuración de red).

## 5. Pruebas automáticas (una sola máquina)

Todas están en `test/Pruebas` y se ejecutan como programas (`main`), igual que las de prácticas anteriores:

| Clase | Qué prueba |
|---|---|
| `P04SerializacionTester` | Serialización de todas las clases de BModel e identificadores tras deserializar |
| `P04ConsultasTester` | `getLocal`, `getLocals`, `mejorBar`, `mejoresRestaurantes` |
| `P04ReviewsTester` | `insertaReview`, `quitaReview`, `insertaReviewFalsa` |
| `P04AdminTester` | `eliminaReview`, `eliminaReviewsDeLocal`, `eliminaReviewsDeUsuario`, `eliminaLocal` |
| `P04RMITester` | Servidor real en otra JVM usado por RMI, y caída del servidor |
| `P04ClientHubTester` | `ClientHub`: tag incorrecto, servidor que arranca tarde y caída entre rondas |
| `P04AdminHubTester` | `AdminHub`, cambios visibles desde `ClientHub` y dos clientes a la vez |

Además siguen pasando `GSILabs.BTesting.P01.Tester` y `Pruebas.P03Tester`.
