import java.io.*;
import java.net.*;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;
    private static volatile boolean expulsado = false;
    private static volatile boolean logueado = false;
    private static volatile boolean enMenu = true;
    private static String usuarioActual = "";

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado al servidor");

            boolean conectado = true;

            while (conectado && !expulsado) {
                if (!logueado) {
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        if (linea.equals("DISCONNECT")) {
                            expulsado = true;
                            conectado = false;
                            break;
                        }
                        
                        System.out.println(linea);
                        if (linea.toLowerCase().contains("seleccione opción")) break;
                    }

                    if (expulsado || !conectado) break;

                    System.out.print("> ");
                    String opcion = teclado.readLine();
                    if (opcion == null || expulsado) break;
                    
                    salida.println(opcion);

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
                            System.out.println("Desconectando...");
                            conectado = false;
                            break;
                        default:
                            String error = entrada.readLine();
                            System.out.println(error);
                    }
                } else {
                    mostrarMenuYProcesar(entrada, salida, teclado);
                }
            }

            if (expulsado) {
                System.out.println("\nTu cuenta ha sido eliminada del sistema.");
                System.out.println("Puedes registrarte nuevamente si lo deseas.");
                System.out.println("Presiona Enter para cerrar...");
                try { teclado.readLine(); } catch (IOException ignored) {}
            }

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }

    private static void mostrarMenuYProcesar(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        enMenu = true; 
        String linea;
        
        // Manejar solicitudes especiales del servidor
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            // Manejar notificaciones mientras está en el menú
            if (enMenu && linea.startsWith("🔔 NUEVO MENSAJE:")) {
                System.out.println("\n" + linea);
                String mensaje = entrada.readLine();
                if (mensaje != null) {
                    System.out.println(mensaje);
                }
                continue;
            }

            // Manejar solicitud de autorización para acceso a archivos
            if (linea.startsWith("AUTHORIZATION_REQUEST:")) {
                String solicitante = linea.split(":", 2)[1];
                manejarSolicitudAutorizacion(salida, solicitante);
                continue;
            }

            System.out.println(linea);
            if (linea.toLowerCase().contains("seleccione opción")) break;
        }

        System.out.print("> ");
        String opcion = teclado.readLine();
        if (opcion == null || expulsado) return;
        
        salida.println(opcion);

        switch (opcion.trim()) {
            case "1":
                enMenu = false;
                bandeja(entrada, salida, teclado);
                enMenu = true; 
                break;
            case "2":
                enMenu = false;
                juego(entrada, salida, teclado);
                enMenu = true;  
                break;
            case "3":
                enMenu = false;
                enviarMensaje(entrada, salida, teclado);
                enMenu = true; 
                break;
            case "4":
                enMenu = false;
                gestionarBloqueos(entrada, salida, teclado);
                enMenu = true;
                break;
            case "5":
                enMenu = false;
                explorarArchivos(entrada, salida, teclado);
                enMenu = true;
                break;
            case "6":
                enMenu = false;
                gestionarMisArchivos(entrada, salida, teclado);
                enMenu = true;
                break;
            case "7": 
                String sesionCerrada = entrada.readLine();
                System.out.println(sesionCerrada);
                logueado = false;
                usuarioActual = "";
                break;
            default:
                String error = entrada.readLine();
                System.out.println(error);
        }
    }

    private static boolean login(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Usuario: ");
        String usuario = teclado.readLine();
        if (usuario == null) return false;
        salida.println(usuario);

        prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Contraseña: ");
        String password = teclado.readLine();
        if (password == null) return false;
        salida.println(password);

        String respuesta = entrada.readLine();
        System.out.println(respuesta);
        
        if (respuesta.contains("eliminada") || respuesta.contains("expulsado")) {
            try {
                String mensajeExtra1 = entrada.readLine();
                String mensajeExtra2 = entrada.readLine();
                if (mensajeExtra1 != null && !mensajeExtra1.isEmpty()) {
                    System.out.println(mensajeExtra1);
                }
                if (mensajeExtra2 != null && !mensajeExtra2.isEmpty()) {
                    System.out.println(mensajeExtra2);
                }
            } catch (IOException ignored) {}
            return false;
        }
        
        if (respuesta.contains("Bienvenido")) {
            usuarioActual = usuario.trim();
            return true;
        }
        
        return false;
    }

    private static void registro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Usuario: ");
        String usuario = teclado.readLine();
        if (usuario == null) return;
        salida.println(usuario);

        String respuesta = entrada.readLine();
        System.out.println(respuesta);

        if (respuesta.contains("ya existe") || respuesta.contains("vacío")) {
            return;
        }

        System.out.print("Contraseña: ");
        String password = teclado.readLine();
        if (password == null) return;
        salida.println(password);

        String respuestaFinal = entrada.readLine();
        System.out.println(respuestaFinal);

        if (respuestaFinal.contains("no válida")) {
            String mensaje = entrada.readLine();
            if (mensaje != null) {
                System.out.println(mensaje);
            }
        }
    }

    private static void gestionarBloqueos(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        while (!expulsado) {
            String linea;
            
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("DISCONNECT")) {
                    expulsado = true;
                    return;
                }
                
                System.out.println(linea);
                if (linea.toLowerCase().contains("seleccione opción")) break;
            }

            if (expulsado) return;

            System.out.print("> ");
            String opcion = teclado.readLine();
            if (opcion == null || expulsado) break;

            salida.println(opcion);

            switch (opcion.trim()) {
                case "1":
                    mostrarRespuestaServidor(entrada);
                    break;
                case "2":
                    bloquearUsuario(entrada, salida, teclado);
                    break;
                case "3":
                    desbloquearUsuario(entrada, salida, teclado);
                    break;
                case "4":
                    return;
                default:
                    String error = entrada.readLine();
                    if (error != null) {
                        System.out.println(error);
                    }
            }
        }
    }

    private static void bloquearUsuario(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String linea;
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            System.out.println(linea);
            if (linea.toLowerCase().contains("ingrese el nombre del usuario a bloquear")) break;
        }

        if (expulsado) return;

        System.out.print("Usuario a bloquear: ");
        String usuario = teclado.readLine();
        if (usuario == null || expulsado) return;
        
        salida.println(usuario);

        String respuesta = entrada.readLine();
        if (respuesta != null) {
            System.out.println(respuesta);
        }
    }

    private static void desbloquearUsuario(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String linea;
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            System.out.println(linea);
            if (linea.toLowerCase().contains("ingrese el nombre del usuario a desbloquear")) {
                break;
            }
            if (linea.toLowerCase().contains("no tienes usuarios bloqueados")) {
                return;
            }
        }

        if (expulsado) return;

        System.out.print("Usuario a desbloquear: ");
        String usuario = teclado.readLine();
        if (usuario == null || expulsado) return;
        
        salida.println(usuario);

        String respuesta = entrada.readLine();
        if (respuesta != null) {
            System.out.println(respuesta);
        }
    }

    private static void mostrarRespuestaServidor(BufferedReader entrada) throws IOException {
        String linea;
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            System.out.println(linea);
            
            if (linea.trim().isEmpty() || 
                linea.toLowerCase().contains("no tienes usuarios bloqueados")) {
                break;
            }
        }
    }

  private static void bandeja(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
    while (!expulsado) {
        String linea;
        
        // Leer TODO lo que envía el servidor (bandeja + opciones)
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            // Ignorar notificaciones de nuevos mensajes mientras estamos viendo la bandeja
            if (linea.startsWith("🔔 NUEVO MENSAJE:")) {
                entrada.readLine(); // Consumir la segunda línea de la notificación
                continue; 
            }
            
            System.out.println(linea);
            
            // Detectar el final del output del servidor
            if (linea.toLowerCase().contains("- 'menu' o 'salir' para volver al menú principal")) {
                break;
            }
        }

        if (expulsado) return;

        // Pedir comando al usuario
        System.out.print("> ");
        String comando = teclado.readLine();
        if (comando == null || expulsado) break;

        // Enviar comando
        salida.println(comando);

        // Si el usuario quiere salir, terminar
        if (comando.trim().equalsIgnoreCase("salir") || comando.trim().equalsIgnoreCase("menu")) {
            return;
        }

        // El bucle continuará y leerá la nueva bandeja que el servidor enviará
    }
}
  
  private static void juego(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
    boolean jugando = true;
    
    while (jugando && !expulsado) {
        String linea;
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            if (linea.startsWith("🔔 NUEVO MENSAJE:")) {
                entrada.readLine();
                continue; 
            }
            
            System.out.println(linea);

            if (linea.contains("Ingresa tu número:")) {
                System.out.print("> ");
                String numero = teclado.readLine();
                if (numero == null || expulsado) return;
                salida.println(numero);
                
                if (numero.trim().equalsIgnoreCase("menu")) {
                    return;
                }
            } else if (linea.toLowerCase().contains("¿quieres jugar otra vez?")) {
                System.out.print("> ");
                String respuesta = teclado.readLine();
                if (respuesta == null || expulsado) return;
                salida.println(respuesta);

                if (respuesta != null && (respuesta.trim().equalsIgnoreCase("n") || 
                                        respuesta.trim().equalsIgnoreCase("menu"))) {
                    jugando = false;
                }
                break;
            }
        }
    }
}

    private static void enviarMensaje(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String linea;
        boolean hayUsuarios = false;
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            System.out.println(linea);
            
            if (linea.toLowerCase().contains("no hay otros usuarios registrados")) {
                return; 
            }
            
            if (linea.matches("^\\d+\\..*")) {
                hayUsuarios = true;
            }
            
            if (linea.toLowerCase().contains("escribe el nombre del usuario")) {
                hayUsuarios = true;
                break;
            }
        }

        if (!hayUsuarios) {
            return;
        }

        System.out.print("> ");
        String destinatario = teclado.readLine();
        if (destinatario == null || expulsado) return;
        
        salida.println(destinatario);

        linea = entrada.readLine();
        if (linea == null) return;
        System.out.println(linea);
        
        if (linea.contains("❌") || 
            linea.toLowerCase().contains("no existe") ||
            linea.toLowerCase().contains("bloqueado") ||
            linea.toLowerCase().contains("ti mismo")) {
            return;
        }

        System.out.print("Mensaje > ");
        String mensaje = teclado.readLine();
        if (mensaje == null || expulsado) return;
        
        salida.println(mensaje);

        String confirmacion = entrada.readLine();
        if (confirmacion != null) {
            System.out.println(confirmacion);
        }
    }

  private static void explorarArchivos(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
    while (!expulsado) {
        String linea;
        boolean esperandoInput = false;
        
        // Leer todas las líneas del servidor hasta encontrar un punto donde necesitemos input
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            System.out.println(linea);
            
            // Detectar si necesitamos input del usuario
            if (linea.toLowerCase().contains("regresando al menú principal")) {
                return; // Volver al menú principal automáticamente
            }
            
            if (linea.toLowerCase().contains("volviendo a la lista de usuarios")) {
                break; // Continuar el bucle para mostrar la lista de usuarios nuevamente
            }
            
            if (linea.toLowerCase().contains("seleccione el número del usuario") ||
                linea.toLowerCase().contains("seleccione una opción") ||
                linea.toLowerCase().contains("ingresa el nombre exacto del archivo") ||
                linea.toLowerCase().contains("confirma la descarga")) {
                esperandoInput = true;
                break;
            }
        }

        if (expulsado) return;
        
        // Si necesitamos input del usuario
        if (esperandoInput) {
            System.out.print("> ");
            String respuesta = teclado.readLine();
            if (respuesta == null || expulsado) break;

            salida.println(respuesta);

            // Si el usuario elige volver al menú principal (opción 0 desde lista de usuarios)
            if (respuesta.trim().equals("0")) {
                // Leer el mensaje de confirmación del servidor
                String confirmacion = entrada.readLine();
                if (confirmacion != null && !confirmacion.equals("DISCONNECT")) {
                    System.out.println(confirmacion);
                }
                
                // Si el mensaje contiene "regresando al menú principal", salir
                if (confirmacion != null && confirmacion.toLowerCase().contains("regresando al menú principal")) {
                    return;
                }
                // Si contiene "volviendo a la lista de usuarios", continuar el bucle
            }
            
            // Para confirmaciones de descarga, leer la respuesta del servidor
            if (respuesta.trim().toLowerCase().startsWith("s") || 
                respuesta.trim().toLowerCase().startsWith("n")) {
                leerRespuestaDescarga(entrada);
            }
        } else {
            // Si no esperamos input, algo salió mal, salir
            break;
        }
    }
}

  private static void leerRespuestaDescarga(BufferedReader entrada) throws IOException {
    String linea;
    
    while ((linea = entrada.readLine()) != null) {
        if (linea.equals("DISCONNECT")) {
            expulsado = true;
            return;
        }
        
        System.out.println(linea);
        
        // Parar de leer cuando alcancemos el final de la respuesta de descarga
        if (linea.toLowerCase().contains("descarga completada") || 
            linea.toLowerCase().contains("descarga cancelada")) {
            // Leer una línea más para el mensaje "En un sistema real..." si está presente
            String extraLinea = entrada.readLine();
            if (extraLinea != null && !extraLinea.equals("DISCONNECT")) {
                System.out.println(extraLinea);
            }
            break;
        }
    }
}

    // MÉTODO MEJORADO: Gestionar mis archivos con mejor flujo de control
 private static void gestionarMisArchivos(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
    while (!expulsado) {
        String linea;
        boolean esperandoOpcionPrincipal = false;
        boolean esperandoOpcionVisualizacion = false;
        boolean esperandoNombreArchivo = false;
        
        // Leer respuesta del servidor
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            System.out.println(linea);
            
            // Detectar menú principal de gestión de archivos
            if (linea.toLowerCase().contains("=== gestionar mis archivos ===")) {
                // Continuar leyendo hasta la línea de selección
                continue;
            }
            
            if (linea.toLowerCase().contains("seleccione una opción:")) {
                esperandoOpcionPrincipal = true;
                break;
            }
            
            // Detectar menú de visualización
            if (linea.toLowerCase().contains("=== opciones de visualización ===")) {
                // Continuar leyendo hasta la línea de selección
                continue;
            }
            
            if (linea.toLowerCase().contains("selecciona una opción:")) {
                esperandoOpcionVisualizacion = true;
                break;
            }
            
            // Detectar solicitud de nombre de archivo
            if (linea.toLowerCase().contains("ingresa el nombre del archivo") ||
                linea.toLowerCase().contains("ingresa el nombre del nuevo archivo")) {
                esperandoNombreArchivo = true;
                break;
            }
            
            // Modo edición
            if (linea.toLowerCase().contains("para terminar, escribe una línea que contenga solo: fin")) {
                manejarEdicionArchivo(entrada, salida, teclado);
                break;
            }
            
            // Comandos de navegación para otros métodos
            if (linea.toLowerCase().contains("escribe 'volver' para regresar o 'salir' para el menú principal")) {
                System.out.print("Comando: ");
                String comando = teclado.readLine();
                if (comando == null || expulsado) return;
                
                salida.println(comando);
                
                if (comando.trim().toLowerCase().equals("salir")) {
                    return;
                } else if (comando.trim().toLowerCase().equals("volver")) {
                    break;
                }
            }
        }

        if (expulsado) return;
        
        // Manejar input según el contexto
        if (esperandoOpcionPrincipal || esperandoOpcionVisualizacion || esperandoNombreArchivo) {
            System.out.print("> ");
            String respuesta = teclado.readLine();
            if (respuesta == null || expulsado) break;

            salida.println(respuesta);

            // Si elige salir del menú principal
            if (esperandoOpcionPrincipal && respuesta.trim().equals("0")) {
                return;
            }
        }
    }
}
  private static void manejarEdicionArchivo(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
    System.out.println("\n🖊️  MODO EDICIÓN ACTIVADO");
    System.out.println("Escribe el contenido línea por línea.");
    System.out.println("Para terminar, escribe: FIN");
    System.out.println();
    
    String linea;
    while ((linea = teclado.readLine()) != null) {
        salida.println(linea);
        
        if (linea.trim().equalsIgnoreCase("FIN")) {
            break;
        }
        
        // Leer confirmación del servidor
        String confirmacion = entrada.readLine();
        if (confirmacion != null && !confirmacion.equals("DISCONNECT")) {
            System.out.println(confirmacion);
        }
    }
    
    // Leer mensajes finales del servidor
    for (int i = 0; i < 3; i++) {
        String mensaje = entrada.readLine();
        if (mensaje != null && !mensaje.equals("DISCONNECT")) {
            System.out.println(mensaje);
            
            // Si el servidor envía el mensaje de comandos, manejarlo aquí
            if (mensaje.toLowerCase().contains("escribe 'volver' para regresar o 'salir' para el menú principal")) {
                System.out.print("Comando: ");
                String comando = teclado.readLine();
                if (comando != null) {
                    salida.println(comando); // Enviar comando al servidor
                }
                return; // Salir del método para que el método padre maneje la respuesta
            }
        }
    }
}


    // NUEVA FUNCIONALIDAD: Manejo de autorización para acceso a archivos
   private static void manejarSolicitudAutorizacion(PrintWriter salida, String solicitante) {
    System.out.println("\n════════════════════════════════");
    System.out.println("🔒 SOLICITUD DE ACCESO A ARCHIVOS");
    System.out.println("════════════════════════════════");
    System.out.println("📋 " + solicitante + " quiere acceder a tu lista de archivos.");
    System.out.println("💭 ¿Deseas autorizar el acceso?");
    System.out.println("");
    System.out.println("✅ Esta solicitud se procesará automáticamente por ahora.");
    System.out.println("🔮 Funcionalidad de autorización manual será implementada próximamente.");
    System.out.println("════════════════════════════════");
    
    // Por ahora, autorizar automáticamente (en el futuro se puede mejorar)
    // salida.println("AUTHORIZATION_RESPONSE:" + solicitante + ":GRANTED");
}

    // FUNCIONALIDAD AUXILIAR: Crear archivos de ejemplo para testing
   public static void crearArchivosEjemplo() {
    try {
        // Crear directorio de archivos para el usuario
        File directorioUsuario = new File("archivos/" + usuarioActual);
        if (!directorioUsuario.exists()) {
            directorioUsuario.mkdirs();
            
            // Crear algunos archivos de ejemplo
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(directorioUsuario, "documento1.txt")))) {
                pw.println("Este es un documento de ejemplo.");
                pw.println("Contiene información importante.");
                pw.println("Creado por: " + usuarioActual);
            }
            
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(directorioUsuario, "notas.txt")))) {
                pw.println("=== MIS NOTAS ===");
                pw.println("- Recordar completar el proyecto");
                pw.println("- Revisar mensajes importantes");
                pw.println("- Actualizar lista de contactos");
            }
            
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(directorioUsuario, "configuracion.txt")))) {
                pw.println("# Archivo de configuración");
                pw.println("usuario=" + usuarioActual);
                pw.println("tema=oscuro");
                pw.println("idioma=español");
            }
            
            System.out.println("📁 Archivos de ejemplo creados en: " + directorioUsuario.getPath());
        }
    } catch (IOException e) {
        System.err.println("Error creando archivos de ejemplo: " + e.getMessage());
    }
}
}