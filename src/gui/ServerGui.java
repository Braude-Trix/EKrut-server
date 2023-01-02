package gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class ServerGui extends Application {
	public static ServerGUIController serverGui;

	@Override
	public void start(Stage primaryStage) {
		serverGui = new ServerGUIController();
		serverGui.start(primaryStage);
	}


	public static void main(String[] args) {
		launch(args);
	}
}
