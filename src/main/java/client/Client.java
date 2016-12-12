package client;

import model.Salon;
import model.Utilisateur;

import javax.net.SocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Logger;

import java.security.*;

public class Client extends Observable implements Runnable {

    public final Logger logger = Logger.getLogger(Client.class.getName());

    private String identifiant, adresseIp, phraseSecrete, sessionUuid;
    private Integer port;

    private List<Salon> salons;
    private Salon salon;

    private SocketFactory factory=(SocketFactory) SocketFactory.getDefault();
    private Socket socket = null;

    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    private volatile LinkedList<Paquet> paquetsAttente;

    private Thread thread;

    public Client()
    {
        this.paquetsAttente = new LinkedList<Paquet>();
        this.thread = new Thread(this);
        this.thread.setDaemon(true);
    }

    private Client(String uneAdresseIP, Integer unNumeroPort, String unIdentifiant, String unePhraseSecrete)
    {

        this.identifiant = unIdentifiant;
        this.adresseIp = uneAdresseIP;
        this.port = unNumeroPort;

        try
        {
            this.phraseSecrete = Client.hash256(unePhraseSecrete);
        }
        catch (NoSuchAlgorithmException e)
        {
            this.logger.severe(e.getMessage());
        }

        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();

    }

    /**
     * Récupère l'identifiant en cours
     * @return String
     */
    public String getIdentifiant() { return this.identifiant; }

    public void join() throws InterruptedException { this.thread.join(); }

    /**
     * Récupère le paquet suivant
     * @return Paquet
     */
    public Paquet nextPaquet()
    {
        return this.paquetsAttente.size() > 0 ? this.paquetsAttente.pop() : null;
    }

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

    /**
     * Établir une connexion au serveur
     * @param uneAdresseIP L'adresse IP cible
     * @param unPortDistant Le port distant
     * @return bool
     */
    public boolean connecter(String uneAdresseIP, Integer unPortDistant)
    {
        try {

            this.socket = factory.createSocket(uneAdresseIP, unPortDistant);
            this.objectOutputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(this.socket.getInputStream());

            this.adresseIp = uneAdresseIP;
            this.port = unPortDistant;

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
     * Négocie l'authentification avec le serveur et sauvegarde la clé de session automatiquement
     * @param unIdentifiant Identifiant utilisateur ou pseudo
     * @param unePhraseSecrete Le mot de passe / phrase secrete
     * @return Vrai si le serveur à répondu positivement
     */
    public boolean authentification(String unIdentifiant, String unePhraseSecrete)
    {
        if (this.isAuthenticated())
        {
            this.logger.warning("Vous êtes déjà authentifié.");
            return false;
        }

        this.logger.info("Début de la procedure d'authentification.");

        if (!this.sendPaquet(Paquet.DEMANDE_INITIALISATION, null) || !this.getPaquet(Paquet.DEMANDE_INITIALISATION))
        {
            this.logger.severe("Le serveur ne répond pas à la demande HELLO. Échec critique..");
            return false;
        }

        this.logger.info("Demande au serveur l'authentification.");

        try
        {
            this.phraseSecrete = Client.hash256(unePhraseSecrete);
        }
        catch (Exception e)
        {
            return false;
        }

        if (this.sendPaquet(Paquet.DEMANDE_AUTHENTIFICATION, new Utilisateur(unIdentifiant, null, this.phraseSecrete)))
        {
            Paquet authRes = this.getPaquet();

            if (authRes != null && authRes.getCommande().equals(Paquet.AUTH_OK))
            {
                this.identifiant = unIdentifiant;
                this.logger.info("Identifiants reconnus par le serveur, en attente de récuperer la session id..");
                this.sessionUuid = (String) authRes.getData();

                if (this.sessionUuid != null)
                {
                    this.logger.info(String.format("Identifiant de session: %s", this.sessionUuid));
                    this.thread.start();

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

    /**
     * Récupère la liste des salons depuis le serveur
     * @return La liste des salons disponibles
     */
    public List<Salon> getSalons()
    {
        if (!this.isAuthenticated())
        {
            this.logger.warning("Impossible d'émettre un message sans être authentifié.");
            return null;
        }

        if (!this.sendPaquet(Paquet.ASK_SALONS, null))
        {
            this.logger.severe("La demande de liste salon n'a pas aboutie.");
            return null;
        }

        Paquet paquet = this.getPaquet();

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

        if (!this.sendPaquet(Paquet.ASK_ENTER_SALON, unSalon))
        {
            this.logger.warning("Erreur dans la demande de changement de salon");
            return false;
        }

        if (this.getPaquet(Paquet.OK))
        {
            this.salon = unSalon;
            return true;
        }

        return false;
    }

    /**
     * Vérifie si la connexion est établie
     * @return bool
     */
    public boolean isOnline() { return this.socket != null && this.socket.isConnected(); }

    /**
     * Vérifie si le client est authentifié
     * @return bool
     */
    public boolean isAuthenticated() { return this.isOnline() && this.sessionUuid != null;}

    /**
     * Récupère le salon en cours.
     * @return Salon
     */
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

        return this.sendPaquet(Paquet.ASK_MESSAGE, unMessage);
    }

    public void run() {
        for (;;)
        {
            Paquet paquet = this.getPaquet();
            if (paquet == null) break;
            this.logger.info(String.format("<Serveur> Requête '%s'.", paquet.getCommande()));
            this.paquetsAttente.push(paquet);
            this.setChanged();
            this.notifyObservers(paquet);
        }
    }

    public static void main(String[] args)
    {
        Client kClient = new Client(); //"127.0.0.1", 3307, "Ousret", "azerty"

        if (kClient.connecter("127.0.0.1", 3307))
        {
            if (kClient.authentification("Ousret", "azerty"))
            {
                kClient.setSalon(kClient.getSalons().get(0));
                kClient.nouveauMessage("Hello world!");
            }
        }

        try
        {
            kClient.join();
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Calcule le hash SHA256 d'une chaîne de caractères
     * @param data La chaîne de caractère dont le hash doit être calculée
     * @return Le hash SHA256 sous forme de String
     * @throws NoSuchAlgorithmException Si la JVM ne dispose pas de l'algorithme de hashage
     */
    public static String hash256(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }

    /**
     * Transforme une suite de byte en une chaîne de caractères (RAW: Affichage hexadécimale)
     * @param bytes La suite de byte
     * @return La suite hexadécimale sous forme String
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}
