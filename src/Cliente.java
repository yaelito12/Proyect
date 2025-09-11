import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;

    public static void main(String[] args) throws IOException {
        // Header bonito
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

            boolean conectado = true;
            while (conectado) {
                // Leer y mostrar menú inicial del servidor
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    System.out.println(linea);
                    if (linea.contains("Seleccione una opcion")) {
                        break;
                    }
                }

                // Enviar opción inicial
                String opcion = "";
                while (opcion.isEmpty()) {
                    System.out.print("➤ ");
                    opcion = teclado.readLine();
                    if (opcion != null) {
                        opcion = opcion.trim();
                    }
                    if (opcion == null || opcion.isEmpty()) {
                        System.out.println("⚠ Por favor, ingrese una opcion valida");
                    }
                }
                salida.println(opcion);

                // Manejar opciones principales
                switch (opcion) {
                    case "1":
                        conectado = !manejarLogin(entrada, salida, teclado);
                        break;
                    case "2":
                        manejarRegistro(entrada, salida, teclado);
                        break;
                    case "3":
                        String despedida = entrada.readLine();
                        System.out.println("\n" + despedida);
                        conectado = false;
                        break;
                    default:
                        // Manejar respuesta de opción inválida
                        String respuesta = entrada.readLine();
                        if (respuesta.startsWith("ERROR:")) {
                            System.out.println("⚠ " + respuesta.substring(6));
                        } else {
                            System.out.println("⚠ " + respuesta);
                        }
                        
                        // Esperar señal de continuar
                        String continuar = entrada.readLine();
                        if (!"CONTINUAR".equals(continuar)) {
                            conectado = false;
                        }
                        System.out.println();
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            System.err.println("   ¿Está el servidor ejecutándose?");
        }

        // Mensaje de cierre
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║            DESCONECTADO               ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }

    // === LOGIN ===
    private static boolean manejarLogin(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
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

            // === MENÚ POST LOGIN ===
            boolean sesionActiva = true;
            while (sesionActiva) {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    System.out.println(linea);
                    if (linea.contains("Elige una opción")) {
                        break;
                    }
                }

                System.out.print("➤ ");
                String opcionPostLogin = leerEntradaNoVacia(teclado, "Entrada no válida");
                salida.println(opcionPostLogin);

                switch (opcionPostLogin) {
                    case "1":
                        manejarJuego(entrada, salida, teclado);
                        break;
                    case "2":
                        manejarChat(entrada, salida, teclado);
                        break;
                    case "3":
                        String despedida = entrada.readLine();
                        System.out.println("✓ " + despedida);
                        sesionActiva = false;
                        break;
                    default:
                        // Manejar opción inválida en post-login
                        String respuestaError = entrada.readLine();
                        if (respuestaError.startsWith("ERROR:")) {
                            System.out.println("⚠ " + respuestaError.substring(6));
                        } else {
                            System.out.println("⚠ " + respuestaError);
                        }
                        
                        // Esperar señal de continuar
                        String continuar = entrada.readLine();
                        if (!"CONTINUAR".equals(continuar)) {
                            sesionActiva = false;
                        }
                        System.out.println();
                        break;
                }
            }
            return true; // Sesión completada exitosamente

        } else if (resultado.startsWith("ERROR")) {
            System.out.println("❌ " + resultado);
        } else {
            System.out.println("⚠ " + resultado);
        }
        return false; // No cerrar conexión, volver al menú principal
    }

    // === REGISTRO ===
    private static void manejarRegistro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String header = entrada.readLine();
        System.out.println("\n" + header);

        String solicitarUsuario = entrada.readLine();
        System.out.print("👤 " + solicitarUsuario + " ");
        String username = leerEntradaNoVacia(teclado, "El usuario no puede estar vacío");
        salida.println(username);

        String respuesta = entrada.readLine();
        if (respuesta.startsWith("ERROR") || respuesta.contains("ya existe")) {
            System.out.println("❌ " + respuesta);
            return;
        }

        System.out.print("🔑 " + respuesta + " ");
        String password = leerEntradaNoVacia(teclado, "La password no puede estar vacía");
        salida.println(password);

        String respuesta2 = entrada.readLine();
        if (respuesta2.startsWith("ERROR")) {
            System.out.println("❌ " + respuesta2);
            return;
        }

        System.out.print("🔑 " + respuesta2 + " ");
        String confirmPassword = leerEntradaNoVacia(teclado, "La confirmación no puede estar vacía");
        salida.println(confirmPassword);

        String resultado = entrada.readLine();
        if (resultado.startsWith("EXITO")) {
            System.out.println("✓ " + resultado);
        } else if (resultado.startsWith("ERROR")) {
            System.out.println("❌ " + resultado);
        } else {
            System.out.println("⚠ " + resultado);
        }
    }

    // === JUEGO ===
    private static void manejarJuego(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String linea;
        while ((linea = entrada.readLine()) != null) {
            System.out.println(linea);

            if (linea.contains("Ingresa tu número:") || linea.contains("¿Quieres jugar otra vez?")) {
                System.out.print("➤ ");
                String respuesta = leerEntradaNoVacia(teclado, "Entrada no válida");
                salida.println(respuesta);
            }

            if (linea.contains("¡Gracias por jugar!")) {
                break;
            }
        }
    }

    // === CHAT MEJORADO ===
    private static void manejarChat(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Esperar señal de inicio de chat
        String inicioChat = entrada.readLine();
        if (!"CHAT_INICIADO".equals(inicioChat)) {
            System.out.println("❌ Error iniciando chat");
            return;
        }

        // Leer mensajes de bienvenida del servidor
        String linea;
        while ((linea = entrada.readLine()) != null) {
            if ("ESPERANDO_MENSAJE".equals(linea)) {
                break;
            }
            System.out.println(linea);
        }

        final boolean[] chatActivo = {true}; // Usar array para modificar desde hilos
        
        // Hilo para recibir mensajes del servidor
        Thread hiloReceptor = new Thread(() -> {
            try {
                String mensajeServidor;
                while (chatActivo[0] && (mensajeServidor = entrada.readLine()) != null) {
                    if ("CHAT_CERRADO_POR_SERVIDOR".equals(mensajeServidor)) {
                        System.out.println("\n⚠ El servidor cerró el chat.");
                        chatActivo[0] = false;
                        break;
                    } else if ("CHAT_CERRADO_POR_CLIENTE".equals(mensajeServidor)) {
                        chatActivo[0] = false;
                        break;
                    } else if (mensajeServidor.startsWith("MENSAJE_SERVIDOR:")) {
                        String mensaje = mensajeServidor.substring("MENSAJE_SERVIDOR:".length());
                        System.out.println("\nServidor: " + mensaje);
                        System.out.print("Tú: ");
                    } else {
                        // Manejar otros mensajes del servidor
                        System.out.println("Servidor: " + mensajeServidor);
                        System.out.print("Tú: ");
                    }
                }
            } catch (IOException e) {
                if (chatActivo[0]) {
                    System.out.println("\n⚠ Error en el chat: " + e.getMessage());
                }
            }
        });
        
        hiloReceptor.start();
        
        // Hilo principal para enviar mensajes
        try {
            String mensajeCliente;
            while (chatActivo[0]) {
                System.out.print("Tú: ");
                mensajeCliente = teclado.readLine();
                
                if (mensajeCliente == null) {
                    chatActivo[0] = false;
                    break;
                }
                
                salida.println(mensajeCliente);
                
                if (mensajeCliente.equalsIgnoreCase("salir")) {
                    System.out.println("🚪 Has salido del chat.");
                    chatActivo[0] = false;
                    break;
                }
            }
        } finally {
            chatActivo[0] = false;
            hiloReceptor.interrupt();
            
            // Pequeña pausa para que el hilo receptor termine limpiamente
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Chat finalizado.\n");
    }

    // === VALIDAR ENTRADA ===
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