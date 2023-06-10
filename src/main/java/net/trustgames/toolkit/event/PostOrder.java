package net.trustgames.toolkit.event;

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
 * The "post order" is a representation of the order, relative to other posts,
 * that a given {@link EventSubscriber} should be posted events.
 *
 * <p>Post orders are represented by {@link Integer}s, and subscribers are
 * ordered using the natural ordering of Java integers.</p>
 *
 * <p>Some "standard" post orders are expressed as constants on this class.</p>
 *
 */
public interface PostOrder {
    /**
     * Marks that the subscriber should be called first, before all other subscribers.
     *
     */
    int FIRST = -100;
    /**
     * Marks that the subscriber should be called before {@link #NORMAL normal} subscribers.
     *
     */
    int EARLY = -50;
    /**
     * Marks that the subscriber should be called with no special priority.
     *
     */
    int NORMAL = 0;
    /**
     * Marks that the subscriber should be called after {@link #NORMAL normal} subscribers.
     *
     */
    int LATE = 50;
    /**
     * Marks that the subscriber should be called last, after all other subscribers.
     *
     */
    int LAST = 100;
}
