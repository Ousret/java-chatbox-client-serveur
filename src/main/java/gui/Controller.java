package gui;

import client.Paquet;
import html.ChatBox;
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
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Message;
import model.Salon;
import model.Utilisateur;

import java.io.IOException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

public class Controller implements Initializable, Observer {

    private String chatBuffer;

    @FXML
    private MenuItem menuitemConnexion;
    @FXML
    private MenuItem menuitemDeconnexion;
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
    private WebView fluxChat;
    @FXML
    private HTMLEditor textChatInput;
    @FXML
    private ComboBox<Salon> comboChannel;
    @FXML
    private ScrollPane scrollPaneBS;

    private WebEngine webEngine;
    private ChatBox chatBoxContentManagement;

    ObservableList<Utilisateur> utilisateursSalon = FXCollections.observableArrayList();

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        Main.getClient().addObserver(this);

        this.webEngine = fluxChat.getEngine();

        try
        {
            this.chatBoxContentManagement = new ChatBox();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage()+"::"+e.getLocalizedMessage());
            System.exit(-1);
        }

        webEngine.loadContent(this.chatBoxContentManagement.getRaw());


        menuitemProfile.setVisible(false);

        buttonEnvoyer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                publierMessage();
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
                stage.showAndWait();

                if (Main.getClient().isAuthenticated())
                {
                    comboChannel.getItems().addAll(Main.getClient().getSalons());
                    comboChannel.setDisable(false);
                    menuitemConnexion.setDisable(true);
                    menuitemDeconnexion.setDisable(false);
                }
            }
        });

        menuitemDeconnexion.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (Main.getClient().isAuthenticated())
                {
                    if (Main.getClient().fermer())
                    {
                        comboChannel.setDisable(true);
                        menuitemConnexion.setDisable(false);
                        menuitemDeconnexion.setDisable(true);

                        comboChannel.getItems().clear();
                        utilisateursSalon.clear();

                        chatBoxContentManagement.clear();
                        webEngine.loadContent(chatBoxContentManagement.getRaw());
                    }
                }
            }
        });

        menuitemQuitter.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (Main.getClient().isAuthenticated())
                {
                    Main.getClient().fermer();
                }
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
                chatBoxContentManagement.clear();
                webEngine.loadContent(chatBoxContentManagement.getRaw());
            }
        });

        comboChannel.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event) {

                if (comboChannel.getSelectionModel().getSelectedItem() == null) return;

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

                textChatInput.setDisable(false);
                buttonEnvoyer.setDisable(false);

                comboChannel.setDisable(true);

                Main.getClient().ecoute();

            }
        });

        textChatInput.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode().equals(KeyCode.ENTER)){
                    publierMessage();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            textChatInput.requestFocus();
                        }
                    });
                    event.consume();
                }
            }
        });
    }

    private void publierMessage()
    {
        if (!Main.getClient().isAuthenticated())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Impossible d'envoyer le message");
            alert.setContentText("Veuillez vous authentifier.");
            alert.showAndWait();
            return;
        }

        if (!Main.getClient().nouveauMessage(textChatInput.getHtmlText()))
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Impossible d'envoyer le message");
            alert.setContentText("Une erreur est survenue lors de l'envoie du message.");
            alert.showAndWait();
            return;
        }

        ecrireMessage(Main.getClient().getIdentifiant(), textChatInput.getHtmlText());
        textChatInput.setHtmlText("");
    }

    private void ecrireMessage(String unAuteur, String unMessage){
        this.chatBoxContentManagement.addMessage(unAuteur, unMessage);
        webEngine.loadContent(this.chatBoxContentManagement.getRaw());
    }

    @Override
    public void update(Observable o, Object arg) {
        Paquet paquet = (Paquet) arg;

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
                    ecrireMessage(((Utilisateur) paquet.getData()).getPseudo(), "Déconnexion ::");
                }
                else if(paquet.getCommande().equals(Paquet.ENTRER_UTILISATEUR))
                {
                    utilisateursSalon.add((Utilisateur) paquet.getData());
                    ecrireMessage(((Utilisateur) paquet.getData()).getPseudo(), "Connecté ::");
                }

            }
        });

    }
}
