import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Serveur implements Runnable {

    private static Serveur instanceUnique;

    public final Logger logger = Logger.getLogger(Serveur.class.getName());

    private ServerSocketFactory serverSocketFactory = (ServerSocketFactory) ServerSocketFactory.getDefault();
    private ServerSocket serverSocket;

    private final Integer SERVEUR_PORT_DEFAULT = 3307;

    private ArrayList<GestionnaireClient> clients;

    public Thread thread;

    private Serveur()
    {
        this.clients = new ArrayList<GestionnaireClient>();

        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public static Serveur getInstance()
    {
        if (Serveur.instanceUnique != null)
        {
            return Serveur.instanceUnique;
        }else{
            Serveur.instanceUnique = new Serveur();
            return Serveur.instanceUnique;
        }
    }

    public void run() {

        this.logger.info(String.format("Création du serveur sur le port %d", this.SERVEUR_PORT_DEFAULT));

        try
        {
            this.serverSocket = (ServerSocket) this.serverSocketFactory.createServerSocket(this.SERVEUR_PORT_DEFAULT);
        }
        catch (IOException e)
        {
            this.logger.severe("Impossible d'initialiser l'usine de socket Java: "+e.getMessage());
            System.exit(-1);
        }

        this.logger.info("En attente de connexion sur le serveur");

        // En attente de client
        for(;;){
            try
            {
                Socket nouveauClient = this.serverSocket.accept();
                this.logger.info(String.format("<Serveur:%s:%d> Négociation client", nouveauClient.getInetAddress(), nouveauClient.getPort()));
                this.clients.add(new GestionnaireClient(nouveauClient, this));
            }catch (IOException e)
            {
                this.logger.warning("Impossible de traiter un client: "+e.getMessage());
            }
        }

    }

    public static void main(String[] args)
    {
        Serveur kServ = Serveur.getInstance();

        try
        {
            kServ.thread.join();
        }catch (InterruptedException e)
        {
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        System.exit(0);
    }

}
