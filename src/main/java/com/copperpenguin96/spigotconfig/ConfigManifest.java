package com.copperpenguin96.spigotconfig;

import java.util.ArrayList;

/**
 * The base for manifests to be searched for in plugins. Allows for a storage space for all the tabs provided by the plugins.
 */
public abstract class ConfigManifest {

    /// Stores all the tabs created by the plugin.
    private ArrayList<ConfigTab> _tabs = new ArrayList<>();

    /**
     * Gets all the tabs created by the plugin.
     * @return
     */
    public ArrayList<ConfigTab> getTabs() {
        return _tabs;
    }

    /**
     * Adds a tab.
     * @param tab
     */
    protected void add(ConfigTab tab) {
        _tabs.add(tab);
    }

    /// Saves the plugin's configs in their own ways.
    public abstract void save();
}
