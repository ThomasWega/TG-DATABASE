package net.trustgames.database;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

public class RabbitManager {

    private Connection connection;
    private Channel channel;
    private final String queueName;

    public RabbitManager(@NotNull String user,
                         @NotNull String password,
                         @NotNull String ip,
                         @NotNull Integer port,
                         @NotNull String queueName) {
        this.queueName = queueName;
        CompletableFuture.runAsync(() -> {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(user);
            factory.setPassword(password);
            factory.setHost(ip);
            factory.setPort(port);
            try {
                this.connection = factory.newConnection();
                this.channel = connection.createChannel();
                this.channel.queueDeclare(queueName, true, false, false, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Send a json message to the specified queue
     *
     * @param json JSON to send as body of the message
     */
    public void send(JSONObject json) {
        try {
            channel.basicPublish("", queueName, null, json.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
