
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
                opcion = teclado.readLine().trim();
                if (opcion.isEmpty()) {
                    System.out.println("⚠ Por favor, ingrese una opcion valida");
                }
            }
            salida.println(opcion);

            // Manejar opciones principales
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

        // Mensaje de cierre
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║            DESCONECTADO               ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }

    // === LOGIN ===
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

            // === MENÚ POST LOGIN ===
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

            if (opcionPostLogin.equals("1")) {
                manejarJuego(entrada, salida, teclado);
            } else if (opcionPostLogin.equals("2")) {
                manejarChat(entrada, salida, teclado);
            }

        } else if (resultado.startsWith("ERROR")) {
            System.out.println("❌ " + resultado);
        } else {
            System.out.println("⚠ " + resultado);
        }
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

        String confirmarPassword = entrada.readLine();
        System.out.print("🔑 " + confirmarPassword + " ");
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

    // === CHAT ===
   private static void manejarChat(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
    System.out.println("\n=== CHAT CON EL SERVIDOR ===");
    System.out.println("👉 Escribe tus mensajes y presiona ENTER.");
    System.out.println("👉 Escribe 'salir' para terminar el chat.\n");

    String mensajeCliente, mensajeServidor;

    while (true) {
        // Cliente escribe
        System.out.print("Tú: ");
        mensajeCliente = teclado.readLine();
        salida.println(mensajeCliente);

        if (mensajeCliente.equalsIgnoreCase("salir")) {
            System.out.println("🚪 Has salido del chat.");
            break;
        }

        // Recibir respuesta del servidor
        mensajeServidor = entrada.readLine();
        if (mensajeServidor == null || mensajeServidor.equalsIgnoreCase("salir")) {
            System.out.println("⚠ El servidor cerró el chat.");
            break;
        }

        System.out.println("Servidor: " + mensajeServidor);
    }
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