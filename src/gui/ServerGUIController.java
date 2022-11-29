package gui;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ServerGUIController {

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

    }

    @FXML
    void ToggleImport(ActionEvent event) {

    }

}
