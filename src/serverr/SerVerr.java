package serverr;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerVerr {
    private static final int PUERTO = 1234;
    private static ExecutorService pool = Executors.newFixedThreadPool(10); 
    
    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando conexiones...");

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());
                
               
                pool.submit(new ManejadorCliente(socket));
            }

        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        } finally {
            pool.shutdown(); 
        }
    }
    
  
    static class ManejadorCliente implements Runnable {
        private Socket socket;
        
        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {
                
                
                String clienteIP = socket.getInetAddress().toString();
                System.out.println("Hilo iniciado para cliente: " + clienteIP);
                
                
                salida.println("¡Hola! Estas conectado al servidor (Hilo: " + Thread.currentThread().getName() + ")");
                salida.println("Escribe algo:");
                
                 String mensaje = entrada.readLine();
                System.out.println("Cliente " + clienteIP + " escribió: " + mensaje);
                
                // Responder al cliente
                salida.println("Servidor recibió: " + mensaje);
       
            } catch (IOException e) {
                     System.err.println("Error manejando cliente: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                  System.out.println("Cliente " + socket.getInetAddress() + " desconectado");   
                } catch (IOException e) {
                    System.err.println("Error cerrando socket: " + e.getMessage());
                }
            }
        }
        
private synchronized boolean guardarUsuario(String usuario, String password) {
    try (FileWriter fw = new FileWriter("usuarios.txt", true);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter pw = new PrintWriter(bw)) {
        pw.println(usuario + ":" + password);
        return true;
    } catch (IOException e) {
        System.err.println("Error guardando usuario: " + e.getMessage());
        return false;
    }
}private boolean usuarioExiste(String usuario) {
    try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
        String linea;
        while ((linea = br.readLine()) != null) {
            String[] partes = linea.split(":");
            if (partes[0].equals(usuario)) {
                return true;
            }
        }
    } catch (IOException e) {
        return false;
    }
    return false;
}
    }
}