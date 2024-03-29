package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import logic.EndOfMonthTask;
import models.Response;
import models.ResponseCode;
import ocsf.server.ConnectionToClient;
import server.Server;
import server.MysqlController;
import serverModels.ClientConnectionData;
import serverModels.ServerConf;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;


/**
 * @author gal
 * This class manages all the functionality enabled in the server's GUI - connecting to the server, importing data and more
 */
public class ServerGUIController implements Initializable {
    private static boolean isConnected;
    private static ServerGUIController controller;
    private static final ObservableList<ClientConnectionData> connectionDataList =
            FXCollections.observableArrayList();
    private static Thread reportsThread;

    private final String ERROR_STYLE = "-fx-border-color: RED; -fx-border-width: 2; -fx-border-radius: 5;";
    private final String SUCCESS_STYLE = "-fx-border-color: #A9A9A9; -fx-border-width: 2; -fx-border-radius: 5;";
    private final String OK_STYLE_BTN = "-fx-background-color: AQUAMARINE;";
    private final String ERROR_STYLE_BTN = "-fx-background-color: ORANGERED;";
    @FXML
    private Button importDataBtn;
    @FXML
    private TextField IPField;

    @FXML
    private Button IpRefreshBtn;

    @FXML
    private TextField PortField;

    @FXML
    private TextField DBField;

    @FXML
    private TextField DBUserField;

    @FXML
    private TextField DBPasswordField;

    @FXML
    private Button ConnectorDisBTN;

    @FXML
    private TableView<ClientConnectionData> ClientsConnectionTable;

    @FXML
    private TableColumn<ClientConnectionData, String> IPCol;

    @FXML
    private TableColumn<ClientConnectionData, String> HostCol;

    @FXML
    private TableColumn<ClientConnectionData, String> StatusCol;

    @FXML
    private Button refreshBtn;

    @FXML
    private TextArea msgBox;

    @FXML
    private ImageView refresh;

    private Stage currentStage;

    /**
     * This method handles the ConnectorDisBTN, and handling its properties according to the status of Server and SQL.
     * i.e. Server connected and SQL couldn't will result the server to shut down and the button will enable connection.
     *
     * @param connected the connection status
     */
    public void setConnected(boolean connected) {
        isConnected = connected;
        String textBtn = isConnected ? "Disconnect" : "Connect";
        String btnStyle = isConnected ? ERROR_STYLE_BTN : OK_STYLE_BTN;
        if (!isConnected) {
            // killing reports creation thread
            if (reportsThread != null && reportsThread.isAlive()) {
                reportsThread.interrupt();
                try {
                    reportsThread.join(5000);
                } catch (InterruptedException e) {
                    printToConsole("Couldn't close reports generator Thread", true);
                    printToConsole(e.getMessage(), true);
                }
            }
            boolean isServerClosed = Server.server_instance.closeServer();
            boolean isDBClosed = true;
            if (Server.server_instance.mysqlController != null) {
                isDBClosed = Server.server_instance.mysqlController.closeConnection();
            }
            if (!isServerClosed || !isDBClosed) {
                printToConsole("One of the resources couldn't close, please reopen the app", true);
                Platform.runLater(() -> controller.ConnectorDisBTN.setDisable(false));
            }
        } else {
            // starting reports creation thread
            reportsThread = new Thread(new EndOfMonthTask());
            reportsThread.start();
        }
        Platform.runLater(() -> {
            controller.ConnectorDisBTN.setText(textBtn);
            controller.ConnectorDisBTN.setStyle(btnStyle);
            controller.ConnectorDisBTN.setDisable(false);
            if(textBtn.equals("Connect")) {
                controller.importDataBtn.setDisable(false);
                controller.importDataBtn.setOpacity(1);
            }
            else{
                controller.importDataBtn.setDisable(true);
                controller.importDataBtn.setOpacity(0.5);
            }

        });
    }

    /**
     * This method describes clicking on connect and trying to connect to the server
     * @param event - clicking on connect
     */
    @FXML
    void ToggleConnect(ActionEvent event) {
        ConnectorDisBTN.setDisable(true);
        if (!isConnected) {
            // building serverConf
            int port = Integer.parseInt(PortField.getText());
            String dbScheme = DBField.getText();
            String dbUserName = DBUserField.getText();
            String dbPassword = DBPasswordField.getText();
            ServerConf serverConf = new ServerConf(Server.getDefaultServerConf().getIp(),
                    port, dbScheme, dbUserName, dbPassword);
            // trying to connect to server
            Server.initServer(serverConf);
            controller.importDataBtn.setDisable(true);
            controller.importDataBtn.setOpacity(0.5);
        } else {
            setAllUsersLoggedOut();
        }
    }

    private void setAllUsersLoggedOut() {
		Response response = new Response();
        MysqlController.disconnectServer(response);
        if (response.getCode() == ResponseCode.OK) {
            setConnected(false);
        } else {
            printToConsole("DB failure updating logout for all users", true);
        }
	}

	@FXML
    void refreshClients(ActionEvent event) {
        controller.checkConnectedClients();
    }

    /**
     * Starting the server-gui, and initializing relevant controller
     *
     * @param primaryStage javafx main stage
     */
    public void start(Stage primaryStage) {
        currentStage = primaryStage;
        AnchorPane pane;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/assets/EchoServerGUI.fxml"));
            pane = loader.load();
            controller = loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Scene s = new Scene(pane);
        s.getStylesheets().add("styles/worker_table.css");
        s.getStylesheets().add("styles/SideButtons.css");
        primaryStage.setTitle("EKrut Server");
        Image iconImage = new Image(Objects.requireNonNull(
                ServerGUIController.class.getResourceAsStream("/assets/serverIcon.png")));
        primaryStage.getIcons().clear();
        primaryStage.getIcons().add(iconImage);
        primaryStage.setScene(s);
        primaryStage.setResizable(false);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> forcedExit(primaryStage));
    }

    private void forcedExit(Stage primaryStage) {
    	if (isConnected) {
        	setAllUsersLoggedOut();
    	}
    	primaryStage.close();
        System.exit(0);
	}

	/**
     * Called before the controller is made
     *
     * @param url            The location used to resolve relative paths for the root object, or
     *                       {@code null} if the location is not known.
     * @param resourceBundle The resources used to localize the root object, or {@code null} if
     *                       the root object was not localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // init ip-refresh btn
        IpRefreshBtn.setText("");
        Image img = new Image("/assets/reload.png", IpRefreshBtn.getMaxWidth(), IpRefreshBtn.getMaxHeight(),
                true, true, true);
        ImageView imgView = new ImageView(img);
        IpRefreshBtn.setGraphic(imgView);
        IpRefreshBtn.setTooltip(new Tooltip("Refresh when your internet connection has changed"));

        // init server conf
        ServerConf serverConf = Server.getDefaultServerConf();
        IPField.setText(serverConf.getIp());
        PortField.setText(String.valueOf(serverConf.getPort()));
        DBField.setText(serverConf.getDbScheme());
        DBUserField.setText(serverConf.getDbUserName());
        DBPasswordField.setText(serverConf.getDbPassword());
        markFieldAsOk(IPField);
        markFieldAsOk(PortField);
        markFieldAsOk(DBField);
        markFieldAsOk(DBUserField);
        markFieldAsOk(DBPasswordField);
        DBPasswordField.setOnMouseEntered(event -> updatePasswordTooltip());
        ConnectorDisBTN.setStyle(OK_STYLE_BTN);

        // init table
        IPCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        HostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        StatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        ClientsConnectionTable.setItems(connectionDataList);
        refreshBtn.setTooltip(new Tooltip("Refresh connected clients"));

        // adding eventListener to buttons
        refreshBtn.setOnAction(event -> refreshConnectedClients());
        PortField.setOnKeyTyped(event -> validatePortField());
        IpRefreshBtn.setOnAction(event -> refreshIpField());

        msgBox.setStyle("-fx-control-inner-background:#000000; -fx-font-family: Consolas; " +
                "-fx-highlight-fill: #00ff00; -fx-highlight-text-fill: #000000; -fx-text-fill:#00ff00;");
    }

    private void updatePasswordTooltip() {
        String password = DBPasswordField.getText();
        DBPasswordField.setTooltip(new Tooltip(password));
    }

    /**
     * Prints messages into the console of the Gui server
     * @param msg - The message displayed on the console
     */
    public void printToConsole(String msg) {
        printToConsole(msg, false);
    }

    /**
     * Prints messages into gui console textArea
     *
     * @param msg     to print
     * @param isError is the message is error msg
     */
    public void printToConsole(String msg, boolean isError) {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM HH:mm:ss,SSS");
        String formattedDate = date.format(dateFormat);
        String preMsg = isError ? "[ERROR] " : "";
        String consoleMsg = String.format("%s: %s%s\n", formattedDate, preMsg, msg);

        Platform.runLater(() -> controller.msgBox.appendText(consoleMsg));
    }

    //    ----------- Event Handlers -----------
    private void refreshConnectedClients() {
        if (Server.server_instance != null) {
            updateClientsDataList();
        }
        ClientsConnectionTable.refresh();
    }

    private void updateClientsDataList() {
        Thread[] clients = Server.server_instance.getClientConnections();
        List<ClientConnectionData> currentClients = new ArrayList<>();
        // appending all current clients
        for (Thread client : clients) {
            ConnectionToClient connection = (ConnectionToClient) client;
            currentClients.add(new ClientConnectionData(connection));
        }
        for (ClientConnectionData oldClientData : connectionDataList) {
            // getting changed client with the same ip and host
            List<ClientConnectionData> currentChangedClients = currentClients.stream()
                    .filter(clientConnectionData -> clientConnectionData.addressEquals(oldClientData))
                    .collect(Collectors.toList());
            if (currentChangedClients.isEmpty()) { // oldClient is now down, or still down
                oldClientData.setStatus(false);
            } else { // oldClient is still connected, or has been reconnected
                oldClientData.setStatus(currentChangedClients.get(0).getStatus());
            }
        }
        // adding new clients (all changed clients has been updated in `connectionDataList`)
        for (ClientConnectionData newClientData : currentClients) {
            if (!connectionDataList.contains(newClientData)) {
                connectionDataList.add(newClientData);
            }
        }
    }

    /**
     * refresh the connected client
     */
    public void checkConnectedClients() {
        controller.refreshBtn.fire();
    }

    private void validatePortField() {
        // port field
        String portRegx = "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";
        if (!PortField.getText().matches(portRegx)) {
            markErrorField(PortField);
        } else {
            markFieldAsOk(PortField);
        }
    }

    private void refreshIpField() {
        ServerConf serverConf = Server.getDefaultServerConf();
        IPField.setText(serverConf.getIp());
    }

    //    ----------- Styling -----------
    private void markErrorField(TextField textField) {
        ConnectorDisBTN.setDisable(true);
        textField.setStyle(ERROR_STYLE);
    }

    private void markFieldAsOk(TextField textField) {
        textField.setStyle(SUCCESS_STYLE);
        ConnectorDisBTN.setDisable(false);
    }

    /**
     * handle when import data button click
     * @param event - ActionEvent object
     */
    @FXML
    void importDataClicked(ActionEvent event) {
        if(importDataConfirmationDialog()) {
            if (MysqlController.importUsersDataFromExternalDB(DBField.getText(), Server.externalDBSchemeName, DBPasswordField.getText())) {
                importDataBtn.setDisable(true);
                importDataBtn.setOpacity(0.5);
            }
        }
    }

    private boolean importDataConfirmationDialog(){
        Alert alertImportDataDialog = new Alert(Alert.AlertType.CONFIRMATION);
        alertImportDataDialog.setTitle("Import data confirmation");
        alertImportDataDialog.setHeaderText("Are you sure ?");
        alertImportDataDialog.setContentText("NOTE: This operation will overwrite your current [users, workers, customers] tables !");
        alertImportDataDialog.getDialogPane().getButtonTypes().clear();
        alertImportDataDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = alertImportDataDialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.managedProperty().bind(closeButton.visibleProperty());
        closeButton.setVisible(false);
        ButtonType proceedButtonType = new ButtonType("Proceed");
        ButtonType cancelButtonType = new ButtonType("Cancel");
        alertImportDataDialog.getDialogPane().getButtonTypes().add(proceedButtonType);
        alertImportDataDialog.getDialogPane().getButtonTypes().add(cancelButtonType);
        Optional<ButtonType> userChoice = alertImportDataDialog.showAndWait();
        if(userChoice.get() == proceedButtonType)
            return true;
        return false;
    }
}
