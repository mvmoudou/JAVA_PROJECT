import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class Server {
    private final int port;
    private final ExecutorService pool;
    private final File[] files;
    private final CopyOnWriteArrayList<String> trustedClients;
    private final Semaphore clientSemaphore;
    private final Logger logger = Logger.getLogger("Server");

    public Server(int port, int poolSize) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(poolSize);
        this.trustedClients = new CopyOnWriteArrayList<>();
        this.clientSemaphore = new Semaphore(poolSize-1);

        // Charge les fichiers à partir du répertoire "fichiers"
        File dir = new File("Fichiers");
        if (!dir.exists()) {
            dir.mkdir(); // Crée le répertoire s'il n'existe pas
        }
        this.files = dir.listFiles(); // Liste les fichiers dans le répertoire
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        logger.info("Serveur démarré sur le port " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleConnection(socket)).start();
        }
    }

    private void handleConnection(Socket socket) {
        try {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            String type = input.readUTF();

            if ("CLIENT_MAIN".equals(type)) {
                clientSemaphore.acquire(); // Bloque si trop de clients principaux
                logger.info("-----------CLIENT_MAIN connecté : " + socket.getInetAddress());

                pool.execute(() -> {
                    try {
                        new Slave(socket, files, trustedClients).run();
                    } catch (Exception e) {
                        logger.warning("Erreur CLIENT_MAIN : " + e.getMessage());
                    } finally {
                        clientSemaphore.release(); // Libère une place
                        try {
                            socket.close();
                        } catch (IOException ignored) {}
                        logger.info("-----------CLIENT_MAIN terminé : " + socket.getInetAddress());
                    }
                });

            } else if ("BLOCK_DOWNLOAD".equals(type)) {
                logger.info("-----------BLOCK_DOWNLOAD connecté : " + socket.getInetAddress());

                pool.execute(() -> {
                    try {
                        new Slave(socket, files, trustedClients).run();
                    } catch (Exception e) {
                        logger.warning("Erreur BLOCK_DOWNLOAD : " + e.getMessage());
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException ignored) {}
                        logger.info("-----------BLOCK_DOWNLOAD terminé : " + socket.getInetAddress());
                    }
                });

            } else {
                logger.warning("Client inconnu rejeté : " + socket.getInetAddress());
                socket.close();
            }

        } catch (IOException | InterruptedException e) {
            logger.warning("Erreur de lecture de type client : " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 12345; // Port du serveur
        int poolSize = 3;  // Taille du pool de threads
        Server server = new Server(port, poolSize);
        server.start();
    }
}
