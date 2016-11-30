package model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "message")
@Table(name = "message")
public class Message implements Serializable {

    private static final long serialVersionUID = -5399605122490343339L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="message_id_seq")
    @SequenceGenerator(name="message_id_seq", sequenceName="message_id_seq", allocationSize=1)
    @Column(name="id")
    private Integer id;

    @Column(name = "dateCreation", nullable = false) @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreation;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur auteur;

    @Column(name = "suspendre", nullable = false)
    private Boolean suspendu;

    @Column(name = "message", nullable = false)
    private String message;

    @ManyToOne(targetEntity = Salon.class)
    @JoinColumn(nullable = false, name = "salon_id")
    private Salon salon;

    public Message() {}

    public Message(Date uneDateCreation, Utilisateur unAuteur, Boolean unEtatModeration, String unMessage, Salon unSalon)
    {
        this.dateCreation = uneDateCreation;
        this.auteur = unAuteur;
        this.suspendu = unEtatModeration;
        this.message = unMessage;
        this.salon = unSalon;
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

    public Salon getSalon() { return this.salon; }
}
