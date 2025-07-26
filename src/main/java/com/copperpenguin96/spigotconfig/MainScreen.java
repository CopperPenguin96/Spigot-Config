package com.copperpenguin96.spigotconfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.AnchorPane;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainScreen implements Initializable {

    /**
     * An array list that holds the name of all data packs other than vanilla that user has installed on the server.
     * Gets used by saving/loading to allow enabling/disabling with ease.
     */
    private final ArrayList<String> _allDataPacks = new ArrayList<>();

    /**
     * Starts the loading process and sets up important fields like spinner and tooltips.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Basic initialization
        setSpinnerValueFactories();
        setToolTips();

        // Loading datapacks
        File datapackDir = new File("datapacks/");
        if (datapackDir.isDirectory() && datapackDir.exists()) {
            for (File file : Objects.requireNonNull(datapackDir.listFiles())) {
                try {
                    String name = getDatapackName(file);
                    if (name.equals("???"))
                        continue;

                    _allDataPacks.add(getDatapackName(file));
                } catch (Exception e) {
                    System.out.println("Unable to load datapack");
                    e.printStackTrace();
                }
            }
        } else {
            listDataEnable.setDisable(true);
            listDataDisable.setDisable(true);
            btnDataEnable.setDisable(true);
            btnDataDisable.setDisable(true);

            // No datapacks have been installed.
            listDataEnable.getItems().add("No datapacks available.");
        }

        // Handle config loading
        load();

        // Handle plugin loading
        File pluginLocation = new File("plugins/");
        if (!pluginLocation.exists()) {
            sendNoPluginsMsg();
        }

        if (pluginLocation.isDirectory()) {
            try {
                for (File file : pluginLocation.listFiles()) {
                    String fileName = file.getName();
                    if (!fileName.contains(".")) continue;

                    int lastIndex = fileName.lastIndexOf(".") + 1;
                    String ext = fileName.substring(lastIndex);

                    if (!ext.equalsIgnoreCase("jar")) continue;

                    try {
                        // We'll use URLClassLoader to load the class
                        URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
                        String mainPackage = readMainPackage(classLoader);

                        if (mainPackage == null) continue; // nothing found.

                        try {
                            // Load the configuration class, it must be in the main package and called SpigotConfig
                            Class<?> cls = classLoader.loadClass(mainPackage + ".SpigotConfig");
                            if (ConfigManifest.class.isAssignableFrom(cls)) {
                                Object obj = cls.getConstructor().newInstance();
                                ConfigManifest ms = (ConfigManifest) obj;
                                updateScreen(ms);
                            } else {
                                continue; // They didn't extend from ConfigManifest. Must extend
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue; // they don't have it, or it's not in the right place
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        continue; // can't load
                    }
                }
            } catch (NullPointerException ex) {
                // No plugins doc
                sendNoPluginsMsg();
            }
        } else {
            sendNoPluginsMsg();
        }

        // Ensures that enabled/disabled values inact right away on the GUI, and so that we can determine if changes were made when exiting.
        attachListeners();

        // These events weren't firing on load, so had to call them manually
        rconCheck(null);
        queryCheck(null);
        whitelistCheck(null);
    }

    /**
     * Establishes value factories, min, max, and default values of number fields.
     */
    private void setSpinnerValueFactories() {
        numEntBroad.setValueFactory(getFactory(10, 1000, 100));
        numFuncPermLevel.setValueFactory(getFactory(1, 4, 2));
        numMaxChain.setValueFactory(getFactory(-1, Integer.MAX_VALUE, 1000000));
        numMaxPlayers.setValueFactory(getFactory(-1, Integer.MAX_VALUE, 20));
        numMaxTickTime.setValueFactory(new LongSpinnerValueFactory(-1, Long.MAX_VALUE, 60000));
        numMaxWorldSize.setValueFactory(getFactory(1, 29999984, 29999984));
        numNetComprThresh.setValueFactory(getFactory(-1, 1500, 256));
        numOpPerm.setValueFactory(getFactory(0, 4, 4));
        numEmptySec.setValueFactory(getFactory(1, Integer.MAX_VALUE, 60));
        numIdleTimeout.setValueFactory(getFactory(0, Integer.MAX_VALUE, 0));
        numQueryPort.setValueFactory(getFactory(0, 65535, 25565));
        numRconPort.setValueFactory(getFactory(1, 65535, 25575));
        numPort.setValueFactory(getFactory(1, 65535, 25565));
        numSimDist.setValueFactory(getFactory(3, 32, 10));
        numSpawnProtection.setValueFactory(getFactory(0, Integer.MAX_VALUE, 16));
        numViewDist.setValueFactory(getFactory(3, 32, 10));
        numRateLimit.setValueFactory(getFactory(0, Integer.MAX_VALUE, 0));
    }

    /**
     * Creates the data factory to simply the code in the factory method.
     * @param min The min value of this factory
     * @param max The max value of this factory
     * @param init The initial value of this factory.
     * @return Gets the factory established with the argued parameters.
     */
    private IntegerSpinnerValueFactory getFactory(int min, int max, int init) {
        return new IntegerSpinnerValueFactory(min, max, init);
    }

    /**
     * Gets the file extension of the specified File object
     * @param file The file
     * @return The extension
     */
    private String getExt(File file) {
        String fileName = file.getName();
        int last = fileName.lastIndexOf(".");
        fileName = fileName.substring(last + 1);
        return fileName;
    }

    /**
     * Gets the datapack name of the specified file. Must be a valid Minecraft datapack or else it won't work.
     * @param file The file
     * @return Gets the datapack name.
     * @throws IOException Will throw if issue with the file.
     */
    private String getDatapackName(File file) throws IOException {
        String json = "";
        if (!file.isDirectory()) {
            String fileName = getExt(file);

            // If file name is not a valid type, signal to the parser to ignore it with ???
            if (!fileName.equals("zip")) {
                return "???";
            }

            // Find the pack.mcmeta file in the zip archive and read it for the name.
            ZipFile pack = new ZipFile(file);
            ZipEntry meta = pack.getEntry("pack.mcmeta");
            InputStream stream = pack.getInputStream(meta);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = br.readLine()) != null) {
                json += line + "\n";
            }
        } else {
            // If the folder isn't zipped
            for (File f : file.listFiles()) {
                if (f.getName().equals("pack.mcmeta")) {
                    for (String line : Files.readAllLines(f.toPath())) {
                        json += line + "\n";
                    }
                }
            }
        }

        // Loads the json so we can strip the name out.
        Gson gson = new Gson();
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        JsonObject packObj = obj.getAsJsonObject("pack");
        JsonObject desc = packObj.getAsJsonObject("description");
        String trans = desc.get("translate").getAsString();
        return trans;
    }

    /**
     * Used to read the main package of plugins installed to Spigot. This is useful so we can find what plugins support this application.
     * @param clLoader
     * @return
     */
    private String readMainPackage(URLClassLoader clLoader) {
        // Read the main package from the plugin itself
        InputStream is = clLoader.getResourceAsStream("plugin.yml");

        try {
            Yaml yaml = new Yaml();
            Map<String, Object> objMap = yaml.load(is);

            String mainClass = objMap.get("main").toString();
            int lastDot = mainClass.lastIndexOf(".");
            return mainClass.substring(0, lastDot);
        } catch (YAMLException ex) {
            return null;
        }
    }

    /// Used to tell if the warning has been shown yet or not. Ensures we don't show the message more than once.
    private boolean _msgShow = false;

    /**
     * Alerts the user if no plugins are installed to Spigot.
     */
    private void sendNoPluginsMsg() {
        if (_msgShow) return;
        Alert warningNoPlugins = new Alert(Alert.AlertType.WARNING);
        warningNoPlugins.setTitle("No plugins found");
        warningNoPlugins.setHeaderText("No plugins found");
        warningNoPlugins.setContentText("There weren't any plugins found containing configurations \n" +
                "I recognize. Only vanilla configurations will be editable. If this \n" +
                "is an error, make sure this config application is placed \n" +
                "in the same root directory your server jar is.");

        warningNoPlugins.showAndWait();
        _msgShow = true;
    }

    /// Used by the application to tell if changes were made when the user hits exit. If there were changes, it prompts them to save.
    private boolean _changesMade = false;

    /**
     * Determines if changes were made or not, if not, exits. If there were changes, prompts them to save.
     */
    public void onExit(ActionEvent event) {
        if (_changesMade) {
            Alert confirmation = new Alert(Alert.AlertType.WARNING);
            confirmation.setTitle("Save before exiting?");
            confirmation.setHeaderText("Save changes?");
            confirmation.setContentText("Changes made will not be saved.");

            ButtonType save = new ButtonType("Save");
            ButtonType exit = new ButtonType("Exit");
            confirmation.getButtonTypes().setAll(save, exit);
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == save) {
                // action save
            } else {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    /// Stores plugin config manifests.
    public static ArrayList<ConfigManifest> ExtConfigs = new ArrayList<ConfigManifest>();

    /**
     * Updates screen with each config tab created by plugins.
     */
    public void updateScreen(ConfigManifest i) {
        ExtConfigs.add(i);

        for (ConfigTab config : i.getTabs()) {
            Tab tab = new Tab(config.Name);

            AnchorPane pane = new AnchorPane();
            pane.setMinHeight(0.0);
            pane.setMinWidth(0.0);
            pane.setPrefHeight(281.0);
            pane.setPrefWidth(574.0);

            for (Control ctrl : config.Controls) {
                pane.getChildren().add(ctrl);
            }

            tab.setContent(pane);
            tpConfig.getTabs().add(tab);
        }
    }

    /**
     * Attempts to load the config. If the config is not found or there is issues, loads defaults and saves them.
     */
    private void load() {
        File file = new File("server.properties");
        if (!file.exists()) {
            Alert warningNoPlugins = new Alert(Alert.AlertType.WARNING);
            warningNoPlugins.setTitle("server.properties not found");
            warningNoPlugins.setHeaderText("No config found");
            warningNoPlugins.setContentText("Defaults will be loaded and saved.");

            warningNoPlugins.showAndWait();
            loadDefaults();
            save(null);
            return;
        }

        HashMap<String, String> loads = new HashMap<>();

        try {
            for (String line : Files.readAllLines(file.toPath())) {
                if (line.toCharArray()[0] == '#') continue; // ignore

                int stop = line.indexOf("=");
                String config = line.substring(0, stop);
                String value = line.substring(stop + 1);
                loads.put(config, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert noLoad = new Alert(Alert.AlertType.WARNING);
            noLoad.setTitle("Cannot load");
            noLoad.setHeaderText("Unable to load");
            noLoad.setContentText("Was unable to load config due to IO Exception. Defaults will be set.");
            noLoad.showAndWait();
            loadDefaults();
            save(null);
        } finally {
            for (String key : loads.keySet()) {
                String value = loads.get(key);

                switch (key) {
                    case "accepts-transfers":
                        chkTransfers.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "allow-flight":
                        chkFlight.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "allow-nether":
                        chkNether.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "broadcast-console-to-ops":
                        chkConsoleOps.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "broadcast-rcon-to-ops":
                        chkRconOps.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "bug-report-link":
                        txtBugLink.setText(value);
                        break;
                    case "difficulty":
                        int sel = 1;
                        switch (value) {
                            case "peaceful": sel = 0; break;
                            case "easy": sel = 1; break;
                            case "normal": sel = 2; break;
                            case "hard": sel = 3; break;
                        }
                        cboDifficulty.getSelectionModel().select(sel);
                        break;
                    case "hardcore":
                        cboDifficulty.getSelectionModel().select(4);
                        break;
                    case "enable-command-block":
                        chkCmdBlock.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "enable-jmx-monitoring":
                        chkJmx.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "enable-query":
                        chkQuery.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "enable-rcon":
                        chkRcon.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "enable-status":
                        chkEnableStatus.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "enforce-secure-profile":
                        chkEnforceSecProfile.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "enforce-whitelist":
                    case "white-list": // todo seperate
                        chkEnforceWhitelist.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "entity-broadcast-range-percentage":
                        numEntBroad.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "force-gamemode":
                        chkForceGameMode.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "generate-structures":
                        chkStructures.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "hide-online-players":
                        chkHideOnline.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "level-name":
                        txtWorldName.setText(value);
                        break;
                    case "level-seed":
                        txtSeed.setText(value);
                        break;
                    case "level-type":
                        int val = 0;
                        String setting = value.substring(value.indexOf(":") + 1);
                        switch (setting) {
                            case "normal": val = 0; break;
                            case "flat": val = 1; break;
                            case "large_biomes": val = 2; break;
                            case "amplified": val = 3; break;
                            case "single_biome_surface": val = 4; break;
                        }

                        cboLevelType.getSelectionModel().select(val);
                        break;
                    case "log-ips":
                        ckLogIp.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "max-chained-neighbor-updates":
                        numMaxChain.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "max-players":
                        numMaxPlayers.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "max-tick-time":
                        numMaxTickTime.getValueFactory().setValue(Long.parseLong(value));
                        break;
                    case "max-world-size":
                        numMaxWorldSize.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "motd":
                        txtMotd.setText(value);
                        break;
                    case "network-compression-threshold":
                        numNetComprThresh.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "online-mode":
                        chkOnlineMode.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "op-permission-level":
                        numOpPerm.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "pause-when-empty-seconds":
                        numEmptySec.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "player-idle-timeout":
                        numIdleTimeout.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "prevent-proxy-connections":
                        chkPreventProxy.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "pvp":
                        chkPvP.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "query.port":
                        numQueryPort.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "rate-limit":
                        numRateLimit.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "rcon.password":
                        txtRconPassword.setText(value);
                        break;
                    case "rcon.port":
                        numRconPort.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "region-file-compression":
                        int x = 0;

                        switch (value) {
                            case "deflate": x = 0; break;
                            case "lz4": x = 1; break;
                            case "none": x = 2; break;
                        }
                        cboRegionFileCompr.getSelectionModel().select(x);
                        break;
                    case "require-resource-pack":
                        chkForceResPack.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "resource-pack":
                        txtResPack.setText(value);
                        break;
                    case "server-ip":
                        txtServerIp.setText(value);
                        break;
                    case "server-port":
                        numPort.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "simulation-distance":
                        numSimDist.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "spawn-monsters":
                        chkMonsters.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "spawn-protection":
                        numSpawnProtection.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    case "sync-chunk-writes":
                        chkSyChunkWr.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "use-native-transport":
                        chkUseNativeTransport.setSelected(Boolean.parseBoolean(value));
                        break;
                    case "view-distance":
                        numViewDist.getValueFactory().setValue(Integer.parseInt(value));
                        break;
                    default:
                        handleNewConfig(key, value);
                        break;
                }
            }
        }
    }

    /**
     * Handles new or glanced over configurations not handled by this application. Future versions will require handling new/changed items here.
     * @param key The key of the property.
     * @param value The value of the property.
     */
    private void handleNewConfig(String key, String value) {
        otherConfigs.put(key, value);
    }

    /// Used to store new/changed/glanced over config items.
    private final HashMap<String, String> otherConfigs = new HashMap<>();

    /// Loads defaults based on vanilla server.properties
    private void loadDefaults() {
        chkTransfers.setSelected(false);
        chkFlight.setSelected(false);
        chkNether.setSelected(true);
        chkConsoleOps.setSelected(true);
        chkRconOps.setSelected(true);
        txtBugLink.setText("https://change.me/");
        cboDifficulty.getSelectionModel().select(1);
        chkCmdBlock.setSelected(false);
        chkJmx.setSelected(false);
        chkQuery.setSelected(false);
        chkRcon.setSelected(false);
        chkEnableStatus.setSelected(true);
        chkEnforceSecProfile.setSelected(true);
        chkEnforceWhitelist.setSelected(false);
        numEntBroad.getValueFactory().setValue(100);
        chkForceGameMode.setSelected(false);
        numFuncPermLevel.getValueFactory().setValue(2);
        cboMode.getSelectionModel().select(0);
        chkStructures.setSelected(true);
        chkHideOnline.setSelected(false);
        txtWorldName.setText("world");
        txtSeed.setText(" ");
        cboLevelType.getSelectionModel().select(0);
        ckLogIp.setSelected(true);
        numMaxChain.getValueFactory().setValue(1000000);
        numMaxPlayers.getValueFactory().setValue(20);
        numMaxTickTime.getValueFactory().setValue(60000);
        numMaxWorldSize.getValueFactory().setValue(29999984);
        txtMotd.setText("A Minecraft Server");
        numNetComprThresh.getValueFactory().setValue(256);
        chkOnlineMode.setSelected(true);
        numOpPerm.getValueFactory().setValue(4);
        numEmptySec.getValueFactory().setValue(60);
        numIdleTimeout.getValueFactory().setValue(0);
        chkPreventProxy.setSelected(false);
        chkPvP.setSelected(true);
        numQueryPort.getValueFactory().setValue(25565);
        numRateLimit.getValueFactory().setValue(0);
        txtRconPassword.setText(" ");
        numRconPort.getValueFactory().setValue(25575);
        cboRegionFileCompr.getSelectionModel().select(2);
        chkForceResPack.setSelected(false);
        txtResPack.setText(" ");
        txtServerIp.setText(" ");
        numPort.getValueFactory().setValue(25565);
        numSimDist.getValueFactory().setValue(10);
        chkMonsters.setSelected(true);
        numSpawnProtection.getValueFactory().setValue(16);
        chkSyChunkWr.setSelected(true);
        chkUseNativeTransport.setSelected(true);
        numViewDist.getValueFactory().setValue(10);
        chkEnforceWhitelist.setSelected(false);

        // data packs
        listDataEnable.getItems().clear();
        listDataEnable.getItems().add("vanilla");
        listDataDisable.getItems().clear();
        for (String pack : _allDataPacks) {
            listDataDisable.getItems().add(pack);
        }
    }

    /// Used to store all properties during saving.
    private ArrayList<String> _props = new ArrayList<>();

    /**
     * Saves the config.
     * @param event
     */
    public void save(ActionEvent event) {
        for (ConfigManifest ext : ExtConfigs) {
            ext.save();
        }

        _props = new ArrayList<String>();

        add("accepts-transfers", chkTransfers);
        add("allow-flight", chkFlight);
        add("allow-nether", chkNether);
        add("broadcast-console-to-ops", chkConsoleOps);
        add("broadcast-rcon-to-ops", chkRconOps);
        add("bug-report-link", txtBugLink);
        add("difficulty", cboDifficulty);
        add("enable-command-block", chkCmdBlock);
        add("enable-jmx-monitoring", chkJmx);
        add("enable-query", chkQuery);
        add("enable-rcon", chkRcon);
        add("enable-status", chkEnableStatus);
        add("enforce-secure-profile", chkEnforceSecProfile);
        add("enforce-whitelist", chkEnforceWhitelist);
        // todo - whitelist setup?
        add("entity-broadcast-range-percentage", numEntBroad);
        add("force-gamemode", chkForceGameMode);
        add("generate-structures", chkStructures);
        _props.add("generator-settings={}"); // online search shows it doesn't work right so this application doesn't support its use
        // ignore hardcore line, handled by difficulty
        add("hide-online-players", chkHideOnline);
        add("level-name", txtWorldName);
        add("level-seed", txtSeed);
        _props.add("level-type=" + getLevelTypeValue());
        add("log-ips", ckLogIp);
        add("max-chained-neighbor-update", numMaxChain);
        add("max-players", numMaxPlayers);
        add("max-tick-time", numMaxTickTime);
        add("max-world-size", numMaxWorldSize);
        add("motd", txtMotd);
        add("network-compression-threshold", numNetComprThresh);
        add("online-mode", chkOnlineMode);
        add("op-permission-level", numOpPerm);
        add("pause-when-empty-seconds", numEmptySec);
        add("player-idle-timeout", numIdleTimeout);
        add("prevent-proxy-connections", chkPreventProxy);
        add("pvp", chkPvP);
        add("query.port", numQueryPort);
        add("rate-limit", numRateLimit);
        add("rcon.password", txtRconPassword);
        add("rcon.port", numRconPort);
        add("region-file-compression", cboRegionFileCompr);
        add("require-resource-pack", chkForceResPack);
        add("resource-pack", txtResPack);
        //add("resource-pack-prompt"); // todo
        add("server-ip", txtServerIp);
        add("server-port", numPort);
        add("simulation-distance", numSimDist);
        add("spawn-monsters", chkMonsters);
        add("spawn-protection", numSpawnProtection);
        add("sync-chunk-writes", chkSyChunkWr);
        add("use-native-transport", chkUseNativeTransport);
        add("view-distance", numViewDist);
        add("white-list", chkEnforceWhitelist);

        // still save other configs
        for (String key : otherConfigs.keySet()) {
            _props.add(key + "=" + otherConfigs.get(key));
        }

        // data packs
        ObservableList<String> enabledPacks = listDataEnable.getItems();
        ObservableList<String> disabledPacks = listDataDisable.getItems();
        String enabledList = "";
        String disabledList = "";

        for (String enabled : enabledPacks) {
            enabledList += enabled + ",";
        }

        for (String disabled : disabledPacks) {
            disabledList += disabled + ",";
        }

        _props.add("initial-disabled-packs=" + disabledList);
        _props.add("initial-enabled-packs=" + enabledList);


        try {
            File file = new File("server.properties");
            if (!file.exists())
                file.delete();

            file.createNewFile();

            FileWriter writer = new FileWriter(file);

            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");

            writer.write("# Minecraft server properties\n");
            writer.write("# " + sdf.format(now) + "\n");
            writer.write("# Generated by SpigotConfig by CopperPenguin96.");

            for (String line : _props) {
                writer.write("\n" + line);
            }

            writer.write("\n# End Config.");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Used by the saving method to filter out what kind of input is being saved.
     * @param key The key of the property being saved.
     * @param ctrl The control used to represent the property.
     */
    private void add(String key, Control ctrl) {
        if (ctrl instanceof CheckBox)
            addBoolean(key, (CheckBox)ctrl);

        if (ctrl instanceof TextField)
            addText(key, (TextField)ctrl);

        if (ctrl instanceof ComboBox)
            addCombo(key, (ComboBox)ctrl);

        if (ctrl instanceof Spinner)
            addNumber(key, (Spinner)ctrl);
    }

    /**
     * Adds the boolean value from the specified checkbox to the config.
     * @param key The key being added.
     * @param box The checkbox control being pulled from.
     */
    private void addBoolean(String key, CheckBox box) {
        _props.add(key + "=" + box.selectedProperty().getValue());
    }

    /**
     * Adds the string value from the specified textbox to the config.
     * @param key The key being added.
     * @param field The textbox control being pulled from.
     */
    private void addText(String key, TextField field) {
        _props.add(key + "=" + field.textProperty().getValue());
    }

    /**
     * Adds the selection string from the specified combobox to the config.
     * @param key The key being added.
     * @param cbo The combobox control being pulled fromm.
     */
    private void addCombo(String key, ComboBox cbo) {
        String value = cbo.valueProperty().getValue().toString().toLowerCase();

        // handle difficulty on own, instead of having a Hardcore checkbox, having hardcore treated like the fifth difficlty.
        if (key.equals("difficulty")) {
            if (value.equalsIgnoreCase("hardcore")) {
                _props.add(key + "=hard");
                _props.add("hardcore=true");
            } else {
                _props.add(key + "=" + value);
                _props.add("hardcore=false");
            }

            return;
        }

        // non-difficulty related items
        _props.add(key + "=" + cbo.valueProperty().getValue().toString().toLowerCase());
    }

    /**
     * Adds the number from the specified spinner to the config.
     * @param key The key being added.
     * @param sp The spinner control being pulled from.
     */
    private void addNumber(String key, Spinner sp) {
        _props.add(key + "=" + sp.valueProperty().getValue());
    }

    /**
     * Event used to enable packs.
     */
    public void enablePack(ActionEvent actionEvent) {
        if (listDataDisable.getItems().isEmpty()) return;

        ObservableList os = listDataDisable.getSelectionModel().getSelectedItems();

        for (int x = 0; x < os.size(); x++) {
            String text = os.get(x).toString();
            listDataDisable.getItems().remove(x);
            listDataEnable.getItems().add(text);
        }
    }

    /**
     * Event used to disable packs.
     */
    public void disablePack(ActionEvent actionEvent) {
        if (listDataEnable.getItems().isEmpty()) return;

        ObservableList os = listDataEnable.getSelectionModel().getSelectedItems();

        for (int x = 0; x < os.size(); x++) {
            String text = os.get(x).toString();
            listDataEnable.getItems().remove(x);
            listDataDisable.getItems().add(text);
        }
    }

    /**
     * Attaches listeners to applicable fields.
     */
    private void attachListeners() {
        ChangeListener<String> textAreas = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                _changesMade = true;
            }
        };

        ChangeListener<Integer> numAreas = new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                _changesMade = true;
            }
        };

        ChangeListener<Boolean> chkAreas = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                _changesMade = true;
            }
        };

        txtServerName.textProperty().addListener(textAreas);
        txtServerIp.textProperty().addListener(textAreas);
        numPort.valueProperty().addListener(numAreas);
        numMaxPlayers.valueProperty().addListener(numAreas);
        txtMotd.textProperty().addListener(textAreas);
        cboMode.valueProperty().addListener(textAreas);
        cboDifficulty.valueProperty().addListener(textAreas);
        chkForceGameMode.selectedProperty().addListener(chkAreas);
        chkEnableStatus.selectedProperty().addListener(chkAreas);
        chkHideOnline.selectedProperty().addListener(chkAreas);
        chkConsoleOps.selectedProperty().addListener(chkAreas);
        chkPvP.selectedProperty().addListener(chkAreas);
        numIdleTimeout.valueProperty().addListener(numAreas);
        txtWorldName.textProperty().addListener(textAreas);
        txtSeed.textProperty().addListener(textAreas);
        chkStructures.selectedProperty().addListener(chkAreas);
        chkMonsters.selectedProperty().addListener(chkAreas);
        numMaxWorldSize.valueProperty().addListener(numAreas);
        numSimDist.valueProperty().addListener(numAreas);
        numViewDist.valueProperty().addListener(numAreas);
        numSpawnProtection.valueProperty().addListener(numAreas);
        chkEnforceWhitelist.selectedProperty().addListener(chkAreas);
        listWhitelist.itemsProperty().addListener(textAreas);
        chkTransfers.selectedProperty().addListener(chkAreas);
        chkEnforceSecProfile.selectedProperty().addListener(chkAreas);
        ckLogIp.selectedProperty().addListener(chkAreas);
        chkOnlineMode.selectedProperty().addListener(chkAreas);
        chkPreventProxy.selectedProperty().addListener(chkAreas);
        chkQuery.selectedProperty().addListener(chkAreas);
        numQueryPort.valueProperty().addListener(numAreas);
        chkRcon.selectedProperty().addListener(chkAreas);
        numRconPort.valueProperty().addListener(numAreas);
        txtRconPassword.textProperty().addListener(textAreas);
        chkRconOps.selectedProperty().addListener(chkAreas);
        numFuncPermLevel.valueProperty().addListener(numAreas);
        numMaxTickTime.valueProperty().addListener(numAreas);
        numRateLimit.valueProperty().addListener(numAreas);
        cboRegionFileCompr.valueProperty().addListener(textAreas);
        numMaxChain.valueProperty().addListener(numAreas);
        chkCmdBlock.selectedProperty().addListener(chkAreas);
        chkJmx.selectedProperty().addListener(chkAreas);
        chkSyChunkWr.selectedProperty().addListener(chkAreas);
        chkUseNativeTransport.selectedProperty().addListener(chkAreas);
        numNetComprThresh.valueProperty().addListener(numAreas);
        txtBugLink.textProperty().addListener(textAreas);
        numOpPerm.valueProperty().addListener(numAreas);
        numEmptySec.valueProperty().addListener(numAreas);
        numEntBroad.valueProperty().addListener(numAreas);
        chkFlight.selectedProperty().addListener(chkAreas);
        chkNether.selectedProperty().addListener(chkAreas);
        txtResPack.textProperty().addListener(textAreas);
        chkForceResPack.selectedProperty().addListener(chkAreas);
        listDataDisable.itemsProperty().addListener(textAreas);
        listDataEnable.itemsProperty().addListener(textAreas);
        cboLevelType.valueProperty().addListener(textAreas);
    }

    private String getLevelTypeValue() {
        String mc = "minecraft\\:";
        if (cboLevelType.getValue() == null) {
            return mc + "normal";
        }

        switch (cboLevelType.getSelectionModel().getSelectedIndex()) {
            case 0:
                return mc + "normal";
            case 1:
                return mc + "flat";
            case 2:
                return mc + "large_biomes";
            case 3:
                return mc + "amplified";
            case 4:
                return mc + "single_biome_surface";
        }

        return null;
    }

    public void whitelistCheck(ActionEvent actionEvent) {
        if (!chkEnforceWhitelist.selectedProperty().get()) {
            listWhitelist.setDisable(true);
            btnAdd.setDisable(true);
            btnRemove.setDisable(true);
        } else {
            listWhitelist.setDisable(false);
            btnAdd.setDisable(false);
            btnRemove.setDisable(false);
        }
    }

    public void rconCheck(ActionEvent actionEvent) {
        if (!chkRcon.selectedProperty().get()) {
            numRconPort.setDisable(true);
            txtRconPassword.setDisable(true);
            chkRconOps.setDisable(true);
        } else {
            numRconPort.setDisable(false);
            txtRconPassword.setDisable(false);
            chkRconOps.setDisable(false);
        }
    }

    public void queryCheck(ActionEvent actionEvent) {
        numQueryPort.setDisable(!chkQuery.selectedProperty().get());
    }

    private void setToolTips() {
        chkTransfers.setTooltip(getTip("accepts-transfers"));
        chkFlight.setTooltip(getTip("allow-flight"));
        chkNether.setTooltip(getTip("allow-nether"));
        chkConsoleOps.setTooltip(getTip("broadcast-console-to-ops"));
        chkRconOps.setTooltip(getTip("broadcast-rcon-to-ops"));
        txtBugLink.setTooltip(getTip("bug-report-link"));
        cboDifficulty.setTooltip(getTip("difficulty"));
        chkCmdBlock.setTooltip(getTip("enable-command-block"));
        chkJmx.setTooltip(getTip("enable-jmx-monitoring"));
        chkQuery.setTooltip(getTip("enable-query"));
        chkRcon.setTooltip(getTip("enable-rcon"));
        chkEnableStatus.setTooltip(getTip("enable-status"));
        chkEnforceSecProfile.setTooltip(getTip("enforce-secure-profile"));
        chkEnforceWhitelist.setTooltip(getTip("enforce-whitelist"));
        numEntBroad.setTooltip(getTip("entity-broadcast-range-percentage"));
        chkForceGameMode.setTooltip(getTip("force-gamemode"));
        numFuncPermLevel.setTooltip(getTip("function-permission-level"));
        cboMode.setTooltip(getTip("gamemode"));
        chkStructures.setTooltip(getTip("generate-structures"));
        chkHideOnline.setTooltip(getTip("hide-online-players"));
        txtWorldName.setTooltip(getTip("level-name"));
        cboLevelType.setTooltip(getTip("level-type"));
        ckLogIp.setTooltip(getTip("log-ips"));
        numMaxChain.setTooltip(getTip("max-chained-neigbor-updates"));
        numMaxPlayers.setTooltip(getTip("max-players"));
        numMaxTickTime.setTooltip(getTip("max-tick-time"));
        numMaxWorldSize.setTooltip(getTip("max-world-size"));
        txtMotd.setTooltip(getTip("motd"));
        numNetComprThresh.setTooltip(getTip("network-compression-threshold"));
        chkOnlineMode.setTooltip(getTip("online-mode"));
        numOpPerm.setTooltip(getTip("op-permissoin-level"));
        numEmptySec.setTooltip(getTip("pause-when-empty-seconds"));
        numIdleTimeout.setTooltip(getTip("player-idle-timeout"));
        chkPreventProxy.setTooltip(getTip("prevent-proxy-connections"));
        chkPvP.setTooltip(getTip("pvp"));
        numQueryPort.setTooltip(getTip("query.port"));
        numRateLimit.setTooltip(getTip("rate-limit"));
        txtRconPassword.setTooltip(getTip("rcon.password"));
        numRconPort.setTooltip(getTip("rcon.port"));
        cboRegionFileCompr.setTooltip(getTip("region-file-compression"));
        chkForceResPack.setTooltip(getTip("require-resource-pack"));
        txtResPack.setTooltip(getTip("resource-pack"));
        txtServerIp.setTooltip(getTip("server-ip"));
        numPort.setTooltip(getTip("server-port"));
        numSimDist.setTooltip(getTip("simulation-distance"));
        chkMonsters.setTooltip(getTip("spawn-monsters"));
        numSpawnProtection.setTooltip(getTip("spawn-protection"));
        chkSyChunkWr.setTooltip(getTip("sync-chunk-writes"));
        chkUseNativeTransport.setTooltip(getTip("use-native-transport"));
        numViewDist.setTooltip(getTip("view-distance"));
    }

    private Tooltip getTip(String configName) {
        Tooltip tip = new Tooltip(getTipMessage(configName));
        tip.setWrapText(true);
        return tip;
    }

    /**
     * Descriptions are from <a href="https://minecraft.wiki/w/Server.properties">the Minecraft Wiki</a>
     */
    private String getTipMessage(String configName) {
        switch (configName) {
            case "accepts-transfers":
                return "Whether to accept incoming transfers via a transfer packet.\n" +
                        "unchecked- incoming transfers are rejected, and the player is disconnected.\n" +
                        "checked- incoming transfers are allowed, and the server must approve it.";
            case "allow-flight":
                return "Whether players can use fliht on the server while in Survival mode by using mods.\n" +
                        "With allow-flight enabled, grifers may become more common, because it makes their work easier.\n" +
                        "In Creative mode, this has no effect.\n\n" +
                        "unchecked- Flight is not allowed (players in air for at least 5s get kicked).\n" +
                        "checked- Flight is allowed, and used if the player has a fly mod installed.";
            case "allow-nether":
                return "Whether players can travel to the Nether.";
            case "broadcast-console-to-ops":
                return "Whether to send console command outputs to all online operators.";
            case "broadcast-rcon-to-ops":
                return "Whether to send rcon console command outputs to all online operations.";
            case "bug-report-link":
                return "The URL for the report_bug server link. If empty, the link is not sent.";
            case "difficulty":
                return "The difficulty (such as damage dealt by mobs and the way hunger and poison affects players) of the server.\n" +
                        "Either peaceful, easy, normal, or hard.";
            case "enable-command-block":
                return "Whether command blocks are enabled.";
            case "enable-jmx-monitoring":
                return "Whether to expose MBean with the Object name net.minecraft.server:type=server and two attributes\n" +
                        "averageTickTime and tickTimes exposing the tick times in milliseconds.\n" +
                        "In order to enable JMX on the Java runtime you also need to use certain JVM flags.";
            case "enable-query":
                return "Whether to enable query, which provides information about the server.";
            case "enable-rcon":
                return "Whether to enable rcon, which allows access to the server console over a network.\n" +
                        "It's not recommended to connect to rcon via untrusted networks, like the internet, as it is not encrypted.\n" +
                        "All data sent between the client and the server (including the rcon password) can be intercepted. Ideally, only connect to rcon from localhost.";
            case "enable-status":
                return "Whether the server appears as \"online\" on the server list. If set to false, status replies to clients are suppressed.\n" +
                        "This means the server appears as offline, but still accepts connections.";
            case "enforce-secure-profile":
                return "Whether to allow players with a Mojang-signed public key to join the server.\n" +
                        "If this is not enabled, all chat messages will be left unsigned and unable to be reported. Clients will get warned about this when connecting to the server.";
            case "enforce-whitelist":
                return "Whether to enforce changes to the whitelist.\n" +
                        "When this option as well as the whitelist is enabled, players not present on the whitelist get kicked from the server after the server reloads.";
            case "entity-broadcast-range-percentage":
                return "How close entities need to be to the player to be sent.\n" +
                        "Higher values means they'll be rendered from farther away, pontially causing more lag.\n" +
                        "This is expressed as a percentage.";
            case "force-gamemode":
                return "Whether to switch players to the default game mode on join.";
            case "function-permission-level":
                return "The default permission level for functions.";
            case "gamemode":
                return "The default game mode. Either survival, creative, adventure, or spectator.";
            case "generate-structures":
                return "Whether structures (such as villages) are generated.\n" +
                        "Dungeons still generate if this is set to false.";
            case "hide-online-players":
                return "Whether to disable sending the player list on status requests.";
            case "level-name":
                return "*** Only applies to the main world.\n" +
                        "The world name/directory. If a directory at this path exists and is a valid world,\n" +
                        "it will be loaded by the server. Otherwise the server will generate a new world in this directory.";
            case "level-seed":
                return "The seed for the generated world. If left blank, a random seed is generated.";
            case "level-type":
                return "The preset for the generated world.\n" +
                        "Either normal, flat, large biomes, amplified, or single biome surface.";
            case "log-ips":
                return "Whether to show client IP addresses in messages printed to the server console or the log file.";
            case "max-chained-neighbor-updates":
                return "The limit of consecutive neighbor updates before skipping additional ones.\n" +
                        "Negative values disable the limit.";
            case "max-players":
                return "The maximum number of players that can play on the server at the same time.\n" +
                        "Ops with the bypassesPlayerLimit enabled can join the server even if the server is full.";
            case "max-tick-time":
                return "The maximum number of milliseconds a single tick may take.";
            case "max-world-size":
                return "The amount of blocks from the center of the world where the world border appears.";
            case "motd":
                return "The message displayed in the server list of the client, below the server name.";
            case "network-compression-threshold":
                return "How big should a packet be to be compressed. -1 disables this.";
            case "online-mode":
                return "Whether to only allow players verified with the Minecraft account database to join.";
            case "op-permission-level":
                return "The default permission level for ops when using /op.";
            case "pause-when-empty-seconds":
                return "How many seconds have to pass after no player has been online before the server is paused.";
            case "player-idle-timeout":
                return "How many minutes does the player have to idle before being kicked. If set to 0, idle players are never kicked.";
            case "prevent-proxy-connections":
                return "Whether to kick players if the ISP/AS sent from the server is different from the one Mojang authenticates.";
            case "pvp":
                return "Whether to enable Player vs. Player combat.";
            case "query.port":
                return "The UDP port number query.";
            case "rate-limit":
                return "The maximum amount of packets a player can send before getting kicked. Setting to 0 disables this.";
            case "rcon.password":
                return "The password for rcon. If the password is blank and rcon is enabled, it will not start as a safeguard.";
            case "rcon.port":
                return "The TCP port number rcon listens on.";
            case "region-file-compression":
                return "The algorithm used for compression chunks in regions.";
            case "require-resource-pack":
                return "Whether players are disconnected if they decline to use the resource pack.";
            case "resource-pack":
                return "The resource pack download URL.";
            case "server-ip":
                return "The IP address the server listens on. If empty, the server listens on all available IP addresses.\n" +
                        "It is recommended to leave this empty.";
            case "server-port":
                return "The TCP Port number for the server.\n" +
                        "The port must be forwarded if the server is hosted in a network using NAT.";
            case "simulation-distance":
                return "The maximum distance from players that living entities may be located in order to be updated by the server, by chunks.";
            case "spawn-monsters":
                return "Whether monsters can spawn.";
            case "spawn-protection":
                return "The side length of the square spawn protection area.";
            case "sync-chunk-writes":
                return "Whether to enable synchronous chunk writes.";
            case "use-native-transport":
                return "Whether to use optimized packet sending/receiving on Linux.";
            case "view-distance":
                return "The amount of world data the server sends the client, by chunks.";
            case "white-list":
                return "Whether the whitelist is enabled.";
        }

        return "";
    }


    // region Controls

    public TabPane tpConfig;
    public TextField txtServerName;
    public TextField txtServerIp;
    public Spinner numPort;
    public Spinner numMaxPlayers;
    public TextField txtMotd;
    public Button btnFormats;
    public ComboBox cboMode;
    public ComboBox cboDifficulty;
    public CheckBox chkForceGameMode;
    public CheckBox chkEnableStatus;
    public CheckBox chkHideOnline;
    public CheckBox chkConsoleOps;
    public CheckBox chkPvP;
    public Spinner numIdleTimeout;
    public TextField txtWorldName;
    public TextField txtSeed;
    public CheckBox chkStructures;
    public CheckBox chkMonsters;
    public Spinner numMaxWorldSize;
    public Spinner numSimDist;
    public Spinner numViewDist;
    public Spinner numSpawnProtection;
    public CheckBox chkEnforceWhitelist;
    public ListView listWhitelist;
    public Button btnAdd;
    public Button btnRemove;
    public CheckBox chkTransfers;
    public CheckBox chkEnforceSecProfile;
    public CheckBox ckLogIp;
    public CheckBox chkOnlineMode;
    public CheckBox chkPreventProxy;
    public CheckBox chkQuery;
    public Spinner numQueryPort;
    public CheckBox chkRcon;
    public Spinner numRconPort;
    public TextField txtRconPassword;
    public CheckBox chkRconOps;
    public Spinner numFuncPermLevel;
    public Spinner numMaxTickTime;
    public Spinner numRateLimit;
    public ComboBox cboRegionFileCompr;
    public Spinner numMaxChain;
    public CheckBox chkCmdBlock;
    public CheckBox chkJmx;
    public CheckBox chkSyChunkWr;
    public CheckBox chkUseNativeTransport;
    public Spinner numNetComprThresh;
    public TextField txtBugLink;
    public Spinner numOpPerm;
    public Spinner numEmptySec;
    public Spinner numEntBroad;
    public CheckBox chkFlight;
    public CheckBox chkNether;
    public TextField txtResPack;
    public CheckBox chkForceResPack;
    public ListView listDataDisable;
    public ListView listDataEnable;
    public Button btnDataEnable;
    public Button btnDataDisable;
    public Button btnExit;
    public Button btnSave;
    public ComboBox cboLevelType;

    // endregion
}
