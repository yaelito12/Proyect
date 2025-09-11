package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    public static void main(String[] args) throws IOException {
        final String HOST = "localhost";
        final int PUERTO = 1234;

        try (
            Socket socket = new Socket(HOST, PUERTO);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Conectado al servidor " + HOST + ":" + PUERTO);
            
            // NUEVO: Leer mensajes del servidor
            String mensajeServidor1 = entrada.readLine();
            System.out.println(mensajeServidor1);
            
            String mensajeServidor2 = entrada.readLine();
            System.out.print(mensajeServidor2 + " ");
            
            // Leer input del usuario y enviarlo
            String respuesta = teclado.readLine();
            salida.println(respuesta);
            
            // NUEVO: Leer confirmación del servidor
            String confirmacion = entrada.readLine();
            System.out.println(confirmacion);
            
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}