module com.discord.bot.discordbotfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires net.dv8tion.jda;
    requires java.desktop;


    opens com.discord.bot.discordbotfx to javafx.fxml;
    exports com.discord.bot;
}