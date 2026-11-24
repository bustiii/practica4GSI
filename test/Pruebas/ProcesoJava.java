package Pruebas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ayudante de las pruebas de la Practica 04: lanza una clase ejecutable en
 * otra JVM (con el mismo classpath), le escribe por la entrada estandar y
 * espera a que aparezcan textos en su salida.
 */
final class ProcesoJava {

    private final String nombre;
    private final Process proceso;
    private final StringBuilder salida = new StringBuilder();

    private ProcesoJava(String nombre, Process proceso) {
        this.nombre = nombre;
        this.proceso = proceso;
        Thread lector = new Thread(() -> {
            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                int c;
                while ((c = out.read()) != -1) {
                    synchronized (salida) {
                        salida.append((char) c);
                        salida.notifyAll();
                    }
                }
            } catch (IOException e) {
                // El proceso ha terminado
            }
        });
        lector.setDaemon(true);
        lector.start();
    }

    /**
     * Lanza una clase en otra JVM.
     *
     * @param clase clase con main
     * @param propiedades propiedades del sistema ("clave=valor") para la JVM
     * @param args argumentos del programa
     * @return el proceso lanzado
     * @throws IOException si no se puede lanzar
     */
    static ProcesoJava lanzar(Class<?> clase, List<String> propiedades, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String p : propiedades) {
            cmd.add("-D" + p);
        }
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        cmd.add(clase.getName());
        cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        return new ProcesoJava(clase.getSimpleName(), pb.start());
    }

    /**
     * Escribe una linea en la entrada estandar del proceso.
     *
     * @param linea texto (sin salto de linea)
     * @throws IOException si el proceso ya no acepta entrada
     */
    void escribir(String linea) throws IOException {
        OutputStream in = proceso.getOutputStream();
        in.write((linea + "\n").getBytes(StandardCharsets.UTF_8));
        in.flush();
    }

    /**
     * Espera a que un texto aparezca al menos {@code veces} veces en la
     * salida.
     *
     * @param texto texto buscado
     * @param veces apariciones necesarias
     * @param segundos tiempo maximo de espera
     * @return true si aparecio a tiempo
     * @throws InterruptedException si se interrumpe la espera
     */
    boolean esperarTexto(String texto, int veces, int segundos) throws InterruptedException {
        long limite = System.currentTimeMillis() + segundos * 1000L;
        synchronized (salida) {
            while (contar(texto) < veces) {
                long resto = limite - System.currentTimeMillis();
                if (resto <= 0) {
                    return false;
                }
                salida.wait(Math.min(resto, 200));
            }
            return true;
        }
    }

    /**
     * @param texto texto buscado
     * @return cuantas veces aparece en la salida hasta ahora
     */
    int contar(String texto) {
        synchronized (salida) {
            int n = 0;
            for (int i = salida.indexOf(texto); i >= 0; i = salida.indexOf(texto, i + texto.length())) {
                n++;
            }
            return n;
        }
    }

    /**
     * Espera a que el proceso termine.
     *
     * @param segundos tiempo maximo de espera
     * @return su codigo de salida, o -1 si no termino a tiempo
     * @throws InterruptedException si se interrumpe la espera
     */
    int esperarFin(int segundos) throws InterruptedException {
        return proceso.waitFor(segundos, TimeUnit.SECONDS) ? proceso.exitValue() : -1;
    }

    /**
     * Termina el proceso si sigue vivo.
     */
    void destruir() {
        if (proceso.isAlive()) {
            proceso.destroyForcibly();
        }
    }

    /**
     * @return la salida del proceso hasta ahora, sangrada y con su nombre
     */
    String salida() {
        synchronized (salida) {
            return "  --- salida de " + nombre + " ---\n  " + salida.toString().replace("\n", "\n  ");
        }
    }

    /**
     * @return un puerto TCP libre en esta maquina
     * @throws IOException si no se puede abrir un socket
     */
    static int puertoLibre() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }
}
