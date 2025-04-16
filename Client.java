import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private static final int tailleBloc = 1024;
    private static final int Dc = 4; // nombre de téléchargements parallèles

    public static void main(String[] args) {
        try (
            Socket socket = new Socket("127.0.0.1", 12345);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            DataInputStream input = new DataInputStream(socket.getInputStream());
            Scanner sc = new Scanner(System.in)
        ) {
            // Demande de la liste des fichiers
            output.writeUTF("LIST");

            int nbFichiers = input.readInt();
            List<String> noms = new ArrayList<>();
            List<Long> tailles = new ArrayList<>();

            System.out.println("Fichiers disponibles :");
            for (int i = 0; i < nbFichiers; i++) {
                String nom = input.readUTF();
                long taille = input.readLong();
                System.out.println(i + " : " + nom + " (" + taille + " octets)");
                noms.add(nom);
                tailles.add(taille);
            }

            System.out.print("Entrez le numéro du fichier à télécharger : ");
            int fileIndex = Integer.parseInt(sc.nextLine());
            String nomFichier = noms.get(fileIndex);
            long tailleFichier = tailles.get(fileIndex);
            int nbBlocs = (int) Math.ceil((double) tailleFichier / tailleBloc);

            byte[] fichierRecu = new byte[(int) tailleFichier];
            ExecutorService pool = Executors.newFixedThreadPool(Dc);
            List<Future<byte[]>> resultats = new ArrayList<>();

            // Téléchargement des blocs
            for (int i = 0; i < nbBlocs; i++) {
                BlocDownloader tache = new BlocDownloader("127.0.0.1", 12345, fileIndex, i);
                Future<byte[]> future = pool.submit(tache);
                resultats.add(future);
            }

            // Réassemblage
            for (int i = 0; i < resultats.size(); i++) {
                byte[] bloc = resultats.get(i).get();
                System.arraycopy(bloc, 0, fichierRecu, i * tailleBloc, bloc.length);
            }

            pool.shutdown();

            // Sauvegarde du fichier
            String nomLocal = UUID.randomUUID().toString() + "_" + nomFichier;
            try (FileOutputStream fos = new FileOutputStream(nomLocal)) {
                fos.write(fichierRecu);
            }
            System.out.println("Fichier téléchargé et enregistré sous : " + nomLocal);

            // Envoi du hash pour vérification (réutilisation du socket initial)
            byte[] hash = Digest.md5(nomLocal);
            String hashHex = bytesToHex(hash);
            System.out.println("Hash du fichier téléchargé : " + hashHex);

            output.writeUTF("HASH " + fileIndex);  // Envoi de la commande HASH
            output.writeUTF(hashHex);  // Envoi du hash au serveur

        } catch (Exception e) {
            e.printStackTrace();
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

