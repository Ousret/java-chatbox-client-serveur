package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ControllerConnexion implements Initializable {
    @FXML
    private Button buttonConnexion;
    @FXML
    private TextField textfieldIP;
    @FXML
    private TextField textfieldPort;
    @FXML
    private TextField textfieldPseudo;
    @FXML
    private PasswordField passwordfieldPW;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        buttonConnexion.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {


                if (!textfieldPort.getText().matches("[0-9]{2,6}"))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de saisie");
                    alert.setContentText(String.format("Le numéro de port est invalide (%s), veuillez vérifier votre saisie.", textfieldPort.getText()));
                    alert.showAndWait();
                    return;
                }

                if (!textfieldPseudo.getText().matches("[a-zA-Z0-9]{1,30}"))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de saisie");
                    alert.setContentText(String.format("Le pseudo est invalide (%s), veuillez vérifier votre saisie.", textfieldPseudo.getText()));
                    alert.showAndWait();
                    return;
                }

                if (!textfieldIP.getText().matches("[0-9.]{5,30}"))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de saisie");
                    alert.setContentText(String.format("L'IP est invalide (%s), veuillez vérifier votre saisie.", textfieldIP.getText()));
                    alert.showAndWait();
                    return;
                }

                if (!Main.getClient().connecter(textfieldIP.getText(), Integer.parseInt(textfieldPort.getText())))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connexion impossible");
                    alert.setContentText(String.format("Impossible de se connecter au serveur %s:%d !", textfieldIP.getText(), Integer.parseInt(textfieldPort.getText())));
                    alert.showAndWait();
                    return;
                }

                if (!Main.getClient().authentification(textfieldPseudo.getText(), passwordfieldPW.getText()))
                {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connexion impossible");
                    alert.setContentText(String.format("Les jetons d'authentification ne sont pas reconnus pour '%s'.", textfieldPseudo.getText()));
                    alert.showAndWait();
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Connexion réussie");
                alert.setContentText(String.format("Vous êtes authentifiés en tant que '%s'.", textfieldPseudo.getText()));
                alert.showAndWait();

                Node  source = (Node)  event.getSource();
                Stage stage  = (Stage) source.getScene().getWindow();
                stage.close();

            }
        });
    }

}
