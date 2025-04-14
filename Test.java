package Version_1;

import java.io.*;


public class Test {
    public static void main(String[] args) {
        // Valeurs par défaut
        int nbClients = 3;
        String fileName = "file1.txt";

        // Lecture des paramètres depuis la ligne de commande
        for (String arg : args) {
            if (arg.startsWith("--clients=")) {
                nbClients = Integer.parseInt(arg.split("=")[1]);
            } else if (arg.startsWith("--file=")) {
                fileName = arg.split("=")[1];
            }
        }

        try {
            // Lancer le serveur (doit être prêt à accepter des connexions)
            ProcessBuilder serverBuilder = new ProcessBuilder(
                    "java", "Version_1.Serveur"
            );
            serverBuilder.redirectOutput(new File("server_output.log"));
            serverBuilder.redirectError(new File("server_error.log"));
            Process server = serverBuilder.start();
            System.out.println(" Serveur lancé.");

            // Attendre un peu que le serveur démarre bien
            Thread.sleep(2000);

            //  Lancer les clients
            for (int i = 0; i < nbClients; i++) {
                ProcessBuilder clientBuilder = new ProcessBuilder(
                        "java", "Version_1.Client", "--file=" + fileName
                );
                clientBuilder.redirectOutput(new File("client" + i + "_output.log"));
                clientBuilder.redirectError(new File("client" + i + "_error.log"));
                clientBuilder.start();
                System.out.println(" Client " + (i + 1) + " lancé.");
            }

            // Attente de la fin du test
            System.out.println(" Test en cours... patientez.");
            Thread.sleep(10000);

            //  (optionnel) Arrêt du serveur après le test
            server.destroy();
            System.out.println("Serveur arrêté.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
