package net.trustgames.toolkit.event;

import net.trustgames.toolkit.message_queue.event.RabbitEvent;
import net.trustgames.toolkit.message_queue.event.RabbitEventBus;
import net.trustgames.toolkit.message_queue.event.RabbitEventManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/*
 * This file is part of event, licensed under the MIT License.
 *
 * Copyright (c) 2017-2021 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * An event bus.
 *
 * @param <E> the event type
 */
public interface EventBus<E extends Event> {

    /**
     * Creates an event bus for RabbitMQ.
     *
     * @param type the event type
     * @param <E> the event type
     * @return an event bus
     */
    static <E extends RabbitEvent> @NotNull RabbitEventBus<E> rabbitEventBus(final @NotNull RabbitEventManager eventManager, final @NotNull Class<E> type) {
        return rabbitEventBus(eventManager, type, Accepts.nonCancelledWhenNotAcceptingCancelled());
    }

    /**
     * Creates an event bus for RabbitMQ.
     *
     * @param type the event type
     * @param <E> the event type
     * @return an event bus
     */
    static <E extends RabbitEvent> @NotNull RabbitEventBus<E> rabbitEventBus(final @NotNull RabbitEventManager eventManager, final @NotNull Class<E> type, final @NotNull Accepts<E> accepts) {
        return new RabbitEventBus<>(eventManager, type, accepts);
    }

    /**
     * Gets the type of events accepted by this event bus.
     *
     * <p>This is represented by the <code>E</code> type parameter.</p>
     *
     * @return the event type
     */
    @NotNull Class<E> type();

    /**
     * Posts an event to all registered subscribers.
     *
     * @param event the event
     * @return the post result of the operation
     */
    @NotNull PostResult post(final @NotNull E event);

    /**
     * Determines whether the specified event has been subscribed to.
     *
     * @param type the event type
     * @return {@code true} if the event has subscribers, {@code false} otherwise
     */
    boolean subscribed(final @NotNull Class<? extends E> type);

    /**
     * Registers the given {@code subscriber} to receive events.
     *
     * @param event the event type
     * @param subscriber the subscriber
     * @param <T> the event type
     */
    <T extends E> void subscribe(final @NotNull Class<T> event, final @NotNull EventSubscriber<? super T> subscriber);

    /**
     * Unregisters the given {@code subscriber} to no longer receive events.
     *
     * @param subscriber the subscriber
     * @param <T> the event type
     */
    <T extends E> void unsubscribe(final @NotNull EventSubscriber<? super T> subscriber);

    /**
     * Unregisters all subscribers matching the {@code predicate}.
     *
     * @param predicate the predicate to test subscribers for removal
     */
    void unsubscribeIf(final @NotNull Predicate<EventSubscriber<? super E>> predicate);

    /**
     * An acceptor.
     *
     * @param <E> the event type
     */
    interface Accepts<E> {
        /**
         * The default acceptor.
         *
         * @param <E> the event type
         * @return the default acceptor
         */
        static <E> @NotNull Accepts<E> nonCancelledWhenNotAcceptingCancelled() {
            return (type, event, subscriber) -> {
                if(!subscriber.acceptsCancelled()) {
                    return !(event instanceof Cancellable) || !((Cancellable) event).isCancelled();
                }
                return true;
            };
        }

        /**
         * Tests if a subscriber accepts an event.
         *
         * @param type the event type
         * @param event the event
         * @param subscriber the event subscriber
         * @return {@code true} if {@code subscriber} accepts the {@code event}
         */
        boolean accepts(final Class<E> type, final @NotNull E event, final @NotNull EventSubscriber<? super E> subscriber);
    }
}
