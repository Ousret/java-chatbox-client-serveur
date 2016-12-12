package console;

import client.Client;

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

    @Override
    public void update(Observable o, Object arg) {

    }

    public static void main(String[] args)
    {
        ClientConsole clientConsole = new ClientConsole();
        clientConsole.initialiser();
    }
}
