import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity(name="utilisateur")
public class Utilisateur {

    private @Id @GeneratedValue @Column(name="id") Integer id;
    private @Column(name = "pseudo", nullable = false) String pseudo;
    private @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP) Date dateCreation;
    private @Column(name = "secret", nullable = false) String secret;
    private @OneToMany(targetEntity = Message.class) @JoinColumn(columnDefinition = "id") Set<Message> messages;

    public Utilisateur() {}

    public Utilisateur(String unPseudo, Date uneDateCreation, String unHashSecret, Set<Message> uneListeMessages)
    {
        this.pseudo = unPseudo;
        this.dateCreation = uneDateCreation;
        this.secret = unHashSecret;
        this.messages = uneListeMessages;
    }

    public Integer getId() { return this.id; }
    public String getPseudo() { return this.pseudo; }
    public String getSecret() { return this.secret; }
    public Date getDateCreation() { return this.dateCreation; }

    public Set<Message> getMessages() {
        return messages;
    }
}
