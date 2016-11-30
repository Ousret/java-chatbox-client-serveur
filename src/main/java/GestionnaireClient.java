import model.Salon;
import model.SessionCliente;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.Socket;

public class GestionnaireClient implements Runnable {

    private final String DEMANDE_INITIALISATION = "HELLO";
    private final String DEMANDE_AUTHENTIFICATION = "AUTH";
    private final String DEMANDE_ANONYME = "ANONYMOUS";
    private final String AUTH_ERREUR = "NOMATCH";
    private final String AUTH_OK = "MATCH";

    private Socket socket;
    private Serveur instanceMere;

    private Salon salon;
    private SessionCliente session;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    private Thread currentThread;

    public GestionnaireClient(Socket unSocketClient, Serveur uneInstanceMere)
    {
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
                this.objectInputStream.close();
                this.objectOutputStream.close();
                this.socket.close();
            }
            catch (IOException e)
            {
                this.instanceMere.logger.severe(String.format("<Client:%s:%d> Impossible de fermer correctement les streams !", this.socket.getInetAddress(), this.socket.getPort()));
                return false;
            }

            return true;
        }

        return false;
    }

    public void run() {

        String rIn;

        try
        {
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        }catch (IOException e)
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

        // 2) Négocier l'authentification, Anonyme ou identifiant.
        if (this.getMessage().equals(this.DEMANDE_AUTHENTIFICATION))
        {
            // Ask User, Pwd
        }
        else
        {
            // Ask User
        }

        // 3) Envoyer la liste des salons disponibles

        // 4) Inscrire dans les events du salon

    }
}
