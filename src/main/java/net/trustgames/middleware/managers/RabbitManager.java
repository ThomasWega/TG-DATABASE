package net.trustgames.middleware.managers;

import com.rabbitmq.client.*;
import net.trustgames.middleware.Middleware;
import net.trustgames.middleware.config.rabbit.RabbitQueues;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
        this.factory = new ConnectionFactory();
        this.factory.setUsername(user);
        this.factory.setPassword(password);
        this.factory.setHost(ip);
        this.factory.setPort(port);
        try {
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            for (RabbitQueues queue : RabbitQueues.values()) {
                this.channel.queueDeclare(queue.name, false, false, false, null);
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a json message to the specified queue in fire-and-forget mode
     * (is run sync)
     *
     * @param queueName Name of the queue to publish the json to
     * @param json      JSON to send as body of the message
     * @param ttl       Time to live in milliseconds
     * @see RabbitManager#fireAndForgetAsync(String, JSONObject, long)
     */
    public void fireAndForget(@NotNull String queueName,
                              @NotNull JSONObject json,
                              long ttl) {

        if (channel == null) {
            return;
        }

        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .expiration(String.valueOf(ttl))
                .build();

        try {
            channel.basicPublish("", queueName, false, properties, json.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a json message to the specified queue in fire-and-forget mode
     * (is run async)
     *
     * @param queueName Name of the queue to publish the json to
     * @param json      JSON to send as body of the message
     * @param ttl       Time to live in milliseconds
     * @see RabbitManager#fireAndForget(String, JSONObject, long)
     */
    public void fireAndForgetAsync(@NotNull String queueName,
                                   @NotNull JSONObject json,
                                   long ttl) {
        CompletableFuture.runAsync(() -> fireAndForget(queueName, json, ttl));
    }

    /**
     * Handle the delivery of the message in the queue and everytime a
     * message is received, a callback is run.
     *
     * @param queueName Name of the queue to consume messages from
     * @param callback  Callback to be run everytime a message is received
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
                            Thread.sleep(500L);
                            onChannelInitialized(callback);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).orTimeout(10L, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    Middleware.getLogger().severe("RabbitMQ channel initialization timed out!");
                    return null;
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
