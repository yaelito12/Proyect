import java.io.*;
import java.net.*;

public class Cliente {
    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;
    private static volatile boolean expulsado = false;

    public static void main(String[] args) {
        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("🔗 Conectado al servidor");

         
            Thread hiloNotificaciones = new Thread(() -> {
                try {
                    String linea;
                    while ((linea = entrada.readLine()) != null && !expulsado) {
                        if (linea.equals("DISCONNECT")) {
                            expulsado = true;
                            System.out.println("\n? Conexión terminada por el servidor.");
                            break;
                        } else if (linea.startsWith("? NUEVO MENSAJE:")) {
                            System.out.println("\n" + linea);
                          
                            String mensaje = entrada.readLine();
                            if (mensaje != null) {
                                System.out.println(mensaje);
                            }
                            System.out.print("➤ "); 
                        }
                    }
                } catch (IOException e) {
                    if (!expulsado) {
                        System.err.println("Error en hilo de notificaciones: " + e.getMessage());
                    }
                }
            });

            boolean conectado = true;
            boolean logueado = false;

            while (conectado && !expulsado) {
                String linea;
                StringBuilder menu = new StringBuilder();

             
                try {
                    while ((linea = entrada.readLine()) != null) {
                        if (linea.equals("DISCONNECT")) {
                            expulsado = true;
                            System.out.println("\n? Has sido expulsado del servidor.");
                            conectado = false;
                            break;
                        }
                        
                        menu.append(linea).append("\n");
                        if (linea.toLowerCase().contains("seleccione opción")) break;
                    }
                } catch (IOException e) {
                    System.err.println(" Error leyendo del servidor: " + e.getMessage());
                    break;
                }

                if (expulsado || !conectado) break;

                System.out.print(menu.toString());
                System.out.print("➤ ");
                
                String opcion = teclado.readLine();
                if (opcion == null || expulsado) break;
                
                salida.println(opcion);

                if (!logueado) {
                    switch (opcion.trim()) {
                        case "1":
                            if (login(entrada, salida, teclado)) {
                                logueado = true;
                                hiloNotificaciones.start(); // Iniciar notificaciones después del login
                            }
                            break;
                        case "2":
                            registro(entrada, salida, teclado);
                            break;
                        case "3":
                            String despedida = entrada.readLine();
                            System.out.println(despedida);
                            System.out.println("👋 Desconectando...");
                            conectado = false;
                            break;
                        default:
                            String error = entrada.readLine();
                            System.out.println(error);
                    }
                } else {
                    switch (opcion.trim()) {
                        case "1":
                            bandeja(entrada, salida, teclado);
                            break;
                        case "2":
                            juego(entrada, salida, teclado);
                            break;
                        case "3":
                            enviarMensaje(entrada, salida, teclado);
                            break;
                        case "4":
                            String sesionCerrada = entrada.readLine(); // "Cerrando sesión..."
                            System.out.println(sesionCerrada);
                            logueado = false;
                            break;
                        default:
                            String error = entrada.readLine();
                            System.out.println(error);
                    }
                }
            }

          
            if (hiloNotificaciones.isAlive()) {
                hiloNotificaciones.interrupt();
            }

        } catch (IOException e) {
            System.err.println(" Error de conexión: " + e.getMessage());
        }
    }

    private static boolean login(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer "Ingrese usuario:"
        String prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Usuario: ");
        String usuario = teclado.readLine();
        salida.println(usuario);

        // Leer "Ingrese contraseña:"
        prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Contraseña: ");
        String password = teclado.readLine();
        salida.println(password);

        // Leer respuesta del servidor
        String respuesta = entrada.readLine();
        System.out.println(respuesta);
        return respuesta.contains("Bienvenido");
    }

    private static void registro(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        // Leer "Ingrese nuevo usuario:"
        String prompt = entrada.readLine();
        System.out.println(prompt);
        System.out.print("Usuario: ");
        String usuario = teclado.readLine();
        salida.println(usuario);

        // Leer primera respuesta (puede ser error de usuario existente)
        String respuesta = entrada.readLine();
        System.out.println(respuesta);

        if (respuesta.contains("ya existe") || respuesta.contains("vacío")) {
            return; // Salir si hay error
        }

        // Si llegamos aquí, pedir contraseña
        System.out.print("Contraseña: ");
        String password = teclado.readLine();
        salida.println(password);

        // Leer respuesta final
        String respuestaFinal = entrada.readLine();
        System.out.println(respuestaFinal);

      
        if (respuestaFinal.contains("no válida")) {
            String mensaje = entrada.readLine();
            if (mensaje != null) {
                System.out.println(mensaje);
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
                System.out.println(linea);
                if (linea.toLowerCase().contains("escribir 'salir'")) break;
            }

            if (expulsado) return;

            System.out.print("➤ ");
            String comando = teclado.readLine();
            if (comando == null || expulsado) break;

            salida.println(comando);

            if (comando.trim().equalsIgnoreCase("salir") || comando.trim().equalsIgnoreCase("menu")) {
                return; // Salir de la bandeja
            }

            // Leer respuesta del comando
            String respuesta = entrada.readLine();
            if (respuesta != null && !respuesta.equals("DISCONNECT")) {
                System.out.println(respuesta);
            }
        }
    }

    private static void juego(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        boolean jugando = true;
        
        while (jugando && !expulsado) {
            String linea;
            
            // Leer mensajes del juego
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("DISCONNECT")) {
                    expulsado = true;
                    return;
                }
                
                System.out.println(linea);

                if (linea.contains("Ingresa tu número:")) {
                    System.out.print("➤ ");
                    String numero = teclado.readLine();
                    if (numero == null || expulsado) return;
                    salida.println(numero);
                    
                    if (numero.trim().equalsIgnoreCase("menu")) {
                        return; // Salir del juego
                    }
                } else if (linea.toLowerCase().contains("¿quieres jugar otra vez?")) {
                    System.out.print("➤ ");
                    String respuesta = teclado.readLine();
                    if (respuesta == null || expulsado) return;
                    salida.println(respuesta);

                    if (respuesta != null && (respuesta.trim().equalsIgnoreCase("n") || 
                                            respuesta.trim().equalsIgnoreCase("menu"))) {
                        jugando = false;
                    }
                    break; // Salir del bucle interno para continuar o terminar
                }
            }
        }
    }

    private static void enviarMensaje(BufferedReader entrada, PrintWriter salida, BufferedReader teclado) throws IOException {
        String linea;
        
        // Leer lista de usuarios disponibles
        while ((linea = entrada.readLine()) != null) {
            System.out.println(linea);
            if (linea.toLowerCase().contains("escribe el nombre del usuario")) break;
        }

        System.out.print("➤ ");
        String destinatario = teclado.readLine();
        if (destinatario == null) return;
        
        salida.println(destinatario);

        // Leer respuesta (puede ser error o solicitud de mensaje)
        linea = entrada.readLine();
        System.out.println(linea);
        
        // Si hay error (no conectado, inválido, etc.), salir
        if (linea.toLowerCase().contains("no") || 
            linea.toLowerCase().contains("inválido") || 
            linea.toLowerCase().contains("conectado")) {
            return;
        }

        // Si llegamos aquí, pedir el mensaje
        System.out.print("Mensaje ➤ ");
        String mensaje = teclado.readLine();
        if (mensaje == null) return;
        
        salida.println(mensaje);

        // Leer confirmación
        String confirmacion = entrada.readLine();
        if (confirmacion != null) {
            System.out.println(confirmacion);
        }
    }
}