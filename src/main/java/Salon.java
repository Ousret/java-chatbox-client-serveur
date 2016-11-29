import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity(name = "salon")
public class Salon {

    @Id @GeneratedValue @Column(name="id")
    private Integer id;

    @ManyToOne(targetEntity = Utilisateur.class) @Column(nullable = false)
    private Utilisateur proprietaire;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "prive", nullable = false)
    private Boolean prive;

    @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @ManyToMany(targetEntity = Utilisateur.class)
    @JoinColumn(columnDefinition = "id")
    private Set<Utilisateur> connectes;

    public Salon() {}

    public Salon(Utilisateur unProprietaire, String uneDesignation, Boolean estPrive, Date uneDateCreation, Set<Utilisateur> uneListeConnectes)
    {
        this.proprietaire = unProprietaire;
        this.designation = uneDesignation;
        this.prive = estPrive;
        this.dateCreation = uneDateCreation;
        this.connectes = uneListeConnectes;
    }

    public Integer getId() { return this.id; }

    public Utilisateur getProprietaire() {
        return proprietaire;
    }

    public String getDesignation() {
        return designation;
    }

    public Boolean getPrive() {
        return prive;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public Set<Utilisateur> getConnectes() {
        return connectes;
    }
}
