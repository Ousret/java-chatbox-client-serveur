package client;

import model.*;

import java.io.Serializable;

public class Paquet implements Serializable {

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
