package net.trustgames.toolkit.message_queue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Getter;
import net.trustgames.toolkit.Toolkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RabbitManager {

    private ConnectionFactory factory;
    private final Connection connection;
    @Getter
    private final Channel channel;
    private final Logger LOGGER = Toolkit.LOGGER;

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
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection or channel", e);
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
                            LOGGER.log(Level.SEVERE, "Exception occurred while sleeping the RabbitMQ channel initialization thread", e);
                        }
                    }
                }).orTimeout(10L, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "RabbitMQ channel initialization timed out!", throwable);
                    return null;
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
     * and set {@link ConnectionFactory} to null
     */
    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (TimeoutException | IOException e) {
                LOGGER.log(Level.SEVERE, "Exception occurred while trying to close RabbitMQ channel", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Exception occurred while trying to close RabbitMQ channel", e);
            }
        }
        factory = null;
    }
}
