package net.trustgames.toolkit.managers.rabbit;

import com.rabbitmq.client.*;
import lombok.Getter;
import net.trustgames.toolkit.managers.rabbit.extras.exchanges.RabbitExchanges;
import net.trustgames.toolkit.managers.rabbit.extras.queues.PlayerDataUpdateQueues;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public final class RabbitManager {

    private ConnectionFactory factory;
    private final Connection connection;

    @Getter
    private final Channel channel;

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
            declareExchanges();
            declarePlayerDataUpdateQueues();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to declare Channels or Exchanges in RabbitMQ", e);
        }
    }

    /**
     * @see RabbitExchanges
     */
    private void declareExchanges() {
        for (RabbitExchanges exchange : RabbitExchanges.values()) {
            try {
                channel.exchangeDeclare(exchange.name, exchange.type, true);
            } catch (IOException e) {
                throw new RuntimeException("Failed to declare Exchanges in RabbitMQ", e);
            }
        }
    }

    /**
     * @see PlayerDataUpdateQueues
     */
    private void declarePlayerDataUpdateQueues() {
        try {
            for (PlayerDataUpdateQueues queue : PlayerDataUpdateQueues.values()) {
                channel.queueDeclare(queue.name, false, false, false, null);
                for (RabbitExchanges exchange : queue.exchanges) {
                    if (exchange == null) return;
                    channel.queueBind(queue.name, exchange.name, queue.routingKey);
                }
            }
        } catch (IOException  e) {
            throw new RuntimeException("Failed to declare Channels or Exchanges for PlayerDataUpdate in RabbitMQ", e);
        }
    }

    /**
     * Send a json message to the specified exchange or queue in fire-and-forget mode
     * (is run sync)
     *
     * @param queue The queue to send the message to. Contains data about the exchanges, names, types
     * @param properties Properties of the message (type, ttl, ...)
     * @param json      JSON to send as body of the message

     * @see RabbitManager#fireAndForgetAsync(PlayerDataUpdateQueues, AMQP.BasicProperties, JSONObject)
     */
    public void fireAndForget(@NotNull PlayerDataUpdateQueues queue,
                              @Nullable AMQP.BasicProperties properties,
                              @NotNull JSONObject json) {

        if (channel == null) {
            return;
        }

        try {
            for (RabbitExchanges exchange : queue.exchanges) {
                if (exchange == null) return;
                channel.basicPublish(exchange.name, queue.routingKey, properties, json.toString().getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a json message to the specified queue in fire-and-forget mode
     * (is run async)
     *
     * @param queue The queue to send the message to. Contains data about the exchanges, names, types
     * @param properties Properties of the message (type, ttl, ...)
     * @param json      JSON to send as body of the message
     * @see RabbitManager#fireAndForget(PlayerDataUpdateQueues, AMQP.BasicProperties, JSONObject)
     */
    public void fireAndForgetAsync(@NotNull PlayerDataUpdateQueues queue,
                                   @NotNull AMQP.BasicProperties properties,
                                   @NotNull JSONObject json) {
        CompletableFuture.runAsync(() -> fireAndForget(queue, properties, json));
    }

    /**
     * Handle the delivery of the message in the queue and everytime a
     * message is received, a callback is run.
     *
     * @param queueName Name of the queue to consume messages from
     * @param callback  Callback to be run everytime a message is received
     */

    public void onDelivery(@NotNull String queueName,
                           Consumer<JSONObject> callback) {
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
                    throw new RuntimeException("RabbitMQ channel initialization timed out!", throwable);
                });
    }

    /**
     * @return
     * true - if channel is initialized<p>
     * false - if channel is not initialized
     */
    public boolean isChannelInitialized() {
        return channel != null;
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
