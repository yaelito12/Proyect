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
            
           
            String linea;
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);
                if (linea.contains("Seleccione una opcion")) {
                    break;
                }
            }
            
            String opcion = teclado.readLine();
            salida.println(opcion);
            
           
            switch (opcion) {
                case "1":
                    manejarLogin(entrada, salida, teclado);
                    break;
                case "2":
                    manejarRegistro(entrada, salida, teclado);
                    break;
                case "3":
                    String despedida = entrada.readLine();
                    System.out.println(despedida);
                    break;
                default:
                    String respuesta = entrada.readLine();
                    System.out.println(respuesta);
                    break;
            }
            
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
    

    private static void manejarLogin(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
     
        String header = entrada.readLine();
        System.out.println(header);
        
      
        String solicitarUsuario = entrada.readLine();
        System.out.print(solicitarUsuario + " ");
        String username = teclado.readLine();
        salida.println(username);
       
        String solicitarPassword = entrada.readLine();
        System.out.print(solicitarPassword + " ");
        String password = teclado.readLine();
        salida.println(password);
        
     
        String resultado = entrada.readLine();
        System.out.println(resultado);
    }
    
 
    private static void manejarRegistro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer header
        String header = entrada.readLine();
        System.out.println(header);
        
     
        String solicitarUsuario = entrada.readLine();
        System.out.print(solicitarUsuario + " ");
        String username = teclado.readLine();
        salida.println(username);
        
      
        String respuesta = entrada.readLine();
        if (respuesta.contains("ya existe")) {
            System.out.println(respuesta);
            return;
        }
        
    
        System.out.print(respuesta + " ");
        String password = teclado.readLine();
        salida.println(password);
        
     
        String confirmarPassword = entrada.readLine();
        System.out.print(confirmarPassword + " ");
        String confirmPassword = teclado.readLine();
        salida.println(confirmPassword);
    
        String resultado = entrada.readLine();
        System.out.println(resultado);
    }
}