import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerVerr {
    private static final int PUERTO = 8080;
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando conexiones...");

            Thread control = new Thread(() -> {
                try (BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))) {
                    String comando;
                    while ((comando = consola.readLine()) != null) {
                        if (comando.equalsIgnoreCase("stop")) {
                            System.out.println("Cerrando servidor...");
                            servidor.close();
                            pool.shutdownNow();
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

                while (true) {
                    salida.println("=== SISTEMA DE AUTENTICACION ===");
                    salida.println("1. Iniciar sesión");
                    salida.println("2. Registrarse");
                    salida.println("3. Salir");
                    salida.println("Seleccione una opción (1-3):");

                    String opcion = entrada.readLine();
                    if (opcion == null) break;

                    switch (opcion.trim()) {
                        case "1":
                            manejarLogin(entrada, salida);
                            break;
                        case "2":
                            manejarRegistro(entrada, salida);
                            break;
                        case "3":
                            salida.println("¡Hasta luego!");
                            return;
                        default:
                            salida.println("⚠ Opción no válida. Intente de nuevo.");
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
                salida.println("¡Bienvenido " + username.trim() + "!");

                // Menú post-login
                boolean opcionValida = false;
                while (!opcionValida) {
                    salida.println("¿Qué deseas hacer?");
                    salida.println("1. Jugar 'Adivina el número'");
                    salida.println("2. Chatear con el servidor");
                    salida.println("Elige una opción (1-2):");
                    String opcion = entrada.readLine();
                    if (opcion == null) return;

                    switch (opcion.trim()) {
                        case "1":
                            opcionValida = true;
                            juegoAdivinaNumero(entrada, salida);
                            break;
                        case "2":
                            opcionValida = true;
                            manejarChat(entrada, salida);
                            break;
                        default:
                            salida.println("⚠ Opción inválida. Por favor, elige 1 o 2.");
                    }
                }

            } else {
                salida.println("⚠ Usuario o password incorrectos");
            }
        }

        private void manejarRegistro(BufferedReader entrada, PrintWriter salida) throws IOException {
            salida.println("=== REGISTRO ===");

            while (true) {
                salida.println("Ingrese nuevo usuario:");
                String username = entrada.readLine();
                if (username == null) return;
                username = username.trim();
                if (username.isEmpty()) {
                    salida.println("⚠ El usuario no puede estar vacío");
                    continue;
                }
                if (usuarioExiste(username)) {
                    salida.println("⚠ El usuario ya existe. Intente otro.");
                    continue;
                }

                salida.println("Ingrese password (mínimo 4 caracteres, sin espacios):");
                String password = entrada.readLine();
                if (password == null) return;
                if (password.length() < 4 || password.contains(" ")) {
                    salida.println("⚠ Contraseña inválida. Intente otra vez.");
                    continue;
                }

                salida.println("Confirme password:");
                String confirmPassword = entrada.readLine();
                if (confirmPassword == null) return;
                if (!password.equals(confirmPassword)) {
                    salida.println("⚠ Las passwords no coinciden. Intente otra vez.");
                    continue;
                }

                if (guardarUsuario(username, hashPassword(password))) {
                    salida.println("✅ Usuario " + username + " registrado correctamente");
                } else {
                    salida.println("⚠ Error registrando usuario");
                }
                break;
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
                    if (partes.length >= 1 && partes[0].equals(usuario)) return true;
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
                    if (partes.length == 2 && partes[0].equals(usuario) && partes[1].equals(passwordHash))
                        return true;
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

        // ===== JUEGO =====
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

                salida.println("¿Quieres jugar otra vez? (s/n):");
                String respuesta = entrada.readLine();
                if (respuesta == null || !respuesta.trim().equalsIgnoreCase("s")) {
                    seguirJugando = false;
                    salida.println("¡Gracias por jugar!");
                }
            }
        }

        // ===== CHAT =====
        private void manejarChat(BufferedReader entrada, PrintWriter salida) throws IOException {
            Scanner consola = new Scanner(System.in);
            salida.println("=== CHAT CON EL SERVIDOR ===");
            salida.println("Escribe 'salir' para terminar.\n");

            Thread recibir = new Thread(() -> {
                try {
                    String mensajeCliente;
                    while ((mensajeCliente = entrada.readLine()) != null) {
                        if (mensajeCliente.equalsIgnoreCase("salir")) {
                            System.out.println("⚠ Cliente salió del chat.");
                            break;
                        }
                        System.out.println("[Cliente]: " + mensajeCliente);
                    }
                } catch (IOException e) {
                    System.out.println("⚠ Chat terminado.");
                }
            });
            recibir.start();

            String mensajeServidor;
            while (true) {
                System.out.print("Servidor: ");
                mensajeServidor = consola.nextLine();
                salida.println(mensajeServidor);
                if (mensajeServidor.equalsIgnoreCase("salir")) {
                    System.out.println("🚪 Chat cerrado por el servidor.");
                    break;
                }
            }

            recibir.interrupt();
        }
    }
}