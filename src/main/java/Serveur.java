import model.Utilisateur;
import org.hibernate.*;
import org.hibernate.persister.entity.EntityPersister;

import javax.net.ServerSocketFactory;
import javax.persistence.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class Serveur implements Runnable {

    private static Serveur instanceUnique;

    public final Logger logger = Logger.getLogger(Serveur.class.getName());

    private ServerSocketFactory serverSocketFactory = (ServerSocketFactory) ServerSocketFactory.getDefault();
    private ServerSocket serverSocket;

    private final Integer SERVEUR_PORT_DEFAULT = 3307;

    private ArrayList<GestionnaireClient> clients;

    public Thread thread;

    @PersistenceUnit(unitName="Ousret")
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext(unitName="Ousret")
    private EntityManager entityManager;

    private Serveur()
    {
        this.entityManagerFactory = Persistence.createEntityManagerFactory("Ousret");
        this.entityManager = this.entityManagerFactory.createEntityManager();

        this.clients = new ArrayList<GestionnaireClient>();

        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    /**
     * Recherche un utilisateur en ayant un hash et salt
     * @param unHash Le hash sha256
     * @param unSalt Le salt du hash
     * @return Utilisateur|null
     */
    public Utilisateur getUtilisateur(String unHash, String unSalt)
    {
        EntityTransaction entityTransaction = this.entityManager.getTransaction();

        entityTransaction.begin();
        Utilisateur utilisateur = this.entityManager.createQuery("SELECT u FROM utilisateur u WHERE u.hash = :hash_cible AND u.salt = :salt_cible", Utilisateur.class)
                .setParameter("hash_cible", unHash)
                .setParameter("salt_cible", unSalt)
                .getSingleResult();

        return utilisateur;
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
            this.serverSocket = this.serverSocketFactory.createServerSocket(this.SERVEUR_PORT_DEFAULT);
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
