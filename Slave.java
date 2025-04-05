package Version_1;

import java.io.*;
import java.net.*;

public class Slave implements Runnable {
    private Socket clientSocket;

    public Slave(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

            File folder = new File(Serveur.FILES_DIRECTORY);
            if (!folder.exists()) folder.mkdirs();

            File[] files = folder.listFiles();
            if (files != null) {
                dos.writeInt(files.length);
                for (File file : files) {
                    dos.writeUTF(file.getName());
                }
            } else {
                dos.writeInt(0);
            }

            System.out.println(" Nombre de fichiers à envoyer : " + (files != null ? files.length : 0));

            String fileName = dis.readUTF();
            File requestedFile = new File(Serveur.FILES_DIRECTORY, fileName);

            if (!requestedFile.exists()) {
                dos.writeUTF("ERREUR: Fichier non trouvé.");
                clientSocket.close();
                return;
            }

            // Envoyer la taille du fichier
            long fileSize = requestedFile.length();
            dos.writeLong(fileSize);

            // Envoyer le fichier
            FileInputStream fis = new FileInputStream(requestedFile);
            byte[] buffer = new byte[Serveur.BLOCK_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            fis.close();
            System.out.println(" Fichier envoyé : " + fileName);

            // Lire le MD5 du client
            String md5Client = dis.readUTF();
            String md5Server = Digest.computeMD5(requestedFile);
            System.out.println(" MD5 client : " + md5Client);
            System.out.println(" MD5 serveur : " + md5Server);

            if (md5Server.equals(md5Client)) {
                dos.writeUTF("MD5_OK");
                dos.flush();
                System.out.println(" Client a bien reçu le fichier.");
            } else {
                dos.writeUTF("MD5_FAIL");
                dos.flush();
                System.out.println(" Erreur d'intégrité.");
            }

            dis.close();
            dos.close();
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
