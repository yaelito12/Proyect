package cliente;

import java.io.IOException;
import java.net.Socket;

public class Cliente {
    public static void main(String[] args) throws IOException {
        
      Socket socket = new Socket("localhost", 1234);
        
         socket.close();
    }
}