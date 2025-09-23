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
    private static Set<String> usuariosExpulsados = ConcurrentHashMap.newKeySet();
    private static Map<String, Set<String>> usuariosBloqueados = new ConcurrentHashMap<>();
    
    private static void guardarMensaje(String usuario, String mensaje) {
        File archivo = new File("mensajes/" + usuario + ".txt");
        archivo.getParentFile().mkdirs(); 
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
            pw.println(mensaje);
        } catch (IOException e) {
            System.err.println("Error guardando mensaje para " + usuario + ": " + e.getMessage());
        }
        
        ClienteInfo cliente = clientes.get(usuario);
        if (cliente != null) {
            cliente.salida.println("🔔 NUEVO MENSAJE: " + mensaje);
            cliente.salida.println("(Escribe 'menu' para volver al menú o continúa con lo que estabas haciendo)");
        }
    }

    private static List<String> cargarMensajes(String usuario) {
        List<String> mensajes = new ArrayList<>();
        File archivo = new File("mensajes/" + usuario + ".txt");

        if (!archivo.exists()) return mensajes;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                mensajes.add(linea);
            }
        } catch (IOException e) {
            System.err.println("Error leyendo mensajes de " + usuario + ": " + e.getMessage());
        }

        return mensajes;
    }

    private static boolean eliminarMensaje(String usuario, int index) {
        File archivo = new File("mensajes/" + usuario + ".txt");
        List<String> mensajes = cargarMensajes(usuario);

        if (index < 0 || index >= mensajes.size()) return false;

        mensajes.remove(index);  

        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            for (String msg : mensajes) {
                pw.println(msg);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean eliminarUsuarioCompleto(String usuario) {
        try {
            if (!eliminarUsuarioDelArchivo(usuario)) {
                System.err.println("Error eliminando usuario del archivo de usuarios");
                return false;
            }

            File archivoMensajes = new File("mensajes/" + usuario + ".txt");
            if (archivoMensajes.exists()) {
                if (archivoMensajes.delete()) {
                    System.out.println("Archivo de mensajes eliminado para: " + usuario);
                } else {
                    System.err.println("No se pudo eliminar el archivo de mensajes de: " + usuario);
                }
            }

            usuariosExpulsados.add(usuario);
            System.out.println("Usuario " + usuario + " eliminado completamente del sistema");
            System.out.println("El usuario puede registrarse nuevamente con el mismo nombre");
            return true;

        } catch (Exception e) {
            System.err.println("Error eliminando usuario " + usuario + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean eliminarUsuarioDelArchivo(String usuarioAEliminar) {
        File archivo = new File("usuarios.txt");
        File archivoTemp = new File("usuarios_temp.txt");

        if (!archivo.exists()) {
            return true; 
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivo));
             PrintWriter pw = new PrintWriter(new FileWriter(archivoTemp))) {

            String linea;
            boolean encontrado = false;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length > 0 && partes[0].equals(usuarioAEliminar)) {
                    encontrado = true;
                    continue;
                }
                pw.println(linea); 
            }

            pw.close();
            br.close();

            if (archivo.delete() && archivoTemp.renameTo(archivo)) {
                return encontrado;
            } else {
                System.err.println("Error reemplazando el archivo de usuarios");
                archivoTemp.delete(); 
                return false;
            }

        } catch (IOException e) {
            System.err.println("Error procesando archivo de usuarios: " + e.getMessage());
            return false;
        }
    }

    private static List<String> obtenerTodosLosUsuarios() {
        List<String> usuarios = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length >= 1) {
                    usuarios.add(partes[0]);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo usuarios: " + e.getMessage());
        }
        return usuarios;
    }

    private static boolean usuarioExisteEnArchivo(String usuario) {
        try (BufferedReader br = new BufferedReader(new FileReader("usuarios.txt"))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(":");
                if (partes.length > 0 && partes[0].equals(usuario)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

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

    private static void expulsarCliente() {
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes conectados.");
            return;
        }

        System.out.println("\n=== CLIENTES CONECTADOS ===");
        List<String> usuarios = new ArrayList<>(clientes.keySet());

        for (int i = 0; i < usuarios.size(); i++) {
            System.out.println((i + 1) + ". " + usuarios.get(i));
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Ingrese el número del cliente a expulsar (0 para cancelar): ");
            String entrada = reader.readLine();

            if (entrada == null || entrada.trim().equals("0")) {
                System.out.println("Operación cancelada.");
                return;
            }

            int index = Integer.parseInt(entrada.trim()) - 1;

            if (index >= 0 && index < usuarios.size()) {
                String usuario = usuarios.get(index);
                ClienteInfo cliente = clientes.get(usuario);

                if (cliente != null) {
                    try {
                        cliente.salida.println("❌ Has sido expulsado del servidor.");
                        cliente.salida.println("Tu cuenta ha sido eliminada del sistema.");
                        cliente.salida.println("Puedes registrarte nuevamente si lo deseas.");
                        cliente.salida.println("DISCONNECT");
                        Thread.sleep(500); 

                        cliente.socket.close();
                        clientes.remove(usuario);

                        if (eliminarUsuarioCompleto(usuario)) {
                            System.out.println("✅ Cliente '" + usuario + "' expulsado y eliminado del sistema.");
                            System.out.println("Puede registrarse nuevamente con el mismo nombre.");
                        } else {
                            System.out.println("⚠️ Cliente '" + usuario + "' expulsado pero hubo errores eliminando sus datos.");
                        }

                    } catch (Exception e) {
                        System.out.println("Error al expulsar cliente, intentando eliminación de datos: " + e.getMessage());
                        clientes.remove(usuario);
                        eliminarUsuarioCompleto(usuario);
                    }
                } else {
                    System.out.println("No se encontró al cliente.");
                }
            } else {
                System.out.println("Índice fuera de rango.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Por favor ingrese un número válido.");
        } catch (Exception e) {
            System.out.println("Error al expulsar cliente: " + e.getMessage());
        }
    }

   

    private static void rehabilitarUsuario() {
        System.out.println("Esta funcionalidad no está disponible.");
        System.out.println("Los usuarios expulsados pueden registrarse nuevamente automáticamente.");
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
                System.out.println("expulsar  - Expulsar y eliminar a un cliente (puede re-registrarse)");
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
               
            case "expulsar":
                expulsarCliente();
                break;

            case "rehabilitar":
                rehabilitarUsuario();
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

            guardarMensaje(nombreUsuario.trim(), "[ADMIN]: " + mensaje.trim());
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
                                enviarMensajeUsuario(entrada);
                                break;
                           
                            case "5":
                                salida.println("Cerrando sesión. Hasta luego " + usuario);
                                logueado = false;
                                break;
                            default:
                                salida.println("Opción inválida. Seleccione 1, 2, 3, 4 o 5.");
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
            salida.println("3. Enviar mensaje a otro usuario");
            salida.println("4. Gestionar bloqueos");
            salida.println("5. Cerrar sesión");
            salida.println("Seleccione opción (1-5):");
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
            if (u.isEmpty()) {
                salida.println("El nombre de usuario no puede estar vacío.");
                return;
            }

            if (usuarioExiste(u)) {
                salida.println("El usuario ya existe");
                return;
            }

            salida.println("Ingrese contraseña (mínimo 6 caracteres, debe tener letras y números):");
            String p = entrada.readLine();
            if (p == null) return;

            if (!esContrasenaSegura(p)) {
                salida.println("Contraseña no válida.");
                salida.println("Debe tener al menos 6 caracteres e incluir letras y números.");
                return;
            }
 
            if (guardarUsuario(u, hashPassword(p))) {
                File archivoMensajes = new File("mensajes/" + u + ".txt");
                if (archivoMensajes.exists()) {
                    archivoMensajes.delete();
                    System.out.println("Se eliminaron mensajes anteriores del usuario: " + u);
                }

                salida.println("Usuario registrado correctamente");
                System.out.println("Nuevo usuario registrado: " + u);
            } else {
                salida.println("Error registrando usuario");
            }
        }

        private void mostrarBandeja(BufferedReader entrada) throws IOException {
            boolean enBandeja = true;

            while (enBandeja) {
                List<String> mensajes = cargarMensajes(usuario);

                salida.println("╔════════════════════════════════╗");
                salida.println("║        BANDEJA DE ENTRADA      ║");
                salida.println("╚════════════════════════════════╝");

                if (mensajes.isEmpty()) {
                    salida.println("📭 No hay mensajes nuevos.");
                } else {
                    salida.println("📬 Mensajes:");
                    for (int i = 0; i < mensajes.size(); i++) {
                        salida.println((i + 1) + ". " + mensajes.get(i));
                    }
                }

                salida.println("\nOpciones:");
                salida.println(" - escribir 'actualizar' para refrescar la bandeja");
                salida.println(" - escribir 'eliminar <número>' para borrar un mensaje");
                salida.println(" - escribir 'salir' para volver al menú principal");
                salida.println(" - escribir 'menu' para ir al menú principal");

                String comando = entrada.readLine();
                if (comando == null) break;

                if (comando.trim().equalsIgnoreCase("salir") || comando.trim().equalsIgnoreCase("menu")) {
                    enBandeja = false;
                } else if (comando.trim().equalsIgnoreCase("actualizar")) {
                    salida.println("Bandeja actualizada...");
                } else if (comando.toLowerCase().startsWith("eliminar")) {
                    try {
                        String[] partes = comando.split(" ");
                        if (partes.length < 2) {
                            salida.println("Uso: eliminar <número>");
                            continue;
                        }
                        int index = Integer.parseInt(partes[1]) - 1;
                        if (eliminarMensaje(usuario, index)) {
                            salida.println("Mensaje eliminado.");
                        } else {
                            salida.println("Índice inválido o error al eliminar.");
                        }
                    } catch (NumberFormatException e) {
                        salida.println("Uso: eliminar <número>");
                    }
                } else {
                    salida.println("Comando no reconocido.");
                }
            }
        }

        private void enviarMensajeUsuario(BufferedReader entrada) throws IOException {
            salida.println("=== USUARIOS REGISTRADOS ===");
            List<String> todosUsuarios = obtenerTodosLosUsuarios();
            List<String> disponibles = new ArrayList<>();

            for (String u : todosUsuarios) {
                if (!u.equals(usuario)) {
                    disponibles.add(u);
                }
            }

            if (disponibles.isEmpty()) {
                salida.println("No hay otros usuarios registrados.");
                return;
            }

            for (int i = 0; i < disponibles.size(); i++) {
                String u = disponibles.get(i);
                String estado = clientes.containsKey(u) ? "(conectado)" : "(desconectado)";
                Set<String> bloqueados = usuariosBloqueados.get(usuario);
                String bloqueado = (bloqueados != null && bloqueados.contains(u)) ? " [BLOQUEADO]" : "";
                salida.println((i + 1) + ". " + u + " " + estado + bloqueado);
            }

            salida.println("Escribe el nombre del usuario al que quieres enviar un mensaje:");
            String destino = entrada.readLine();
            if (destino == null || destino.trim().isEmpty()) {
                salida.println("Usuario inválido.");
                return;
            }

            destino = destino.trim();

            if (!usuarioExisteEnArchivo(destino)) {
                salida.println("❌ Error: El usuario '" + destino + "' no existe.");
                return;
            }

            salida.println("Escribe tu mensaje:");
            String mensaje = entrada.readLine();
            if (mensaje == null || mensaje.trim().isEmpty()) {
                salida.println("Mensaje vacío. Cancelado.");
                return;
            }

            String mensajeFinal = "[" + usuario + "]: " + mensaje.trim();
            guardarMensaje(destino, mensajeFinal);
            salida.println("✅ Mensaje enviado a " + destino);
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
                salida.println("(Puedes escribir 'menu' para volver al menú principal)");

                while (intentos > 0 && !acertado) {
                    salida.println("Ingresa tu número:");
                    String resp = entrada.readLine();
                    if (resp == null) return;

                    if (resp.trim().equalsIgnoreCase("menu")) {
                        salida.println("Volviendo al menú principal...");
                        return;
                    }

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

                salida.println("¿Quieres jugar otra vez? (s/n) o 'menu' para volver:");
                String respuesta = entrada.readLine();

                if (respuesta == null || respuesta.trim().equalsIgnoreCase("n") || 
                    respuesta.trim().equalsIgnoreCase("menu")) {
                    seguirJugando = false;
                }
            }
        }

        private boolean esContrasenaSegura(String contrasena) {
            if (contrasena.length() < 6) return false;

            boolean tieneLetra = false;
            boolean tieneNumero = false;

            for (char c : contrasena.toCharArray()) {
                if (Character.isLetter(c)) tieneLetra = true;
                else if (Character.isDigit(c)) tieneNumero = true;
            }

            return tieneLetra && tieneNumero;
        }

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
                    String[] partes = linea.split(":");
                    if (partes.length > 0 && partes[0].equals(usuario)) {
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