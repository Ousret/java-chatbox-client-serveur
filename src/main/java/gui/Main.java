package gui;

import client.Client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Observable;
import java.util.Observer;

public class Main extends Application implements Observer {

    private static final String APP_NAME = "IMIE Projet Java";

    private Client client;

    @Override
    public void start(Stage primaryStage) throws Exception{

        this.client = new Client();
        this.client.addObserver(this);

        Parent root = FXMLLoader.load(getClass().getResource("../fxml/sample.fxml"));
        primaryStage.setTitle(Main.APP_NAME);
        primaryStage.setScene(new Scene(root, 1080, 600));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void update(Observable o, Object arg) {

    }
}
