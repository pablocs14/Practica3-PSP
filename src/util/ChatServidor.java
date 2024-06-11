package util;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServidor implements Runnable {

    private int puerto = 12345;
    private ServerSocket serverSocket;
    private boolean salir;
    private ConcurrentHashMap<String, OutputStreamWriter> clientes;
    private List<String> historialMensajes;

    public ChatServidor() {
        clientes = new ConcurrentHashMap<>();
        historialMensajes = new ArrayList<>();
        salir = false;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(puerto);

            while (!salir) {
                Socket socket = serverSocket.accept();

                // Crear un nuevo handler para manejar la comunicación con el cliente
                ClientHandler clientHandler = new ClientHandler(socket, this);
                // Iniciar un nuevo hilo para manejar este cliente
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void procesarMensaje(String mensaje, OutputStreamWriter writer) {
        try {
            if (mensaje.startsWith("/nickname")) {
                String nickname = mensaje.split(" ", 2)[1].trim();
                if (clientes.containsKey(nickname)) {
                    // Si el nickname ya existe, informar al usuario
                    String error = "Nickname ya en uso. Por favor, elige otro.";
                    writer.write(error + "\n");
                    writer.flush();
                } else {
                    // Nuevo usuario, añadirlo
                    clientes.put(nickname, writer);
                    String confirmacion = "Bienvenido al chat, " + nickname + "!";
                    writer.write(confirmacion + "\n");
                    writer.flush();
                    broadcastMessage(nickname + " se ha unido al chat.");

                    // Enviar el historial de mensajes al nuevo usuario
                    for (String mensajeHistorial : historialMensajes) {
                        writer.write(mensajeHistorial + "\n");
                        writer.flush();
                    }
                }
            } else {
                // Mensaje normal, retransmitir a todos los clientes
                for (String nickname : clientes.keySet()) {
                    OutputStreamWriter clientWriter = clientes.get(nickname);
                    String broadcastMensaje = mensaje;
                    historialMensajes.add(broadcastMensaje);
                    if (!socketIsClosed(clientWriter)) {
                        clientWriter.write(broadcastMensaje + "\n");
                        clientWriter.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        for (OutputStreamWriter writer : clientes.values()) {
            try {
                if (!socketIsClosed(writer)) {
                    writer.write(message + "\n");
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean socketIsClosed(OutputStreamWriter writer) {
        try {
            writer.write(""); // Intentamos escribir algo en el writer
            writer.flush();
            return false; // Si no hay excepción, el socket no está cerrado
        } catch (IOException e) {
            return true; // Si hay excepción, el socket está cerrado
        }
    }

    public static void main(String[] args) {
        ChatServidor chatServidor = new ChatServidor();
        new Thread(chatServidor).start();
    }
}
