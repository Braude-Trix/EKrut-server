package gui;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainGUI extends Application {
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
