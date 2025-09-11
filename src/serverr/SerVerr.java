package serverr;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerVerr {
    private static final int PUERTO = 1234;
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

                boolean activo = true;
                while (activo) {
                    // Menú de autenticación
                    salida.println("=== MENU ===");
                    salida.println("1. Login");
                    salida.println("2. Registro");
                    salida.println("3. Salir");
                    salida.println("Elija una opción:");
                    String opcion = entrada.readLine();

                    if (opcion == null) break;

                    switch (opcion) {
                        case "1":
                            manejarLogin(entrada, salida);
                            break;
                        case "2":
                            manejarRegistro(entrada, salida);
                            break;
                        case "3":
                            salida.println("Desconectando...");
                            activo = false;
                            break;
                        default:
                            salida.println("Opción inválida");
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

            salida.println("Ingrese password:");
            String password = entrada.readLine();

            if (verificarLogin(username, hashPassword(password))) {
                salida.println("¡Bienvenido " + username + "!");
                System.out.println("Usuario " + username + " inicio sesion correctamente");
            } else {
                salida.println("Usuario o password incorrectos");
                System.out.println("Intento de login fallido para: " + username);
            }
        }

        // === REGISTRO ===
        private void manejarRegistro(BufferedReader entrada, PrintWriter salida) throws IOException {
            salida.println("=== REGISTRO ===");
            salida.println("Ingrese nuevo usuario:");
            String username = entrada.readLine();

            if (username == null || username.trim().isEmpty()) {
                salida.println("El usuario no puede estar vacío");
                return;
            }

            if (usuarioExiste(username)) {
                salida.println("El usuario ya existe");
                System.out.println("Intento de registro fallido: usuario " + username + " ya existe");
                return;
            }

            salida.println("Ingrese password:");
            String password = entrada.readLine();

            if (password == null || password.length() < 4) {
                salida.println("La password debe tener al menos 4 caracteres");
                return;
            }

            if (password.contains(" ")) {
                salida.println("La contraseña no puede contener espacios");
                return;
            }

            salida.println("Confirme password:");
            String confirmPassword = entrada.readLine();

            if (!password.equals(confirmPassword)) {
                salida.println("Las passwords no coinciden");
                return;
            }

            if (guardarUsuario(username, hashPassword(password))) {
                salida.println("Usuario " + username + " registrado correctamente");
                System.out.println("Usuario " + username + " registrado correctamente");
            } else {
                salida.println("Error registrando usuario");
                System.out.println("Error registrando usuario " + username);
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
        }

        private boolean usuarioExiste(String usuario) {
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

        private boolean verificarLogin(String usuario, String passwordHash) {
            try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] partes = linea.split(":");
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
    }
}