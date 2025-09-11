

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 1234;
    
    public static void main(String[] args) throws IOException {
        // NUEVO: Header mejorado
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║      CLIENTE DE AUTENTICACION        ║");
        System.out.println("╚═══════════════════════════════════════╝");
        System.out.println("Conectando al servidor...");

        try (
            Socket socket = new Socket(HOST, PUERTO);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("✓ Conectado al servidor " + HOST + ":" + PUERTO + "\n");
            
            // Leer y mostrar el menú del servidor
            String linea;
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);
                if (linea.contains("Seleccione una opcion")) {
                    break;
                }
            }
            
            // NUEVO: Validar entrada del usuario
            String opcion = "";
            while (opcion.isEmpty()) {
                System.out.print("➤ ");
                opcion = teclado.readLine().trim();
                if (opcion.isEmpty()) {
                    System.out.println("⚠ Por favor, ingrese una opcion valida");
                }
            }
            salida.println(opcion);
            
            // Manejar cada opción
            switch (opcion) {
                case "1":
                    manejarLogin(entrada, salida, teclado);
                    break;
                case "2":
                    manejarRegistro(entrada, salida, teclado);
                    break;
                case "3":
                    String despedida = entrada.readLine();
                    System.out.println("\n" + despedida);
                    break;
                default:
                    String respuesta = entrada.readLine();
                    System.out.println("⚠ " + respuesta);
                    break;
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            System.err.println("   ¿Está el servidor ejecutándose?");
        }
        
        // NUEVO: Mensaje de cierre
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║            DESCONECTADO               ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }
    
    // NUEVO: Login con interfaz mejorada
    private static void manejarLogin(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer header
        String header = entrada.readLine();
        System.out.println("\n" + header);
        
        // Solicitar usuario
        String solicitarUsuario = entrada.readLine();
        System.out.print("👤 " + solicitarUsuario + " ");
        String username = leerEntradaNoVacia(teclado, "El usuario no puede estar vacío");
        salida.println(username);
        
        // Solicitar password
        String solicitarPassword = entrada.readLine();
        System.out.print("🔑 " + solicitarPassword + " ");
        String password = leerEntradaNoVacia(teclado, "La password no puede estar vacía");
        salida.println(password);
        
        // Leer resultado
        String resultado = entrada.readLine();
        if (resultado.startsWith("¡Bienvenido")) {
            System.out.println("✓ " + resultado);
        } else if (resultado.startsWith("ERROR")) {
            System.out.println("❌ " + resultado);
        } else {
            System.out.println("⚠ " + resultado);
        }
    }
    
    // NUEVO: Registro con interfaz mejorada
    private static void manejarRegistro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer header
        String header = entrada.readLine();
        System.out.println("\n" + header);
        
        // Solicitar nuevo usuario
        String solicitarUsuario = entrada.readLine();
        System.out.print("👤 " + solicitarUsuario + " ");
        String username = leerEntradaNoVacia(teclado, "El usuario no puede estar vacío");
        salida.println(username);
        
        // Leer respuesta
        String respuesta = entrada.readLine();
        
        // Si hay error, mostrar y terminar
        if (respuesta.startsWith("ERROR") || respuesta.contains("ya existe")) {
            System.out.println("❌ " + respuesta);
            return;
        }
        
        // Continuar con password
        System.out.print("🔑 " + respuesta + " ");
        String password = leerEntradaNoVacia(teclado, "La password no puede estar vacía");
        salida.println(password);
        
        // Confirmar password
        String confirmarPassword = entrada.readLine();
        System.out.print("🔑 " + confirmarPassword + " ");
        String confirmPassword = leerEntradaNoVacia(teclado, "La confirmación no puede estar vacía");
        salida.println(confirmPassword);
        
        // Leer resultado final
        String resultado = entrada.readLine();
        if (resultado.startsWith("EXITO")) {
            System.out.println("✓ " + resultado);
        } else if (resultado.startsWith("ERROR")) {
            System.out.println("❌ " + resultado);
        } else {
            System.out.println("⚠ " + resultado);
        }
    }
    
    // NUEVO: Método helper para validar entrada
    private static String leerEntradaNoVacia(BufferedReader teclado, String mensajeError) throws IOException {
        String entrada;
        while (true) {
            entrada = teclado.readLine();
            if (entrada != null && !entrada.trim().isEmpty()) {
                return entrada.trim();
            }
            System.out.print("⚠ " + mensajeError + ". Intente de nuevo: ");
        }
    }
}