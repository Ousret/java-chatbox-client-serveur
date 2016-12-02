package serveur;

import client.Paquet;
import model.Message;
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

    private final String ASK_SALONS = "ASK_SALONS";

    private final String ASK_MESSAGE = "ASK_MESSAGE";

    private final String OK = "OK";
    private final String KO = "KO";

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

    private boolean isOnline() { return this.socket.isConnected(); }
    private boolean isAuthenticated() { return this.isOnline() && this.sessionCliente != null; }

    /**
     * Envoie un paquet d'instruction vers le client
     * @param uneCommande Le nom de la commande
     * @param uneDonnee Le conteneur d'objet
     * @return bool
     */
    private boolean sendPaquet(String uneCommande, Object uneDonnee)
    {
        if (!this.isOnline())
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas envoyer de données car connexion inactive.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionCliente));
            return false;
        }

        Paquet paquet = new Paquet(this.sessionCliente != null ? this.sessionCliente.getUuid() : null, uneCommande, uneDonnee);

        try
        {
            this.objectOutputStream.writeObject(paquet);
            return true;
        }
        catch (IOException e)
        {
            this.instanceMere.logger.severe(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas envoyer de données car IOException '%s'.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionCliente, e.getMessage()));
        }

        return false;
    }

    /**
     * Récupère un paquet en attente de lecture sur le objectInputStream
     * @return Paquet|null
     */
    private Paquet getPaquet()
    {
        if (!this.isOnline())
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas envoyer de données car connexion inactive.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionCliente));
            return null;
        }

        Paquet paquet;

        try
        {
            paquet = (Paquet) this.objectInputStream.readObject();
            return paquet;
        }
        catch (IOException e)
        {
            this.instanceMere.logger.severe(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas recevoir de données car IOException '%s'.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionCliente, e.getMessage()));
        }
        catch (ClassNotFoundException e)
        {
            this.instanceMere.logger.severe(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas recevoir de données car ClassNotFoundException '%s'.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionCliente, e.getMessage()));
        }

        return null;
    }

    /**
     * Vérifie la présence d'un paquet en attente
     * @param uneCommandeAttendue La commande que l'on attend
     * @return bool
     */
    private boolean getPaquet(String uneCommandeAttendue)
    {
        Paquet paquet = this.getPaquet();
        return paquet != null && paquet.getCommande().equals(uneCommandeAttendue);
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
                this.objectOutputStream.writeObject(new Paquet(this.sessionCliente.getUuid(), this.NOTIFIE_FERMETURE, null));

                this.objectInputStream.close();
                this.objectOutputStream.close();
                this.socket.close();

                this.instanceMere.retirer(this);
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
    private boolean authentification(Paquet unPaquetAuth)
    {
        if (unPaquetAuth == null || !unPaquetAuth.getCommande().equals(this.DEMANDE_AUTHENTIFICATION))
        {
            this.instanceMere.logger.severe(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas authentifier le client car la commande ne correspond pas.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionCliente));
            return false;
        }

        Utilisateur utilisateur = (Utilisateur) unPaquetAuth.getData();

        /* Recherche d'un match */
        try
        {
            utilisateur = this.instanceMere.getUtilisateur(utilisateur.getPseudo(), utilisateur.getSecret());
        }
        catch (NoResultException e)
        {
            this.instanceMere.logger.info(String.format("<Client:%s:%d> Les jetons d'authentification ne correspondent a aucun utilisateur.", this.socket.getInetAddress(), this.socket.getPort()));
            return false;
        }

        if (utilisateur != null)
        {
            this.instanceMere.logger.info(String.format("<Client:%s:%d> Authentifié en tant que '%s'.", this.socket.getInetAddress(), this.socket.getPort(), utilisateur.getPseudo()));

            this.sessionCliente = this.creerSession(utilisateur);
            this.sendPaquet(this.AUTH_OK, this.sessionCliente.getUuid());

            return true;
        }

        this.sendPaquet(this.AUTH_ERREUR, null);
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
     * Envoie la liste des salons disponibles
     * @return bool
     */
    private boolean envoyerSalons()
    {
        return this.sendPaquet(this.ASK_SALONS, this.instanceMere.getEtat());
    }

    private boolean setSalon(Salon unSalon)
    {
        this.salon = unSalon;
        return this.sendPaquet(this.OK, unSalon);
    }

    private boolean publierMessage(String unMessage)
    {
        Paquet paquet = new Paquet(this.sessionCliente.getUuid(), this.OK, unMessage);

        try
        {
            EntityTransaction entityTransaction = this.entityManager.getTransaction();
            entityTransaction.begin();
            Message message = new Message(new Date(), this.sessionCliente.getUtilisateur(), false, unMessage, this.salon);
            this.entityManager.persist(message);
            entityTransaction.commit();
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    public void run() {

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
        if (!this.sendPaquet(this.DEMANDE_INITIALISATION, null) || !this.getPaquet(this.DEMANDE_INITIALISATION))
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> Aucune demande d'initialisation valable reçu.", this.socket.getInetAddress(), this.socket.getPort()));
            this.fermer();
            return;
        }

        // 2) Négocier l'authentification, Anonyme ou identifiant.
        if (!this.authentification(this.getPaquet()))
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> L'authentification n'a pas aboutie, BYE!", this.socket.getInetAddress(), this.socket.getPort()));
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
                this.instanceMere.logger.warning(String.format("<Client:%s:%d> Fin de communication client", this.socket.getInetAddress(), this.socket.getPort()));
                this.fermer();
                return;
            }

            if (!paquetClient.getSessionUuid().equals(this.sessionCliente.getUuid()))
            {
                this.instanceMere.logger.warning(String.format("<Client:%s:%d> Cookie érronée, impossible de vérifier l'identité du client.", this.socket.getInetAddress(), this.socket.getPort()));
                this.fermer();
                return;
            }

            /* Vérification de la commande */
            if (paquetClient.getCommande().equals(this.ASK_SALONS))
            {
                if (this.envoyerSalons())
                {
                    this.instanceMere.logger.info(String.format("<Client:%s:%d> Envoie de la liste des salons", this.socket.getInetAddress(), this.socket.getPort()));
                }
                else
                {
                    this.instanceMere.logger.warning(String.format("<Client:%s:%d> Impossible d'émettre la liste des salons..", this.socket.getInetAddress(), this.socket.getPort()));
                }
            }
            else if(paquetClient.getCommande().equals(this.ASK_ENTER_SALON))
            {
                Salon nouveauSalon = (Salon) paquetClient.getData();
                this.instanceMere.logger.info(String.format("<Client:%s:%d> Passage sur le salon '%s'.", this.socket.getInetAddress(), this.socket.getPort(), nouveauSalon.getDesignation()));
                this.setSalon(nouveauSalon);
            }
            else if(paquetClient.getCommande().equals(this.ASK_MESSAGE))
            {
                this.instanceMere.logger.info(String.format("<Client:%s:%d> Message '%s'.", this.socket.getInetAddress(), this.socket.getPort(),(String) paquetClient.getData()));
            }

        }

    }

    public void update(Observable o, Object arg) {

    }
}
