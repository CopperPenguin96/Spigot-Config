package com.copperpenguin96.spigotconfig;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Tab created by the plugin.
 */
public abstract class ConfigTab {
    /// The name of the tab
    public String Name;

    /// All the controls needed for the tab.
    public ArrayList<Control> Controls;

    public Scene Scene;

    public ConfigTab(String name) {
        Name = name;
    }

    private boolean _changesMade = false;

    protected void tellChangesMade() {
        _changesMade = true;
    }

    public boolean changesMade() {
        return _changesMade;
    }

    protected HashMap<String, EventHandler> EventRegistry = new HashMap<>();
    protected ArrayList<EventType> TypeRegistry = new ArrayList<>();

    Class<?> typeClass;
    protected void setClass(Class<?> cls) {
        typeClass = cls;
    }

    protected void setScene(InputStream stream) {
        try {
            Scene = getSceneFromResources(stream);
            Controls = new ArrayList<>();
            proliferateScene();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }

    protected Scene getSceneFromResources(InputStream stream) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setController(typeClass);
        return new Scene(loader.load(stream));
    }

    private void proliferateScene() {
        Parent root = Scene.getRoot();
        for (Node child : root.getChildrenUnmodifiable()) {
            Control childCtrl = (Control)child;

            // lookup events
            for (int x = 0; x < EventRegistry.size(); x++) {
                String key = EventRegistry.keySet().toArray()[x].toString();

                if (key.equals(childCtrl.getId())) {
                    EventHandler handler = (EventHandler)EventRegistry.values().toArray()[x];
                    childCtrl.addEventHandler(TypeRegistry.get(x), handler);
                }
            }

            Controls.add(childCtrl);
        }
    }

    protected void addEvent(String controlName, EventType eveType, EventHandler handler) {
        EventRegistry.put(controlName, handler);
        TypeRegistry.add(eveType);
    }
}
