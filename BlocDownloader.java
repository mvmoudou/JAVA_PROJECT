import java.io.*;
import java.net.*;
import java.util.concurrent.Callable;

public class BlocDownloader implements Callable<byte[]> {
    private final String serverAddress;
    private final int serverPort;
    private final int fileIndex;
    private final int blocIndex;

    public BlocDownloader(String serverAddress, int serverPort, int fileIndex, int blocIndex) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.fileIndex = fileIndex;
        this.blocIndex = blocIndex;
    }

    @Override
    public byte[] call() {
        try (
            Socket socket = new Socket(serverAddress, serverPort);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            out.writeUTF("REQUIRE " + fileIndex + " " + blocIndex);
            int tailleBloc = in.readInt();
            byte[] buffer = new byte[tailleBloc];
            in.readFully(buffer);
            return buffer;
        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement du bloc " + blocIndex);
            e.printStackTrace();
            return new byte[0];
        }
    }
}
