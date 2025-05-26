package com.discord.bot;

public class MessageEntry {
    private String time;
    private String channel;
    private String author;
    private String message;

    public MessageEntry(String time, String channel, String author, String message) {
        this.time = time;
        this.channel = channel;
        this.author = author;
        this.message = message;
    }

    // Getters
    public String getTime() { return time; }
    public String getChannel() { return channel; }
    public String getAuthor() { return author; }
    public String getMessage() { return message; }
}