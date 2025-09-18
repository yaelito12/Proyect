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
            
        
            while ((linea = entrada.readLine()) != null) {
                if (linea.equals("DISCONNECT")) {
                    expulsado = true;
                    return;
                }
                
             
                if (linea.startsWith("? NUEVO MENSAJE:")) {
                 
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
            
       
            if (linea.toLowerCase().contains("no hay otros usuarios conectados")) {
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
        
        if (linea.toLowerCase().contains("no") || 
            linea.toLowerCase().contains("inválido") || 
            linea.toLowerCase().contains("conectado")) {
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
}