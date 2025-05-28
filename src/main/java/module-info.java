module com.discord.bot {

    requires javafx.controls;

    requires javafx.fxml;

    requires net.dv8tion.jda;

    requires java.desktop;


    // Öffne das korrekte Package für JavaFX

    opens com.discord.bot to javafx.fxml;

    exports com.discord.bot;

}
}