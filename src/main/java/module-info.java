module es.ieslosmontecillos.appayuda {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.jsobject;
    requires javafx.web;


    opens es.ieslosmontecillos.appayuda to javafx.fxml;
    exports es.ieslosmontecillos.appayuda;
}