package es.ieslosmontecillos.appayuda;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;

public class AppAyuda extends Application {
    //atributos y variables
    private Scene scene;

    @Override
    public void start(Stage primaryStage) {
        //Creamos la escenea
        primaryStage.setTitle("AppAyuda");
        scene = new Scene(new Browser(), 750, 500, Color.web("666970"));
        primaryStage.setScene(scene);
        scene.getStylesheets().add(AppAyuda.class.getResource("css/BrowserToolbar.css").toExternalForm());
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}

class Browser extends Region {

    //crea una barra de herramientas
    private HBox toolBar;
    private static String[] imageFiles = new String[]{
            "Images/moodle.jpg",
            "Images/facebook.jpg",
            "Images/twitter.jpg",
            "Images/help.png"
    };
    private static String[] captions = new String[]{
            "Aula Montecillos",
            "Facebook",
            "Twitter",
            "Help"
    };

    private static String[] urls = new String[]{
            "http://www.ieslosmontecillos.es/",
            "https://es-es.facebook.com/",
            "https://twitter.com",
            AppAyuda.class.getResource("help.html").toExternalForm()
    };

    final ImageView selectedImage = new ImageView();
    final Hyperlink[] hpls = new Hyperlink[captions.length];
    final Image[] images = new Image[imageFiles.length];

    //crea el WebView y el WebEngine
    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();

    final Button toggleHelpTopics = new Button("Toogle Help Topics");
    final WebView smallView = new WebView();
    private boolean needDocumentationButton = false;
    final ComboBox comboBox = new ComboBox();

    public Browser() {
        getStyleClass().add("browser");

        //Gestiona los enlaces
        for (int i = 0; i < captions.length; i++) {
            Hyperlink hpl = hpls[i] = new Hyperlink(captions[i]);
            Image image = images[i] = new Image(getClass().getResourceAsStream(imageFiles[i]));
            hpl.setGraphic(new ImageView(image));
            final String url = urls[i];
            final boolean addButton = (hpl.getText().equals("Help"));

            //gestiona el evento
            hpl.setOnAction((ActionEvent arg0) -> {
                needDocumentationButton = addButton;
                webEngine.load(url);
            });
        }

        //carga la web
        webEngine.load("http://www.ieslosmontecillos.es/");

        comboBox.setPrefWidth(60);

        //crea el toolbar
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(hpls);
        toolBar.getChildren().add(createSpacer());
        toolBar.getChildren().add(comboBox);

        //gestiona el evento del botón
        toggleHelpTopics.setOnAction(new EventHandler() {
            @Override
            public void handle(Event t) {
                webEngine.executeScript("toggle_visibility('help_topics')");
            }
        });

        //handle popup windows
        webEngine.setCreatePopupHandler(
                new Callback<PopupFeatures, WebEngine>() {
                    @Override public WebEngine call(PopupFeatures config) {
                        smallView.setFontScale(0.8);
                        if (!toolBar.getChildren().contains(smallView)) {
                            toolBar.getChildren().add(smallView);
                        }
                        return smallView.getEngine();
                    }
                }
        );

        //ajusta el tamaño de la ventana
        smallView.setPrefSize(120, 80);

        webEngine.setCreatePopupHandler((PopupFeatures config) -> {
            smallView.setFontScale(0.8);
            if (!toolBar.getChildren().contains(smallView)) {
                toolBar.getChildren().add(smallView);
            }
            return smallView.getEngine();
        });

        //procesa el historial
        final WebHistory history = webEngine.getHistory();
        history.getEntries().addListener(
                (ListChangeListener.Change<? extends WebHistory.Entry> c) -> {
                    c.next();
                    c.getRemoved().stream().forEach((e) -> {
                        comboBox.getItems().remove(e.getUrl());
                    });
                    c.getAddedSubList().stream().forEach((e) -> {
                        comboBox.getItems().add(e.getUrl());
                    });
                });

        //set the behavior for the history combobox
        comboBox.setOnAction((Event ev) -> {
            int offset
                    = comboBox.getSelectionModel().getSelectedIndex()
                    - history.getCurrentIndex();
            history.go(offset);
        });

        //listener y evento de cargar la pagina
        webEngine.getLoadWorker().stateProperty().addListener(
                (ObservableValue<? extends State> ov, State oldState, State newState) -> {
                    toolBar.getChildren().remove(toggleHelpTopics);
                    if (newState == State.SUCCEEDED) {
                        JSObject win = (JSObject) webEngine.executeScript("window");
                        win.setMember("AppAyuda", new JavaApp());
                        if (needDocumentationButton) {
                            toolBar.getChildren().add(toggleHelpTopics);
                        }
                    }
                });

        //añadimos a la escena
        getChildren().add(toolBar);
        getChildren().add(browser);
    }

    // JavaScript interface object
    public class JavaApp {
        public void exit() {
            Platform.exit();
        }
    }

    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(browser, 0, 0, w, h - tbHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(toolBar, 0, h - tbHeight, w, tbHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    //Ajustan el tamaño
    @Override
    protected double computePrefWidth(double height) {
        return 750;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 500;
    }


}