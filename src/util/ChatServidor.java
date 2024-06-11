package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatServidor implements Runnable {

    private int puerto = 12345;
    private DatagramSocket socket;
    private boolean salir;
    private ConcurrentHashMap<String, InetAddress> clientes;  // Nickname y su dirección IP
    private ConcurrentHashMap<String, Integer> puertos;       // Nickname y su puerto
    private ConcurrentLinkedQueue<String> historialMensajes;  // Historial de mensajes (opcional)

    public ChatServidor() {
        clientes = new ConcurrentHashMap<>();
        puertos = new ConcurrentHashMap<>();
        historialMensajes = new ConcurrentLinkedQueue<>();
        salir = false;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(puerto);
            byte[] buffer = new byte[1024];

            while (!salir) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String mensaje = new String(packet.getData(), 0, packet.getLength());
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                if (mensaje.startsWith("/nickname")) {
                    String nickname = mensaje.split(" ", 2)[1].trim();
                    if (clientes.containsKey(nickname)) {
                        // Si el nickname ya existe, informar al usuario
                        String error = "Nickname ya en uso. Por favor, elige otro.";
                        enviarMensaje(error, address, port);
                    } else {
                        // Nuevo usuario, añadirlo
                        clientes.put(nickname, address);
                        puertos.put(nickname, port);
                        String confirmacion = "Bienvenido al chat, " + nickname + "!";
                        enviarMensaje(confirmacion, address, port);
                        broadcastMessage(nickname + " se ha unido al chat.");

                        // Opcional: enviar el historial de mensajes al nuevo usuario
                        for (String mensajeHistorial : historialMensajes) {
                            enviarMensaje(mensajeHistorial, address, port);
                        }
                    }
                } else {
                    // Mensaje normal, retransmitir a todos los clientes
                    String senderNickname = null;
                    for (String nick : clientes.keySet()) {
                        if (clientes.get(nick).equals(address) && puertos.get(nick).equals(port)) {
                            senderNickname = nick;
                            break;
                        }
                    }
                    if (senderNickname != null) {
                        String broadcastMensaje = senderNickname + ": " + mensaje;
                        historialMensajes.add(broadcastMensaje);  // Añadir al historial de mensajes (opcional)
                        broadcastMessage(broadcastMensaje);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void enviarMensaje(String message, InetAddress address, int port) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        for (String nickname : clientes.keySet()) {
            InetAddress clientAddress = clientes.get(nickname);
            int clientPort = puertos.get(nickname);

            try {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, clientAddress, clientPort);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServidor chatServidor = new ChatServidor();
        new Thread(chatServidor).start();
    }
}
