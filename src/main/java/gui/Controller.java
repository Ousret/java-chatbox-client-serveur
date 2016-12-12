package gui;

import client.Paquet;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Message;
import model.Salon;
import model.Utilisateur;

import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

public class Controller implements Initializable, Observer {
    @FXML
    private MenuItem menuitemConnexion;
    @FXML
    private MenuItem menuitemQuitter;
    @FXML
    private MenuItem menuitemProfile;
    @FXML
    private MenuItem menuitemNettoyer;
    @FXML
    private Button buttonEnvoyer;
    @FXML
    private ListView<Utilisateur> treeListeClient;
    @FXML
    private TextFlow textflowChatOutput;
    @FXML
    private TextArea textChatInput;
    @FXML
    private ScrollPane scrollpaneChatOutput;
    @FXML
    private ComboBox<Salon> comboChannel;
    @FXML
    private ScrollPane scrollPaneBS;

    ObservableList<Utilisateur> utilisateursSalon = FXCollections.observableArrayList();

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        Main.getClient().addObserver(this);
        menuitemProfile.setVisible(false);
        //refreshListeClient();

        buttonEnvoyer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                if (!Main.getClient().isAuthenticated())
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Impossible d'envoyer le message");
                    alert.setContentText(String.format("Veuillez vous authentifier.", comboChannel.getSelectionModel().getSelectedItem().getDesignation()));
                    alert.showAndWait();
                    return;
                }

                if (!Main.getClient().nouveauMessage(textChatInput.getText()))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Impossible d'envoyer le message");
                    alert.setContentText(String.format("Une erreur est survenue lors de l'envoie du message.", comboChannel.getSelectionModel().getSelectedItem().getDesignation()));
                    alert.showAndWait();
                    return;
                }

                ecrireMessage(Main.getClient().getIdentifiant(), textChatInput.getText());
                textChatInput.clear();

            }
        });

        menuitemConnexion.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Stage stage = new Stage();
                Parent root = null;
                try {
                    root = FXMLLoader.load(getClass().getResource("../fxml/connexion.fxml"));
                }catch(Exception error) {
                    error.printStackTrace();
                }
                stage.setScene(new Scene(root));
                stage.setTitle("Connexion");
                stage.initModality(Modality.APPLICATION_MODAL);
                System.out.println(stage.getOwner());
                stage.showAndWait();

                if (Main.getClient().isAuthenticated())
                {
                    comboChannel.getItems().addAll(Main.getClient().getSalons());
                }
            }
        });

        menuitemQuitter.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Platform.exit();
            }
        });

        menuitemProfile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println(event.getEventType().toString());
            }
        });

        menuitemNettoyer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                textflowChatOutput.getChildren().clear();
            }
        });

        comboChannel.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event) {

                if (!Main.getClient().setSalon(comboChannel.getSelectionModel().getSelectedItem()))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Impossible de changer le salon");
                    alert.setContentText(String.format("Impossible de passer au salon '%s'.", comboChannel.getSelectionModel().getSelectedItem().getDesignation()));
                    alert.showAndWait();
                    return;
                }

                utilisateursSalon.addAll(Main.getClient().getSalonUtilisateurs());
                treeListeClient.setItems(utilisateursSalon);
                treeListeClient.setCellFactory(ComboBoxListCell.forListView(utilisateursSalon));

                Main.getClient().ecoute();

            }
        });

        textChatInput.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode().equals(KeyCode.ENTER)){
                    //envoyerMessage();
                    event.consume();
                }
            }
        });
    }

    public void ecrireMessage(String unAuteur, String unMessage){
        textflowChatOutput.getChildren().addAll(new Text(String.format("<%s> :: %s\n", unAuteur, unMessage)));
        scrollpaneChatOutput.setVvalue(1);
    }

    @Override
    public void update(Observable o, Object arg) {
        Paquet paquet = (Paquet) arg;

        System.out.println("Debug #0 :: "+paquet.getCommande());

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                if (paquet.getCommande().equals(Paquet.NOUVEAU_MESSAGE))
                {
                    Message message = (Message) paquet.getData();
                    ecrireMessage(message.getAuteur().getPseudo(), message.getMessage());
                }
                else if(paquet.getCommande().equals(Paquet.SORTIE_UTILISATEUR))
                {
                    utilisateursSalon.remove((Utilisateur) paquet.getData());
                }

            }
        });

    }
}
