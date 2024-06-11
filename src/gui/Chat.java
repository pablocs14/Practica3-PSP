package gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Chat extends JFrame {
    public JLabel nicknameLabel;
    private JButton sendButton;
    public JTextArea chatBox;
    public JTextField messageTextField;
    private JPanel chatPanel;
    private static String nickname;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort = 12345;

    public Chat(String nickname) {//asdasdasdasdasdas
        Chat.nickname = nickname;
        nicknameLabel.setText(nickname);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageTextField.getText();
                if (!message.isEmpty()) {
                    // Envía el mensaje al servidor
                    sendMessage(message);
                    messageTextField.setText("");
                } else {
                    JOptionPane.showMessageDialog(null, "No puedes enviar un mensaje vacío");
                }
            }
        });

        // Inicializa la UI en el Event Dispatch Thread
        SwingUtilities.invokeLater(this::initializeNetworking);
    }

    private void initializeNetworking() {
        try {
            String host = "localhost";
            serverAddress = InetAddress.getByName(host);
            socket = new DatagramSocket();

            // Envía el nickname al servidor
            sendMessage("/nickname " + nickname);

            // Inicia un nuevo hilo para recibir mensajes continuamente del servidor
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String receivedMessage = new String(packet.getData(), 0, packet.getLength());

                // Usar SwingUtilities.invokeLater para actualizar la UI de forma segura
                SwingUtilities.invokeLater(() -> chatBox.append(receivedMessage + "\n"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        nickname = JOptionPane.showInputDialog("Introduce tu nombre: ");

        // Asegurar la creación de la UI en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            Chat c = new Chat(nickname);

            c.chatBox.setEditable(false);
            c.setContentPane(c.chatPanel);
            c.setSize(400, 500);
            c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            c.setTitle("Chat");
            c.setVisible(true);
        });
    }
}
