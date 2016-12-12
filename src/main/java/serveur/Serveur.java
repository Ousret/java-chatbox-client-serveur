package serveur;

import model.Salon;
import model.Utilisateur;

import javax.net.ServerSocketFactory;
import javax.persistence.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

public class Serveur extends Observable implements Runnable {

    private static Serveur instanceUnique;

    public final Logger logger = Logger.getLogger(Serveur.class.getName());

    private ServerSocketFactory serverSocketFactory = (ServerSocketFactory) ServerSocketFactory.getDefault();
    private ServerSocket serverSocket;

    private final Integer SERVEUR_PORT_DEFAULT = 3307;

    private ArrayList<GestionnaireClient> clients;

    private Thread thread;

    @PersistenceUnit(unitName="Ousret")
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext(unitName="Ousret")
    private EntityManager entityManager;

    /**
     * Création d'une instance serveur
     */
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
     * @return Utilisateur|null
     */
    public Utilisateur getUtilisateur(String unPseudo, String unHash) throws NoResultException
    {
        EntityTransaction entityTransaction = this.entityManager.getTransaction();

        return this.entityManager.createQuery("SELECT u FROM utilisateur u WHERE u.pseudo = :pseudo_cible AND u.secret = :hash_cible", Utilisateur.class)
                .setParameter("pseudo_cible", unPseudo)
                .setParameter("hash_cible", unHash)
                .getSingleResult();
    }

    /**
     * Principe du pattern Singleton
     * @return L'unique instance serveur executé sur la JVM
     */
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

    public void join() throws InterruptedException { this.thread.join(); }

    /**
     * Supprime un client de la liste des clients
     * @param unClient Le client cible
     * @return Vrai si le client a été retiré
     */
    public boolean retirer(GestionnaireClient unClient)
    {
        return this.clients.remove(unClient);
    }

    public void run() {

        GestionnaireClient nouveauGestionnaireClient;

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
                this.logger.info(String.format("<serveur.Serveur:%s:%d> Négociation client", nouveauClient.getInetAddress(), nouveauClient.getPort()));
                nouveauGestionnaireClient = new GestionnaireClient(nouveauClient, this);
                this.clients.add(nouveauGestionnaireClient);
                this.addObserver(nouveauGestionnaireClient);
            }catch (IOException e)
            {
                this.logger.warning("Impossible de traiter un client: "+e.getMessage());
            }
        }

    }

    /**
     * Récupère la liste des salons depuis la base de données
     * @return La liste des salons
     */
    public List<Salon> getEtat()
    {
        return this.entityManager.createQuery("SELECT s FROM salon s", Salon.class).getResultList();
    }

    public static void main(String[] args)
    {
        Serveur kServ = Serveur.getInstance();

        try
        {
            kServ.join();
        }catch (InterruptedException e)
        {
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        System.exit(0);
    }

}
