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

    // NUEVA FUNCIONALIDAD: Crear archivo de lista de archivos para cada usuario
private static void crearListaArchivos(String usuario) {
    File directorioUsuario = new File("archivos/" + usuario);
    File archivoLista = new File("listas/" + usuario + "_archivos.txt");
    
    // Crear directorios si no existen
    directorioUsuario.mkdirs();
    archivoLista.getParentFile().mkdirs();
    
    try (PrintWriter pw = new PrintWriter(new FileWriter(archivoLista))) {
        pw.println("=== ARCHIVOS DE " + usuario.toUpperCase() + " ===");
        
        File[] archivos = directorioUsuario.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        
        if (archivos == null || archivos.length == 0) {
            pw.println("📭 No hay archivos .txt disponibles.");
        } else {
            pw.println("📂 Archivos .txt disponibles:");
            
            // Separar archivos originales y descargados
            java.util.List<File> archivosOriginales = new java.util.ArrayList<>();
            java.util.List<File> archivosDescargados = new java.util.ArrayList<>();
            
            for (File archivo : archivos) {
                if (archivo.getName().startsWith("de_")) {
                    archivosDescargados.add(archivo);
                } else {
                    archivosOriginales.add(archivo);
                }
            }
            
            int contador = 1;
            
            // Mostrar archivos originales primero
            if (!archivosOriginales.isEmpty()) {
                pw.println("\n📝 ARCHIVOS ORIGINALES:");
                for (File archivo : archivosOriginales) {
                    long bytes = archivo.length();
                    String tamaño = formatearTamaño(bytes);
                    pw.println(contador + ". 📄 " + archivo.getName() + " (" + tamaño + ")");
                    contador++;
                }
            }
            
            // Mostrar archivos descargados
            if (!archivosDescargados.isEmpty()) {
                pw.println("\n📥 ARCHIVOS DESCARGADOS:");
                for (File archivo : archivosDescargados) {
                    long bytes = archivo.length();
                    String tamaño = formatearTamaño(bytes);
                    
                    // Extraer información del propietario original del nombre
                    String propietarioOriginal = extraerPropietarioOriginal(archivo.getName());
                    
                    pw.println(contador + ". 📥 " + archivo.getName() + " (" + tamaño + ")");
                    pw.println("    └── Descargado de: " + propietarioOriginal);
                    contador++;
                }
            }
        }
        pw.println("\nPara descargar un archivo, solicita el nombre exacto con extensión .txt");
        pw.println("Los archivos descargados incluyen metadatos del propietario original");
    } catch (IOException e) {
        System.err.println("Error creando lista de archivos para " + usuario + ": " + e.getMessage());
    }
}
private static String formatearTamaño(long bytes) {
    if (bytes < 1024) return bytes + " bytes";
    if (bytes < 1048576) return (bytes/1024) + " KB";
    return (bytes/1048576) + " MB";
}
private static String extraerPropietarioOriginal(String nombreArchivo) {
    if (nombreArchivo.startsWith("de_")) {
        String sinPrefijo = nombreArchivo.substring(3); // Remover "de_"
        int siguienteGuion = sinPrefijo.indexOf('_');
        if (siguienteGuion > 0) {
            return sinPrefijo.substring(0, siguienteGuion);
        }
    }
    return "desconocido";
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

            // Eliminar archivos del usuario
            File directorioArchivos = new File("archivos/" + usuario);
            if (directorioArchivos.exists()) {
                eliminarDirectorio(directorioArchivos);
            }
            
            // Eliminar lista de archivos
            File archivoLista = new File("listas/" + usuario + "_archivos.txt");
            if (archivoLista.exists()) {
                archivoLista.delete();
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
    
    private static void eliminarDirectorio(File directorio) {
        File[] archivos = directorio.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.isDirectory()) {
                    eliminarDirectorio(archivo);
                } else {
                    archivo.delete();
                }
            }
        }
        directorio.delete();
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

    private static void mostrarUsuariosExpulsados() {
        System.out.println("\n=== USUARIOS EXPULSADOS ===");
        if (usuariosExpulsados.isEmpty()) {
            System.out.println("No hay usuarios expulsados.");
        } else {
            int count = 1;
            for (String usuario : usuariosExpulsados) {
                System.out.println(count + ". " + usuario + " (puede registrarse nuevamente)");
                count++;
            }
        }
        System.out.println();
    }

    private static void rehabilitarUsuario() {
        System.out.println("Esta funcionalidad no está disponible.");
        System.out.println("Los usuarios expulsados pueden registrarse nuevamente automáticamente.");
    }
  
 
private static void procesarComando(String comando) {
    // Verificar si es un comando de envío directo (formato: <usuario> <mensaje>)
    if (!comando.toLowerCase().equals("mensaje") && 
        !comando.toLowerCase().equals("ayuda") && 
        !comando.toLowerCase().equals("estado") && 
        !comando.toLowerCase().equals("clientes") && 
        !comando.toLowerCase().equals("usuarios") && 
        !comando.toLowerCase().equals("expulsados") && 
        !comando.toLowerCase().equals("expulsar") && 
        !comando.toLowerCase().equals("rehabilitar") && 
        !comando.toLowerCase().equals("parar") && 
        comando.contains(" ")) {
        
     
        enviarMensajeDirecto(comando);
        return;
    }

    switch (comando.toLowerCase()) {
        case "ayuda":
            System.out.println("\n=== COMANDOS DEL SERVIDOR ===");
            System.out.println("ayuda     - Ver comandos");
            System.out.println("estado    - Ver estado del servidor");
            System.out.println("clientes  - Ver clientes conectados");
            System.out.println("usuarios  - Ver usuarios registrados");
            System.out.println("expulsados- Ver usuarios que han sido expulsados");
            System.out.println("mensaje   - Ver usuarios disponibles para enviar mensaje");
            System.out.println("expulsar  - Expulsar y eliminar a un cliente (puede re-registrarse)");
            System.out.println("parar     - Cerrar servidor\n");
            break;

        case "mensaje":
            enviarMensajeACliente();
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
            System.out.println("Usuarios expulsados: " + usuariosExpulsados.size());
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

        case "expulsados":
            mostrarUsuariosExpulsados();
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
  private static void enviarMensajeDirecto(String comandoCompleto) {
    // Formato: <usuario> <mensaje>
    String[] partes = comandoCompleto.split(" ", 2);
    
    if (partes.length < 2) {
        System.out.println("Formato incorrecto. Uso: <usuario> <mensaje>");
        System.out.println("Ejemplo: juan Hola, este es un mensaje del administrador");
        return;
    }
    
    String usuario = partes[0].trim();
    String mensaje = partes[1].trim();
    
    if (usuario.isEmpty()) {
        System.out.println("Nombre de usuario vacío");
        return;
    }
    
    if (mensaje.isEmpty()) {
        System.out.println("Mensaje vacío");
        return;
    }
    
    // Verificar que el usuario existe
    if (!usuarioExisteEnArchivo(usuario)) {
        System.out.println("Error: El usuario '" + usuario + "' no existe");
        return;
    }
    
    // Enviar el mensaje
    guardarMensaje(usuario, "[ADMIN]: " + mensaje);
    
    // Mostrar confirmación
    if (clientes.containsKey(usuario)) {
        System.out.println("✅ Mensaje enviado en tiempo real a '" + usuario + "' (conectado)");
    } else {
        System.out.println("✅ Mensaje guardado para '" + usuario + "' (desconectado)");
    }
}

// Método para enviar mensajes mostrando usuarios primero
private static void enviarMensajeACliente() {
    // Obtener todos los usuarios registrados
    List<String> todosUsuarios = obtenerTodosLosUsuarios();
    
    if (todosUsuarios.isEmpty()) {
        System.out.println("No hay usuarios registrados");
        return;
    }

    System.out.println("\n=== USUARIOS DISPONIBLES ===");
    for (int i = 0; i < todosUsuarios.size(); i++) {
        String usuario = todosUsuarios.get(i);
        String estado = clientes.containsKey(usuario) ? "(conectado)" : "(desconectado)";
        System.out.println((i + 1) + ". " + usuario + " " + estado);
    }
    
    System.out.println("\nPara enviar un mensaje, escribe:");
    System.out.println("<nombre> <mensaje>");
   
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
                                    // Crear lista de archivos al loguearse
                                    crearListaArchivos(usuario);
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
                            case "4":
                                gestionarBloqueos(entrada);
                                break;
                         case "5":
    explorarArchivos(entrada);
    break;
    
                        
  case "6":  // NUEVA OPCIÓN
        gestionarMisArchivos(entrada);
        break;
    
    
case "7": 
    salida.println("Cerrando sesión. Hasta luego " + usuario);
    logueado = false;
    break;
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
    salida.println("5. Explorar archivos de otros usuarios");
    salida.println("6. Gestionar mis archivos");
    salida.println("7. Cerrar sesión");
    salida.println("Seleccione opción (1-7):");

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
        
        // Crear archivos de ejemplo si no existen
        crearArchivosEjemplo(usuario);
        // Crear lista de archivos al loguearse
        crearListaArchivos(usuario);
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

    usuariosExpulsados.remove(u);
    
    // Crear archivos de ejemplo para el nuevo usuario
    crearArchivosEjemplo(u);
    new File("listas").mkdirs();
    
    salida.println("Usuario registrado correctamente");
    System.out.println("Nuevo usuario registrado: " + u);
} else {
    salida.println("Error registrando usuario");
}}

        private void gestionarBloqueos(BufferedReader entrada) throws IOException {
            boolean gestionando = true;

            while (gestionando) {
                salida.println("=== GESTIONAR BLOQUEOS ===");
                salida.println("1. Ver usuarios bloqueados");
                salida.println("2. Bloquear usuario");
                salida.println("3. Desbloquear usuario");
                salida.println("4. Vol al menú principal");
                salida.println("Seleccione opción (1-4):");

                String opcion = entrada.readLine();
                if (opcion == null) break;

                switch (opcion.trim()) {
                    case "1":
                        mostrarUsuariosBloqueados();
                        break;
                    case "2":
                        bloquearUsuario(entrada);
                        break;
                    case "3":
                        desbloquearUsuario(entrada);
                        break;
                    case "4":
                        gestionando = false;
                        break;
                    default:
                        salida.println("Opción inválida. Seleccione 1, 2, 3 o 4.");
                }
            }
        }

        private void mostrarUsuariosBloqueados() {
            Set<String> bloqueados = usuariosBloqueados.get(usuario);
            salida.println("=== USUARIOS BLOQUEADOS ===");
            if (bloqueados == null || bloqueados.isEmpty()) {
                salida.println("No tienes usuarios bloqueados.");
            } else {
                int i = 1;
                for (String usuarioBloqueado : bloqueados) {
                    salida.println(i + ". " + usuarioBloqueado);
                    i++;
                }
            }
        }

        private void bloquearUsuario(BufferedReader entrada) throws IOException {
            List<String> todosUsuarios = obtenerTodosLosUsuarios();
            List<String> usuariosDisponibles = new ArrayList<>();

            for (String u : todosUsuarios) {
                if (!u.equals(usuario)) {
                    usuariosDisponibles.add(u);
                }
            }

            if (usuariosDisponibles.isEmpty()) {
                salida.println("No hay otros usuarios para bloquear.");
                return;
            }

            salida.println("=== USUARIOS DISPONIBLES PARA BLOQUEAR ===");
            for (int i = 0; i < usuariosDisponibles.size(); i++) {
                String u = usuariosDisponibles.get(i);
                String estado = clientes.containsKey(u) ? "(conectado)" : "(desconectado)";
                salida.println((i + 1) + ". " + u + " " + estado);
            }

            salida.println("Ingrese el nombre del usuario a bloquear:");
            String usuarioABloquear = entrada.readLine();
            if (usuarioABloquear == null || usuarioABloquear.trim().isEmpty()) {
                salida.println("Nombre de usuario vacío.");
                return;
            }

            usuarioABloquear = usuarioABloquear.trim();

            if (!usuarioExisteEnArchivo(usuarioABloquear)) {
                salida.println("❌ Error: El usuario '" + usuarioABloquear + "' no existe.");
                return;
            }

            if (usuarioABloquear.equals(usuario)) {
                salida.println("No puedes bloquearte a ti mismo.");
                return;
            }

            usuariosBloqueados.computeIfAbsent(usuario, k -> ConcurrentHashMap.newKeySet()).add(usuarioABloquear);
            salida.println("✅ Usuario '" + usuarioABloquear + "' bloqueado correctamente.");
        }

 
private void desbloquearUsuario(BufferedReader entrada) throws IOException {
            Set<String> bloqueados = usuariosBloqueados.get(usuario);
            if (bloqueados == null || bloqueados.isEmpty()) {
                salida.println("No tienes usuarios bloqueados.");
                return;
            }

            salida.println("=== USUARIOS BLOQUEADOS ===");
            int i = 1;
            for (String usuarioBloqueado : bloqueados) {
                salida.println(i + ". " + usuarioBloqueado);
                i++;
            }

            salida.println("Ingrese el nombre del usuario a desbloquear:");
            String usuarioADesbloquear = entrada.readLine();
            if (usuarioADesbloquear == null || usuarioADesbloquear.trim().isEmpty()) {
                salida.println("Nombre de usuario vacío.");
                return;
            }

            usuarioADesbloquear = usuarioADesbloquear.trim();

            if (bloqueados.remove(usuarioADesbloquear)) {
                salida.println("✅ Usuario '" + usuarioADesbloquear + "' desbloqueado correctamente.");
            } else {
                salida.println("❌ El usuario '" + usuarioADesbloquear + "' no estaba bloqueado.");
            }
        }

      private void mostrarBandeja(BufferedReader entrada) throws IOException {
            boolean enBandeja = true;
            int paginaActual = 1;
            final int MENSAJES_POR_PAGINA = 10;

            while (enBandeja) {
                List<String> mensajes = cargarMensajes(usuario);
                int totalMensajes = mensajes.size();
                int totalPaginas = (totalMensajes + MENSAJES_POR_PAGINA - 1) / MENSAJES_POR_PAGINA;

                if (totalMensajes == 0) {
                    salida.println("=== BANDEJA DE ENTRADA ===");
                    salida.println("📭 No tienes mensajes");
                    salida.println("\nComandos disponibles:");
                    salida.println("- 'menu' o 'salir' para volver al menú principal");
                    salida.println("- 'actualizar' para revisar nuevos mensajes");
                } else {
                    salida.println("=== BANDEJA DE ENTRADA ===");
                    salida.println("📧 Tienes " + totalMensajes + " mensaje(s) - Página " + paginaActual + "/" + totalPaginas);
                    salida.println("");

                    int inicio = (paginaActual - 1) * MENSAJES_POR_PAGINA;
                    int fin = Math.min(inicio + MENSAJES_POR_PAGINA, totalMensajes);

                    for (int i = inicio; i < fin; i++) {
                        salida.println((i + 1) + ". " + mensajes.get(i));
                    }

                    salida.println("\nComandos disponibles:");
                    if (paginaActual > 1) salida.println("- 'anterior' para página anterior");
                    if (paginaActual < totalPaginas) salida.println("- 'siguiente' para página siguiente");
                    salida.println("- 'pagina N' para ir a página específica");
                    salida.println("- 'eliminar N' para eliminar mensaje número N");
                    salida.println("- 'actualizar' para revisar nuevos mensajes");
                    salida.println("- 'menu' o 'salir' para volver al menú principal");
                }

                String comando = entrada.readLine();
                if (comando == null) break;

                comando = comando.trim().toLowerCase();

                if (comando.equals("menu") || comando.equals("salir")) {
                    enBandeja = false;
                } else if (comando.equals("actualizar")) {
                    salida.println("✅ Bandeja actualizada");
                } else if (comando.equals("siguiente") && paginaActual < totalPaginas) {
                    paginaActual++;
                    salida.println("➡️ Página " + paginaActual);
                } else if (comando.equals("anterior") && paginaActual > 1) {
                    paginaActual--;
                    salida.println("⬅️ Página " + paginaActual);
                } else if (comando.startsWith("pagina ")) {
                    try {
                        int nuevaPagina = Integer.parseInt(comando.substring(7).trim());
                        if (nuevaPagina >= 1 && nuevaPagina <= totalPaginas) {
                            paginaActual = nuevaPagina;
                            salida.println("📄 Página " + paginaActual);
                        } else {
                            salida.println("❌ Página fuera de rango (1-" + totalPaginas + ")");
                        }
                    } catch (NumberFormatException e) {
                        salida.println("❌ Número de página inválido");
                    }
                } else if (comando.startsWith("eliminar ")) {
                    try {
                        int numeroMensaje = Integer.parseInt(comando.substring(9).trim());
                        if (eliminarMensaje(usuario, numeroMensaje - 1)) {
                            salida.println("✅ Mensaje eliminado correctamente");
                            // Ajustar página si es necesario
                            mensajes = cargarMensajes(usuario);
                            totalPaginas = (mensajes.size() + MENSAJES_POR_PAGINA - 1) / MENSAJES_POR_PAGINA;
                            if (paginaActual > totalPaginas && totalPaginas > 0) {
                                paginaActual = totalPaginas;
                            }
                        } else {
                            salida.println("❌ Error eliminando mensaje o número inválido");
                        }
                    } catch (NumberFormatException e) {
                        salida.println("❌ Número de mensaje inválido");
                    }
                } else {
                    salida.println("❌ Comando no reconocido");
                }
            }
        }


            private void juegoAdivinaNumero(BufferedReader entrada) throws IOException { 
    boolean jugando = true;

    while (jugando) {
        int numeroSecreto = (int) (Math.random() * 10) + 1; // Genera un número entre 1 y 10
        int intentos = 0;
        int maxIntentos = 3; // Número máximo de intentos permitidos
        boolean adivinado = false;

        salida.println("=== JUEGO: ADIVINA EL NÚMERO ===");
        salida.println("🎯 He pensado un número entre 1 y 10"); // Indica el rango del número secreto
        salida.println("🎲 Tienes " + maxIntentos + " intentos para adivinarlo");
        salida.println("💡 Escribe 'menu' para volver al menú principal\n");

        while (!adivinado && intentos < maxIntentos) {
            salida.println("Intento " + (intentos + 1) + "/" + maxIntentos);
            salida.println("Ingresa tu número:");

            String respuesta = entrada.readLine();
            if (respuesta == null) return;

            if (respuesta.trim().equalsIgnoreCase("menu")) {
                return; // Permite volver al menú principal
            }

            try {
                int numero = Integer.parseInt(respuesta.trim());
                
                // Validar que el número esté en el rango permitido
                if (numero < 1 || numero > 10) {
                    salida.println("❌ Por favor ingresa un número entre 1 y 10");
                    continue; // No cuenta como intento
                }
                
                intentos++;

                if (numero == numeroSecreto) {
                    salida.println("🎉 ¡FELICIDADES! Adivinaste el número " + numeroSecreto);
                    salida.println("✨ Lo lograste en " + intentos + " intento(s)");
                    adivinado = true;
                } else if (numero < numeroSecreto) {
                    salida.println("📈 El número es MAYOR que " + numero);
                } else {
                    salida.println("📉 El número es MENOR que " + numero);
                }

                if (!adivinado && intentos == maxIntentos) {
                    salida.println("😔 Se acabaron los intentos. El número era: " + numeroSecreto);
                }

            } catch (NumberFormatException e) {
                salida.println("❌ Por favor ingresa un número válido entre 1 y 10");
                // No cuenta como intento si el valor ingresado no es un número
            }
        }

        salida.println("¿Quieres jugar otra vez? (s/n) o 'menu' para volver:");
        String continuar = entrada.readLine();
        if (continuar == null || 
            continuar.trim().equalsIgnoreCase("n") || 
            continuar.trim().equalsIgnoreCase("menu")) {
            jugando = false; // Finaliza el juego
        }
    }
}
        private void enviarMensajeUsuario(BufferedReader entrada) throws IOException {
            List<String> todosUsuarios = obtenerTodosLosUsuarios();
            List<String> usuariosDisponibles = new ArrayList<>();

            // Filtrar usuarios (no incluir el propio usuario)
            for (String u : todosUsuarios) {
                if (!u.equals(usuario)) {
                    usuariosDisponibles.add(u);
                }
            }

            if (usuariosDisponibles.isEmpty()) {
                salida.println("❌ No hay otros usuarios registrados en el sistema.");
                return;
            }

            salida.println("=== USUARIOS DISPONIBLES ===");
            for (int i = 0; i < usuariosDisponibles.size(); i++) {
                String u = usuariosDisponibles.get(i);
                String estado = clientes.containsKey(u) ? "(conectado)" : "(desconectado)";
                salida.println((i + 1) + ". " + u + " " + estado);
            }

            salida.println("\nEscribe el nombre del usuario destinatario:");
            String destinatario = entrada.readLine();
            if (destinatario == null || destinatario.trim().isEmpty()) {
                salida.println("❌ Nombre de destinatario vacío");
                return;
            }

            destinatario = destinatario.trim();

            // Verificar que el destinatario existe
            if (!usuarioExisteEnArchivo(destinatario)) {
                salida.println("❌ El usuario '" + destinatario + "' no existe");
                return;
            }

            // Verificar que no es él mismo
            if (destinatario.equals(usuario)) {
                salida.println("❌ No puedes enviarte mensajes a ti mismo");
                return;
            }

            // Verificar si el usuario actual está bloqueado por el destinatario
            Set<String> bloqueadosPorDestinatario = usuariosBloqueados.get(destinatario);
            if (bloqueadosPorDestinatario != null && bloqueadosPorDestinatario.contains(usuario)) {
                salida.println("❌ No puedes enviar mensajes a " + destinatario + " (te ha bloqueado)");
                return;
            }

            salida.println("✅ Destinatario válido: " + destinatario);
            salida.println("Escribe tu mensaje:");

            String mensaje = entrada.readLine();
            if (mensaje == null || mensaje.trim().isEmpty()) {
                salida.println("❌ Mensaje vacío");
                return;
            }

            // Guardar el mensaje
            guardarMensaje(destinatario, "De " + usuario + ": " + mensaje);
            salida.println("✅ Mensaje enviado correctamente a " + destinatario);
        }

    private void explorarArchivos(BufferedReader entrada) throws IOException {
    // Obtener usuarios conectados (excluyendo al usuario actual)
    List<String> usuariosConectados = new ArrayList<>();
    for (String u : clientes.keySet()) {
        if (!u.equals(usuario)) {
            usuariosConectados.add(u);
        }
    }

    if (usuariosConectados.isEmpty()) {
        salida.println("❌ No hay otros usuarios conectados actualmente.");
        salida.println("🔙 Regresando al menú principal...");
        return;
    }

    boolean explorando = true;
    while (explorando) {
        salida.println("=== EXPLORAR ARCHIVOS DE OTROS USUARIOS ===");
        salida.println("👥 Usuarios conectados disponibles:");

        for (int i = 0; i < usuariosConectados.size(); i++) {
            salida.println((i + 1) + ". " + usuariosConectados.get(i));
        }

        salida.println("0. Volver al menú principal");
        salida.println("Seleccione el número del usuario:");

        String opcion = entrada.readLine();
        if (opcion == null) break;

        if (opcion.trim().equals("0")) {
            salida.println("🔙 Regresando al menú principal...");
            explorando = false;
            return; // Salir completamente del método
        }

        try {
            int index = Integer.parseInt(opcion.trim()) - 1;
            if (index >= 0 && index < usuariosConectados.size()) {
                String propietario = usuariosConectados.get(index);
                
                // Solicitar autorización (por ahora automática)
                // solicitarAutorizacionArchivos(usuario, propietario);
                
                mostrarArchivosUsuario(propietario, entrada);
                
                // Después de mostrar archivos, volver a mostrar la lista de usuarios
                // No salir del bucle aquí
            } else {
                salida.println("❌ Número fuera de rango");
                salida.println("Presione Enter para continuar...");
                entrada.readLine();
            }
        } catch (NumberFormatException e) {
            salida.println("❌ Por favor ingrese un número válido");
            salida.println("Presione Enter para continuar...");
            entrada.readLine();
        }
    }
}

     private void mostrarArchivosUsuario(String propietario, BufferedReader entrada) throws IOException {
    File archivoLista = new File("listas/" + propietario + "_archivos.txt");
    
    if (!archivoLista.exists()) {
        crearListaArchivos(propietario);
    }

    // Mostrar la lista de archivos
    try (BufferedReader br = new BufferedReader(new FileReader(archivoLista))) {
        String linea;
        salida.println("\n" + "=".repeat(50));
        while ((linea = br.readLine()) != null) {
            salida.println(linea);
        }
        salida.println("=".repeat(50));
    } catch (IOException e) {
        salida.println("❌ Error leyendo la lista de archivos de " + propietario);
    }

    boolean navegando = true;
    while (navegando) {
        salida.println("\n🔽 OPCIONES:");
        salida.println("1. Descargar un archivo");
        salida.println("2. Actualizar lista de archivos");
        salida.println("0. Volver a la lista de usuarios");
        salida.println("Seleccione una opción:");

        String opcion = entrada.readLine();
        if (opcion == null) break;

        switch (opcion.trim()) {
            case "0":
                salida.println("🔙 Volviendo a la lista de usuarios...");
                navegando = false;
                return; // Volver al método explorarArchivos
            case "1":
                descargarArchivo(propietario, entrada);
                break;
            case "2":
                crearListaArchivos(propietario);
                salida.println("✅ Lista actualizada");
                // Mostrar la lista actualizada
                try (BufferedReader br = new BufferedReader(new FileReader(archivoLista))) {
                    String linea;
                    salida.println("\n" + "=".repeat(50));
                    while ((linea = br.readLine()) != null) {
                        salida.println(linea);
                    }
                    salida.println("=".repeat(50));
                }
                break;
            default:
                salida.println("❌ Opción inválida");
        }
    }
}
      private void descargarArchivo(String propietario, BufferedReader entrada) throws IOException {
    salida.println("📎 Ingresa el nombre exacto del archivo (con extensión .txt):");
    String nombreArchivo = entrada.readLine();
    
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
        salida.println("❌ Nombre de archivo vacío");
        return;
    }

    nombreArchivo = nombreArchivo.trim();
    
    if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
        salida.println("❌ Solo se pueden descargar archivos .txt");
        return;
    }

    File archivo = new File("archivos/" + propietario + "/" + nombreArchivo);
    
    if (!archivo.exists()) {
        salida.println("❌ El archivo '" + nombreArchivo + "' no existe");
        return;
    }

    salida.println("📄 Archivo encontrado: " + nombreArchivo);
    salida.println("📊 Tamaño: " + archivo.length() + " bytes");
    salida.println("¿Confirma la descarga? (s/n):");

    String confirmacion = entrada.readLine();
    if (confirmacion != null && confirmacion.trim().toLowerCase().startsWith("s")) {
        
        // Copiar archivo manteniendo el nombre original
        File directorioDestino = new File("archivos/" + usuario);
        directorioDestino.mkdirs();
        
        File archivoDestino = new File(directorioDestino, nombreArchivo);
        
        // Si ya existe el archivo, preguntar qué hacer
        if (archivoDestino.exists()) {
            salida.println("⚠️ Ya tienes un archivo con el nombre '" + nombreArchivo + "'");
            salida.println("¿Quieres sobrescribirlo? (s/n):");
            String sobrescribir = entrada.readLine();
            if (sobrescribir == null || !sobrescribir.trim().toLowerCase().startsWith("s")) {
                salida.println("❌ Descarga cancelada");
                return;
            }
        }
        
        try {
            // Copiar el contenido con información de fuente
            copiarArchivoConFuente(archivo, archivoDestino, propietario);
            
            // Mostrar el contenido como antes
            salida.println("\n" + "=".repeat(60));
            salida.println("📁 CONTENIDO DEL ARCHIVO: " + nombreArchivo);
            salida.println("👤 PROPIETARIO: " + propietario);
            salida.println("=".repeat(60));

            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
                String linea;
                int numeroLinea = 1;
                while ((linea = br.readLine()) != null) {
                    salida.println(String.format("%3d | %s", numeroLinea, linea));
                    numeroLinea++;
                }
            } catch (IOException e) {
                salida.println("❌ Error leyendo el archivo: " + e.getMessage());
            }

            salida.println("=".repeat(60));
            salida.println("✅ Descarga completada");
            salida.println("📁 Archivo guardado como: " + nombreArchivo);
            salida.println("💾 El archivo ahora está disponible en 'Gestionar mis archivos'");
            
        } catch (IOException e) {
            salida.println("❌ Error copiando el archivo: " + e.getMessage());
        }
        
    } else {
        salida.println("❌ Descarga cancelada");
    }
}
private void copiarArchivoConFuente(File origen, File destino, String propietarioOriginal) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(origen));
         PrintWriter pw = new PrintWriter(new FileWriter(destino))) {
        
        // Agregar cabecera con información de descarga al inicio
        pw.println("================================================================");
        pw.println("ARCHIVO DESCARGADO DE: " + propietarioOriginal);
        pw.println("DESCARGADO POR: " + usuario);
        pw.println("FECHA: " + java.time.LocalDateTime.now().toString());
        pw.println("================================================================");
        pw.println("");
        
        // Copiar contenido original sin modificaciones
        String linea;
        while ((linea = br.readLine()) != null) {
            pw.println(linea);
        }
        
        pw.println("");
        pw.println("================================================================");
        pw.println("FIN DEL ARCHIVO DESCARGADO DE: " + propietarioOriginal);
        pw.println("================================================================");
    }
}
private String generarNombreUnico(String propietario, String nombreOriginal) {
    String nombreBase = nombreOriginal;
    String extension = "";
    
    // Separar nombre y extensión
    int puntoIndex = nombreOriginal.lastIndexOf('.');
    if (puntoIndex > 0) {
        nombreBase = nombreOriginal.substring(0, puntoIndex);
        extension = nombreOriginal.substring(puntoIndex);
    }
    
    // Agregar prefijo con el nombre del propietario
    String nombreNuevo = "de_" + propietario + "_" + nombreBase + extension;
    
    // Verificar si ya existe y agregar número si es necesario
    File directorioUsuario = new File("archivos/" + usuario);
    File archivoTest = new File(directorioUsuario, nombreNuevo);
    
    int contador = 1;
    while (archivoTest.exists()) {
        nombreNuevo = "de_" + propietario + "_" + nombreBase + "_" + contador + extension;
        archivoTest = new File(directorioUsuario, nombreNuevo);
        contador++;
    }
    
    return nombreNuevo;
}
private void copiarArchivo(File origen, File destino, String propietarioOriginal) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(origen));
         PrintWriter pw = new PrintWriter(new FileWriter(destino))) {
        
        // Agregar cabecera con información de descarga
        pw.println("# ================================================================");
        pw.println("# ARCHIVO DESCARGADO");
        pw.println("# Propietario original: " + propietarioOriginal);
        pw.println("# Descargado por: " + usuario);
        pw.println("# Fecha de descarga: " + java.time.LocalDateTime.now().toString());
        pw.println("# Archivo original: " + origen.getName());
        pw.println("# ================================================================");
        pw.println("");
        pw.println("--- CONTENIDO ORIGINAL ---");
        pw.println("");
        
        // Copiar contenido original
        String linea;
        while ((linea = br.readLine()) != null) {
            pw.println(linea);
        }
        
        pw.println("");
        pw.println("--- FIN DEL CONTENIDO ORIGINAL ---");
        pw.println("# Descarga completada exitosamente");
    }
}
 private void gestionarMisArchivos(BufferedReader entrada) throws IOException {
    boolean gestionando = true;

    while (gestionando) {
        // Crear lista actualizada de archivos SIEMPRE que se entre al menú
        crearListaArchivos(usuario);
        
        // Mostrar archivos del usuario
        File archivoLista = new File("listas/" + usuario + "_archivos.txt");
        if (archivoLista.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(archivoLista))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    salida.println(linea);
                }
            }
        } else {
            salida.println("❌ No se pudo acceder a la lista de archivos");
        }

        salida.println("\n=== GESTIONAR MIS ARCHIVOS ===");
        salida.println("1. Ver contenido de un archivo");
        salida.println("2. Editar un archivo");
        salida.println("3. Crear nuevo archivo");
        salida.println("4. Eliminar un archivo");
        salida.println("5. Actualizar lista de archivos");
        salida.println("6. Ver información de archivo descargado");
        salida.println("0. Volver al menú principal");
        salida.println("Seleccione una opción:");

        String opcion = entrada.readLine();
        if (opcion == null) break;

        switch (opcion.trim()) {
            case "0":
                gestionando = false;
                break;
            case "1":
                verArchivo(entrada);
                break;
            case "2":
                editarArchivo(entrada);
                String respuestaEditar = entrada.readLine();
                if (respuestaEditar != null && respuestaEditar.trim().toLowerCase().equals("salir")) {
                    gestionando = false;
                }
                break;
            case "3":
                crearNuevoArchivo(entrada);
                String respuestaCrear = entrada.readLine();
                if (respuestaCrear != null && respuestaCrear.trim().toLowerCase().equals("salir")) {
                    gestionando = false;
                }
                break;
            case "4":
                eliminarArchivo(entrada);
                String respuestaEliminar = entrada.readLine();
                if (respuestaEliminar != null && respuestaEliminar.trim().toLowerCase().equals("salir")) {
                    gestionando = false;
                }
                break;
            case "5":
                // Actualizar lista explícitamente
                crearListaArchivos(usuario);
                salida.println("✅ Lista de archivos actualizada");
                salida.println("📊 Se han detectado todos los archivos, incluidos los descargados recientemente");
                break;
            case "6":
                verInformacionArchivoDescargado(entrada);
                break;
            default:
                salida.println("❌ Opción inválida. Seleccione 0-6.");
                break;
        }
    }
}
private void verInformacionArchivoDescargado(BufferedReader entrada) throws IOException {
    salida.println("📋 Ingresa el nombre del archivo descargado (con extensión .txt):");
    String nombreArchivo = entrada.readLine();
    
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
        salida.println("❌ Nombre de archivo vacío");
        return;
    }

    nombreArchivo = nombreArchivo.trim();
    if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
        salida.println("❌ Solo se pueden consultar archivos .txt");
        return;
    }

    File archivo = new File("archivos/" + usuario + "/" + nombreArchivo);
    if (!archivo.exists()) {
        salida.println("❌ El archivo '" + nombreArchivo + "' no existe");
        return;
    }

    // Leer las primeras líneas para extraer metadatos
    try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
        String linea;
        boolean esArchivoDescargado = false;
        String propietarioOriginal = "";
        String fechaDescarga = "";
        String archivoOriginal = "";
        String descargadoPor = "";
        
        // Leer cabecera de metadatos
        while ((linea = br.readLine()) != null) {
            if (linea.startsWith("# ARCHIVO DESCARGADO")) {
                esArchivoDescargado = true;
            } else if (linea.startsWith("# Propietario original:")) {
                propietarioOriginal = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("# Descargado por:")) {
                descargadoPor = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("# Fecha de descarga:")) {
                fechaDescarga = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("# Archivo original:")) {
                archivoOriginal = linea.substring(linea.indexOf(":") + 1).trim();
            } else if (linea.startsWith("--- CONTENIDO ORIGINAL ---")) {
                break; // Parar de leer metadatos
            }
        }
        
        if (esArchivoDescargado) {
            salida.println("\n" + "=".repeat(50));
            salida.println("📥 INFORMACIÓN DE ARCHIVO DESCARGADO");
            salida.println("=".repeat(50));
            salida.println("📁 Archivo actual: " + nombreArchivo);
            salida.println("📄 Archivo original: " + archivoOriginal);
            salida.println("👤 Propietario original: " + propietarioOriginal);
            salida.println("👤 Descargado por: " + descargadoPor);
            salida.println("📅 Fecha de descarga: " + fechaDescarga);
            salida.println("📊 Tamaño actual: " + archivo.length() + " bytes");
            salida.println("=".repeat(50));
        } else {
            salida.println("📄 Este archivo no es un archivo descargado.");
            salida.println("💡 Es un archivo original creado por ti.");
        }
        
    } catch (IOException e) {
        salida.println("❌ Error leyendo información del archivo: " + e.getMessage());
    }
}
private void verArchivo(BufferedReader entrada) throws IOException {
    salida.println("📄 Ingresa el nombre del archivo (con extensión .txt):");
    String nombreArchivo = entrada.readLine();
    
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
        salida.println("❌ Nombre de archivo vacío");
        return;
    }

    nombreArchivo = nombreArchivo.trim();
    if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
        salida.println("❌ Solo se pueden gestionar archivos .txt");
        return;
    }

    File archivo = new File("archivos/" + usuario + "/" + nombreArchivo);
    if (!archivo.exists()) {
        salida.println("❌ El archivo '" + nombreArchivo + "' no existe");
        return;
    }

    // Bucle para el menú de visualización
    boolean viendoArchivo = true;
    while (viendoArchivo) {
        // Mostrar contenido del archivo
        salida.println("\n" + "=".repeat(60));
        salida.println("📁 CONTENIDO DEL ARCHIVO: " + nombreArchivo);
        salida.println("=".repeat(60));

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numeroLinea = 1;
            while ((linea = br.readLine()) != null) {
                salida.println(String.format("%3d | %s", numeroLinea, linea));
                numeroLinea++;
            }
        } catch (IOException e) {
            salida.println("❌ Error leyendo el archivo: " + e.getMessage());
        }

        salida.println("=".repeat(60));
        
        // Menú de opciones para el archivo
        salida.println("\n=== OPCIONES DE VISUALIZACIÓN ===");
        salida.println("1. Ver archivo nuevamente");
        salida.println("2. Ver otro archivo");
        salida.println("0. Volver al menú de gestión de archivos");
        salida.println("Selecciona una opción:");
        
        String opcionVer = entrada.readLine();
        if (opcionVer == null) break;
        
        switch (opcionVer.trim()) {
            case "0":
                viendoArchivo = false;
                return; // Volver al menú de gestión
            case "1":
                // Continuar el bucle para mostrar el archivo nuevamente
                break;
            case "2":
                // Pedir nuevo nombre de archivo
                salida.println("📄 Ingresa el nombre del nuevo archivo (con extensión .txt):");
                String nuevoArchivo = entrada.readLine();
                
                if (nuevoArchivo == null || nuevoArchivo.trim().isEmpty()) {
                    salida.println("❌ Nombre de archivo vacío");
                    continue;
                }
                
                nuevoArchivo = nuevoArchivo.trim();
                if (!nuevoArchivo.toLowerCase().endsWith(".txt")) {
                    salida.println("❌ Solo se pueden gestionar archivos .txt");
                    continue;
                }
                
                File nuevoArchivoFile = new File("archivos/" + usuario + "/" + nuevoArchivo);
                if (!nuevoArchivoFile.exists()) {
                    salida.println("❌ El archivo '" + nuevoArchivo + "' no existe");
                    continue;
                }
                
                // Cambiar al nuevo archivo
                nombreArchivo = nuevoArchivo;
                archivo = nuevoArchivoFile;
                break;
            default:
                salida.println("❌ Opción inválida. Seleccione 0, 1 o 2.");
                break;
        }
    }
}
// MÉTODO CORREGIDO para editarArchivo
private void editarArchivo(BufferedReader entrada) throws IOException {
    salida.println("✏️ Ingresa el nombre del archivo a editar (con extensión .txt):");
    String nombreArchivo = entrada.readLine();
    
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
        salida.println("❌ Nombre de archivo vacío");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    nombreArchivo = nombreArchivo.trim();
    if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
        salida.println("❌ Solo se pueden editar archivos .txt");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    File archivo = new File("archivos/" + usuario + "/" + nombreArchivo);
    if (!archivo.exists()) {
        salida.println("❌ El archivo '" + nombreArchivo + "' no existe");
        salida.println("¿Quieres crearlo? (s/n):");
        String crear = entrada.readLine();
        if (crear == null || !crear.trim().toLowerCase().startsWith("s")) {
            salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
            return;
        }
    }

    // Mostrar contenido actual si existe
    if (archivo.exists()) {
        salida.println("\n📖 CONTENIDO ACTUAL:");
        salida.println("-".repeat(40));
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numeroLinea = 1;
            while ((linea = br.readLine()) != null) {
                salida.println(String.format("%2d | %s", numeroLinea, linea));
                numeroLinea++;
            }
        } catch (IOException e) {
            salida.println("❌ Error leyendo el archivo");
        }
        salida.println("-".repeat(40));
    }

    salida.println("\n✏️ MODO EDICIÓN - '" + nombreArchivo + "'");
    salida.println("📝 Escribe el nuevo contenido línea por línea.");
    salida.println("💡 Para terminar, escribe una línea que contenga solo: FIN");
    salida.println("⚠️ El contenido anterior será reemplazado completamente.");
    salida.println("");

    try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
        String linea;
        int numeroLinea = 1;
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.trim().equalsIgnoreCase("FIN")) {
                break;
            }
            pw.println(linea);
            salida.println(String.format("✓ Línea %d guardada", numeroLinea));
            numeroLinea++;
        }
        
        salida.println("\n✅ Archivo '" + nombreArchivo + "' guardado exitosamente");
        salida.println("📊 Total de líneas: " + (numeroLinea - 1));
        
    } catch (IOException e) {
        salida.println("❌ Error guardando el archivo: " + e.getMessage());
    }

    salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
}


private void crearNuevoArchivo(BufferedReader entrada) throws IOException {
    salida.println("📝 Ingresa el nombre del nuevo archivo (sin extensión):");
    String nombreArchivo = entrada.readLine();
    
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
        salida.println("❌ Nombre de archivo vacío");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    nombreArchivo = nombreArchivo.trim();
    if (!nombreArchivo.endsWith(".txt")) {
        nombreArchivo += ".txt";
    }

    // Validar nombre de archivo
    if (nombreArchivo.contains("/") || nombreArchivo.contains("\\") || 
        nombreArchivo.contains(":") || nombreArchivo.contains("*") ||
        nombreArchivo.contains("?") || nombreArchivo.contains("<") ||
        nombreArchivo.contains(">") || nombreArchivo.contains("|")) {
        salida.println("❌ Nombre de archivo inválido. No uses caracteres especiales.");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    File archivo = new File("archivos/" + usuario + "/" + nombreArchivo);
    if (archivo.exists()) {
        salida.println("⚠️ El archivo '" + nombreArchivo + "' ya existe");
        salida.println("¿Quieres sobrescribirlo? (s/n):");
        String sobrescribir = entrada.readLine();
        if (sobrescribir == null || !sobrescribir.trim().toLowerCase().startsWith("s")) {
            return;
        }
    }

    salida.println("\n✏️ CREAR ARCHIVO - '" + nombreArchivo + "'");
    salida.println("📝 Escribe el contenido línea por línea.");
    salida.println("💡 Para terminar, escribe una línea que contenga solo: FIN");
    salida.println("");

    try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
        String linea;
        int numeroLinea = 1;
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.trim().equalsIgnoreCase("FIN")) {
                break;
            }
            pw.println(linea);
            salida.println(String.format("✓ Línea %d guardada", numeroLinea));
            numeroLinea++;
        }
        
        salida.println("\n✅ Archivo '" + nombreArchivo + "' creado exitosamente");
        salida.println("📊 Total de líneas: " + (numeroLinea - 1));
        
    } catch (IOException e) {
        salida.println("❌ Error creando el archivo: " + e.getMessage());
    }

    salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
}

private void eliminarArchivo(BufferedReader entrada) throws IOException {
    salida.println("🗑️ Ingresa el nombre del archivo a eliminar (con extensión .txt):");
    String nombreArchivo = entrada.readLine();
    
    if (nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
        salida.println("❌ Nombre de archivo vacío");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    nombreArchivo = nombreArchivo.trim();
    if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
        salida.println("❌ Solo se pueden eliminar archivos .txt");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    File archivo = new File("archivos/" + usuario + "/" + nombreArchivo);
    if (!archivo.exists()) {
        salida.println("❌ El archivo '" + nombreArchivo + "' no existe");
        salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
        return;
    }

    salida.println("⚠️ ¿Estás seguro de que quieres eliminar '" + nombreArchivo + "'?");
    salida.println("📊 Tamaño del archivo: " + archivo.length() + " bytes");
    salida.println("🔥 Esta acción no se puede deshacer. (s/n):");
    
    String confirmacion = entrada.readLine();
    if (confirmacion != null && confirmacion.trim().toLowerCase().startsWith("s")) {
        if (archivo.delete()) {
            salida.println("✅ Archivo '" + nombreArchivo + "' eliminado exitosamente");
        } else {
            salida.println("❌ Error eliminando el archivo");
        }
    } else {
        salida.println("❌ Eliminación cancelada");
    }

    salida.println("Escribe 'volver' para regresar o 'salir' para el menú principal:");
}
        private static boolean verificarLogin(String usuario, String password) {
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

        private static boolean usuarioExiste(String usuario) {
            return usuarioExisteEnArchivo(usuario);
        }

        private static boolean esContrasenaSegura(String password) {
            if (password.length() < 6) return false;
            
            boolean tieneLetra = false;
            boolean tieneNumero = false;
            
            for (char c : password.toCharArray()) {
                if (Character.isLetter(c)) tieneLetra = true;
                if (Character.isDigit(c)) tieneNumero = true;
            }
            
            return tieneLetra && tieneNumero;
        }

        private static boolean guardarUsuario(String usuario, String password) {
            try (PrintWriter pw = new PrintWriter(new FileWriter("usuarios.txt", true))) {
                pw.println(usuario + ":" + password);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        private static String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                return password; // Fallback sin hash en caso de error
            }
        }
    }  private static void crearArchivosEjemplo(String usuario) {
    File directorioUsuario = new File("archivos/" + usuario);
    
    // Solo crear archivos si el directorio no existe o está vacío
    if (!directorioUsuario.exists() || (directorioUsuario.listFiles() != null && 
        directorioUsuario.listFiles().length == 0)) {
        
        directorioUsuario.mkdirs();
        
        try {
            // Crear documento1.txt
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(directorioUsuario, "documento1.txt")))) {
                pw.println("=== DOCUMENTO PERSONAL ===");
                pw.println("Propietario: " + usuario);
                pw.println("Fecha de creación: " + java.time.LocalDateTime.now().toString());
                pw.println("");
                pw.println("Este es un documento de ejemplo que contiene");
                pw.println("información importante para el usuario " + usuario + ".");
                pw.println("");
                pw.println("Contenido:");
                pw.println("- Lista de tareas pendientes");
                pw.println("- Notas importantes");
                pw.println("- Recordatorios personales");
            }
            
            // Crear notas.txt
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(directorioUsuario, "notas.txt")))) {
                pw.println("=== MIS NOTAS PERSONALES ===");
                pw.println("Usuario: " + usuario);
                pw.println("");
                pw.println("📝 TAREAS PENDIENTES:");
                pw.println("- Completar proyecto de programación");
                pw.println("- Revisar mensajes importantes");
                pw.println("- Actualizar lista de contactos");
                pw.println("- Organizar archivos del sistema");
                pw.println("");
                pw.println("💡 IDEAS:");
                pw.println("- Implementar sistema de notificaciones");
                pw.println("- Mejorar interfaz de usuario");
                pw.println("- Añadir más funcionalidades");
                pw.println("");
                pw.println("📅 RECORDATORIOS:");
                pw.println("- Hacer backup semanal");
                pw.println("- Revisar logs del servidor");
                pw.println("- Actualizar documentación");
            }
            
            // Crear configuracion.txt
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(directorioUsuario, "configuracion.txt")))) {
                pw.println("# Archivo de configuración de usuario");
                pw.println("# Generado automáticamente");
                pw.println("");
                pw.println("[USUARIO]");
                pw.println("nombre=" + usuario);
                pw.println("tipo=estandar");
                pw.println("activo=true");
                pw.println("");
                pw.println("[PREFERENCIAS]");
                pw.println("tema=oscuro");
                pw.println("idioma=español");
                pw.println("notificaciones=activadas");
                pw.println("sonidos=desactivados");
                pw.println("");
                pw.println("[ARCHIVOS]");
                pw.println("directorio_principal=archivos/" + usuario);
                pw.println("backup_automatico=true");
                pw.println("max_archivos=100");
            }
            
            System.out.println("Archivos de ejemplo creados para: " + usuario);
            
        } catch (IOException e) {
            System.err.println("Error creando archivos de ejemplo para " + usuario + ": " + e.getMessage());
        }
    }
}
}