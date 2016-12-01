package model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Entity(name = "salon")
public class Salon implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="salon_id_seq")
    @SequenceGenerator(name="salon_id_seq", sequenceName="salon_id_seq", allocationSize=1)
    @Column(name="id")
    private Integer id;

    @ManyToOne(targetEntity = Utilisateur.class)
    @JoinColumn(nullable = false, name = "utilisateur_id")
    private Utilisateur proprietaire;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "prive", nullable = false)
    private Boolean prive;

    @Column(name = "dateCreation", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
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

    public String toString()
    {
        return String.format("%s <%s>", this.getDesignation(), this.getProprietaire().getPseudo());
    }
}
