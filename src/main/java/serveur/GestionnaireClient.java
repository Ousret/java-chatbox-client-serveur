package serveur;

import client.Paquet;
import model.Salon;
import model.SessionCliente;
import model.Utilisateur;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class GestionnaireClient implements Runnable, Observer {

    private final String DEMANDE_INITIALISATION = "HELLO";

    private final String NOTIFIE_FERMETURE = "BYE";

    private final String DEMANDE_AUTHENTIFICATION = "AUTH";
    private final String DEMANDE_ANONYME = "ANONYMOUS";

    private final String AUTH_ERREUR = "NOMATCH";
    private final String AUTH_OK = "MATCH";

    private final String ASK_USERNAME = "ASK_USERNAME";
    private final String ASK_PASSWD = "ASK_PASSWD";

    private final String ASK_ENTER_SALON = "ASK_ENTER_SALON";

    private Socket socket;
    private Serveur instanceMere;

    private Salon salon;
    private SessionCliente sessionCliente;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    @PersistenceUnit(unitName="Ousret")
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext(unitName="Ousret")
    private EntityManager entityManager;

    private Thread currentThread;

    public GestionnaireClient(Socket unSocketClient, Serveur uneInstanceMere)
    {

        this.entityManagerFactory = Persistence.createEntityManagerFactory("Ousret");
        this.entityManager = this.entityManagerFactory.createEntityManager();

        this.socket = unSocketClient;
        this.instanceMere = uneInstanceMere;

        this.currentThread = new Thread(this);
        this.currentThread.setDaemon(true);
        this.currentThread.start();
    }

    /**
     * Vérifie que le client nous envoie bien le message spécifié
     * @param unMessageCible Le message à attendre
     * @return bool
     */
    private boolean waitMessage(String unMessageCible)
    {
        String rIn;

        try
        {
            rIn = (String) this.objectInputStream.readObject();
        }
        catch (IOException e)
        {
            this.instanceMere.logger.warning("Une erreur de stream client est survenue");
            return false;
        }
        catch (ClassNotFoundException e)
        {
            this.instanceMere.logger.warning("Impossible de reconnaitre l'objet envoyé par le client");
            return false;
        }

        return rIn.equals(unMessageCible);
    }

    /**
     * Envoie un message au client
     * @param unMessage Le message à envoyer
     * @return bool
     */
    private boolean envoyerMessage(String unMessage)
    {
        try
        {
            this.objectOutputStream.writeObject(unMessage);
            objectOutputStream.flush();
        }
        catch (IOException e)
        {
            this.instanceMere.logger.warning("Une erreur de stream client est survenue");
            return false;
        }

        return true;
    }

    /**
     * Récupère un objet String depuis le canal input
     * @return String|null
     */
    @Nullable
    private String getMessage()
    {
        String kIn;

        try
        {
            kIn = (String) this.objectInputStream.readObject();
        }
        catch (IOException e)
        {
            return null;
        }
        catch (ClassNotFoundException e)
        {
            return null;
        }

        return kIn;
    }

    /**
     * Ferme la connexion avec le client si existante
     * @return bool
     */
    private boolean fermer()
    {
        if (this.socket.isConnected())
        {
            this.instanceMere.logger.info(String.format("<Client:%s:%d> Fermeture de connexion", this.socket.getInetAddress(), this.socket.getPort()));

            try
            {
                this.objectOutputStream.writeObject(this.NOTIFIE_FERMETURE);

                this.objectInputStream.close();
                this.objectOutputStream.close();
                this.socket.close();
            }
            catch (IOException e)
            {
                this.instanceMere.logger.severe(String.format("<Client:%s:%d> Impossible de fermer correctement les streams ! (%s)", this.socket.getInetAddress(), this.socket.getPort(), e.getMessage()));
                return false;
            }

            this.supprimerSession();

            return true;
        }

        return false;
    }

    /**
     * Gestion de l'authentification, récupère le pseudo et les jetons d'authentifications et créer une session si match il y a.
     * @return bool
     */
    private boolean gererAuth()
    {
        String hash, salt, username;
        Utilisateur utilisateur;

        if (this.envoyerMessage(this.ASK_USERNAME))
        {
            username = this.getMessage();

            if (username != null && this.envoyerMessage(ASK_PASSWD))
            {
                hash = this.getMessage();

                if (hash == null)
                {
                    this.instanceMere.logger.warning(String.format("<Client:%s:%d> Les jetons d'authentifications ne sont pas acquis.", this.socket.getInetAddress(), this.socket.getPort()));
                    return false;
                }

                /* Recherche d'un match */
                try
                {
                    utilisateur = this.instanceMere.getUtilisateur(hash);
                }
                catch (NoResultException e)
                {
                    this.envoyerMessage(this.AUTH_ERREUR);
                    return false;
                }

                if (utilisateur != null)
                {
                    this.envoyerMessage(this.AUTH_OK);
                    this.instanceMere.logger.info(String.format("<Client:%s:%d> Authentifié en tant que '%s'.", this.socket.getInetAddress(), this.socket.getPort(), utilisateur.getPseudo()));
                    this.envoyerMessage(this.creerSession(utilisateur).getUuid());
                    return true;
                }
                else
                {
                    this.envoyerMessage(this.AUTH_ERREUR);
                    return false;
                }
            }

        }

        return false;
    }

    /**
     * Création d'une session pour un utilisateur
     * @param utilisateur L'utilisateur cible
     * @return SessionCliente
     */
    private SessionCliente creerSession(Utilisateur utilisateur)
    {
        EntityTransaction entityTransaction = this.entityManager.getTransaction();
        entityTransaction.begin();

        this.sessionCliente = new SessionCliente(utilisateur, UUID.randomUUID().toString(), new Date("2016/01/01"));
        this.entityManager.persist(this.sessionCliente);
        entityTransaction.commit();

        return this.sessionCliente;
    }

    /**
     * Supprime la session courante si elle existe
     */
    private void supprimerSession()
    {
        if (this.sessionCliente != null)
        {
            this.instanceMere.logger.info(String.format("<Client:%s:%d> Clôture de la session '%s'.", this.socket.getInetAddress(), this.socket.getPort(), this.sessionCliente.getUuid()));
            EntityTransaction entityTransaction = this.entityManager.getTransaction();
            entityTransaction.begin();
            this.entityManager.remove(this.sessionCliente);
            entityTransaction.commit();
        }
    }

    /**
     * Demande au client le choix de salon
     * @return
     */
    private Salon clientChoisirSalon()
    {
        List<Salon> salonsDisponible = this.instanceMere.getEtat();
        Salon salonSelection;

        try
        {
            this.instanceMere.logger.info(String.format("<Client:%s:%d> Envoie des salons disponibles..", this.socket.getInetAddress(), this.socket.getPort()));
            this.objectOutputStream.writeObject(salonsDisponible);
            salonSelection = (Salon) this.objectInputStream.readObject();
        }
        catch (IOException e)
        {
            this.instanceMere.logger.severe(String.format("<Client:%s:%d> Le client n'a pas pu choisir de salon '%s:%s'.", this.socket.getInetAddress(), this.socket.getPort(), this.sessionCliente.getUtilisateur().getPseudo(), this.sessionCliente.getUuid()));
            return null;
        }
        catch (ClassNotFoundException e)
        {
            this.instanceMere.logger.severe(String.format("<Client:%s:%d> Le client n'a pas selectionné de salon valide '%s:%s'.", this.socket.getInetAddress(), this.socket.getPort(), this.sessionCliente.getUtilisateur().getPseudo(), this.sessionCliente.getUuid()));
            return null;
        }

        return salonSelection;
    }

    public void run() {

        String rIn;
        Paquet paquetClient;

        try
        {
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException e)
        {
            this.instanceMere.logger.warning(String.format("Impossible d'obtenir les canaux d'entrée et de sortie pour le client %s:%d", socket.getInetAddress(), socket.getPort()));
            return;
        }

        // 1) Attendre le message HELLO et envoyer à notre tour un HELLO
        if (!this.waitMessage(this.DEMANDE_INITIALISATION) || !this.envoyerMessage(this.DEMANDE_INITIALISATION))
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> Aucune demande d'initialisation valable reçu.", this.socket.getInetAddress(), this.socket.getPort()));
            this.fermer();
            return;
        }

        rIn = this.getMessage();

        // 2) Négocier l'authentification, Anonyme ou identifiant.
        if (rIn == null || !rIn.equals(this.DEMANDE_AUTHENTIFICATION) || !this.gererAuth())
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> Format d'authentification non reconnue. BYE!", this.socket.getInetAddress(), this.socket.getPort()));
            this.fermer();
            return;
        }

        this.instanceMere.logger.info(String.format("<Client:%s:%d> Début de l'écoute client..", this.socket.getInetAddress(), this.socket.getPort()));

        for(;;)
        {

            try
            {
                paquetClient = (Paquet) this.objectInputStream.readObject();
            }
            catch (Exception e)
            {
                this.instanceMere.logger.warning(String.format("<Client:%s:%d> Format de données reçu invalide."));
                this.fermer();
                return;
            }

            if (!paquetClient.getSessionUuid().equals(this.sessionCliente.getUuid()))
            {
                this.instanceMere.logger.warning(String.format("<Client:%s:%d> Cookie érronée, impossible de vérifier l'identité du client."));
                this.fermer();
                return;
            }

            if (paquetClient.getCommande().equals(this.ASK_ENTER_SALON))
            {
                this.clientChoisirSalon();
            }

        }

        // 3) Envoyer la liste des salons disponibles
        //this.clientChoisirSalon();

        // 4) Inscrire dans les events du salon

    }

    public void update(Observable o, Object arg) {

    }
}
