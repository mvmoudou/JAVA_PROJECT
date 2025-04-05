package Version_1;

import java.io.*;
import java.net.*;

public class Serveur {
    public static final int PORT = 1234;
    public static final String FILES_DIRECTORY = "Version_1/fichiers"; // Dossier où sont stockés les fichiers
    public static final int BLOCK_SIZE = 1024; // Taille des blocs (1 KB)

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Serveur en attente de connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(" Nouveau client connecté : " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                // Gérer le client dans un thread séparé
                new Thread(new Slave(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
