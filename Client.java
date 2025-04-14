package Version_1;

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1234;
    public static final int BLOCK_SIZE = 10;
    private static final String DOWNLOAD_DIRECTORY = "Version_1/downloads";

    public static void main(String[] args) {
        String requestedFile = null;
        int dc = 1; // Nombre de connexions par défaut

        // Lire les arguments
        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                requestedFile = arg.substring("--file=".length());
            } else if (arg.startsWith("--DC=")) {
                dc = Integer.parseInt(arg.substring("--DC=".length()));
            }
        }

        if (requestedFile == null) {
            System.out.println("Erreur : veuillez spécifier un fichier avec --file=...");
            return;
        }

        try {
            // Étape 1 : connexion initiale pour obtenir la taille du fichier
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            // Envoyer juste le nom du fichier (pas de blocIndex)
            dos.writeUTF(requestedFile);
            dos.flush();

            long fileSize = dis.readLong(); // taille totale
            // Indiquer qu'on demande juste la taille (flag = -1)
            dos.writeInt(-1); // flag spécial
            dos.writeUTF(requestedFile);
            dis.close();
            dos.close();
            socket.close();

            System.out.println("Taille du fichier : " + fileSize + " octets");

            // Étape 2 : calcul du nombre de blocs
            int totalBlocs = (int) Math.ceil((double) fileSize / BLOCK_SIZE);
            byte[][] blocs = new byte[totalBlocs][];

            // Étape 3 : lancer les threads pour télécharger les blocs
            Thread[] threads = new Thread[totalBlocs];
            for (int i = 0; i < totalBlocs; i++) {
                threads[i] = new Thread(new BlocTelecharge(requestedFile, i, BLOCK_SIZE, blocs));
                threads[i].start();
            }

            // Étape 4 : attendre la fin des threads
            for (Thread t : threads) {
                t.join();
            }

            // Étape 5 : assembler les blocs dans le fichier final
            File downloadFolder = new File(DOWNLOAD_DIRECTORY);
            if (!downloadFolder.exists()) downloadFolder.mkdirs();

            File outputFile = new File(DOWNLOAD_DIRECTORY + "/" + requestedFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            for (int i = 0; i < blocs.length; i++) {
                fos.write(blocs[i]);
            }
            fos.close();

            System.out.println("Fichier reconstruit avec succès : " + outputFile.getName());

            // Étape 6 : vérification MD5
            String md5 = Digest.computeMD5(outputFile);
            System.out.println("MD5 client : " + md5);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
