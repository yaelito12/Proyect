
package serverr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class SerVerr {

  
    public static void main(String[] args) {
      
      
        final int PUERTO = 8080;
        
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado. Esperando conexiones...");

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());
                
                
   BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
                
                  salida.println("¿Quieres registrarte (1) o iniciar sesión (2)?");
                String opcion = entrada.readLine();

                
                  if ("1".equals(opcion)) {
                    registrarUsuario(entrada, salida);
                } else if ("2".equals(opcion)) {
                    iniciarSesion(entrada, salida);
                } else {
                    salida.println("Opción inválida.");
                }

                   socket.close();

                socket.close();
            }

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
        
    }
}    
  
