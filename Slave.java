import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;

public class Slave implements Runnable {
    private Socket socket;
    private File[] files;
    private static final int tailleBloc = 1024; // Taille d'un bloc de téléchargement (en octets)
    private Set<String> trustedClients;
    private static final Logger logger = Log.setup("Slave", "slave.log");
    private int T;
    private double P;

    public Slave(Socket socket, File[] files, Set<String> trustedClients, int T, double P) {
        this.socket = socket;
        this.files = files;
        this.trustedClients = trustedClients;
        this.T = T; // Nombre de téléchargements simultanés
        this.P = P; // Probabilité de corruption
    }

    @Override
    public void run() {
        try (
            DataOutputStream outputClient = new DataOutputStream(socket.getOutputStream());
            DataInputStream inputClient = new DataInputStream(socket.getInputStream())
        ) {
            // Lire la demande du client
            String commande = inputClient.readUTF();
            logger.info(commande + " : " + socket.getInetAddress().getHostAddress());
            
            if ("LIST".equals(commande)) {
                // Envoyer la liste des fichiers disponibles au client
                sendFileList(outputClient);
            } else if (commande.startsWith("REQUIRE")) {
                // Si la commande est de type "REQUIRE", on gère le téléchargement d'un bloc
                handleDownload(commande, outputClient);
                return;
            }

            String hashCommande = inputClient.readUTF();
            if (hashCommande.startsWith("HASH")) {
                // Si la commande est de type "HASH", on gère la vérification du hash
                handleFileHash(inputClient, outputClient, hashCommande);
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFileList(DataOutputStream outputClient) throws IOException {
        // Envoyer la liste des fichiers disponibles au client
        logger.info("Envoi de la liste des fichiers disponibles au client.");
        outputClient.writeInt(files.length);  // Envoi du nombre de fichiers
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                outputClient.writeUTF(files[i].getName()); // Nom du fichier
                outputClient.writeLong(files[i].length()); // Taille du fichier
                logger.info(files[i].getName() + " : " + files[i].length() + " octets");
            }
        }
    }

    private void handleFileHash(DataInputStream inputClient, DataOutputStream outputClient, String commande) throws IOException {
        // Recevoir et traiter le hash envoyé par le client
        logger.info("Traitement de la commande de hash.");
        String fileIndex = commande.split(" ")[1];  // Extraire l'index du fichier
        logger.info(commande);
        int index = Integer.parseInt(fileIndex);
        File fichier = files[index];

        String hashServeur;
        try {
            hashServeur = bytesToHex(Digest.md5(fichier.getPath()));
            logger.info("hashServ : " + hashServeur);
            String hashClient = inputClient.readUTF(); // Hash reçu du client
            logger.info("hashCli : " + hashClient);

        // Comparer les hashes
        if (hashServeur.equals(hashClient)) {
            System.out.println("Le fichier a bien été téléchargé et vérifié.");
            // Ajout du client à la liste des clients de confiance
            String clientInfo = socket.getInetAddress().getHostAddress();
            synchronized (trustedClients) {
                trustedClients.add(clientInfo);
                logger.info("Ajouté à la liste des clients de confiance : " + clientInfo);
                System.out.println("Ajouté à la liste des clients de confiance : " + clientInfo);
            }            
            outputClient.writeUTF("OK");
        } else {
            System.out.println("Le fichier a été corrompu pendant le transfert.");
            logger.warning("Le fichier a été corrompu pendant le transfert.");
            outputClient.writeUTF("ERROR");
        }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleDownload(String commande, DataOutputStream outputClient) throws IOException {
        // Extraire l'index du fichier et l'index du bloc à télécharger
        logger.info("Traitement de la commande de téléchargement.");
        String[] parts = commande.split(" ");
        int fileIndex = Integer.parseInt(parts[1]);
        int blockIndex = Integer.parseInt(parts[2]);
        logger.info(commande);

        File fichier = files[fileIndex];
        long fileSize = fichier.length();
        long startByte = blockIndex * tailleBloc;
        long endByte = Math.min(startByte + tailleBloc, fileSize);
        logger.info("Fichier : " + fichier.getName() + ", Bloc : " + blockIndex + ", Taille : " + (endByte - startByte) + " octets");

        // Envoi du nom et de la taille du bloc
        outputClient.writeInt((int)(endByte - startByte)); // Taille du bloc

        // Lecture du fichier et envoi du bloc au client
        try (FileInputStream fis = new FileInputStream(fichier)) {
            byte[] buffer = new byte[tailleBloc];
            fis.skip(startByte); // Sauter jusqu'à l'index du bloc
            int bytesRead = fis.read(buffer, 0, (int)(endByte - startByte));
            outputClient.write(buffer, 0, bytesRead); // Envoi du bloc au client
            System.out.println("Bloc " + blockIndex + " du fichier " + fichier.getName() + " envoyé.");
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}