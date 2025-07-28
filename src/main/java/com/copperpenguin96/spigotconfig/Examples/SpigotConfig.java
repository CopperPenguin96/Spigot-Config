package com.copperpenguin96.spigotconfig.Examples;

import com.copperpenguin96.spigotconfig.ConfigManifest;
import java.io.IOException;

public class SpigotConfig extends ConfigManifest {

    public SpigotConfig() throws IOException {
        ExampleTab exampleTab = new ExampleTab();

        this.add(exampleTab);
    }

    @Override
    public void save() {
        // Insert saving logic here
    }
}
