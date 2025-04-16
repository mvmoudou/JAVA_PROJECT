
import java.io.File;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {
    private ExecutorService pool;
	private int port;
	private int poolSize;
    private ServerSocket server;
    private File[] files;
    private final Set<String> trustedClients = Collections.synchronizedSet(new HashSet<>());
    private static final Logger logger = Log.setup("Server", "server.log");


    public Server(int port, int poolSize) {
        this.port = port;
        this.poolSize = poolSize;
        this.pool = Executors.newFixedThreadPool(poolSize);
        File dir = new File("fichiers");
        this.files = dir.listFiles();
        try {
            this.server = new ServerSocket(port);
            System.out.println("Serveur demarre sur le port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void manageRequest() {
        int T = 5;
        double P = 0.3;
        try {
            while(true) {
                this.pool.execute(new Slave(server.accept(), this.files, this.trustedClients, T, P));
                logger.info("Un client s'est connecte au serveur");
                System.out.println("Un client s'est connecte au serveur");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void afficherClientsDeConfiance() {
        System.out.println("Clients de confiance :");
        for (String client : trustedClients) {
            System.out.println("- " + client);
        }
    }

    public static void main(String[] args) {
        int port = 12345;
        int poolSize = 3;
        Server server = new Server(port, poolSize);
        server.manageRequest();
    }
}
