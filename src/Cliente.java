import java.io.*;
import java.net.*;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            // Hilo para mostrar mensajes del servidor
            new Thread(() -> {
                try {
                    String linea;
                    while ((linea=entrada.readLine())!=null) {
                        System.out.println(linea);
                    }
                } catch(IOException e) {
                    System.out.println("Desconectado del servidor");
                }
            }).start();

            String lineaTeclado;
            while ((lineaTeclado = teclado.readLine()) != null) {
                salida.println(lineaTeclado.trim());
                if(lineaTeclado.equals("3")) break; // Salir
            }

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}