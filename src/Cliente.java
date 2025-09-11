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

            System.out.println("🔗 Conectado al servidor");
            
            boolean conectado = true;
            boolean logueado = false;
            
            while (conectado) {
                // Leer menú del servidor
                String linea;
                StringBuilder menu = new StringBuilder();
                while ((linea = entrada.readLine()) != null) {
                    menu.append(linea).append("\n");
                    if (linea.contains("Seleccione opción")) break;
                }
                System.out.print(menu.toString());

                System.out.print("➤ ");
                String opcion = teclado.readLine();
                salida.println(opcion);

                if (!logueado) {
                    // Menú de autenticación
                    switch (opcion.trim()) {
                        case "1": 
                            if (login(entrada, salida, teclado)) {
                                logueado = true;
                            }
                            break;
                        case "2": 
                            registro(entrada, salida, teclado); 
                            break;
                        case "3": 
                            System.out.println("👋 Desconectando...");
                            conectado = false; 
                            break;
                        default:
                            // Leer respuesta del servidor para opción inválida
                            System.out.println(entrada.readLine());
                    }
                } else {
                    // Menú post-login
                    switch (opcion.trim()) {
                        case "1": 
                            bandeja(entrada, salida, teclado); 
                            break;
                        case "2": 
                            juego(entrada, salida, teclado); 
                            break;
                        case "3": 
                            // Leer mensaje de cierre de sesión
                            System.out.println(entrada.readLine());
                            logueado = false; 
                            break;
                        default:
                            // Leer respuesta del servidor para opción inválida
                            System.out.println(entrada.readLine());
                    }
                }
            }

        } catch (IOException e) { 
            System.err.println("❌ Error de conexión: " + e.getMessage()); 
        }
    }

    private static boolean login(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer prompt de usuario
        System.out.println(entrada.readLine());
        System.out.print("Usuario: ");
        salida.println(teclado.readLine());
        
        // Leer prompt de contraseña
        System.out.println(entrada.readLine());
        System.out.print("Contraseña: ");
        salida.println(teclado.readLine());

        // Leer resultado del login
        String resultado = entrada.readLine();
        System.out.println(resultado);
        
        // Retornar true si el login fue exitoso
        return resultado.contains("Bienvenido");
    }

    private static void registro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer prompt de usuario
        System.out.println(entrada.readLine());
        System.out.print("Usuario: ");
        salida.println(teclado.readLine());
        
        // Leer respuesta (puede ser que ya existe)
        String respuesta = entrada.readLine();
        System.out.println(respuesta);
        
        if (respuesta.contains("ya existe")) {
            return; // Salir si el usuario ya existe
        }
        
        // Leer prompt de contraseña
        System.out.println(entrada.readLine());
        System.out.print("Contraseña: ");
        salida.println(teclado.readLine());
        
        // Leer confirmación de registro
        System.out.println(entrada.readLine());
    }

    private static void bandeja(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        while (true) {
            String linea;
            // Leer todo el contenido de la bandeja hasta encontrar el prompt de salida
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);
                if (linea.contains("Escribe 'salir'")) {
                    break;
                }
            }

            System.out.print("➤ ");
            String comando = teclado.readLine();
            
            if (comando == null || comando.trim().isEmpty()) {
                System.out.println("⚠ Entrada vacía no permitida.");
                continue;
            }
            
            salida.println(comando);
            
            if (comando.trim().equalsIgnoreCase("salir")) {
                return; // Salir de la bandeja
            }
            
            // Leer respuesta del servidor para comandos no reconocidos
            String respuesta = entrada.readLine();
            if (respuesta != null) {
                System.out.println(respuesta);
            }
        }
    }

    private static void juego(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        while (true) {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);
                
                if (linea.contains("Ingresa tu número:")) {
                    System.out.print("➤ ");
                    String numero = teclado.readLine();
                    salida.println(numero);
                } else if (linea.contains("¿Quieres jugar otra vez?")) {
                    System.out.print("➤ ");
                    String respuesta = teclado.readLine();
                    salida.println(respuesta);
                    
                    if (respuesta != null && respuesta.trim().equalsIgnoreCase("n")) {
                        return; // Salir del juego
                    }
                    break; // Continuar con otra ronda
                } else if (linea.contains("Correcto") || linea.contains("Perdiste")) {
                    // Continuar leyendo para ver si hay pregunta de continuar
                    continue;
                }
            }
        }
    }
}