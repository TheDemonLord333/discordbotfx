package com.discord.bot;

import com.discord.bot.DiscordBotFX;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

class BotEventListenerFX extends ListenerAdapter {
    private DiscordBotFX gui;

    public BotEventListenerFX(DiscordBotFX gui) {
        this.gui = gui;
    }

    @Override
    public void onReady(ReadyEvent event) {
        gui.log("Bot ready! Logged in as: " + event.getJDA().getSelfUser().getAsTag());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        String author = event.getAuthor().getName();
        String channel = event.getChannel().getName();

        gui.log("Message in #" + channel + " by " + author + ": " + message);
        gui.addMessageToHistory(channel, author, message);

        // Simple auto-responses
        if (message.equals("!ping")) {
            event.getChannel().sendMessage("Pong! (from JavaFX)").queue();
        }
    }
}