package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    public static void main(String[] args) throws IOException {
  final String HOST = "localhost";
        final int PUERTO = 1234;

        try (
            Socket socket = new Socket(HOST, PUERTO);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
         
            String mensajeServidor;
            
              mensajeServidor = entrada.readLine();
            System.out.println(mensajeServidor);
            String opcion = teclado.readLine();
            salida.println(opcion);

            
            
            
        }}}