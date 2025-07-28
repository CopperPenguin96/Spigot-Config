package com.copperpenguin96.spigotconfig.Examples;

import com.copperpenguin96.spigotconfig.ConfigApplication;
import com.copperpenguin96.spigotconfig.ConfigTab;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class ExampleTab extends ConfigTab {

    public ExampleTab() {
        // Initiate with the name
        super("Example Tab");

        // Register your events first.
        registerEvents();

        // Set the location of the scene, should be the name from your resources
        setScene(ConfigApplication.class.getResourceAsStream("ExampleTab.fxml"));

        // Set the class, as this is the controller
        setClass(this.getClass());
    }

    private void registerEvents() {
        addEvent("btnAction", ActionEvent.ACTION, (EventHandler<ActionEvent>) event -> {
            System.out.println("Yo123");
        });

    }

    // Ensure to declare controls here so you have access to them.
    private Button btnAction;
}
