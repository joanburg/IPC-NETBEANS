/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import upv.ipc.sportlib.SportActivityApp;

/**
 *
 * @author jose
 */
public class MapApp extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        SportActivityApp app = SportActivityApp.getInstance();
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/Welcome.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Welcome");
        stage.setScene(scene);
        
        stage.setOnCloseRequest(event -> {
        if (app.getCurrentUser() != null) {
            app.logout();
        }
    });
            
        stage.show();
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}