# Spigot Config

Spigot Config is a GUI that makes editing the Minecraft server.properties easier for Spigot users. It also has plugin support for Spigot plugins. Currently available for Spigot 1.21.7.

## An Organized GUI

Spigot Config has an organized GUI that makes changing your server's settings easier. Not sure which config to edit? Look through the tabs and see what's available. Not sure what an item does? Hover over it and see what the tip says.

![enter image description here](https://i.imgur.com/0mklk0f.png)

## Installing
Looking to use this on your server? It's super easy. Download me from releases and place me in the root directory of your server. Open opening, it will automatically start reading your config or it will create a new one if it can't find it.

## Plugin Support

Spigot plugin developers can create their own tabs for this GUI. It takes some simple setup to do this.
First, add Spigot Config through maven. The repository you need depends on if you want to use snapshots or official releases. If you are using snapshots, you will get them from github packages. For official releases, see below.
In pom.xml, add this repository:

    <repository>
	    <id>repsy</id>
	    <name>CopperPenguin on Repsy</name>
	    <url>https://repo.repsy.io/mvn/copperpenguin96/rusty-mineshaft</url>
	</repository>
And for your dependency:

    <dependency>
	    <groupId>com.copperpenguin96</groupId>
	    <artifactId>spigot-config</artifactId>
	    <version>1.0</version>
    </dependency>
 
Once you've successfully added the package (and ensure you have JavaFX as well as that is what the GUI uses), you'll need to create your first Configuration Tab. The recommended dimensions of your `AnchorPane` are height 281 and width 574 unless you use a scrollview. *Note: Do not set events or controllers in your fxml. This will cause a crash because it doesn't know how to handle preset items like that.* Here is `Example.fxml`:

    <?xml version="1.0" encoding="UTF-8"?>  
	<?import javafx.scene.control.*?>  
	<?import javafx.scene.layout.*?>  
	<AnchorPane prefHeight="281.0" prefWidth="574.0" xmlns="http://javafx.com/javafx/17.0.12" xmlns:fx="http://javafx.com/fxml/1">  
		<children> 
			<TextField fx:id="txtExampleField" layoutX="213.0" layoutY="128.0" />  
			<Button fx:id="btnAction" layoutX="261.0" layoutY="172.0" mnemonicParsing="false" text="Button" />
		</children>
	</AnchorPane>
	
Make sure to make a class to handle any code or functions you want to incorporate with your tab. It's important that you maintain the order here for everything to work. Here is `ExampleTab.java`:

	public class ExampleTab extends ConfigTab {  
  
	    public ExampleTab(String name, URL resource) {  
		    /*
		    * Initiate with the name.
		    * This is what shows to the end user
		    */
	        super("Example Tab"); 

			// Create our events.
			registerEvents();
			
			// Set the scene, best bet is to to put it in your project's resources.
			setScene(getClass().getClassLoader().getResourceAsStream("ExampleTab.fxml");
			
			// This sets the controller to this class so that it can be used.
			setClass(this.getClass()); 
	    }

		private void registerEvents() {
			// The name you put here must match the fx:id
			addEvent("btnAction", ActionEvent.ACTION, (EventHandler<ActionEvent> event -> {
				System.out.println("Hello world!");
			}
		}

		// Make sure to declare your controls here as well so you can use them.
		private Button btnAction;
	}

Then, in the main package specified in your plugin.yml, add a new class called `SpigotConfig`. You'll need a parameterless constructer and override the save() method. **Note**: *Spigot Config does not handle saving for you.* The Save button will call the save() method in your SpigotConfig.java, but if you do not handle this, *nothing will happen*. Spigot-Config only handles server.proprties.

	public class SpigotConfig extends ConfigManifest {
		public SpigotConfg() throws IOException {
			// Load our tab
			ExampleTab tab = new ExampleTab();
			this.add(tab);
		}
	}

*Viola!* Your tab appears when you open the Config.



## Contributing?

Feel free to open any issues or submit any pull requests.