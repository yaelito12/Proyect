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
            
            
            String header = entrada.readLine();
            System.out.println(header);
            
              String solicitudUsuario = entrada.readLine();
            System.out.print(solicitudUsuario + " ");
            String usuario = teclado.readLine();
            salida.println(usuario);
           
            String respuesta = teclado.readLine();
            salida.println(respuesta);
            
            
            String confirmacion = entrada.readLine();
            System.out.println(confirmacion);
            
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}