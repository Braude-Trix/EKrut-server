package gui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 *This class runs the server application
 *
 */
public class ServerGui extends Application {
	public static ServerGUIController serverGui;

	/**
	 * start method, starts the server gui.
	 */
	@Override
	public void start(Stage primaryStage) {
		serverGui = new ServerGUIController();
		serverGui.start(primaryStage);
	}


	public static void main(String[] args) {
		launch(args);
	}
}
