module com.example.maxchat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.net;


    opens com.example.maxchat to javafx.fxml;
    exports com.example.maxchat;
}