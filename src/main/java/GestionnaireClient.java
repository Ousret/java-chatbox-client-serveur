import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GestionnaireClient implements Runnable {

    Socket socket;
    Serveur instanceMere;

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

            System.out.println("<Debug> Un client n'est plus en ligne.");

            out.close();
            in.close();
            socket.close();
            //instancePrincipale.removeClient(this);

        } catch (Exception e) {

            System.out.println("<Debug> Exception: " + e);

        }

    }
}
