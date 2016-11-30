package model;

import javax.persistence.*;
import java.io.Serializable;

import java.util.Date;
import java.util.Set;

@Entity(name="utilisateur")
@Table(name = "utilisateur")
public class Utilisateur implements Serializable {

    @Id @GeneratedValue @Column(name="id")
    private Integer id;

    @Column(name = "pseudo", nullable = false)
    private String pseudo;

    @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @Column(name = "secret", nullable = false)
    private String secret;

    @Column(name = "salt", nullable = false)
    private String salt;

    @OneToMany(mappedBy = "auteur")
    private Set<Message> messages;

    public Utilisateur() {}

    public Utilisateur(String unPseudo, Date uneDateCreation, String unHashSecret, String unSalt)
    {
        this.pseudo = unPseudo;
        this.dateCreation = uneDateCreation;
        this.secret = unHashSecret;
        this.salt = unSalt;
    }

    public Integer getId() { return this.id; }
    public String getPseudo() { return this.pseudo; }
    public String getSecret() { return this.secret; }
    public String getSalt() { return this.salt; }
    public Date getDateCreation() { return this.dateCreation; }

    public Set<Message> getMessages() {
        return messages;
    }
}
