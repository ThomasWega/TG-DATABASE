package net.trustgames.database;

import com.rabbitmq.client.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

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
    public void send(JSONObject json) {
        CompletableFuture.runAsync(() -> {
            try {
                channel.basicPublish("", queueName, null, json.toString().getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handle the delivery of the message in the queue and everytime a
     * message is recieved, a callback is run.
     *
     * @param callback Callback to be run everytime a message is received
     * @throws IOException if an error occurred
     */
    public void onDelivery(Consumer<JSONObject> callback) throws IOException {
        channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, com.rabbitmq.client.AMQP.BasicProperties properties, byte[] body) {
                String fullMessage = new String(body, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(fullMessage);
                callback.accept(json);
            }
        });
    }

    /**
     * Close all connections, channels
     * and set factory to null
     */
    public void close() throws IOException {
        if (channel != null) {
            try {
                channel.close();
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        if (connection != null) {
            connection.close();
        }
        factory = null;
    }

}
