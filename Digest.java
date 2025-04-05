package Version_1;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Classe utilitaire pour le calcul du hash MD5 d'un fichier.
 * Utilisée pour vérifier l'intégrité des fichiers transférés entre client et serveur.
 *
 * Méthode principale : {@code computeMD5(File file)}.
 *
 * Exemple d'utilisation :
 * <pre>{@code
 * String hash = Digest.computeMD5(new File("chemin/vers/fichier.txt"));
 * }</pre>
 */
public class Digest {

    /**
     * Calcule le hash MD5 d'un fichier donné.
     *
     * @param file Le fichier dont on veut calculer le hash.
     * @return La chaîne hexadécimale du hash MD5, ou une chaîne vide en cas d'erreur.
     */
    public static String computeMD5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            fis.close();
            byte[] hashBytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
