package gui;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainGUI extends Application {

	@Override
	public void start(Stage primaryStage) {
		AnchorPane pane;
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("EchoServerGUI.fxml"));
			pane =  loader.load();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Scene s = new Scene(pane);
		
		primaryStage.setTitle("EKrut Server window");
		primaryStage.setScene(s);
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
