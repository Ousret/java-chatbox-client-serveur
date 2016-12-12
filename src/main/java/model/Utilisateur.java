package model;

import javax.persistence.*;
import java.io.Serializable;

import java.util.Date;
import java.util.Set;

@Entity(name="utilisateur")
@Table(name = "utilisateur")
public class Utilisateur implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="utilisateur_id_seq")
    @SequenceGenerator(name="utilisateur_id_seq", sequenceName="utilisateur_id_seq", allocationSize=1)
    @Column(name="id")
    private Integer id;

    @Column(name = "pseudo", nullable = false, unique = true)
    private String pseudo;

    @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @Column(name = "secret", nullable = false, unique = true)
    private String secret;

    @OneToMany(mappedBy = "auteur")
    private Set<Message> messages;

    public Utilisateur() {}

    public Utilisateur(String unPseudo, Date uneDateCreation, String unHashSecret)
    {
        this.pseudo = unPseudo;
        this.dateCreation = uneDateCreation;
        this.secret = unHashSecret;
    }

    public boolean equals(Object aNoTher)
    {
        return (aNoTher instanceof Utilisateur) && this.getId().equals(((Utilisateur)aNoTher).getId());
    }

    public Integer getId() { return this.id; }
    public String getPseudo() { return this.pseudo; }
    public String getSecret() { return this.secret; }
    public Date getDateCreation() { return this.dateCreation; }

    public Set<Message> getMessages() {
        return messages;
    }

    public String toString()
    {
        return this.getPseudo();
    }
}
