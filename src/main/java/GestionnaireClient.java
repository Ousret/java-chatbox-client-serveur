import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GestionnaireClient implements Runnable {

    private Socket socket;
    private Serveur instanceMere;

    private Salon salon;
    private Session session;

    public GestionnaireClient(Socket unSocketClient, Serveur uneInstanceMere)
    {
        this.socket = unSocketClient;
        this.instanceMere = uneInstanceMere;

        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {

        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            String inputLine, outputLine;

			/* Boucle pour la communication */
            while ((inputLine = in.readLine()) != null) {
                this.instanceMere.logger.info(String.format("<Client:%s:%d> Message> %s", this.socket.getInetAddress(), this.socket.getPort(), inputLine));
                outputLine = inputLine;
                out.println(outputLine);
                out.flush();
            }

            this.instanceMere.logger.info(String.format("<Client:%s:%d> s'est déconnecté.", this.socket.getInetAddress(), this.socket.getPort()));

            out.close();
            in.close();

            this.socket.close();

        } catch (Exception e) {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> Une exception dans la gestion cliente s'est produite: %s", this.socket.getInetAddress(), this.socket.getPort(), e.getMessage()));
        }

    }
}
