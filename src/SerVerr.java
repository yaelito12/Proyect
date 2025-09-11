import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class SerVerr {
    private static final int PUERTO = 8080;
    private static ExecutorService pool = Executors.newFixedThreadPool(10);
    private static Map<String, ClienteInfo> clientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO);
             BufferedReader consola = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Servidor iniciado en puerto " + PUERTO);

            // Hilo de comandos del servidor
            new Thread(() -> {
                try {
                    String comando;
                    while ((comando = consola.readLine()) != null) {
                        if (comando.equalsIgnoreCase("stop")) {
                            System.out.println("Cerrando servidor...");
                            for (ClienteInfo c : clientes.values()) {
                                c.salida.println("El servidor se ha cerrado.");
                            }
                            pool.shutdownNow();
                            System.exit(0);
                        } else if (comando.equalsIgnoreCase("clientes")) {
                            System.out.println("Clientes conectados: " + clientes.size());
                            for (String u : clientes.keySet()) System.out.println(u);
                        } else if (comando.startsWith("mensaje ")) {
                            String msg = comando.substring(8);
                            enviarMensajeATodos(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Aceptar clientes
            while (true) {
                Socket socket = servidor.accept();
                pool.submit(new ClienteHandler(socket));
            }

        } catch (IOException e) {
            System.out.println("Servidor detenido: " + e.getMessage());
        }
    }

    private static void enviarMensajeATodos(String mensaje) {
        for (ClienteInfo c : clientes.values()) {
            c.bandeja.add("SERVIDOR: " + mensaje);
            c.salida.println("Nuevo mensaje recibido. Ve a 'Bandeja de entrada' para leerlo.");
        }
    }

    static class ClienteInfo {
        String usuario;
        PrintWriter salida;
        Socket socket;
        List<String> bandeja = new ArrayList<>();

        ClienteInfo(String usuario, PrintWriter salida, Socket socket) {
            this.usuario = usuario;
            this.salida = salida;
            this.socket = socket;
        }
    }

    static class ClienteHandler implements Runnable {
        private Socket socket;
        private PrintWriter salida;
        private String usuario;

        public ClienteHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                salida = new PrintWriter(socket.getOutputStream(), true);

                boolean conectado = true;
                while (conectado) {
                    salida.println("=== SISTEMA DE AUTENTICACION ===");
                    salida.println("1. Iniciar sesión");
                    salida.println("2. Registrarse");
                    salida.println("3. Salir");
                    salida.println("Seleccione una opción (1-3):");

                    String opcion = entrada.readLine();
                    if (opcion == null) return;

                    switch (opcion.trim()) {
                        case "1":
                            if (login(entrada)) conectado = menuPostLogin(entrada);
                            break;
                        case "2":
                            registro(entrada);
                            break;
                        case "3":
                            salida.println("¡Hasta luego!");
                            conectado = false;
                            break;
                        default:
                            salida.println("ERROR: Opción no válida");
                            break;
                    }
                }

            } catch (IOException e) {
                System.err.println("Error manejando cliente: " + e.getMessage());
            } finally {
                if (usuario != null) clientes.remove(usuario);
                try { socket.close(); } catch (IOException ignored) {}
                System.out.println("Cliente desconectado: " + usuario);
            }
        }

        private boolean login(BufferedReader entrada) throws IOException {
            salida.println("Ingrese usuario:");
            String u = entrada.readLine();
            salida.println("Ingrese contraseña:");
            String p = entrada.readLine();

            if (verificarLogin(u.trim(), hashPassword(p))) {
                usuario = u.trim();
                clientes.put(usuario, new ClienteInfo(usuario, salida, socket));
                salida.println("¡Bienvenido " + usuario + "!");
                System.out.println("Usuario " + usuario + " conectado.");
                return true;
            } else {
                salida.println("Usuario o contraseña incorrectos");
                return false;
            }
        }

        private void registro(BufferedReader entrada) throws IOException {
            salida.println("Ingrese nuevo usuario:");
            String u = entrada.readLine();
            if (usuarioExiste(u)) {
                salida.println("El usuario ya existe");
                return;
            }
            salida.println("Ingrese contraseña:");
            String p = entrada.readLine();
            if (guardarUsuario(u, hashPassword(p))) {
                salida.println("Usuario registrado correctamente");
            } else {
                salida.println("Error registrando usuario");
            }
        }

        private boolean menuPostLogin(BufferedReader entrada) throws IOException {
            boolean sesionActiva = true;
            while (sesionActiva) {
                salida.println("=== MENU ===");
                salida.println("1. Bandeja de entrada");
                salida.println("2. Jugar 'Adivina el número'");
                salida.println("3. Salir");
                salida.println("Seleccione opción (1-3):");

                String opcion = entrada.readLine();
                if (opcion == null) return false;

                switch (opcion.trim()) {
                    case "1":
                        mostrarBandeja();
                        break;
                    case "2":
                        juegoAdivinaNumero(entrada, salida);
                        break;
                    case "3":
                        salida.println("Cerrando sesión. ¡Hasta luego!");
                        sesionActiva = false;
                        break;
                    default:
                        salida.println("Opción inválida");
                        break;
                }
            }
            return true;
        }

        private void mostrarBandeja() {
            ClienteInfo c = clientes.get(usuario);
            if (c.bandeja.isEmpty()) {
                salida.println("No hay mensajes nuevos.");
            } else {
                salida.println("=== BANDEJA DE ENTRADA ===");
                for (String msg : c.bandeja) {
                    salida.println(msg);
                }
                c.bandeja.clear(); // Limpiar bandeja después de mostrar
            }
        }

        private void juegoAdivinaNumero(BufferedReader entrada, PrintWriter salida) throws IOException {
            int numeroSecreto = 1 + (int)(Math.random()*10);
            int intentos = 3;
            boolean acertado = false;
            salida.println("=== JUEGO: ADIVINA EL NÚMERO ===");
            salida.println("Número entre 1 y 10. Intentos: " + intentos);

            while (intentos>0 && !acertado) {
                salida.println("Ingresa tu número:");
                String resp = entrada.readLine();
                if (resp == null) return;
                try {
                    int n = Integer.parseInt(resp.trim());
                    if (n == numeroSecreto) { salida.println("¡Correcto!"); acertado=true; }
                    else if (n<numeroSecreto) salida.println("Es MAYOR");
                    else salida.println("Es MENOR");
                    intentos--;
                } catch (NumberFormatException e) { salida.println("Número inválido"); }
            }
            if (!acertado) salida.println("Perdiste. Era: " + numeroSecreto);
        }

        // --- Funciones de usuarios ---
        private synchronized boolean guardarUsuario(String usuario, String pass) {
            try (PrintWriter pw = new PrintWriter(new FileWriter("usuarios.txt", true))) {
                pw.println(usuario + ":" + pass); return true;
            } catch (IOException e) { return false; }
        }

        private boolean usuarioExiste(String usuario) {
            try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                String linea;
                while ((linea=br.readLine())!=null) if(linea.split(":")[0].equals(usuario)) return true;
            } catch (IOException ignored) {}
            return false;
        }

        private boolean verificarLogin(String usuario, String pass) {
            try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                String linea;
                while ((linea=br.readLine())!=null) {
                    String[] partes = linea.split(":");
                    if(partes[0].equals(usuario) && partes[1].equals(pass)) return true;
                }
            } catch (IOException ignored) {}
            return false;
        }

        private String hashPassword(String pass) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(pass.getBytes());
                StringBuilder sb = new StringBuilder();
                for(byte b:hash) sb.append(String.format("%02x",b));
                return sb.toString();
            } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
        }
    }
}