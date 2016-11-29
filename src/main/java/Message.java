import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "message")
public class Message implements Serializable {

    private static final long serialVersionUID = -5399605122490343339L;

    @Id @GeneratedValue @Column(name="id")
    private Integer id;

    @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @ManyToOne(targetEntity = Utilisateur.class) @Column(nullable = false)
    private Utilisateur auteur;

    @Column(name = "suspendre", nullable = false)
    private Boolean suspendu;

    @Column(name = "message", nullable = false)
    private String message;

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
