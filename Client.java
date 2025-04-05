package Version_1;

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1234;
    private static final int BLOCK_SIZE = 1024;
    private static final String DOWNLOAD_DIRECTORY = "Version_1/downloads";

    public static void main(String[] args) {
        String requestedFile = null;

        // Lire le nom du fichier à partir des arguments
        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                requestedFile = arg.substring("--file=".length());
            }
        }

        if (requestedFile == null) {
            System.out.println(" Erreur : aucun fichier spécifié avec --file=...");
            return;
        }

        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println(" Connexion établie avec le serveur.");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Lire la liste des fichiers
            int fileCount = dis.readInt();
            for (int i = 0; i < fileCount; i++) {
                dis.readUTF();
            }

            // Envoyer le nom du fichier
            dos.writeUTF(requestedFile);
            dos.flush();

            // Lire la taille du fichier
            long fileSize = dis.readLong();

            // Préparer dossier
            File downloadFolder = new File(DOWNLOAD_DIRECTORY);
            if (!downloadFolder.exists()) {
                downloadFolder.mkdirs();
            }

            File downloadedFile = new File(DOWNLOAD_DIRECTORY + "/" + requestedFile);
            FileOutputStream fos = new FileOutputStream(downloadedFile);

            byte[] buffer = new byte[BLOCK_SIZE];
            long totalRead = 0;

            while (totalRead < fileSize) {
                int toRead = (int) Math.min(buffer.length, fileSize - totalRead);
                int bytesRead = dis.read(buffer, 0, toRead);
                if (bytesRead == -1) break;
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            fos.close();
            System.out.println(" Fichier téléchargé : " + requestedFile);

            // Envoi MD5
            String md5Client = Digest.computeMD5(downloadedFile);
            System.out.println(" MD5 client : " + md5Client);
            dos.writeUTF(md5Client);
            dos.flush();

            // Réponse serveur
            String serverResponse = dis.readUTF();
            System.out.println(" Réponse du serveur : " + serverResponse);
            if ("MD5_OK".equals(serverResponse)) {
                System.out.println("Intégrité vérifiée.");
            } else {
                System.out.println(" Problème d'intégrité.");
            }

            dis.close();
            dos.close();
            socket.close();
            System.out.println("Connexion terminée.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
