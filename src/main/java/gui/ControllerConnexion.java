package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

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
                
                System.out.println("Connexion");
            }
        });
    }

}
