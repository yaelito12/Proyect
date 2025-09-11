package serverr;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerVerr {
  private static final int PUERTO = 1234;
    private static ExecutorService pool = Executors.newFixedThreadPool(10); // NUEVO: Pool de hilos
    
    public static void main(String[] args) {
    
        
       try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando conexiones...");

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());
                
                // NUEVO: Crear hilo para manejar cada cliente
                pool.submit(new ManejadorCliente(socket));
            }

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        } finally {
            pool.shutdown(); // NUEVO: Cerrar pool de hilos
        }
    }
    }
}
