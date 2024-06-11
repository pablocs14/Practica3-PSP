package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ChatServidor chatServidor;

    public ClientHandler(Socket clientSocket, ChatServidor chatServidor) {
        this.clientSocket = clientSocket;
        this.chatServidor = chatServidor;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStreamWriter writer = new OutputStreamWriter(clientSocket.getOutputStream());

            // Lógica para manejar la comunicación con el cliente
            String mensaje;
            while ((mensaje = reader.readLine()) != null) {
                // Procesar el mensaje recibido
                chatServidor.procesarMensaje(mensaje, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
