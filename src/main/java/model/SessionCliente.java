package model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "sessioncliente")
@Table(name = "sessioncliente")
public class SessionCliente implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="sessioncliente_id_seq")
    @SequenceGenerator(name="sessioncliente_id_seq", sequenceName="sessioncliente_id_seq", allocationSize=1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "uuid", unique = true)
    private String uuid;

    @Column(name = "debutSession", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date debutSession;

    public SessionCliente() {}

    public SessionCliente(Utilisateur unUtilisateur, String unUuid, Date uneDateDebut)
    {
        this.utilisateur = unUtilisateur;
        this.uuid = unUuid;
        this.debutSession = uneDateDebut;
    }

    public Integer getId() { return this.id; }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public String getUuid() { return this.uuid; }

    public Date getDebutSession() {
        return debutSession;
    }
}
