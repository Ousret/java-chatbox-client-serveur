package client;

import model.Salon;

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

    public final Logger logger = Logger.getLogger(Client.class.getName());

    private String identifiant, adresseIp, phraseSecrete, sessionUuid;
    private Integer port;

    private List<Salon> salons;

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

    private boolean connecter()
    {
        try {

            this.socket = factory.createSocket(this.adresseIp, this.port);
            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());

        }catch(UnknownHostException e)
        {
            this.logger.severe(e.getMessage());
            return false;
        }catch(IOException e)
        {
            this.logger.severe(e.getMessage());
            return false;
        }

        return true;
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

    private String waitMessage()
    {
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
            this.logger.warning("Une erreur de stream client est survenue");
            return false;
        }

        return true;
    }

    private boolean authentification()
    {
        this.logger.info("Début de la procedure d'authentification.");

        if (!this.envoyerMessage(this.DEMANDE_INITIALISATION) || !this.waitMessage(this.DEMANDE_INITIALISATION))
        {
            this.logger.severe("Le serveur ne répond pas à la demande HELLO. Échec critique..");
            return false;
        }

        this.logger.info("Demande au serveur l'authentification.");

        if (this.envoyerMessage(this.DEMANDE_AUTHENTIFICATION))
        {
            if (!this.waitMessage(this.ASK_USERNAME))
            {
                this.logger.severe("Le serveur n'a pas fait de requête ASK_USERNAME.");
                return false;
            }

            if (!this.envoyerMessage(this.getIdentifiant()))
            {
                this.logger.severe("Impossible de communiquer l'identifiant au serveur");
                return false;
            }

            if (!this.waitMessage(this.ASK_PASSWD))
            {
                this.logger.severe("Le serveur n'a pas fait la demande de mot de passe");
                return false;
            }

            if (!this.envoyerMessage(this.phraseSecrete))
            {
                this.logger.severe("Impossible de communiquer la phrase secrete");
                return false;
            }

            if (this.waitMessage(this.AUTH_OK))
            {
                this.logger.info("Identifiants reconnus par le serveur, en attente de récuperer la session id..");
                this.sessionUuid = this.waitMessage();

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
