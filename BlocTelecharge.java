package Version_1;

import java.io.*;
import java.net.*;

public class BlocTelecharge implements Runnable {
    private final String fileName;
    private final int blockIndex;
    private final int blockSize;
    private final byte[][] blocs;

    public BlocTelecharge(String fileName, int blockIndex, int blockSize, byte[][] blocs) {
        this.fileName = fileName;
        this.blockIndex = blockIndex;
        this.blockSize = blockSize;
        this.blocs = blocs;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket("localhost", 1234);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Indiquer qu'on veut juste un bloc
            dos.writeUTF(fileName);
            dos.writeInt(blockIndex); // nouveau paramètre côté serveur
            dos.flush();

            // Lire la taille réelle reçue (dernier bloc peut être < blockSize)
            int bytesToRead = dis.readInt(); // le serveur envoie la taille exacte du bloc
            byte[] buffer = new byte[bytesToRead];
            dis.readFully(buffer);

            // Stocker dans la bonne case du tableau partagé
            synchronized (blocs) {
                blocs[blockIndex] = buffer;
            }

            dis.close();
            dos.close();
            socket.close();

            System.out.println("Bloc " + blockIndex + " téléchargé.");

        } catch (Exception e) {
            System.err.println("Erreur bloc " + blockIndex + " : " + e.getMessage());
        }
    }
}
