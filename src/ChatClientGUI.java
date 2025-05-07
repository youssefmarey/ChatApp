// ChatClientGUI.java - مع إصلاح الاتصال وتحويل الواجهة إلى إنجليزية

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatClientGUI extends Application {
    private VBox messagesBox;
    private TextField messageField;
    private ListView<String> userList;
    private Label onlineCountLabel;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;
    private String currentTarget = "Public";
    private Map<String, List<String>> messageHistory = new HashMap<>();
    private Scene scene;
    private boolean isDarkMode = false;

    @Override
    public void start(Stage stage) {
        username = askUsername();
        if (username == null) {
            Platform.exit();
            return;
        }

        userList = new ListView<>();
        userList.setPrefWidth(200);
        userList.setCellFactory(param -> new ListCell<>() {
            private final ImageView imageView = new ImageView(new Image("file:resources/avatar.png", 30, 30, false, true));
            private final Label label = new Label();
            private final HBox hbox = new HBox(10);

            {
                Circle clip = new Circle(15, 15, 15);
                imageView.setClip(clip);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(2));
                hbox.getChildren().addAll(imageView, label);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.setText(item);
                    setGraphic(hbox);
                }
            }
        });
        userList.getItems().add("Public");
        userList.getSelectionModel().select("Public");
        userList.setOnMouseClicked(event -> {
            String selected = userList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentTarget = selected;
                refreshChat();
            }
        });

        onlineCountLabel = new Label("Online: 1");
        onlineCountLabel.setFont(Font.font("Arial", 14));
        VBox leftPane = new VBox(10, onlineCountLabel, userList);
        leftPane.setPadding(new Insets(10));
        leftPane.setPrefWidth(200);
        leftPane.setStyle("-fx-background-color: #075E54; -fx-text-fill: white;");

        messagesBox = new VBox(5);
        messagesBox.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #ECE5DD;");

        messageField = new TextField();
        messageField.setPromptText("Type your message here...");
        messageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendMessage();
        });
        messageField.setStyle("-fx-background-color: white; -fx-font-size: 14;");

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        sendButton.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-weight: bold;");

        Button darkModeBtn = new Button("🌙");
        darkModeBtn.setOnAction(e -> toggleTheme());

        Button quitBtn = new Button("Quit");
        quitBtn.setOnAction(e -> {
            if (out != null) out.println("/quit");
            Platform.exit();
        });

        HBox inputBox = new HBox(10, messageField, sendButton, darkModeBtn, quitBtn);
        inputBox.setPadding(new Insets(10));
        inputBox.setAlignment(Pos.CENTER);

        Label footer = new Label("© 2025 Nexus Network\nTeam Leader: Youssef Marey");
        footer.setFont(Font.font(10));
        footer.setTextFill(Color.GRAY);
        footer.setPadding(new Insets(5));

        VBox rightPane = new VBox(scrollPane, inputBox, footer);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(leftPane);
        root.setCenter(rightPane);

        scene = new Scene(root, 800, 500);
        stage.setScene(scene);
        stage.setTitle("WhatsApp - " + username);
        stage.show();

        Platform.runLater(this::connectToServer);
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            scene.getRoot().setStyle("-fx-background-color: #2e2e2e;");
            messagesBox.setStyle("-fx-background-color: #2e2e2e;");
            messageField.setStyle("-fx-control-inner-background: #3c3c3c; -fx-text-fill: white;");
            userList.setStyle("-fx-control-inner-background: #252526; -fx-text-fill: white;");
            onlineCountLabel.setStyle("-fx-text-fill: white;");
        } else {
            scene.getRoot().setStyle("");
            messagesBox.setStyle("");
            messageField.setStyle("");
            userList.setStyle("");
            onlineCountLabel.setStyle("");
        }
    }

    private String askUsername() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText("Enter your username:");
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 1236);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        if (serverMsg.startsWith("ENTER_NAME")) {
                            out.println(username);
                        } else if (serverMsg.startsWith("NAME_EXISTS")) {
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Username already exists!", ButtonType.OK);
                                alert.showAndWait();
                                Platform.exit();
                            });
                        } else if (serverMsg.startsWith("USERS:")) {
                            updateUserList(serverMsg);
                        } else if (serverMsg.startsWith("ONLINE_COUNT:")) {
                            updateOnlineCount(serverMsg);
                        } else {
                            handleMessage(serverMsg);
                        }
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> addMessage("❌ Connection lost to the server", false, false));
                }
            }).start();
        } catch (IOException e) {
            addMessage("❌ Cannot connect to server", false, false);
        }
    }

    private void updateUserList(String msg) {
        String[] users = msg.substring(6).split(",");
        Platform.runLater(() -> {
            ObservableList<String> list = FXCollections.observableArrayList("Public");
            for (String user : users) {
                if (!user.equals(username) && !user.isBlank()) list.add(user);
            }
            userList.setItems(list);
            userList.getSelectionModel().select(currentTarget);
        });
    }

    private void updateOnlineCount(String msg) {
        String count = msg.substring("ONLINE_COUNT:".length());
        Platform.runLater(() -> onlineCountLabel.setText("Online: " + count));
    }

    private void handleMessage(String msg) {
        Platform.runLater(() -> {
            boolean isPrivate = msg.contains("[PM from") || msg.contains("[PM to");
            boolean isOwn = msg.contains("[PM to") || msg.startsWith(username);
            addMessage(msg, isPrivate, isOwn);
        });
    }

    private void addMessage(String msg, boolean isPrivate, boolean isOwn) {
        Label label = new Label(msg);
        label.setWrapText(true);
        label.setPadding(new Insets(8));
        label.setMaxWidth(400);
        label.setFont(Font.font(14));

        if (isPrivate) {
            label.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10;");
        } else {
            label.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10;");
        }

        HBox wrapper = new HBox(label);
        wrapper.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messagesBox.getChildren().add(wrapper);
    }

    private void refreshChat() {
        messagesBox.getChildren().clear();
        List<String> messages = messageHistory.getOrDefault(currentTarget, new ArrayList<>());
        for (String m : messages) {
            boolean isPrivate = m.contains("[PM from") || m.contains("[PM to");
            boolean isOwn = m.contains("[PM to") || m.startsWith(username);
            addMessage(m, isPrivate, isOwn);
        }
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty() || out == null) return;

        if (currentTarget.equals("Public")) {
            out.println(msg);
        } else {
            out.println("/pm " + currentTarget + " " + msg);
        }

        messageField.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
