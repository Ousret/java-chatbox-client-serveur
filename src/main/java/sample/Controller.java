package sample;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.*;
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
    private TreeView<String> treeListeClient;
    @FXML
    private TextFlow textflowChatOutput;
    @FXML
    private TextArea textChatInput;
    @FXML
    private ScrollPane scrollpaneChatOutput;
    @FXML
    private ComboBox<String> comboChannel;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        menuitemProfile.setVisible(false);
        refreshListeClient();
        /*treeListeClient.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if(event.getClickCount() == 2 && (treeListeClient.getSelectionModel().getSelectedItem().getValue() instanceof String)){
                    treeListeClient.getRoot().getChildren().add(new TreeItem<String>("Test"));
                    System.out.println(event.getPickResult().getIntersectedNode().focusedProperty().getBean());
                    System.out.println(treeListeClient.getFocusModel().getFocusedItem());
                }
            }
        });*/
        comboChannel.getItems().addAll("Python","Java","Ruby","Php");

        buttonEnvoyer.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                envoyerMessage();
            }
        });

        menuitemConnexion.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Stage stage = new Stage();
                Parent root = null;
                try {
                    root = FXMLLoader.load(getClass().getResource("connexion.fxml"));
                }catch(Exception error) {
                    error.printStackTrace();
                }
                stage.setScene(new Scene(root));
                System.out.println("WUT ?");
                stage.setTitle("Connexion");
                stage.initModality(Modality.APPLICATION_MODAL);
                System.out.println(stage.getOwner());
                stage.showAndWait();

                //System.out.println(root.getChildrenUnmodifiable().get(0));
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

        textChatInput.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode().equals(KeyCode.ENTER)){
                    envoyerMessage();
                    event.consume();
                }
            }
        });
    }
    public void envoyerMessage(){
        textflowChatOutput.getChildren().addAll(new Text("Skurra : " + textChatInput.getText() + "\n"));
        textChatInput.setText("");
        scrollpaneChatOutput.setVvalue(1);
    }

    public void refreshListeClient() {
        TreeItem<String> Channel1 = new TreeItem<String>("Channel 1");
        Channel1.setExpanded(true);
        Channel1.getChildren().addAll(new TreeItem<String>("PERSO 1"), new TreeItem<String>("PERSO 2"));
        treeListeClient.setRoot(Channel1);
    }

    @Override
    public void update(Observable o, Object arg) {
        switch((String)arg){
            case "connecte":
                break;
        }
    }
}
