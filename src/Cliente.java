import java.io.*;
import java.net.*;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;
    private static volatile boolean expulsado = false;
    private static volatile boolean logueado = false;
    private static volatile boolean enMenu = true;

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
        
        while ((linea = entrada.readLine()) != null) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            if (enMenu && linea.startsWith("🔔 NUEVO MENSAJE:")) {
                System.out.println("\n" + linea);
                String mensaje = entrada.readLine();
                if (mensaje != null) {
                    System.out.println(mensaje);
                }
                continue;
            }

            if (linea.startsWith("FILE_LIST_REQUEST:")) {
                String solicitante = linea.split(":", 2)[1];
                manejarSolicitudListaArchivos(salida, solicitante);
                continue;
            }

            if (linea.startsWith("FILE_TRANSFER_REQUEST:")) {
                String[] partes = linea.split(":", 3);
                String solicitante = partes[1];
                String nombreArchivo = partes[2];
                manejarSolicitudTransferencia(salida, solicitante, nombreArchivo);
                continue;
            }

            if (linea.startsWith("FILE_CONTENT:")) {
                String[] partes = linea.split(":", 4);
                String remitente = partes[1];
                String nombreArchivo = partes[2];
                String contenido = partes[3];
                guardarArchivoRecibido(nombreArchivo, contenido, remitente);
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
                String sesionCerrada = entrada.readLine();
                System.out.println(sesionCerrada);
                logueado = false;
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
        
        return respuesta.contains("Bienvenido");
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
                
                if (linea.toLowerCase().contains("escribir 'salir'") || 
                    linea.toLowerCase().contains("escribir 'menu'")) break;
            }

            if (expulsado) return;

            System.out.print("> ");
            String comando = teclado.readLine();
            if (comando == null || expulsado) break;

            salida.println(comando);

            if (comando.trim().equalsIgnoreCase("salir") || comando.trim().equalsIgnoreCase("menu")) {
                return;
            }

            if (comando.trim().toLowerCase().startsWith("siguiente") ||
                comando.trim().toLowerCase().startsWith("anterior") ||
                comando.trim().toLowerCase().startsWith("pagina") ||
                comando.trim().toLowerCase().startsWith("actualizar")) {
                
                String respuesta = entrada.readLine();
                if (respuesta != null && !respuesta.equals("DISCONNECT")) {
                    System.out.println(respuesta);
                }
            } else if (comando.trim().toLowerCase().startsWith("eliminar")) {
                String respuesta = entrada.readLine();
                if (respuesta != null && !respuesta.equals("DISCONNECT")) {
                    System.out.println(respuesta);
                }
            } else {
                String respuesta = entrada.readLine();
                if (respuesta != null && !respuesta.equals("DISCONNECT")) {
                    System.out.println(respuesta);
                }
            }
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
            
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("DISCONNECT")) {
                    expulsado = true;
                    return;
                }
                
                System.out.println(linea);
                
                if (linea.toLowerCase().contains("no hay otros usuarios conectados")) {
                    System.out.print("");
                    String enter = teclado.readLine();
                    return;
                }
                
                if (linea.toLowerCase().contains("seleccione el número del usuario") ||
                    linea.toLowerCase().contains("seleccione una opción") ||
                    linea.toLowerCase().contains("archivo:")) {
                    break;
                }
            }

            if (expulsado) return;

            System.out.print("> ");
            String respuesta = teclado.readLine();
            if (respuesta == null || expulsado) break;

            salida.println(respuesta);

            if (respuesta.trim().equals("0")) {
                return;
            } else if (respuesta.trim().matches("\\d+")) {
                continue;
            } else if (respuesta.trim().equals("3")) {
                continue;
            } else if (respuesta.trim().toLowerCase().startsWith("s") || 
                      respuesta.trim().toLowerCase().startsWith("n")) {
                leerRespuestaServidor(entrada);
            } else {
                leerRespuestaServidor(entrada);
            }
        }
    }

    private static void leerRespuestaServidor(BufferedReader entrada) throws IOException {
        String linea;
        int lineasLeidas = 0;
        
        while ((linea = entrada.readLine()) != null && lineasLeidas < 10) {
            if (linea.equals("DISCONNECT")) {
                expulsado = true;
                return;
            }
            
            System.out.println(linea);
            lineasLeidas++;
            
            if (linea.toLowerCase().contains("presiona enter para continuar") ||
                linea.toLowerCase().contains("presiona enter para volver")) {
                break;
            }
            
            if (linea.trim().isEmpty() && lineasLeidas > 2) {
                break;
            }
        }
    }
    
    // MÉTODOS PARA MANEJO DE ARCHIVOS
    private static void manejarSolicitudListaArchivos(PrintWriter salida, String solicitante) {
        System.out.println("\n📁 " + solicitante + " está explorando tus archivos...");
        
        File directorio = new File(".");
        File[] archivos = directorio.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        
        StringBuilder lista = new StringBuilder();
        lista.append("=== ARCHIVOS DISPONIBLES ===\n");
        
        if (archivos == null || archivos.length == 0) {
            lista.append("📭 No hay archivos .txt disponibles en este directorio.\n");
        } else {
            lista.append("📂 Archivos .txt encontrados:\n");
            for (int i = 0; i < archivos.length; i++) {
                long bytes = archivos[i].length();
                String tamaño = bytes < 1024 ? bytes + " bytes" : 
                              bytes < 1048576 ? (bytes/1024) + " KB" : 
                              (bytes/1048576) + " MB";
                
                lista.append((i + 1) + ". 📄 " + archivos[i].getName() + 
                            " (" + tamaño + ")\n");
            }
            lista.append("\nPara descargar un archivo, " + solicitante + " debe solicitar el nombre exacto.\n");
        }
        
        guardarMensajeParaUsuario(salida, solicitante, lista.toString());
        System.out.println("✅ Lista de archivos enviada a " + solicitante);
    }

    private static void manejarSolicitudTransferencia(PrintWriter salida, String solicitante, String nombreArchivo) {
        System.out.println("\n📤 " + solicitante + " solicita descargar: " + nombreArchivo);
        
        File archivo = new File(nombreArchivo);
        
        if (!archivo.exists()) {
            enviarError(salida, solicitante, "❌ El archivo '" + nombreArchivo + "' no existe en mi directorio.");
            System.out.println(" Archivo no encontrado: " + nombreArchivo);
            return;
        }
        
        if (!archivo.isFile()) {
            enviarError(salida, solicitante, " '" + nombreArchivo + "' no es un archivo válido.");
            System.out.println(" No es un archivo válido: " + nombreArchivo);
            return;
        }
        
        if (!nombreArchivo.toLowerCase().endsWith(".txt")) {
            enviarError(salida, solicitante, " Solo se pueden transferir archivos .txt por seguridad.");
            System.out.println(" Tipo de archivo no permitido: " + nombreArchivo);
            return;
        }
        
        if (archivo.length() > 1048576) {
            enviarError(salida, solicitante, " El archivo es demasiado grande (máximo 1MB).");
            System.out.println(" Archivo muy grande: " + nombreArchivo);
            return;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            StringBuilder contenido = new StringBuilder();
            String linea;
            int lineas = 0;
            
            while ((linea = br.readLine()) != null && lineas < 1000) {
                contenido.append(linea).append("\n");
                lineas++;
            }
            
            salida.println("SEND_FILE:" + solicitante + ":" + nombreArchivo + ":" + contenido.toString());
            System.out.println(" Archivo '" + nombreArchivo + "' enviado a " + solicitante + " (" + lineas + " líneas)");
            
        } catch (IOException e) {
            enviarError(salida, solicitante, " Error al leer el archivo: " + e.getMessage());
            System.out.println(" Error leyendo archivo " + nombreArchivo + ": " + e.getMessage());
        }
    }

    private static void guardarArchivoRecibido(String nombreArchivo, String contenido, String remitente) {
        File directorioDescargas = new File("descargas");
        if (!directorioDescargas.exists()) {
            directorioDescargas.mkdirs();
        }
        
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nombreFinal = "descargas/[" + timestamp + "]_" + remitente + "_" + nombreArchivo;
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(nombreFinal))) {
            pw.print(contenido);
            
            File archivoGuardado = new File(nombreFinal);
            long tamaño = archivoGuardado.length();
            String tamañoStr = tamaño < 1024 ? tamaño + " bytes" : 
                              tamaño < 1048576 ? (tamaño/1024) + " KB" : 
                              (tamaño/1048576) + " MB";
            
            System.out.println("\n📥 ¡ARCHIVO DESCARGADO EXITOSAMENTE!");
            System.out.println("   Remitente: " + remitente);
            System.out.println("   Archivo original: " + nombreArchivo);
            System.out.println("   Guardado como: " + nombreFinal);
            System.out.println("   Tamaño: " + tamañoStr);
            System.out.println("   ✅ Descarga completada");
            
        } catch (IOException e) {
            System.out.println("\nERROR AL GUARDAR ARCHIVO");
            System.out.println("   Remitente: " + remitente);
            System.out.println("   Archivo: " + nombreArchivo);
            System.out.println("   Error: " + e.getMessage());
        }
    }
    
    private static void enviarError(PrintWriter salida, String destinatario, String mensaje) {
        guardarMensajeParaUsuario(salida, destinatario, mensaje);
    }

    private static void guardarMensajeParaUsuario(PrintWriter salida, String destinatario, String mensaje) {
        salida.println("SEND_MESSAGE:" + destinatario + ":" + mensaje);
    }
}