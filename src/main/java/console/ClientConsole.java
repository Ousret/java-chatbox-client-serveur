package console;

import client.Client;
import client.Paquet;
import model.Message;
import model.Salon;
import model.Utilisateur;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

public class ClientConsole implements Observer{

    private Scanner scanner;
    private Client client;

    public ClientConsole()
    {

        this.scanner = new Scanner(System.in);

        this.client = new Client();
        this.client.addObserver(this);
    }

    public boolean initialiser()
    {
        System.out.println("IP: >_");
        String adresseIp = this.scanner.next();
        System.out.println("Port: >_");
        Integer port = this.scanner.nextInt();

        System.out.println("Identifiant: >_");
        String identifiant = this.scanner.next();
        System.out.println("Mot de passe: >_");
        String phraseSecrete = this.scanner.next();

        return this.client.connecter(adresseIp, port) && this.client.authentification(identifiant, phraseSecrete);
    }

    public void choixSalon()
    {
        List<Salon> salons = this.client.getSalons();

        System.out.println("---- Choix salons");

        for(Salon salon : salons)
        {
            System.out.println(String.format("[%d] %s", salons.indexOf(salon), salon.getDesignation()));
        }

        System.out.println("Votre choix: _");
        Integer choix = this.scanner.nextInt();

        this.client.setSalon(salons.get(choix));

        System.out.println("------------Liste des connectés du salon");
        System.out.println(this.client.getSalonUtilisateurs());

        this.client.ecoute();
    }

    public void ecrireMessage()
    {
        System.out.println("Votre message >: _");
        String message = this.scanner.nextLine();
        this.client.nouveauMessage(message);
    }

    @Override
    public void update(Observable o, Object arg) {
        Paquet paquet = (Paquet) arg;

        if (paquet.getCommande().equals(Paquet.NOUVEAU_MESSAGE))
        {
            Message message = (Message) paquet.getData();
            System.out.println(String.format("<%s> %s", message.getAuteur().getPseudo(), message.getMessage()));
        }
        else if(paquet.getCommande().equals(Paquet.SORTIE_UTILISATEUR))
        {
            System.out.println(String.format("<!%s> C'est déconnecté..", ((Utilisateur) paquet.getData()).getPseudo()));
        }
    }

    public static void main(String[] args)
    {
        ClientConsole clientConsole = new ClientConsole();

        clientConsole.initialiser();
        clientConsole.choixSalon();

        for (;;)
        {
            clientConsole.ecrireMessage();
        }
    }
}
