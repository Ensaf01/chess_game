module com.example.mychess {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.mychess to javafx.fxml;
    exports com.example.mychess;
}