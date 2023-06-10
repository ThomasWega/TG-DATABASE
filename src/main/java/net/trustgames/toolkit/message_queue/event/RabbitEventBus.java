package net.trustgames.toolkit.message_queue.event;

import net.trustgames.toolkit.event.EventBus;
import net.trustgames.toolkit.event.EventSubscriber;
import net.trustgames.toolkit.event.PostResult;
import net.trustgames.toolkit.message_queue.event.config.RabbitEventConfig;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
 * Similar to normal {@link EventBus} however, when an event is sent with {@link RabbitEventBus},
 * rather than looping through the subscribers, the event data is converted to JSON and then published
 * as a message to RabbitMQ. From there the event can be received on multiple servers
 * (instances of toolkit and event buses)
 *
 * @param <E> Event type
 */
public final class RabbitEventBus<E extends RabbitEvent> {
    private static final Comparator<EventSubscriber<?>> COMPARATOR = Comparator.comparingInt(EventSubscriber::postOrder);
    private final Map<Class<? extends E>, Collection<? extends Class<?>>> classes = new HashMap<>();
    private final Map<Class<? extends E>, List<EventSubscriber<? super E>>> unbaked = new HashMap<>();
    private final Map<Class<? extends E>, List<EventSubscriber<? super E>>> baked = new HashMap<>();
    private final Object lock = new Object();
    private final Class<E> type;
    private final EventBus.Accepts<E> accepts;
    private final RabbitEventManager rabbitEventManager;

    public RabbitEventBus(final RabbitEventManager rabbitEventManager, final Class<E> type, final EventBus.Accepts<E> accepts) {
        this.type = type;
        this.accepts = accepts;
        this.rabbitEventManager = rabbitEventManager;
    }

    
    public @NotNull Class<E> type() {
        return this.type;
    }

    
    @SuppressWarnings("unchecked")
    @NotNull PostResult post(@NotNull final E event) {
        Map<EventSubscriber<?>, Throwable> exceptions = null; // save on an allocation
        final List<EventSubscriber<? super E>> subscribers = this.subscribers((Class<? extends E>) event.getClass());
        for(final EventSubscriber<? super E> subscriber : subscribers) {
            if(this.accepts(event, subscriber)) {
                try {
                    subscriber.onEvent(event);
                } catch(final Throwable t) {
                    if(exceptions == null) {
                        exceptions = new HashMap<>();
                    }
                    exceptions.put(subscriber, t);
                }
            }
        }
        if(exceptions == null) {
            return PostResult.success();
        } else {
            return PostResult.failure(exceptions);
        }
    }

    private boolean accepts(final E event, final EventSubscriber<? super E> subscriber) {
        return this.accepts.accepts(this.type, event, subscriber);
    }

    
    public boolean subscribed(final @NotNull Class<? extends E> type) {
        return !this.subscribers(type).isEmpty();
    }

    @SuppressWarnings({"unchecked"})
    public <T extends E> void subscribe(final @NotNull Class<T> event, RabbitEventConfig<? super T> config, final @NotNull EventSubscriber<? super T> subscriber) {
        synchronized(this.lock) {
            final List<EventSubscriber<? super T>> subscribers = yayGenerics(this.unbaked.computeIfAbsent(event, key -> new ArrayList<>()));
            subscribers.add(subscriber);
            rabbitEventManager.listen(this, config);
            this.baked.clear();
        }
    }

    public void unsubscribeIf(final @NotNull Predicate<EventSubscriber<? super E>> predicate) {
        synchronized(this.lock) {
            boolean dirty = false;
            for(final List<EventSubscriber<? super E>> subscribers : this.unbaked.values()) {
                dirty |= subscribers.removeIf(predicate);
            }
            if(dirty) {
                this.baked.clear();
            }
        }
    }

    private List<EventSubscriber<? super E>> subscribers(final @NotNull Class<? extends E> event) {
        synchronized(this.lock) {
            return this.baked.computeIfAbsent(event, this::subscribers0);
        }
    }

    private List<EventSubscriber<? super E>> subscribers0(final @NotNull Class<? extends E> event) {
        final List<EventSubscriber<? super E>> subscribers = new ArrayList<>();
        final Collection<? extends Class<?>> types = this.classes.computeIfAbsent(event, this::findClasses);
        for(final Class<?> type : types) {
            subscribers.addAll(this.unbaked.getOrDefault(type, Collections.emptyList()));
        }
        subscribers.sort(COMPARATOR);
        return subscribers;
    }

    private Collection<? extends Class<?>> findClasses(final Class<?> type) {
        final Collection<? extends Class<?>> classes = Internals.ancestors(type);
        classes.removeIf(klass -> !this.type.isAssignableFrom(klass));
        return classes;
    }

    @SuppressWarnings("unchecked")
    private static <T extends U, U> List<U> yayGenerics(final List<T> list) {
        return (List<U>) list;
    }
}