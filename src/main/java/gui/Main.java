package gui;

import client.Client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Observable;
import java.util.Observer;

public class Main extends Application {

    private static final String APP_NAME = "IMIE Projet Java";

    private static Client client;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Main.client = new Client();

        Parent root = FXMLLoader.load(getClass().getResource("../fxml/sample.fxml"));
        primaryStage.setTitle(Main.APP_NAME);
        primaryStage.setScene(new Scene(root, 1080, 600));
        primaryStage.show();
    }

    public static Client getClient() { return Main.client; }

    public static void main(String[] args) {
        launch(args);
    }
}
