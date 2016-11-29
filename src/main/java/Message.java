import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "message")
public class Message implements Serializable {

    private static final long serialVersionUID = -5399605122490343339L;

    private @Id @GeneratedValue @Column(name="id") Integer id;
    private @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP) Date dateCreation;
    private @ManyToOne(targetEntity = Utilisateur.class) @Column(nullable = false) Utilisateur auteur;
    private @Column(name = "suspendre", nullable = false) Boolean suspendu;
    private @Column(name = "message", nullable = false) String message;

    public Message() {}

    public Message(Date uneDateCreation, Utilisateur unAuteur, Boolean unEtatModeration, String unMessage)
    {
        this.dateCreation = uneDateCreation;
        this.auteur = unAuteur;
        this.suspendu = unEtatModeration;
        this.message = unMessage;
    }

    public Integer getId() { return this.id; }

    public Date getDateCreation() {
        return dateCreation;
    }

    public Utilisateur getAuteur() {
        return auteur;
    }

    public Boolean getSuspendu() {
        return suspendu;
    }

    public String getMessage() {
        return message;
    }
}
