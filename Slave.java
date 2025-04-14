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
    
            String fileName = dis.readUTF();
            File file = new File(Serveur.FILES_DIRECTORY, fileName);
    
            if (!file.exists()) {
                dos.writeUTF("ERREUR: Fichier non trouvé.");
                clientSocket.close();
                return;
            }
    
            // ✅ ESSAYER de lire un entier (numéro de bloc)
            clientSocket.setSoTimeout(100); // délai court pour savoir si un entier arrive
            int blockIndex = -1;
            boolean isBlockRequest = false;
    
            try {
                blockIndex = dis.readInt();
                isBlockRequest = true;
            } catch (IOException e) {
                // Pas de numéro de bloc → mode normal
            }
    
            if (isBlockRequest) {
                // 🔹 Mode DC (envoi d’un bloc spécifique)
                long fileLength = file.length();
                long skipBytes = (long) blockIndex * Serveur.BLOCK_SIZE;
    
                if (skipBytes >= fileLength) {
                    dos.writeInt(-1);
                    clientSocket.close();
                    return;
                }
    
                FileInputStream fis = new FileInputStream(file);
                fis.skip(skipBytes);
    
                int bytesToSend = (int) Math.min(Serveur.BLOCK_SIZE, fileLength - skipBytes);
                byte[] buffer = new byte[bytesToSend];
                int read = fis.read(buffer);
    
                dos.writeInt(read);
                dos.write(buffer, 0, read);
                dos.flush();
    
                System.out.println("Bloc " + blockIndex + " envoyé.");
                fis.close();
            } else {
                // 🔹 Mode classique (fichier entier)
                dos.writeLong(file.length());
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[Serveur.BLOCK_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
                fis.close();
    
                System.out.println("Fichier envoyé : " + fileName);
    
                // Vérifier MD5
                String md5Client = dis.readUTF();
                String md5Server = Digest.computeMD5(file);
                System.out.println(" MD5 client : " + md5Client);
                System.out.println(" MD5 serveur : " + md5Server);
    
                if (md5Server.equals(md5Client)) {
                    dos.writeUTF("MD5_OK");
                    System.out.println("Client a bien reçu le fichier.");
                } else {
                    dos.writeUTF("MD5_FAIL");
                    System.out.println("Erreur d'intégrité.");
                }
            }
    
            dis.close();
            dos.close();
            clientSocket.close();
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
