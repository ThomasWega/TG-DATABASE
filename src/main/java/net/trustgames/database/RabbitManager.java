package net.trustgames.database;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

public class RabbitManager {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private final String queueName;
    @Getter
    private final boolean disabled;

    /**
     * Sets parameters and creates new channel and queue.
     * (is run async)
     */
    public RabbitManager(@NotNull String user,
                         @NotNull String password,
                         @NotNull String ip,
                         @NotNull Integer port,
                         @NotNull String queueName,
                         boolean disabled) {
        this.queueName = queueName;
        this.disabled = disabled;
        if (isDisabled()) return;
        CompletableFuture.runAsync(() -> {
            this.factory = new ConnectionFactory();
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
     * (is run async)
     *
     * @param json JSON to send as body of the message
     */
    public void send(@NotNull JSONObject json) {
        CompletableFuture.runAsync(() -> {
            try {
                channel.basicPublish("", queueName, null, json.toString().getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
