package net.trustgames.middleware.managers;

import com.rabbitmq.client.*;
import net.trustgames.middleware.config.RabbitQueues;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class RabbitManager {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    /**
     * Sets parameters and creates new channel and queue.
     * (is run async)
     */
    public RabbitManager(@NotNull String user,
                         @NotNull String password,
                         @NotNull String ip,
                         @NotNull Integer port) {
        CompletableFuture.runAsync(() -> {
            this.factory = new ConnectionFactory();
            factory.setUsername(user);
            factory.setPassword(password);
            factory.setHost(ip);
            factory.setPort(port);
            try {
                this.connection = factory.newConnection();
                this.channel = connection.createChannel();
                for (RabbitQueues queue : RabbitQueues.values()){
                    this.channel.queueDeclare(queue.name, false, false, false, null);
                }
            } catch (IOException | TimeoutException e){
                e.printStackTrace();
            }
        });
    }

    /**
     * Send a json message to the specified queue
     * (is run async)
     *
     * @param queueName Name of the queue to publish the json to
     * @param json JSON to send as body of the message
     */
    public void send(String queueName, JSONObject json) {
        if (channel == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                channel.basicPublish("", queueName, null, json.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handle the delivery of the message in the queue and everytime a
     * message is received, a callback is run.
     *
     * @param queueName Name of the queue to consume messages from
     * @param callback Callback to be run everytime a message is received
     */
    public void onDelivery(@NotNull String queueName, Consumer<JSONObject> callback) {
        try {
            channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                    String fullMessage = new String(body, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(fullMessage);
                    callback.accept(json);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param callback When channel is initialized
     */
    public void onChannelInitialized(Runnable callback) {
        CompletableFuture.runAsync(() -> {
            if (channel != null) {
                callback.run();
            } else {
                // if channel is null, schedule the callback to be run when it is initialized
                try {
                    Thread.sleep(10000L);
                    onChannelInitialized(callback);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Close all connections, channels
     * and set factory to null
     */
    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (TimeoutException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        factory = null;
    }
}
