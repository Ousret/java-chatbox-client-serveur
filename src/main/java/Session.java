import javax.persistence.*;
import java.util.Date;

@Entity(name = "session")
public class Session {

    @Id @GeneratedValue
    private Integer id;

    @OneToMany(targetEntity = Utilisateur.class) @Column(nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "dateCreation", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date debutSession;

    public Session() {}

    public Session(Utilisateur unUtilisateur, Date uneDateDebut)
    {
        this.utilisateur = unUtilisateur;
        this.debutSession = uneDateDebut;
    }

    public Integer getId() { return this.id; }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public Date getDebutSession() {
        return debutSession;
    }
}
