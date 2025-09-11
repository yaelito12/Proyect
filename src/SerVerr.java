package serverr;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerVerr {
    private static final int PUERTO = 8080;
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando conexiones...");

            // Hilo de control para apagar con "stop"
            Thread control = new Thread(() -> {
                try (BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))) {
                    String comando;
                    while ((comando = consola.readLine()) != null) {
                        if (comando.equalsIgnoreCase("stop")) {
                            System.out.println("Cerrando servidor...");
                            try {
                                servidor.close();
                                pool.shutdownNow();
                            } catch (IOException e) {
                                System.err.println("Error cerrando servidor: " + e.getMessage());
                            }
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error leyendo consola: " + e.getMessage());
                }
            });
            control.start();

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());
                pool.submit(new ManejadorCliente(socket));
            }

        } catch (IOException e) {
            System.out.println("Servidor detenido: " + e.getMessage());
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

                boolean salirMenu = false;
                while (!salirMenu) {
                    salida.println("=== SISTEMA DE AUTENTICACION ===");
                    salida.println("1. Iniciar sesion");
                    salida.println("2. Registrarse");
                    salida.println("3. Salir");
                    salida.println("Seleccione una opcion (1-3):");

                    String opcion = entrada.readLine();
                    if (opcion == null) return;

                    switch (opcion.trim()) {
                        case "1":
                            manejarLogin(entrada, salida);
                            break;
                        case "2":
                            manejarRegistro(entrada, salida);
                            break;
                        case "3":
                            salida.println("¡Hasta luego!");
                            salirMenu = true;
                            break;
                        default:
                            salida.println("ERROR: Opcion no valida. Por favor seleccione 1, 2 o 3.");
                            salida.println("CONTINUAR");
                            break;
                    }
                }

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

        private void manejarLogin(BufferedReader entrada, PrintWriter salida) throws IOException {
            salida.println("=== LOGIN ===");
            salida.println("Ingrese usuario:");
            String username = entrada.readLine();
            if (username == null) return;
            salida.println("Ingrese password:");
            String password = entrada.readLine();
            if (password == null) return;

            if (verificarLogin(username.trim(), hashPassword(password))) {
                manejarLoginDirecto(entrada, salida, username.trim());
            } else {
                salida.println("Usuario o password incorrectos");
                System.out.println("Intento de login fallido para: " + username.trim());
            }
        }

        private void manejarRegistro(BufferedReader entrada, PrintWriter salida) throws IOException {
            salida.println("=== REGISTRO ===");
            salida.println("Ingrese nuevo usuario:");
            String username = entrada.readLine();
            if (username == null) return;
            username = username.trim();

            if (username.isEmpty()) {
                salida.println("ERROR: El usuario no puede estar vacío");
                return;
            }

            if (usuarioExiste(username)) {
                salida.println("El usuario ya existe");
                return;
            }

            salida.println("Ingrese password:");
            String password = entrada.readLine();
            if (password == null) return;

            if (password.length() < 4) {
                salida.println("ERROR: La password debe tener al menos 4 caracteres");
                return;
            }

            if (password.contains(" ")) {
                salida.println("ERROR: La contraseña no puede contener espacios");
                return;
            }

            salida.println("Confirme password:");
            String confirmPassword = entrada.readLine();
            if (confirmPassword == null) return;

            if (!password.equals(confirmPassword)) {
                salida.println("ERROR: Las passwords no coinciden");
                return;
            }

            if (guardarUsuario(username, hashPassword(password))) {
                salida.println("EXITO: Usuario " + username + " registrado correctamente");
                System.out.println("Usuario " + username + " registrado correctamente");

                // 🔑 Aquí inicia sesión automáticamente
                manejarLoginDirecto(entrada, salida, username);
            } else {
                salida.println("ERROR: Error registrando usuario");
            }
        }

        private void manejarLoginDirecto(BufferedReader entrada, PrintWriter salida, String username) throws IOException {
            salida.println("¡Bienvenido " + username + "!");
            System.out.println("Usuario " + username + " inició sesión correctamente");

            boolean salirPostLogin = false;
            while (!salirPostLogin) {
                salida.println("¿Qué deseas hacer?");
                salida.println("1. Jugar 'Adivina el número'");
                salida.println("2. Chatear con el servidor");
                salida.println("3. Cerrar sesión");
                salida.println("Elige una opción (1-3):");

                String opcionPostLogin = entrada.readLine();
                if (opcionPostLogin == null) return;

                switch (opcionPostLogin.trim()) {
                    case "1":
                        juegoAdivinaNumero(entrada, salida);
                        break;
                    case "2":
                        manejarChat(entrada, salida);
                        break;
                    case "3":
                        salida.println("Sesión cerrada. ¡Hasta luego!");
                        salirPostLogin = true;
                        break;
                    default:
                        salida.println("ERROR: Opción no válida. Por favor seleccione 1, 2 o 3.");
                        salida.println("CONTINUAR");
                        break;
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
                return false;
            }
        }

        private boolean usuarioExiste(String usuario) {
            try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] partes = linea.split(":", 2);
                    if (partes.length >= 1 && partes[0].equals(usuario)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                return false;
            }
            return false;
        }

        private boolean verificarLogin(String usuario, String passwordHash) {
            try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] partes = linea.split(":", 2);
                    if (partes.length == 2 && partes[0].equals(usuario) && partes[1].equals(passwordHash)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                return false;
            }
            return false;
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Error creando hash", e);
            }
        }

        private void manejarChat(BufferedReader entrada, PrintWriter salida) throws IOException {
            salida.println("CHAT_INICIADO");
            salida.println("=== CHAT CON EL SERVIDOR ===");
            salida.println("Escribe tus mensajes. Para salir escribe 'salir'");
            salida.println("ESPERANDO_MENSAJE");

            System.out.println("\n=== CHAT INICIADO CON CLIENTE ===");
            System.out.println("Cliente: " + socket.getInetAddress());

            BufferedReader consolaChat = new BufferedReader(new InputStreamReader(System.in));
            final boolean[] chatActivo = {true};

            Thread hiloReceptor = new Thread(() -> {
                try {
                    String mensajeCliente;
                    while (chatActivo[0] && (mensajeCliente = entrada.readLine()) != null) {
                        if (mensajeCliente.equalsIgnoreCase("salir")) {
                            System.out.println("\n[SISTEMA] Cliente salió del chat.");
                            salida.println("CHAT_CERRADO_POR_CLIENTE");
                            chatActivo[0] = false;
                            break;
                        }
                        System.out.println("\n[Cliente]: " + mensajeCliente);
                        System.out.print("Servidor: ");
                    }
                } catch (IOException e) {
                    if (chatActivo[0]) {
                        System.out.println("\n[SISTEMA] Error en el chat: " + e.getMessage());
                    }
                }
            });

            hiloReceptor.start();

            try {
                String mensajeServidor;
                while (chatActivo[0]) {
                    System.out.print("Servidor: ");
                    mensajeServidor = consolaChat.readLine();

                    if (mensajeServidor == null || mensajeServidor.equalsIgnoreCase("salir")) {
                        salida.println("CHAT_CERRADO_POR_SERVIDOR");
                        salida.println("El servidor cerró el chat.");
                        System.out.println("[SISTEMA] Chat cerrado por el servidor.");
                        chatActivo[0] = false;
                        break;
                    }

                    if (chatActivo[0]) {
                        salida.println("MENSAJE_SERVIDOR:" + mensajeServidor);
                    }
                }
            } finally {
                chatActivo[0] = false;
                hiloReceptor.interrupt();
                consolaChat.close();
            }

            System.out.println("[SISTEMA] Chat finalizado.\n");
        }

        private void juegoAdivinaNumero(BufferedReader entrada, PrintWriter salida) throws IOException {
            boolean seguirJugando = true;
            while (seguirJugando) {
                int numeroSecreto = 1 + (int) (Math.random() * 10);
                int intentos = 3;
                boolean acertado = false;

                salida.println("=== JUEGO: ADIVINA EL NÚMERO ===");
                salida.println("Estoy pensando en un número del 1 al 10.");
                salida.println("Tienes " + intentos + " intentos. ¡Suerte!");

                while (intentos > 0 && !acertado) {
                    salida.println("Ingresa tu número:");
                    String respuesta = entrada.readLine();
                    if (respuesta == null) return;

                    int numero;
                    try {
                        numero = Integer.parseInt(respuesta.trim());
                        if (numero < 1 || numero > 10) {
                            salida.println("⚠ Por favor, ingresa un número entre 1 y 10.");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        salida.println("⚠ Entrada inválida. Ingresa un número entre 1 y 10.");
                        continue;
                    }

                    if (numero == numeroSecreto) {
                        salida.println("🎉 ¡Correcto! El número era " + numeroSecreto);
                        acertado = true;
                    } else if (numero < numeroSecreto) {
                        salida.println("El número secreto es MAYOR.");
                    } else {
                        salida.println("El número secreto es MENOR.");
                    }
                    intentos--;
                }

                if (!acertado) {
                    salida.println("❌ Perdiste. El número era: " + numeroSecreto);
                }

                boolean respuestaValida = false;
                while (!respuestaValida) {
                    salida.println("¿Quieres jugar otra vez? (s/n):");
                    String respuesta = entrada.readLine();
                    if (respuesta == null) return;

                    respuesta = respuesta.trim().toLowerCase();
                    if (respuesta.equals("s") || respuesta.equals("si")) {
                        respuestaValida = true;
                        seguirJugando = true;
                    } else if (respuesta.equals("n") || respuesta.equals("no")) {
                        respuestaValida = true;
                        seguirJugando = false;
                        salida.println("¡Gracias por jugar!");
                    } else {
                        salida.println("⚠ Por favor, responde 's' para sí o 'n' para no.");
                    }
                }
            }
        }
    }
}