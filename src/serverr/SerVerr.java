package serverr;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerVerr {
    public static void main(String[] args) {
    private static final int PUERTO = 1234;
    private static ExecutorService pool = Executors.newFixedThreadPool(10);
        
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando conexiones...");

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());

             
                try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {
                 
                    salida.println("¡Hola! Estas conectado al servidor");
                    salida.println("Escribe algo:");
                    
                
                    String mensaje = entrada.readLine();
                    System.out.println("Cliente escribió: " + mensaje);
                    
                   
                    salida.println("Servidor recibió: " + mensaje);
                    
                } catch (IOException e) {
                    System.out.println("Error comunicándose con cliente: " + e.getMessage());
                }
                
                socket.close();
                System.out.println("Cliente desconectado");
            }

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }
}
