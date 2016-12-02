package client;

import model.Salon;
import model.Utilisateur;

import javax.net.SocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Observable;
import java.util.logging.Logger;

import java.security.*;

public class Client extends Observable implements Runnable {

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

    public final Logger logger = Logger.getLogger(Client.class.getName());

    private String identifiant, adresseIp, phraseSecrete, sessionUuid;
    private Integer port;

    private List<Salon> salons;
    private Salon salon;

    private SocketFactory factory=(SocketFactory) SocketFactory.getDefault();
    private Socket socket = null;

    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    private Thread thread;

    public Client(String uneAdresseIP, Integer unNumeroPort, String unIdentifiant, String unePhraseSecrete)
    {

        this.identifiant = unIdentifiant;
        this.adresseIp = uneAdresseIP;
        this.port = unNumeroPort;

        try
        {
            this.phraseSecrete = Client.hash256(unePhraseSecrete);
            this.logger.info(this.phraseSecrete);
        }
        catch (NoSuchAlgorithmException e)
        {
            this.logger.severe(e.getMessage());
        }

        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();

    }

    public String getIdentifiant() { return this.identifiant; }

    public void join() throws InterruptedException { this.thread.join(); }

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
            this.logger.warning(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas envoyer de données car connexion inactive.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionUuid));
            return false;
        }

        Paquet paquet = new Paquet(this.sessionUuid, uneCommande, uneDonnee);

        try
        {
            this.objectOutputStream.writeObject(paquet);
            return true;
        }
        catch (IOException e)
        {
            this.logger.severe(String.format("<Client:%s:%d:/sId:%s/> Ne peux pas envoyer de données car IOException '%s'.", this.socket.getInetAddress().toString(), this.socket.getPort(), this.sessionUuid, e.getMessage()));
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
            this.logger.warning("Ne peux pas envoyer de données car connexion inactive.");
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
            this.logger.severe(String.format("Ne peux pas recevoir de données car IOException '%s'.", e.getMessage()));
        }
        catch (ClassNotFoundException e)
        {
            this.logger.severe(String.format("Ne peux pas recevoir de données car ClassNotFoundException '%s'.", e.getMessage()));
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

    private boolean connecter()
    {
        try {

            this.socket = factory.createSocket(this.adresseIp, this.port);
            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());

            return true;

        }catch(UnknownHostException e)
        {
            this.logger.severe(e.getMessage());
        }catch(IOException e)
        {
            this.logger.severe(e.getMessage());
        }

        return false;
    }

    /**
     * Vérifie que le client nous envoie bien le message spécifié
     * @param unMessageCible Le message à attendre
     * @return bool
     */
    @Deprecated
    private boolean waitMessage(String unMessageCible)
    {
        if (!this.isOnline())
        {
            this.logger.warning("Il faut au préalable avoir ouvert une connexion pour recevoir une commande brute.");
            return false;
        }

        String rIn;

        try
        {
            rIn = (String) this.objectInputStream.readObject();
        }
        catch (IOException e)
        {
            this.logger.warning("Une erreur de stream client est survenue");
            return false;
        }
        catch (ClassNotFoundException e)
        {
            this.logger.warning("Impossible de reconnaitre l'objet envoyé par le client");
            return false;
        }

        return rIn.equals(unMessageCible);
    }

    /**
     * Attendre un message au format String
     * @return String|null
     */
    @Deprecated
    private String waitMessage()
    {
        if (!this.isOnline())
        {
            this.logger.warning("Il faut au préalable avoir ouvert une connexion pour recevoir une commande brute.");
            return "";
        }

        String rIn;

        try
        {
            rIn = (String) this.objectInputStream.readObject();
        }
        catch (IOException e)
        {
            this.logger.warning("Une erreur de stream client est survenue");
            return null;
        }
        catch (ClassNotFoundException e)
        {
            this.logger.warning("Impossible de reconnaitre l'objet envoyé par le client");
            return null;
        }

        return rIn;
    }

    /**
     * Envoie un message au client (Commande String)
     * @param unMessage Le message à envoyer
     * @return bool
     */
    @Deprecated
    private boolean envoyerMessage(String unMessage)
    {
        if (!this.isOnline())
        {
            this.logger.warning("Il faut au préalable avoir ouvert une connexion pour envoyer une commande brute.");
            return false;
        }

        try
        {
            this.objectOutputStream.writeObject(unMessage);
            objectOutputStream.flush();
        }
        catch (IOException e)
        {
            this.logger.warning("Une erreur de stream client est survenue");
            return false;
        }

        return true;
    }

    private boolean authentification()
    {
        if (this.isAuthenticated())
        {
            this.logger.warning("Vous êtes déjà authentifié.");
            return false;
        }

        this.logger.info("Début de la procedure d'authentification.");

        if (!this.sendPaquet(this.DEMANDE_INITIALISATION, null) || !this.getPaquet(this.DEMANDE_INITIALISATION))
        {
            this.logger.severe("Le serveur ne répond pas à la demande HELLO. Échec critique..");
            return false;
        }

        this.logger.info("Demande au serveur l'authentification.");

        if (this.sendPaquet(this.DEMANDE_AUTHENTIFICATION, new Utilisateur(this.identifiant, null, this.phraseSecrete)))
        {

            Paquet authRes = this.getPaquet();

            if (authRes != null && authRes.getCommande().equals(this.AUTH_OK))
            {
                this.logger.info("Identifiants reconnus par le serveur, en attente de récuperer la session id..");
                this.sessionUuid = (String) authRes.getData();

                if (this.sessionUuid != null)
                {
                    this.logger.info(String.format("Identifiant de session: %s", this.sessionUuid));
                    return true;
                }
                else
                {
                    this.logger.severe("Le serveur ne nous a pas attribué de session id");
                    return false;
                }
            }
            else
            {
                this.logger.warning("Identifiants non reconnus");
                return false;
            }

        }

        this.logger.severe("La demande d'authentification n'a pas aboutie.");
        return false;

    }

    public List<Salon> getSalons()
    {
        if (!this.isAuthenticated())
        {
            this.logger.warning("Impossible d'émettre un message sans être authentifié.");
            return null;
        }

        Paquet paquet = new Paquet(this.sessionUuid, this.ASK_SALONS, null);

        if (!this.sendPaquet(this.ASK_SALONS, null))
        {
            this.logger.severe("La demande de liste salon n'a pas aboutie.");
            return null;
        }

        paquet = this.getPaquet();

        if (paquet != null)
        {
            return (List<model.Salon>) paquet.getData();
        }

        return null;
    }

    /**
     * Selectionne un nouveau salon
     * @param unSalon Le salon cible (doit exister..)
     * @return bool
     */
    public boolean setSalon(Salon unSalon)
    {
        if (!this.isAuthenticated())
        {
            this.logger.warning("Impossible d'émettre un message sans être authentifié.");
            return false;
        }

        if (!this.sendPaquet(this.ASK_ENTER_SALON, unSalon))
        {
            this.logger.warning("Erreur dans la demande de changement de salon");
            return false;
        }

        if (this.getPaquet(this.OK))
        {
            this.salon = unSalon;
            return true;
        }

        return false;
    }

    public boolean isOnline() { return this.socket.isConnected(); }
    public boolean isAuthenticated() { return this.isOnline() && this.sessionUuid != null;}
    public Salon getSalon() { return this.salon; }

    /**
     * Envoyer un message
     * @param unMessage Le corps du message
     * @return bool
     */
    public boolean nouveauMessage(String unMessage)
    {
        if (!this.isAuthenticated())
        {
            this.logger.warning("Impossible d'émettre un message sans être authentifié.");
            return false;
        }else if(this.getSalon() == null)
        {
            this.logger.warning("Il faut d'abord rejoindre un salon avant d'émettre un message.");
            return false;
        }

        return this.sendPaquet(this.ASK_MESSAGE, unMessage);
    }

    public void run() {

        this.logger.info("Lancement du client..");

        /* 1) Connexion */
        if (!this.connecter())
        {
            this.logger.severe("Impossible d'établir une connexion au serveur");
            return;
        }

        /* 2) Authentification */
        if (!this.authentification())
        {
            this.logger.severe("Authentification échouée.");
            return;
        }

        List<Salon> salons = this.getSalons();

        this.logger.info(salons.toString());

        if (this.setSalon(salons.get(0)))
        {
            this.logger.info(String.format("Passage vers le salon '%s'.", salons.get(0).getDesignation()));
            this.nouveauMessage("Salut les enfants!!!");
            this.nouveauMessage("Comment allez-vous ?");
        }

        for (;;)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (Exception e)
            {
                continue;
            }
        }

    }

    public static void main(String[] args)
    {
        Client kClient = new Client("127.0.0.1", 3307, "Ousret", "azerty");

        try
        {
            kClient.join();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

    }

    public static String hash256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
