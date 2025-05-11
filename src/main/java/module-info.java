module org.aiacon.simuladordemobilidadeurbana {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;


    opens org.aiacon.simuladordemobilidadeurbana to javafx.fxml;
    exports org.aiacon.simuladordemobilidadeurbana;
}