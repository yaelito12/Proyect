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
            System.out.println("Escribe 'ayuda' para ver los comandos.");

          
            new Thread(() -> {
                try {
                    String comando;
                    while ((comando = consola.readLine()) != null) {
                        procesarComando(comando.trim());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

         
            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Nuevo cliente conectado: " + socket.getInetAddress());
                pool.submit(new ClienteHandler(socket));
            }

        } catch (IOException e) {
            System.out.println("Servidor detenido: " + e.getMessage());
        }
    }

    private static void procesarComando(String comando) {
        switch (comando.toLowerCase()) {
            case "ayuda":
                System.out.println("\n=== COMANDOS DEL SERVIDOR ===");
                System.out.println("ayuda     - Ver comandos");
                System.out.println("estado    - Ver estado del servidor");
                System.out.println("clientes  - Ver clientes conectados");
                System.out.println("usuarios  - Ver usuarios registrados");
                System.out.println("mensaje   - Enviar mensaje a un cliente");
                System.out.println("parar     - Cerrar servidor\n");
                break;

            case "parar":
                System.out.println("Cerrando servidor...");
                for (ClienteInfo c : clientes.values()) {
                    c.salida.println("El servidor se ha cerrado.");
                }
                pool.shutdownNow();
                System.exit(0);
                break;

            case "estado":
                System.out.println("\n=== ESTADO DEL SERVIDOR ===");
                System.out.println("Puerto: " + PUERTO);
                System.out.println("Clientes conectados: " + clientes.size());
                System.out.println("Estado: ACTIVO\n");
                break;

            case "clientes":
                System.out.println("\n=== CLIENTES CONECTADOS ===");
                if (clientes.isEmpty()) {
                    System.out.println("No hay clientes conectados");
                } else {
                    int i = 1;
                    for (ClienteInfo cliente : clientes.values()) {
                        System.out.println(i + ". " + cliente.usuario);
                        i++;
                    }
                }
                System.out.println();
                break;

            case "usuarios":
                System.out.println("\n=== USUARIOS REGISTRADOS ===");
                try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                    String linea;
                    int count = 0;
                    while ((linea = br.readLine()) != null) {
                        String[] partes = linea.split(":");
                        if (partes.length >= 1) {
                            count++;
                            String estado = clientes.containsKey(partes[0]) ? "(conectado)" : "(desconectado)";
                            System.out.println(count + ". " + partes[0] + " " + estado);
                        }
                    }
                    if (count == 0) {
                        System.out.println("No hay usuarios registrados");
                    }
                } catch (IOException e) {
                    System.out.println("No hay usuarios registrados");
                }
                System.out.println();
                break;

            case "mensaje":
                enviarMensajeACliente();
                break;

            default:
                System.out.println("Comando no reconocido. Escribe 'ayuda'");
        }
    }

    private static void enviarMensajeACliente() {
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes conectados");
            return;
        }

        System.out.println("\n=== CLIENTES DISPONIBLES ===");
        int i = 1;
        for (String usuario : clientes.keySet()) {
            System.out.println(i + ". " + usuario);
            i++;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Escribe el nombre del usuario: ");
            String nombreUsuario = reader.readLine();

            if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
                System.out.println("Nombre de usuario vacío");
                return;
            }

            ClienteInfo cliente = clientes.get(nombreUsuario.trim());
            if (cliente == null) {
                System.out.println("Usuario no encontrado o no está conectado");
                return;
            }

            System.out.print("Escribe el mensaje: ");
            String mensaje = reader.readLine();

            if (mensaje == null || mensaje.trim().isEmpty()) {
                System.out.println("Mensaje vacío");
                return;
            }

            cliente.bandeja.add("[ADMIN]: " + mensaje.trim());
            System.out.println("Mensaje enviado a " + nombreUsuario);

        } catch (IOException e) {
            System.err.println("Error enviando mensaje: " + e.getMessage());
        }
    }

   
    private static class ClienteInfo {
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

    private static class ClienteHandler implements Runnable {
        private Socket socket;
        private PrintWriter salida;
        private String usuario;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                salida = new PrintWriter(socket.getOutputStream(), true);
                boolean conectado = true;
                boolean logueado = false;

                while (conectado) {
                    if (!logueado) {
                       
                        mostrarMenuPrincipal();
                        String opcion = entrada.readLine();
                        if (opcion == null) break;

                        switch (opcion.trim()) {
                            case "1":
                                if (login(entrada)) {
                                    logueado = true;
                                }
                                break;
                            case "2":
                                registro(entrada);
                                break;
                            case "3":
                                salida.println("Hasta luego");
                                conectado = false;
                                break;
                            default:
                                salida.println("Opción inválida. Seleccione 1, 2 o 3.");
                        }
                    } else {
                     
                        mostrarMenuPostLogin();
                        String opcion = entrada.readLine();
                        if (opcion == null) break;

                        switch (opcion.trim()) {
                            case "1":
                                mostrarBandeja(entrada);
                                break;
                            case "2":
                                juegoAdivinaNumero(entrada);
                                break;
                            case "3":
                                salida.println("Cerrando sesión. Hasta luego " + usuario);
                                logueado = false;
                                break;
                            default:
                                salida.println("Opción inválida. Seleccione 1, 2 o 3.");
                        }
                    }
                }

            } catch (IOException e) {
                System.err.println("Error manejando cliente: " + e.getMessage());
            } finally {
                if (usuario != null) {
                    clientes.remove(usuario);
                    System.out.println("Cliente desconectado: " + usuario);
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void mostrarMenuPrincipal() {
            salida.println("=== SISTEMA DE AUTENTICACION ===");
            salida.println("1. Iniciar sesión");
            salida.println("2. Registrarse");
            salida.println("3. Salir");
            salida.println("Seleccione opción (1-3):");
        }

        private void mostrarMenuPostLogin() {
            salida.println("=== MENU PRINCIPAL ===");
            salida.println("1. Bandeja de entrada");
            salida.println("2. Jugar 'Adivina número'");
            salida.println("3. Cerrar sesión");
            salida.println("Seleccione opción (1-3):");
        }

        private boolean login(BufferedReader entrada) throws IOException {
            salida.println("Ingrese usuario:");
            String u = entrada.readLine();
            if (u == null) return false;

            salida.println("Ingrese contraseña:");
            String p = entrada.readLine();
            if (p == null) return false;

            if (verificarLogin(u.trim(), hashPassword(p))) {
                usuario = u.trim();
                clientes.put(usuario, new ClienteInfo(usuario, salida, socket));
                salida.println("Bienvenido " + usuario);
                System.out.println("Login exitoso: " + usuario);
                return true;
            } else {
                salida.println("Usuario o contraseña incorrectos");
                return false;
            }
        }

        private void registro(BufferedReader entrada) throws IOException {
            salida.println("Ingrese nuevo usuario:");
            String u = entrada.readLine();
            if (u == null) return;

            u = u.trim();
            if (usuarioExiste(u)) {
                salida.println("El usuario ya existe");
                return;
            }

            salida.println("Ingrese contraseña:");
            String p = entrada.readLine();
            if (p == null) return;

            if (guardarUsuario(u, hashPassword(p))) {
                salida.println("Usuario registrado correctamente");
                System.out.println("Nuevo usuario registrado: " + u);
            } else {
                salida.println("Error registrando usuario");
            }
        }

        private void mostrarBandeja(BufferedReader entrada) throws IOException {
            ClienteInfo c = clientes.get(usuario);
            boolean enBandeja = true;

            while (enBandeja) {
                salida.println("=== BANDEJA DE ENTRADA ===");
                if (c.bandeja.isEmpty()) {
                    salida.println("No hay mensajes nuevos.");
                } else {
                    salida.println("Mensajes:");
                    for (int i = 0; i < c.bandeja.size(); i++) {
                        salida.println((i + 1) + ". " + c.bandeja.get(i));
                    }
                    c.bandeja.clear();
                }

                salida.println("Escribe 'salir' para volver al menú principal");
                String comando = entrada.readLine();
                if (comando == null) break;

                if (comando.trim().equalsIgnoreCase("salir")) {
                    enBandeja = false;
                } else {
                    salida.println("Comando no reconocido. Solo puedes escribir 'salir'.");
                }
            }
        }

        private void juegoAdivinaNumero(BufferedReader entrada) throws IOException {
            boolean seguirJugando = true;

            while (seguirJugando) {
                int numeroSecreto = 1 + (int)(Math.random() * 10);
                int intentos = 3;
                boolean acertado = false;

                salida.println("=== ADIVINA EL NÚMERO ===");
                salida.println("Adivina el número entre 1 y 10");
                salida.println("Tienes " + intentos + " intentos");

                while (intentos > 0 && !acertado) {
                    salida.println("Ingresa tu número:");
                    String resp = entrada.readLine();
                    if (resp == null) return;

                    try {
                        int numero = Integer.parseInt(resp.trim());

                        if (numero < 1 || numero > 10) {
                            salida.println("El número debe estar entre 1 y 10");
                            continue;
                        }

                        if (numero == numeroSecreto) {
                            salida.println("¡Correcto! El número era " + numeroSecreto);
                            acertado = true;
                        } else {
                            intentos--;
                            if (intentos > 0) {
                                if (numero < numeroSecreto) {
                                    salida.println("Es mayor. Te quedan " + intentos + " intentos");
                                } else {
                                    salida.println("Es menor. Te quedan " + intentos + " intentos");
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        salida.println("Por favor ingresa un número válido");
                    }
                }

                if (!acertado) {
                    salida.println("Perdiste. El número era: " + numeroSecreto);
                }

                salida.println("¿Quieres jugar otra vez? (s/n):");
                String respuesta = entrada.readLine();

                if (respuesta == null || respuesta.trim().equalsIgnoreCase("n")) {
                    seguirJugando = false;
                }
            }
        }

        // ----------- Métodos auxiliares -----------

        private synchronized boolean guardarUsuario(String usuario, String password) {
            try (PrintWriter pw = new PrintWriter(new FileWriter("usuarios.txt", true))) {
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
                    if (linea.split(":")[0].equals(usuario)) {
                        return true;
                    }
                }
            } catch (IOException ignored) {}
            return false;
        }

        private boolean verificarLogin(String usuario, String password) {
            try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    String[] partes = linea.split(":");
                    if (partes.length >= 2 && partes[0].equals(usuario) && partes[1].equals(password)) {
                        return true;
                    }
                }
            } catch (IOException ignored) {}
            return false;
        }

        private String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }
}