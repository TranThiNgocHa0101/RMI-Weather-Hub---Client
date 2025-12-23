module com.weatherclientapp {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires java.rmi;

    requires jakarta.mail;
    requires itextpdf;
    requires jdk.jsobject;


    // Application entry
    exports com.weatherclientapp.client;
    opens com.weatherclientapp.client to javafx.fxml;

    // FXML controllers
    opens com.weatherclientapp.Login to javafx.fxml;
    opens com.weatherclientapp.Home1 to javafx.fxml;

    // RMI
    exports com.weatherclientapp.common to java.rmi;
    opens com.weatherclientapp.common to java.rmi;
    opens com.weatherclientapp.Admin to javafx.fxml,javafx.base;
    opens com.weatherclientapp.data to javafx.base, javafx.fxml;


}
