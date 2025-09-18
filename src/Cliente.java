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
                String linea;
                StringBuilder menu = new StringBuilder();

                // Leer el menú completo
                while ((linea = entrada.readLine()) != null) {
                    menu.append(linea).append("\n");
                    if (linea.toLowerCase().contains("seleccione opción")) break;
                }

                System.out.print(menu.toString());
                System.out.print("➤ ");
                String opcion = teclado.readLine();
                if (opcion == null) break;
                
                salida.println(opcion);

                if (!logueado) {
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
                            String despedida = entrada.readLine();
                            System.out.println(despedida);
                            System.out.println("👋 Desconectando...");
                            conectado = false;
                            break;
                        default:
                            String error = entrada.readLine();
                            System.out.println(error);
                    }
                } else {
                    switch (opcion.trim()) {
                        case "1":
                            bandeja(entrada, salida, teclado);
                            break;
                        case "2":
                            juego(entrada, salida, teclado);
                            break;
                        case "3":
                            enviarMensaje(entrada, salida, teclado);
                            break;
                        case "4":
                            String sesionCerrada = entrada.readLine(); // "Cerrando sesión..."
                            System.out.println(sesionCerrada);
                            logueado = false;
                            break;
                        default:
                            String error = entrada.readLine();
                            System.out.println(error);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
        }
    }

    private static boolean login(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer "Ingrese usuario:"
        String prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Usuario: ");
        String usuario = teclado.readLine();
        salida.println(usuario);

        // Leer "Ingrese contraseña:"
        prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Contraseña: ");
        String password = teclado.readLine();
        salida.println(password);

        // Leer respuesta del servidor
        String respuesta = entrada.readLine();
        System.out.println(respuesta);
        return respuesta.contains("Bienvenido");
    }

    private static void registro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer "Ingrese nuevo usuario:"
        String prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Usuario: ");
        String usuario = teclado.readLine();
        salida.println(usuario);

        // Leer primera respuesta (puede ser error de usuario existente)
        String respuesta = entrada.readLine();
        System.out.println(respuesta);

        if (respuesta.contains("ya existe") || respuesta.contains("vacío")) {
            return; // Salir si hay error
        }

        // Si llegamos aquí, pedir contraseña
        System.out.print("Contraseña: ");
        String password = teclado.readLine();
        salida.println(password);

        // Leer respuesta final
        String respuestaFinal = entrada.readLine();
        System.out.println(respuestaFinal);

        // Si la contraseña no es válida, puede haber otro mensaje
        if (respuestaFinal.contains("no válida")) {
            String mensaje = entrada.readLine();
            if (mensaje != null) {
                System.out.println(mensaje);
            }
        }
    }

    private static void bandeja(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        while (true) {
            String linea;
            
            // Leer todo el contenido de la bandeja hasta las opciones
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);
                if (linea.toLowerCase().contains("escribir 'salir'")) break;
            }

            System.out.print("➤ ");
            String comando = teclado.readLine();
            if (comando == null) break;

            salida.println(comando);

            if (comando.trim().equalsIgnoreCase("salir")) {
                return; // Salir de la bandeja
            }

            // Leer respuesta del comando
            String respuesta = entrada.readLine();
            if (respuesta != null) {
                System.out.println(respuesta);
            }
        }
    }

    private static void juego(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        boolean jugando = true;
        
        while (jugando) {
            String linea;
            
            // Leer mensajes del juego
            while ((linea = entrada.readLine()) != null) {
                System.out.println(linea);

                if (linea.contains("Ingresa tu número:")) {
                    System.out.print("➤ ");
                    String numero = teclado.readLine();
                    salida.println(numero);
                } else if (linea.toLowerCase().contains("¿quieres jugar otra vez?")) {
                    System.out.print("➤ ");
                    String respuesta = teclado.readLine();
                    salida.println(respuesta);

                    if (respuesta != null && respuesta.trim().equalsIgnoreCase("n")) {
                        jugando = false;
                    }
                    break; // Salir del bucle interno para continuar o terminar
                }
            }
        }
    }

    private static void enviarMensaje(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String linea;
        
        // Leer lista de usuarios disponibles
        while ((linea = entrada.readLine()) != null) {
            System.out.println(linea);
            if (linea.toLowerCase().contains("escribe el nombre del usuario")) break;
        }

        System.out.print("➤ ");
        String destinatario = teclado.readLine();
        if (destinatario == null) return;
        
        salida.println(destinatario);

        // Leer respuesta (puede ser error o solicitud de mensaje)
        linea = entrada.readLine();
        System.out.println(linea);
        
        // Si hay error (no conectado, inválido, etc.), salir
        if (linea.toLowerCase().contains("no") || 
            linea.toLowerCase().contains("inválido") || 
            linea.toLowerCase().contains("conectado")) {
            return;
        }

        // Si llegamos aquí, pedir el mensaje
        System.out.print("Mensaje ➤ ");
        String mensaje = teclado.readLine();
        if (mensaje == null) return;
        
        salida.println(mensaje);

        // Leer confirmación
        String confirmacion = entrada.readLine();
        if (confirmacion != null) {
            System.out.println(confirmacion);
        }
    }
}