module com.copperpenguin96.spigotconfig {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.yaml.snakeyaml;
    requires com.google.gson;
    requires javafx.web;


    opens com.copperpenguin96.spigotconfig to javafx.fxml;
    exports com.copperpenguin96.spigotconfig;
}