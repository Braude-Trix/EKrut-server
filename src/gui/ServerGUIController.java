package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import server.Server;
import serverModels.ServerConf;

import java.io.IOException;

public class ServerGUIController {
    private ServerGUIController controller;

    private Server server_instance;
    private boolean isConnected = false;

    @FXML
    private VBox BoxForClientAConsole;

    @FXML
    private Button ConnectorDisBTN;

    @FXML
    private VBox ConsolemsgVBox;

    @FXML
    private TextField DBField;

    @FXML
    private Label DBLabel;

    @FXML
    private TextField DBPasswordField;

    @FXML
    private Label DBPasswordLabel;

    @FXML
    private TextField DBUserField;

    @FXML
    private Label DBUserLabel;

    @FXML
    private HBox HBoxDisplay;

    @FXML
    private TableColumn<?, ?> HostCol;

    @FXML
    private TableColumn<?, ?> IPCol;

    @FXML
    private TextField IPField;

    @FXML
    private Label IPLabel;

    @FXML
    private VBox InputVBox;

    @FXML
    private TextField PortField;

    @FXML
    private Label PortLabel;

    @FXML
    private TableView<?> ServerConnectionTable;

    @FXML
    private TableColumn<?, ?> StatusCol;

    @FXML
    private VBox VboxForConnection;

    @FXML
    private HBox boxForBTN;

    @FXML
    private HBox boxFortxt;

    @FXML
    private Button importBTN;

    @FXML
    private Text mainTitle;

    @FXML
    private VBox mainVBox;

    @FXML
    private TextArea msgBox;

    @FXML
    private Text titleForClients;

    @FXML
    private Text titleForConfig;

    @FXML
    private Text titleForConfig11;

    @FXML
    void ToggleConnect(ActionEvent event) {
        if (!isConnected) {
            // building serverConf
            int port = Integer.parseInt(PortField.getText());
            String dbScheme = DBField.getText();
            String dbUserName = DBUserField.getText();
            String dbPassword = DBPasswordField.getText();
            ServerConf serverConf = new ServerConf(Server.getDefaultServerConf().getIp(),
                                                   port, dbScheme, dbUserName, dbPassword);
            server_instance = Server.initServer(serverConf);
            ConnectorDisBTN.textProperty().setValue("Disconnect");
            isConnected = true;
        } else {
            server_instance.closeServer();
            ConnectorDisBTN.textProperty().setValue("Connect");
            isConnected = false;
        }
    }

    @FXML
    void ToggleImport(ActionEvent event) {
    
    }

    public void start(Stage primaryStage) {
        AnchorPane pane;
        try {
//            pane = FXMLLoader.load(getClass().getResource("EchoServerGUI.fxml"));
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("EchoServerGUI.fxml"));
            pane = loader.load();
            controller = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Scene s = new Scene(pane);
        primaryStage.setTitle("EKrut Server window");
        primaryStage.setScene(s);
        primaryStage.setResizable(false);
        init();
        primaryStage.show();
    }

    void init() {
        ServerConf serverConf = Server.getDefaultServerConf();
        controller.IPField.setText(serverConf.getIp());
        controller.PortField.setText(String.valueOf(serverConf.getPort()));
        controller.DBField.setText(serverConf.getDbScheme());
        controller.DBUserField.setText(serverConf.getDbUserName());
        controller.DBPasswordField.setText(serverConf.getDbPassword());
    }
}
