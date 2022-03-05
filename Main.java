/**
 * File: Main.java
 * Names: Dylan Tymkiw, Alex Yu, Jasper Loverude
 * Class: CS 361
 * Project 4
 * Date: February 28th
 */

package proj4TymkiwYuLoverude;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main class serves as the controller for the window.
 *
 * @author Dylan Tymkiw
 * @author Alex Yu
 * @author Jasper Loverude
 */
public class Main extends Application {

    /**
     * Implements the start method of the Application class. This method will
     * be called after {@code launch} method, and it is responsible for initializing
     * the contents of the window.
     *
     * @param primaryStage A Stage object that is created by the {@code launch}
     *                     method
     *                     inherited from the Application class.
     */
    @Override
    public void start(Stage primaryStage) throws java.io.IOException {

        // Load fxml file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Project 4");
        Scene scene = new Scene(root);

        // get controller and set up the initial stage
        Controller controller = loader.getController();
        controller.setStage(primaryStage);

        // Load css files
        scene.getStylesheets().addAll(getClass().getResource("java-keywords.css").toExternalForm(),
                                      getClass().getResource("Main.css").toExternalForm());
        primaryStage.setScene(scene);

        // Set the minimum height and width of the stage
        primaryStage.setMinHeight(250);
        primaryStage.setMinWidth(300);

        // Show the stage
        primaryStage.show();

    }

    /**
     * Main method of the program that calls {@code launch} inherited from the
     * Application class
     *
     * @param args Command line argument, if given
     */
    public static void main(String[] args) {
        launch(args);
    }
}
