package client;

import model.*;

import java.io.Serializable;

public class Paquet implements Serializable {

    public static final String DEMANDE_INITIALISATION = "HELLO";

    public static final String NOTIFIE_FERMETURE = "BYE";

    public static final String DEMANDE_AUTHENTIFICATION = "AUTH";
    public static final String DEMANDE_ANONYME = "ANONYMOUS";

    public static final String AUTH_ERREUR = "NOMATCH";
    public static final String AUTH_OK = "MATCH";

    public static final String ASK_ENTER_SALON = "ASK_ENTER_SALON";
    public static final String ASK_SALONS = "ASK_SALONS";

    public static final String ASK_MESSAGE = "ASK_MESSAGE";

    public static final String OK = "OK";
    public static final String KO = "KO";

    private String sessionUuid;
    private String commande;

    private Object data;

    public Paquet(String unIdentifiantSession, String uneCommande, Object unObjet)
    {
        this.sessionUuid = unIdentifiantSession;
        this.commande = uneCommande;
        this.data = unObjet;
    }

    public String getCommande() {
        return commande;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public Object getData() {
        return data;
    }
}
