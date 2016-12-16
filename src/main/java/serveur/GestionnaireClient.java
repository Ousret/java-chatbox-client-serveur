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
     * Récupère le salon selectionnée par le client
     * @return Salon
     */
    public Salon getSalon() { return this.salon; }

    /**
     * Récupère l'utilisateur courant de ce thread
     * @return Utilisateur | null
     */
    public Utilisateur getUtilisateur()
    {
        return this.isAuthenticated() ? this.sessionCliente.getUtilisateur() : null;
    }

    /**
     * Vérifie si le client est encore relié au serveur
     * @return Vrai si c'est le cas
     */
    public boolean isOnline() { return this.socket.isConnected(); }

    /**
     * Vérifie si le client est authentifié auprès du serveur
     * @return Vrai si c'est le cas
     */
    public boolean isAuthenticated() { return this.isOnline() && this.sessionCliente != null; }

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
                this.objectOutputStream.writeObject(new Paquet(this.sessionCliente.getUuid(), Paquet.NOTIFIE_FERMETURE, null));

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

            return this.instanceMere.detruireSession(this.sessionCliente) && this.instanceMere.retirer(this);
        }

        return false;
    }

    /**
     * Gestion de l'authentification, récupère le pseudo et les jetons d'authentifications et créer une session si match il y a.
     * @return bool
     */
    private boolean authentification(Paquet unPaquetAuth)
    {
        if (unPaquetAuth == null || !unPaquetAuth.getCommande().equals(Paquet.DEMANDE_AUTHENTIFICATION))
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

            this.sessionCliente = this.instanceMere.creerSession(utilisateur);
            this.sendPaquet(Paquet.AUTH_OK, this.sessionCliente.getUuid());

            return true;
        }

        this.sendPaquet(Paquet.AUTH_ERREUR, null);
        return false;
    }

    /**
     * Envoie la liste des salons disponibles
     * @return bool
     */
    private boolean envoyerSalons()
    {
        return this.sendPaquet(Paquet.ASK_SALONS, this.instanceMere.getSalons());
    }

    /**
     * Envoie la liste des utilisateurs
     * @return Vrai si la liste a été envoyé
     */
    private boolean envoyerSalonUtilisateurs()
    {
        return this.sendPaquet(Paquet.ASK_SALON_USERS, this.instanceMere.getSalonUtilisateurs(this.getSalon()));
    }

    /**
     * Change le salon sur lequel le client est assigné
     * @param unSalon Le salon cible
     * @return Vrai si le salon a été changé
     */
    private boolean setSalon(Salon unSalon)
    {
        this.salon = unSalon;
        return this.sendPaquet(Paquet.OK, unSalon);
    }

    /**
     * Publie un message sur le salon
     * @param unMessage Le message à publier
     * @return Vrai si le message est publié
     */
    private boolean publierMessage(String unMessage)
    {

        if (!this.isAuthenticated())
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> Impossible de publier le message '%s' (anonyme)", socket.getInetAddress(), socket.getPort(), unMessage));
            this.sendPaquet(Paquet.KO, unMessage);
            return false;
        }
        else if(this.getSalon() == null)
        {
            this.instanceMere.logger.warning(String.format("<Client:%s:%d> Impossible de publier le message '%s' (aucun salon)", socket.getInetAddress(), socket.getPort(), unMessage));
            this.sendPaquet(Paquet.KO, unMessage);
            return false;
        }

        return this.instanceMere.publierMessage(this.getUtilisateur(), this.getSalon(), unMessage) && this.sendPaquet(Paquet.OK, unMessage);
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
        if (!this.sendPaquet(Paquet.DEMANDE_INITIALISATION, null) || !this.getPaquet(Paquet.DEMANDE_INITIALISATION))
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

        // Boucle d'écoute du client
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
            if (paquetClient.getCommande().equals(Paquet.ASK_SALONS))
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
            else if(paquetClient.getCommande().equals(Paquet.ASK_ENTER_SALON))
            {
                Salon nouveauSalon = (Salon) paquetClient.getData();

                this.instanceMere.logger.info(String.format("<Client:%s:%d> Passage sur le salon '%s'.", this.socket.getInetAddress(), this.socket.getPort(), nouveauSalon.getDesignation()));
                this.setSalon(nouveauSalon);

                this.instanceMere.notifierUtilisateursSalon(this.getSalon(), new Paquet(null, Paquet.ENTRER_UTILISATEUR, this.getUtilisateur()));
            }
            else if(paquetClient.getCommande().equals(Paquet.ASK_MESSAGE))
            {
                this.instanceMere.logger.info(String.format("<Client:%s:%d> Message '%s'.", this.socket.getInetAddress(), this.socket.getPort(),(String) paquetClient.getData()));
                if (!this.publierMessage((String) paquetClient.getData()))
                {
                    this.instanceMere.logger.warning(String.format("<Client:%s:%d> Message non publié :: '%s'.", this.socket.getInetAddress(), this.socket.getPort(),(String) paquetClient.getData()));
                }
            }
            else if(paquetClient.getCommande().equals(Paquet.ASK_SALON_USERS))
            {
                this.instanceMere.logger.warning(String.format("<Client:%s:%d> Envoie la liste des utilisateurs du salon..", this.socket.getInetAddress(), this.socket.getPort()));
                if (!this.envoyerSalonUtilisateurs())
                {
                    this.instanceMere.logger.warning(String.format("<Client:%s:%d> Impossible de transmettre la liste des utilisateurs du salon", this.socket.getInetAddress(), this.socket.getPort()));
                }
            }
            else if(paquetClient.getCommande().equals(Paquet.NOTIFIE_FERMETURE))
            {
                this.instanceMere.logger.info(String.format("<Client:%s:%d> Demande la fermeture de la connexion..", this.socket.getInetAddress(), this.socket.getPort()));
                this.fermer();
                break;
            }

        }

    }

    public void update(Observable o, Object arg) {
        Paquet paquet = (Paquet) arg;

        if (paquet.getCommande().equals(Paquet.NOUVEAU_MESSAGE))
        {
            Message message = (Message) paquet.getData();

            if (!message.getAuteur().equals(this.getUtilisateur()) && this.salon.equals(message.getSalon()))
            {
                this.sendPaquet(Paquet.NOUVEAU_MESSAGE, message);
            }
        }
        else if(paquet.getCommande().equals(Paquet.SORTIE_UTILISATEUR))
        {
            this.sendPaquet(Paquet.SORTIE_UTILISATEUR, paquet.getData());
        }
        else if(paquet.getCommande().equals(Paquet.ENTRER_UTILISATEUR) && !((Utilisateur)paquet.getData()).equals(this.getUtilisateur()))
        {
            this.sendPaquet(Paquet.ENTRER_UTILISATEUR, paquet.getData());
        }

    }
}
