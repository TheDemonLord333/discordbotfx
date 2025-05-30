package com.discord.bot;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DiscordBotFX extends Application {

    // Core Components
    private JDA jda;
    private Stage primaryStage;

    // UI Components
    private TextField tokenField;
    private TextField messageField;
    private TextField channelIdField;
    private TextField statusField;
    private ComboBox<String> activityTypeBox;
    private ComboBox<String> onlineStatusBox; // NEU: Anwesenheitsstatus ComboBox
    private Button setOnlineStatusButton; // NEU: Button für Anwesenheitsstatus
    private TextArea logArea;
    private TextArea controlsLogArea; // Separate log area for Controls tab
    private TextArea dmLogArea; // Separate log area for DM tab
    private Button connectButton;
    private Button disconnectButton;
    private Button sendMessageButton;
    private Button setStatusButton;
    private Label connectionStatus;
    private Label uptimeLabel;
    private Label guildCountLabel;
    private Label userCountLabel;

    // Direct Messages Components
    private TextField dmUserIdField;
    private TextField dmMessageField;
    private Button sendDmButton;
    private Button sendDmEmbedButton; // NEU: Button für DM Embeds
    private TableView<MessageEntry> dmHistoryTable;
    private ObservableList<MessageEntry> dmHistory;

    // Message History
    private TableView<MessageEntry> messageTable;
    private ObservableList<MessageEntry> messageHistory;

    // Statistics
    private int messagesSent = 0;
    private int messagesReceived = 0;
    private long startTime = 0;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Discord Bot Controller - JavaFX");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(750);

        Scene scene = new Scene(createMainLayout(), 1000, 750);

        // Load Discord theme CSS
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("CSS file not found, using default styling");
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Setup shutdown hook
        primaryStage.setOnCloseRequest(e -> {
            if (jda != null) {
                jda.shutdown();
            }
            Platform.exit();
        });

        startUptimeTimer();
    }

    private BorderPane createMainLayout() {
        BorderPane root = new BorderPane();

        // Create tab pane for different sections
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Connection Tab
        Tab connectionTab = new Tab("Connection");
        connectionTab.setContent(createConnectionPane());

        // Controls Tab
        Tab controlsTab = new Tab("Controls");
        controlsTab.setContent(createControlsPane());

        // Message History Tab
        Tab historyTab = new Tab("Messages");
        historyTab.setContent(createMessageHistoryPane());

        // Direct Messages Tab
        Tab dmTab = new Tab("Direct Messages");
        dmTab.setContent(createDirectMessagesPane());

        // Statistics Tab
        Tab statsTab = new Tab("Statistics");
        statsTab.setContent(createStatisticsPane());

        tabPane.getTabs().addAll(connectionTab, controlsTab, historyTab, dmTab, statsTab);

        root.setCenter(tabPane);
        root.setBottom(createStatusBar());

        return root;
    }

    private VBox createConnectionPane() {
        VBox connectionPane = new VBox(15);
        connectionPane.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Bot Connection");
        titleLabel.getStyleClass().add("title-label");

        // Token input
        HBox tokenBox = new HBox(10);
        tokenBox.setAlignment(Pos.CENTER_LEFT);
        Label tokenLabel = new Label("Bot Token:");
        tokenLabel.setPrefWidth(100);
        tokenLabel.getStyleClass().add("subsection-label");
        tokenField = new TextField();
        tokenField.setPrefWidth(300);
        tokenField.setPromptText("Enter your Discord bot token here...");

        tokenBox.getChildren().addAll(tokenLabel, tokenField);

        // Connection buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        connectButton = new Button("Connect Bot");
        connectButton.setPrefWidth(120);
        connectButton.getStyleClass().add("connect-button");
        connectButton.setOnAction(e -> connectBot());

        disconnectButton = new Button("Disconnect Bot");
        disconnectButton.setPrefWidth(120);
        disconnectButton.getStyleClass().add("disconnect-button");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> disconnectBot());

        buttonBox.getChildren().addAll(connectButton, disconnectButton);

        // Connection status
        connectionStatus = new Label("Status: Disconnected");
        connectionStatus.getStyleClass().add("status-disconnected");

        // Log area
        Label logLabel = new Label("Connection Log:");
        logLabel.getStyleClass().add("section-label");

        logArea = new TextArea();
        logArea.setPrefHeight(200);
        logArea.setEditable(false);
        logArea.getStyleClass().add("log-area");
        logArea.setWrapText(true);

        connectionPane.getChildren().addAll(
                titleLabel,
                new Separator(),
                tokenBox,
                buttonBox,
                connectionStatus,
                new Separator(),
                logLabel,
                logArea
        );

        return connectionPane;
    }

    private VBox createControlsPane() {
        VBox controlsPane = new VBox(15);
        controlsPane.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Bot Controls");
        titleLabel.getStyleClass().add("title-label");

        // Message sending section
        VBox messageSection = createMessageSection();

        // Status setting section (ERWEITERT)
        VBox statusSection = createStatusSection();

        // NEU: Anwesenheitsstatus section
        VBox presenceSection = createPresenceSection();

        // Quick actions
        VBox quickActionsSection = createQuickActionsSection();

        // Bot Log section for Controls tab
        VBox logSection = createControlsLogSection();

        controlsPane.getChildren().addAll(
                titleLabel,
                new Separator(),
                messageSection,
                new Separator(),
                statusSection,
                new Separator(),
                presenceSection, // NEU
                new Separator(),
                quickActionsSection,
                new Separator(),
                logSection
        );

        return controlsPane;
    }

    private VBox createMessageSection() {
        VBox messageSection = new VBox(10);

        Label sectionLabel = new Label("Send Message");
        sectionLabel.getStyleClass().add("section-label");

        // Channel ID input
        HBox channelBox = new HBox(10);
        channelBox.setAlignment(Pos.CENTER_LEFT);
        Label channelLabel = new Label("Channel ID:");
        channelLabel.setPrefWidth(100);
        channelLabel.getStyleClass().add("subsection-label");
        channelIdField = new TextField();
        channelIdField.setPromptText("Enter channel ID...");
        channelIdField.setPrefWidth(200);

        channelBox.getChildren().addAll(channelLabel, channelIdField);

        // Message input
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        Label messageLabel = new Label("Message:");
        messageLabel.setPrefWidth(100);
        messageLabel.getStyleClass().add("subsection-label");
        messageField = new TextField();
        messageField.setPromptText("Enter message to send...");
        messageField.setPrefWidth(300);
        messageField.setOnAction(e -> sendMessage());

        sendMessageButton = new Button("Send");
        sendMessageButton.setPrefWidth(80);
        sendMessageButton.getStyleClass().add("send-button");
        sendMessageButton.setDisable(true);
        sendMessageButton.setOnAction(e -> sendMessage());

        messageBox.getChildren().addAll(messageLabel, messageField, sendMessageButton);

        messageSection.getChildren().addAll(sectionLabel, channelBox, messageBox);
        return messageSection;
    }

    private VBox createStatusSection() {
        VBox statusSection = new VBox(10);

        Label sectionLabel = new Label("Bot Activity Status");
        sectionLabel.getStyleClass().add("section-label");

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label activityLabel = new Label("Activity:");
        activityLabel.setPrefWidth(100);
        activityLabel.getStyleClass().add("subsection-label");

        activityTypeBox = new ComboBox<>();
        activityTypeBox.getItems().addAll("Playing", "Listening", "Watching", "Competing", "Streaming");
        activityTypeBox.setValue("Playing");
        activityTypeBox.setPrefWidth(120);

        statusField = new TextField();
        statusField.setPromptText("Enter status text...");
        statusField.setPrefWidth(200);
        statusField.setOnAction(e -> setStatus());

        setStatusButton = new Button("Set Activity");
        setStatusButton.setPrefWidth(100);
        setStatusButton.getStyleClass().add("status-button");
        setStatusButton.setDisable(true);
        setStatusButton.setOnAction(e -> setStatus());

        statusBox.getChildren().addAll(activityLabel, activityTypeBox, statusField, setStatusButton);
        statusSection.getChildren().addAll(sectionLabel, statusBox);

        return statusSection;
    }

    // NEU: Presence/Anwesenheitsstatus Section
    private VBox createPresenceSection() {
        VBox presenceSection = new VBox(10);

        Label sectionLabel = new Label("Bot Presence Status");
        sectionLabel.getStyleClass().add("section-label");

        HBox presenceBox = new HBox(10);
        presenceBox.setAlignment(Pos.CENTER_LEFT);

        Label presenceLabel = new Label("Anwesenheit:");
        presenceLabel.setPrefWidth(100);
        presenceLabel.getStyleClass().add("subsection-label");

        onlineStatusBox = new ComboBox<>();
        onlineStatusBox.getItems().addAll(
                "Online",
                "Unsichtbar",
                "Abwesend",
                "Bitte nicht Stören"
        );
        onlineStatusBox.setValue("Online");
        onlineStatusBox.setPrefWidth(150);

        setOnlineStatusButton = new Button("Set Presence");
        setOnlineStatusButton.setPrefWidth(120);
        setOnlineStatusButton.getStyleClass().add("status-button");
        setOnlineStatusButton.setDisable(true);
        setOnlineStatusButton.setOnAction(e -> setOnlineStatus());

        presenceBox.getChildren().addAll(presenceLabel, onlineStatusBox, setOnlineStatusButton);
        presenceSection.getChildren().addAll(sectionLabel, presenceBox);

        return presenceSection;
    }

    private VBox createQuickActionsSection() {
        VBox quickSection = new VBox(10);

        Label sectionLabel = new Label("Quick Actions");
        sectionLabel.getStyleClass().add("section-label");

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        Button guildsButton = new Button("Show Guilds");
        guildsButton.getStyleClass().add("button-secondary");
        guildsButton.setOnAction(e -> showGuilds());

        Button channelsButton = new Button("Show Channels");
        channelsButton.getStyleClass().add("button-secondary");
        channelsButton.setOnAction(e -> showChannels());

        Button pingButton = new Button("Get Ping");
        pingButton.getStyleClass().add("button-secondary");
        pingButton.setOnAction(e -> showPing());

        Button clearLogButton = new Button("Clear Log");
        clearLogButton.getStyleClass().add("button-secondary");
        clearLogButton.setOnAction(e -> clearLogs());

        Button embedButton = new Button("Send Embed");
        embedButton.getStyleClass().add("button-secondary");
        embedButton.setOnAction(e -> openEmbedBuilder());

        buttonsBox.getChildren().addAll(guildsButton, channelsButton, pingButton, clearLogButton, embedButton);
        quickSection.getChildren().addAll(sectionLabel, buttonsBox);

        return quickSection;
    }

    private VBox createControlsLogSection() {
        VBox logSection = new VBox(10);

        Label sectionLabel = new Label("Bot Activity Log");
        sectionLabel.getStyleClass().add("section-label");

        controlsLogArea = new TextArea();
        controlsLogArea.setPrefHeight(150);
        controlsLogArea.setEditable(false);
        controlsLogArea.getStyleClass().add("log-area");
        controlsLogArea.setWrapText(true);

        // Controls for the log
        HBox logControlsBox = new HBox(10);
        logControlsBox.setAlignment(Pos.CENTER_LEFT);

        Button clearControlsLogButton = new Button("Clear");
        clearControlsLogButton.getStyleClass().add("button-secondary");
        clearControlsLogButton.setOnAction(e -> controlsLogArea.clear());

        Button scrollToBottomButton = new Button("Scroll to Bottom");
        scrollToBottomButton.getStyleClass().add("button-secondary");
        scrollToBottomButton.setOnAction(e -> {
            controlsLogArea.setScrollTop(Double.MAX_VALUE);
            controlsLogArea.positionCaret(controlsLogArea.getLength());
        });

        logControlsBox.getChildren().addAll(clearControlsLogButton, scrollToBottomButton);

        logSection.getChildren().addAll(sectionLabel, controlsLogArea, logControlsBox);

        return logSection;
    }

    private VBox createDirectMessagesPane() {
        VBox dmPane = new VBox(15);
        dmPane.setPadding(new Insets(20));

        // Initialize DM history
        dmHistory = FXCollections.observableArrayList();

        // Title
        Label titleLabel = new Label("Direct Messages");
        titleLabel.getStyleClass().add("title-label");

        // DM Sending Section (ERWEITERT)
        VBox dmSendSection = createDmSendSection();

        // DM History Section
        VBox dmHistorySection = createDmHistorySection();

        // DM Log Section
        VBox dmLogSection = createDmLogSection();

        dmPane.getChildren().addAll(
                titleLabel,
                new Separator(),
                dmSendSection,
                new Separator(),
                dmHistorySection,
                new Separator(),
                dmLogSection
        );

        return dmPane;
    }

    private VBox createDmSendSection() {
        VBox dmSendSection = new VBox(10);

        Label sectionLabel = new Label("Send Direct Message");
        sectionLabel.getStyleClass().add("section-label");

        // User ID input
        HBox userIdBox = new HBox(10);
        userIdBox.setAlignment(Pos.CENTER_LEFT);
        Label userIdLabel = new Label("User ID:");
        userIdLabel.setPrefWidth(100);
        userIdLabel.getStyleClass().add("subsection-label");
        dmUserIdField = new TextField();
        dmUserIdField.setPromptText("Enter Discord User ID...");
        dmUserIdField.setPrefWidth(200);

        userIdBox.getChildren().addAll(userIdLabel, dmUserIdField);

        // Message input
        HBox dmMessageBox = new HBox(10);
        dmMessageBox.setAlignment(Pos.CENTER_LEFT);
        Label dmMessageLabel = new Label("Message:");
        dmMessageLabel.setPrefWidth(100);
        dmMessageLabel.getStyleClass().add("subsection-label");
        dmMessageField = new TextField();
        dmMessageField.setPromptText("Enter DM message...");
        dmMessageField.setPrefWidth(300);
        dmMessageField.setOnAction(e -> sendDirectMessage());

        sendDmButton = new Button("Send DM");
        sendDmButton.setPrefWidth(100);
        sendDmButton.getStyleClass().add("send-button");
        sendDmButton.setDisable(true);
        sendDmButton.setOnAction(e -> sendDirectMessage());

        dmMessageBox.getChildren().addAll(dmMessageLabel, dmMessageField, sendDmButton);

        // NEU: DM Embed Button
        HBox dmEmbedBox = new HBox(10);
        dmEmbedBox.setAlignment(Pos.CENTER_LEFT);

        sendDmEmbedButton = new Button("Send DM Embed");
        sendDmEmbedButton.setPrefWidth(150);
        sendDmEmbedButton.getStyleClass().add("status-button");
        sendDmEmbedButton.setDisable(true);
        sendDmEmbedButton.setOnAction(e -> openDmEmbedBuilder());

        dmEmbedBox.getChildren().add(sendDmEmbedButton);

        // Quick DM Actions
        HBox quickDmActions = new HBox(10);
        quickDmActions.setAlignment(Pos.CENTER_LEFT);

        Button getDmChannelsButton = new Button("Show DM Channels");
        getDmChannelsButton.getStyleClass().add("button-secondary");
        getDmChannelsButton.setOnAction(e -> showDmChannels());

        Button getUserInfoButton = new Button("Get User Info");
        getUserInfoButton.getStyleClass().add("button-secondary");
        getUserInfoButton.setOnAction(e -> getUserInfo());

        quickDmActions.getChildren().addAll(getDmChannelsButton, getUserInfoButton);

        dmSendSection.getChildren().addAll(sectionLabel, userIdBox, dmMessageBox, dmEmbedBox, quickDmActions);
        return dmSendSection;
    }

    private VBox createDmHistorySection() {
        VBox dmHistorySection = new VBox(10);

        Label sectionLabel = new Label("DM History");
        sectionLabel.getStyleClass().add("section-label");

        // Create DM history table
        dmHistoryTable = new TableView<>(dmHistory);
        dmHistoryTable.setPrefHeight(200);

        TableColumn<MessageEntry, String> dmTimeColumn = new TableColumn<>("Time");
        dmTimeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        dmTimeColumn.setPrefWidth(100);

        TableColumn<MessageEntry, String> dmUserColumn = new TableColumn<>("User");
        dmUserColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        dmUserColumn.setPrefWidth(150);

        TableColumn<MessageEntry, String> dmDirectionColumn = new TableColumn<>("Direction");
        dmDirectionColumn.setCellValueFactory(new PropertyValueFactory<>("channel"));
        dmDirectionColumn.setPrefWidth(80);

        TableColumn<MessageEntry, String> dmMessageColumn = new TableColumn<>("Message");
        dmMessageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        dmMessageColumn.setPrefWidth(350);

        dmHistoryTable.getColumns().addAll(dmTimeColumn, dmUserColumn, dmDirectionColumn, dmMessageColumn);

        // DM History controls
        HBox dmHistoryControls = new HBox(10);
        dmHistoryControls.setAlignment(Pos.CENTER_LEFT);

        Button clearDmHistoryButton = new Button("Clear DM History");
        clearDmHistoryButton.getStyleClass().add("button-secondary");
        clearDmHistoryButton.setOnAction(e -> dmHistory.clear());

        Button exportDmHistoryButton = new Button("Export DM History");
        exportDmHistoryButton.getStyleClass().add("button-secondary");
        exportDmHistoryButton.setOnAction(e -> exportDmHistory());

        dmHistoryControls.getChildren().addAll(clearDmHistoryButton, exportDmHistoryButton);

        dmHistorySection.getChildren().addAll(sectionLabel, dmHistoryTable, dmHistoryControls);
        VBox.setVgrow(dmHistoryTable, Priority.ALWAYS);

        return dmHistorySection;
    }

    private VBox createDmLogSection() {
        VBox dmLogSection = new VBox(10);

        Label sectionLabel = new Label("DM Activity Log");
        sectionLabel.getStyleClass().add("section-label");

        dmLogArea = new TextArea();
        dmLogArea.setPrefHeight(150);
        dmLogArea.setEditable(false);
        dmLogArea.getStyleClass().add("log-area");
        dmLogArea.setWrapText(true);

        // DM Log controls
        HBox dmLogControls = new HBox(10);
        dmLogControls.setAlignment(Pos.CENTER_LEFT);

        Button clearDmLogButton = new Button("Clear DM Log");
        clearDmLogButton.getStyleClass().add("button-secondary");
        clearDmLogButton.setOnAction(e -> dmLogArea.clear());

        Button scrollDmLogButton = new Button("Scroll to Bottom");
        scrollDmLogButton.getStyleClass().add("button-secondary");
        scrollDmLogButton.setOnAction(e -> {
            dmLogArea.setScrollTop(Double.MAX_VALUE);
            dmLogArea.positionCaret(dmLogArea.getLength());
        });

        dmLogControls.getChildren().addAll(clearDmLogButton, scrollDmLogButton);

        dmLogSection.getChildren().addAll(sectionLabel, dmLogArea, dmLogControls);

        return dmLogSection;
    }

    private VBox createMessageHistoryPane() {
        VBox historyPane = new VBox(10);
        historyPane.setPadding(new Insets(20));

        Label titleLabel = new Label("Message History");
        titleLabel.getStyleClass().add("title-label");

        // Initialize message history
        messageHistory = FXCollections.observableArrayList();
        messageTable = new TableView<>(messageHistory);

        TableColumn<MessageEntry, String> timeColumn = new TableColumn<>("Time");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeColumn.setPrefWidth(120);

        TableColumn<MessageEntry, String> channelColumn = new TableColumn<>("Channel");
        channelColumn.setCellValueFactory(new PropertyValueFactory<>("channel"));
        channelColumn.setPrefWidth(120);

        TableColumn<MessageEntry, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorColumn.setPrefWidth(120);

        TableColumn<MessageEntry, String> messageColumn = new TableColumn<>("Message");
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));
        messageColumn.setPrefWidth(400);

        messageTable.getColumns().addAll(timeColumn, channelColumn, authorColumn, messageColumn);

        Button clearHistoryButton = new Button("Clear History");
        clearHistoryButton.getStyleClass().add("button-secondary");
        clearHistoryButton.setOnAction(e -> messageHistory.clear());

        historyPane.getChildren().addAll(titleLabel, messageTable, clearHistoryButton);
        VBox.setVgrow(messageTable, Priority.ALWAYS);

        return historyPane;
    }

    private VBox createStatisticsPane() {
        VBox statsPane = new VBox(15);
        statsPane.setPadding(new Insets(20));

        Label titleLabel = new Label("Bot Statistics");
        titleLabel.getStyleClass().add("title-label");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(15);
        statsGrid.getStyleClass().add("stats-grid");

        // Uptime
        Label uptimeKeyLabel = new Label("Uptime:");
        uptimeKeyLabel.getStyleClass().add("stats-label");
        statsGrid.add(uptimeKeyLabel, 0, 0);
        uptimeLabel = new Label("00:00:00");
        uptimeLabel.getStyleClass().add("stats-value");
        statsGrid.add(uptimeLabel, 1, 0);

        // Guild count
        Label guildKeyLabel = new Label("Guilds:");
        guildKeyLabel.getStyleClass().add("stats-label");
        statsGrid.add(guildKeyLabel, 0, 1);
        guildCountLabel = new Label("0");
        guildCountLabel.getStyleClass().add("stats-value");
        statsGrid.add(guildCountLabel, 1, 1);

        // User count
        Label userKeyLabel = new Label("Total Users:");
        userKeyLabel.getStyleClass().add("stats-label");
        statsGrid.add(userKeyLabel, 0, 2);
        userCountLabel = new Label("0");
        userCountLabel.getStyleClass().add("stats-value");
        statsGrid.add(userCountLabel, 1, 2);

        // Messages sent
        Label sentKeyLabel = new Label("Messages Sent:");
        sentKeyLabel.getStyleClass().add("stats-label");
        statsGrid.add(sentKeyLabel, 0, 3);
        Label messagesSentLabel = new Label("0");
        messagesSentLabel.getStyleClass().add("stats-value");
        statsGrid.add(messagesSentLabel, 1, 3);

        // Messages received
        Label receivedKeyLabel = new Label("Messages Received:");
        receivedKeyLabel.getStyleClass().add("stats-label");
        statsGrid.add(receivedKeyLabel, 0, 4);
        Label messagesReceivedLabel = new Label("0");
        messagesReceivedLabel.getStyleClass().add("stats-value");
        statsGrid.add(messagesReceivedLabel, 1, 4);

        statsPane.getChildren().addAll(titleLabel, new Separator(), statsGrid);

        return statsPane;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.getStyleClass().add("status-bar");

        Label statusLabel = new Label("Discord Bot Controller - Ready");
        statusLabel.getStyleClass().add("subsection-label");
        statusBar.getChildren().add(statusLabel);

        return statusBar;
    }

    private void connectBot() {
        String token = tokenField.getText();
        if (token.isEmpty()) {
            showAlert("Error", "Please enter a bot token!", Alert.AlertType.ERROR);
            return;
        }

        Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    connectButton.setDisable(true);
                    connectionStatus.setText("Status: Connecting...");
                    connectionStatus.getStyleClass().removeAll("status-connected", "status-disconnected");
                    connectionStatus.getStyleClass().add("status-connecting");
                    log("Connecting to Discord...");
                });

                jda = JDABuilder.createDefault(token)
                        .setActivity(Activity.playing("JavaFX Controller"))
                        .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                        .addEventListeners(new BotEventListenerFX(DiscordBotFX.this))
                        .build();

                jda.awaitReady();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    onBotConnected();
                    log("Bot connected successfully!");
                    startTime = System.currentTimeMillis();
                    updateStats();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    log("Connection failed: " + getException().getMessage());
                    showAlert("Connection Error", "Failed to connect: " + getException().getMessage(), Alert.AlertType.ERROR);
                });
            }
        };

        new Thread(connectTask).start();
    }

    private void disconnectBot() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
            onBotDisconnected();
            log("Bot disconnected.");
        }
    }

    private void onBotConnected() {
        connectionStatus.setText("Status: Connected");
        connectionStatus.getStyleClass().removeAll("status-disconnected", "status-connecting");
        connectionStatus.getStyleClass().add("status-connected");
        connectButton.setDisable(true);
        disconnectButton.setDisable(false);
        sendMessageButton.setDisable(false);
        setStatusButton.setDisable(false);
        setOnlineStatusButton.setDisable(false); // NEU
        sendDmButton.setDisable(false);
        sendDmEmbedButton.setDisable(false); // NEU
    }

    private void onBotDisconnected() {
        connectionStatus.setText("Status: Disconnected");
        connectionStatus.getStyleClass().removeAll("status-connected", "status-connecting");
        connectionStatus.getStyleClass().add("status-disconnected");
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        sendMessageButton.setDisable(true);
        setStatusButton.setDisable(true);
        setOnlineStatusButton.setDisable(true); // NEU
        sendDmButton.setDisable(true);
        sendDmEmbedButton.setDisable(true); // NEU
    }

    private void sendMessage() {
        if (jda == null) return;

        String channelId = channelIdField.getText().trim();
        String message = messageField.getText().trim();

        if (channelId.isEmpty() || message.isEmpty()) {
            showAlert("Error", "Please enter channel ID and message!", Alert.AlertType.ERROR);
            return;
        }

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue(
                        success -> Platform.runLater(() -> {
                            log("Message sent to #" + channel.getName() + ": " + message);
                            messageField.clear();
                            messagesSent++;
                        }),
                        error -> Platform.runLater(() -> log("Failed to send message: " + error.getMessage()))
                );
            } else {
                log("Channel not found: " + channelId);
            }
        } catch (Exception e) {
            log("Error sending message: " + e.getMessage());
        }
    }

    private void sendDirectMessage() {
        if (jda == null) return;

        String userId = dmUserIdField.getText().trim();
        String message = dmMessageField.getText().trim();

        if (userId.isEmpty() || message.isEmpty()) {
            showAlert("Error", "Please enter User ID and message!", Alert.AlertType.ERROR);
            return;
        }

        try {
            jda.retrieveUserById(userId).queue(user -> {
                user.openPrivateChannel().queue(privateChannel -> {
                    privateChannel.sendMessage(message).queue(
                            success -> Platform.runLater(() -> {
                                logDm("DM sent to " + user.getName() + "#" + user.getDiscriminator() + ": " + message);
                                addDmToHistory(user.getName(), "Sent", message);
                                dmMessageField.clear();
                                messagesSent++;
                            }),
                            error -> Platform.runLater(() -> logDm("Failed to send DM to " + user.getName() + ": " + error.getMessage()))
                    );
                }, error -> Platform.runLater(() -> logDm("Failed to open DM channel with user " + userId + ": " + error.getMessage())));
            }, error -> Platform.runLater(() -> logDm("Failed to retrieve user " + userId + ": " + error.getMessage())));
        } catch (Exception e) {
            logDm("Error sending DM: " + e.getMessage());
        }
    }

    private void setStatus() {
        if (jda == null) return;

        String status = statusField.getText().trim();
        if (status.isEmpty()) return;

        String activityType = activityTypeBox.getValue();
        Activity activity;

        switch (activityType) {
            case "Playing":
                activity = Activity.playing(status);
                break;
            case "Listening":
                activity = Activity.listening(status);
                break;
            case "Watching":
                activity = Activity.watching(status);
                break;
            case "Competing":
                activity = Activity.competing(status);
                break;
            case "Streaming":
                activity = Activity.streaming(status, "https://twitch.tv/example");
                break;
            default:
                activity = Activity.playing(status);
        }

        jda.getPresence().setActivity(activity);
        log("Activity set to: " + activityType + " " + status);
    }

    // NEU: OnlineStatus setzen
    private void setOnlineStatus() {
        if (jda == null) return;

        String statusText = onlineStatusBox.getValue();
        OnlineStatus onlineStatus;

        switch (statusText) {
            case "Online":
                onlineStatus = OnlineStatus.ONLINE;
                break;
            case "Unsichtbar":
                onlineStatus = OnlineStatus.INVISIBLE;
                break;
            case "Abwesend":
                onlineStatus = OnlineStatus.IDLE;
                break;
            case "Bitte nicht Stören":
                onlineStatus = OnlineStatus.DO_NOT_DISTURB;
                break;
            default:
                onlineStatus = OnlineStatus.ONLINE;
        }

        jda.getPresence().setStatus(onlineStatus);
        log("Anwesenheitsstatus gesetzt auf: " + statusText);
    }

    private void showGuilds() {
        if (jda == null) return;

        StringBuilder guilds = new StringBuilder("Connected Guilds:\n");
        jda.getGuilds().forEach(guild ->
                guilds.append("- ").append(guild.getName()).append(" (").append(guild.getId()).append(")\n")
        );
        log(guilds.toString());
    }

    private void showChannels() {
        if (jda == null) return;

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Guild Channels");
        dialog.setHeaderText("Show Channels");
        dialog.setContentText("Enter Guild ID:");

        dialog.showAndWait().ifPresent(guildId -> {
            var guild = jda.getGuildById(guildId.trim());
            if (guild != null) {
                StringBuilder channels = new StringBuilder("Channels in " + guild.getName() + ":\n");
                guild.getTextChannels().forEach(channel ->
                        channels.append("- #").append(channel.getName()).append(" (").append(channel.getId()).append(")\n")
                );
                log(channels.toString());
            } else {
                log("Guild not found: " + guildId);
            }
        });
    }

    private void showPing() {
        if (jda == null) return;

        long ping = jda.getGatewayPing();
        log("Bot Ping: " + ping + "ms");
    }

    private void showDmChannels() {
        if (jda == null) return;

        StringBuilder dmChannels = new StringBuilder("Open DM Channels:\n");
        jda.getPrivateChannels().forEach(channel -> {
            var user = channel.getUser();
            if (user != null) {
                dmChannels.append("- ").append(user.getName())
                        .append("#").append(user.getDiscriminator())
                        .append(" (ID: ").append(user.getId()).append(")\n");
            }
        });

        if (dmChannels.toString().equals("Open DM Channels:\n")) {
            dmChannels.append("No open DM channels found.\n");
        }

        logDm(dmChannels.toString());
    }

    private void getUserInfo() {
        if (jda == null) return;

        String userId = dmUserIdField.getText().trim();
        if (userId.isEmpty()) {
            showAlert("Error", "Please enter a User ID!", Alert.AlertType.ERROR);
            return;
        }

        try {
            jda.retrieveUserById(userId).queue(user -> {
                StringBuilder userInfo = new StringBuilder("User Information:\n");
                userInfo.append("Name: ").append(user.getName()).append("#").append(user.getDiscriminator()).append("\n");
                userInfo.append("ID: ").append(user.getId()).append("\n");
                userInfo.append("Bot: ").append(user.isBot() ? "Yes" : "No").append("\n");
                userInfo.append("Avatar URL: ").append(user.getAvatarUrl()).append("\n");
                userInfo.append("Account Created: ").append(user.getTimeCreated().toLocalDate()).append("\n");

                Platform.runLater(() -> logDm(userInfo.toString()));
            }, error -> Platform.runLater(() -> logDm("Failed to get user info for ID " + userId + ": " + error.getMessage())));
        } catch (Exception e) {
            logDm("Error getting user info: " + e.getMessage());
        }
    }

    private void openEmbedBuilder() {
        createEmbedBuilderWindow(false);
    }

    // NEU: DM Embed Builder
    private void openDmEmbedBuilder() {
        String userId = dmUserIdField.getText().trim();
        if (userId.isEmpty()) {
            showAlert("Error", "Please enter a User ID first!", Alert.AlertType.ERROR);
            return;
        }
        createEmbedBuilderWindow(true);
    }

    private void exportDmHistory() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Save DM History");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        java.io.File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                writer.println("Time,User,Direction,Message");
                dmHistory.forEach(dm -> {
                    writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\"",
                            dm.getTime(), dm.getAuthor(), dm.getChannel(),
                            dm.getMessage().replace("\"", "\"\"")));
                });

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Complete");
                alert.setContentText("DM history exported successfully!");
                alert.showAndWait();
            } catch (java.io.IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setContentText("Failed to export DM history: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void updateStats() {
        if (jda != null) {
            Platform.runLater(() -> {
                guildCountLabel.setText(String.valueOf(jda.getGuilds().size()));
                long userCount = jda.getGuilds().stream()
                        .mapToLong(guild -> guild.getMemberCount())
                        .sum();
                userCountLabel.setText(String.valueOf(userCount));
            });
        }
    }

    private void startUptimeTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (startTime > 0) {
                long uptime = System.currentTimeMillis() - startTime;
                long seconds = uptime / 1000;
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                seconds = seconds % 60;

                uptimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void log(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String logEntry = "[" + timestamp + "] " + message + "\n";

            // Add to main log area
            logArea.appendText(logEntry);
            logArea.setScrollTop(Double.MAX_VALUE);

            // Add to controls log area if it exists
            if (controlsLogArea != null) {
                controlsLogArea.appendText(logEntry);
                controlsLogArea.setScrollTop(Double.MAX_VALUE);
            }
        });
    }

    public void logDm(String message) {
        Platform.runLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String logEntry = "[" + timestamp + "] " + message + "\n";

            // Add to DM log area
            if (dmLogArea != null) {
                dmLogArea.appendText(logEntry);
                dmLogArea.setScrollTop(Double.MAX_VALUE);
            }

            // Also add to main log
            log("DM: " + message);
        });
    }

    private void clearLogs() {
        logArea.clear();
        if (controlsLogArea != null) {
            controlsLogArea.clear();
        }
        if (dmLogArea != null) {
            dmLogArea.clear();
        }
    }

    public void addMessageToHistory(String channel, String author, String message) {
        Platform.runLater(() -> {
            if (messageHistory.size() >= 1000) {
                messageHistory.remove(0);
            }
            messageHistory.add(new MessageEntry(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    channel, author, message
            ));
            messagesReceived++;
        });
    }

    public void addDmToHistory(String user, String direction, String message) {
        Platform.runLater(() -> {
            if (dmHistory.size() >= 1000) {
                dmHistory.remove(0);
            }
            dmHistory.add(new MessageEntry(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    direction, user, message
            ));
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // NEU: Discord-Style Embed Builder Window
    private void createEmbedBuilderWindow(boolean isDm) {
        Stage embedStage = new Stage();
        embedStage.initOwner(primaryStage);
        embedStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        embedStage.setTitle(isDm ? "DM Embed Builder" : "Channel Embed Builder");
        embedStage.setWidth(500);
        embedStage.setHeight(600);
        embedStage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root");

        // Title
        Label titleLabel = new Label(isDm ? "Create DM Embed" : "Create Channel Embed");
        titleLabel.getStyleClass().add("title-label");

        // Form Grid
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));

        // Embed Title
        Label embedTitleLabel = new Label("Title:");
        embedTitleLabel.getStyleClass().add("subsection-label");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter embed title...");
        titleField.setPrefWidth(300);

        grid.add(embedTitleLabel, 0, 0);
        grid.add(titleField, 1, 0);

        // Description
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("subsection-label");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Enter embed description...");
        descArea.setPrefRowCount(4);
        descArea.setPrefWidth(300);
        descArea.setWrapText(true);

        grid.add(descLabel, 0, 1);
        grid.add(descArea, 1, 1);

        // Color Picker
        Label colorLabel = new Label("Color:");
        colorLabel.getStyleClass().add("subsection-label");
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setValue(isDm ? javafx.scene.paint.Color.web("#5865f2") : javafx.scene.paint.Color.web("#0099ff"));
        colorPicker.setPrefWidth(150);

        grid.add(colorLabel, 0, 2);
        grid.add(colorPicker, 1, 2);

        // Author (nur für DM)
        TextField authorField = null;
        if (isDm) {
            Label authorLabel = new Label("Author:");
            authorLabel.getStyleClass().add("subsection-label");
            authorField = new TextField();
            authorField.setPromptText("Author name (optional)");
            authorField.setPrefWidth(300);

            grid.add(authorLabel, 0, 3);
            grid.add(authorField, 1, 3);
        }

        // Footer
        Label footerLabel = new Label("Footer:");
        footerLabel.getStyleClass().add("subsection-label");
        TextField footerField = new TextField();
        footerField.setPromptText("Footer text (optional)");
        footerField.setPrefWidth(300);

        int footerRow = isDm ? 4 : 3;
        grid.add(footerLabel, 0, footerRow);
        grid.add(footerField, 1, footerRow);

        // Image URL (nur für DM)
        TextField imageField = null;
        if (isDm) {
            Label imageLabel = new Label("Image URL:");
            imageLabel.getStyleClass().add("subsection-label");
            imageField = new TextField();
            imageField.setPromptText("Image URL (optional)");
            imageField.setPrefWidth(300);

            grid.add(imageLabel, 0, 5);
            grid.add(imageField, 1, 5);
        }

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button sendButton = new Button("Send Embed");
        sendButton.setPrefWidth(120);
        sendButton.getStyleClass().add("send-button");

        Button cancelButton = new Button("Cancel");
        cancelButton.setPrefWidth(120);
        cancelButton.getStyleClass().add("button-secondary");

        buttonBox.getChildren().addAll(sendButton, cancelButton);

        // Event Handlers
        final TextField finalAuthorField = authorField;
        final TextField finalImageField = imageField;

        sendButton.setOnAction(e -> {
            if (jda == null) {
                showAlert("Error", "Bot not connected!", Alert.AlertType.ERROR);
                return;
            }

            String title = titleField.getText().trim();
            String description = descArea.getText().trim();

            if (title.isEmpty() && description.isEmpty()) {
                showAlert("Error", "Please enter at least a title or description!", Alert.AlertType.ERROR);
                return;
            }

            // Convert JavaFX Color to AWT Color
            javafx.scene.paint.Color fxColor = colorPicker.getValue();
            java.awt.Color awtColor = new java.awt.Color(
                    (float) fxColor.getRed(),
                    (float) fxColor.getGreen(),
                    (float) fxColor.getBlue()
            );

            if (isDm) {
                sendDmEmbed(title, description, awtColor,
                        finalAuthorField != null ? finalAuthorField.getText().trim() : "",
                        footerField.getText().trim(),
                        finalImageField != null ? finalImageField.getText().trim() : "");
            } else {
                sendChannelEmbed(title, description, awtColor, footerField.getText().trim());
            }

            embedStage.close();
        });

        cancelButton.setOnAction(e -> embedStage.close());

        // Layout
        root.getChildren().addAll(titleLabel, new Separator(), grid, buttonBox);

        Scene scene = new Scene(root);

        // Apply Discord CSS
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ex) {
            System.out.println("CSS file not found for embed builder");
        }

        embedStage.setScene(scene);
        embedStage.showAndWait();
    }

    // Helper method for Channel Embeds
    private void sendChannelEmbed(String title, String description, java.awt.Color color, String footer) {
        String channelId = channelIdField.getText().trim();
        if (channelId.isEmpty()) {
            showAlert("Error", "Please enter a Channel ID first!", Alert.AlertType.ERROR);
            return;
        }

        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                var embedBuilder = new net.dv8tion.jda.api.EmbedBuilder()
                        .setColor(color);

                if (!title.isEmpty()) {
                    embedBuilder.setTitle(title);
                }
                if (!description.isEmpty()) {
                    embedBuilder.setDescription(description);
                }
                if (!footer.isEmpty()) {
                    embedBuilder.setFooter(footer);
                }

                var embed = embedBuilder.build();

                channel.sendMessageEmbeds(embed).queue(
                        success -> Platform.runLater(() -> {
                            log("Embed sent to #" + channel.getName() + ": " + title);
                            messagesSent++;
                        }),
                        error -> Platform.runLater(() -> log("Failed to send embed: " + error.getMessage()))
                );
            } else {
                log("Channel not found: " + channelId);
            }
        } catch (Exception e) {
            log("Error sending channel embed: " + e.getMessage());
        }
    }

    // Helper method for DM Embeds
    private void sendDmEmbed(String title, String description, java.awt.Color color, String author, String footer, String imageUrl) {
        String userId = dmUserIdField.getText().trim();
        if (userId.isEmpty()) {
            showAlert("Error", "Please enter a User ID first!", Alert.AlertType.ERROR);
            return;
        }

        try {
            jda.retrieveUserById(userId).queue(user -> {
                user.openPrivateChannel().queue(privateChannel -> {
                    var embedBuilder = new net.dv8tion.jda.api.EmbedBuilder()
                            .setColor(color);

                    if (!title.isEmpty()) {
                        embedBuilder.setTitle(title);
                    }
                    if (!description.isEmpty()) {
                        embedBuilder.setDescription(description);
                    }
                    if (!author.isEmpty()) {
                        embedBuilder.setAuthor(author);
                    }
                    if (!footer.isEmpty()) {
                        embedBuilder.setFooter(footer);
                    }
                    if (!imageUrl.isEmpty()) {
                        embedBuilder.setImage(imageUrl);
                    }

                    var embed = embedBuilder.build();

                    privateChannel.sendMessageEmbeds(embed).queue(
                            success -> Platform.runLater(() -> {
                                logDm("DM Embed sent to " + user.getName() + "#" + user.getDiscriminator() + ": " + title);
                                addDmToHistory(user.getName(), "Sent", "[EMBED] " + title);
                                messagesSent++;
                            }),
                            error -> Platform.runLater(() -> logDm("Failed to send DM embed to " + user.getName() + ": " + error.getMessage()))
                    );
                }, error -> Platform.runLater(() -> logDm("Failed to open DM channel with user " + userId + ": " + error.getMessage())));
            }, error -> Platform.runLater(() -> logDm("Failed to retrieve user " + userId + ": " + error.getMessage())));
        } catch (Exception e) {
            logDm("Error sending DM embed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}