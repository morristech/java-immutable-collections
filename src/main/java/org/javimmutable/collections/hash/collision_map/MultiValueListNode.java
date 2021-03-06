///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2018, Burton Computer Corporation
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//     Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//
//     Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in
//     the documentation and/or other materials provided with the
//     distribution.
//
//     Neither the name of the Burton Computer Corporation nor the names
//     of its contributors may be used to endorse or promote products
//     derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.javimmutable.collections.hash.collision_map;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Func1;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.Sequence;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.common.EmptySequence;
import org.javimmutable.collections.common.MutableDelta;
import org.javimmutable.collections.cursors.SequenceCursor;
import org.javimmutable.collections.iterators.SequenceIterator;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class MultiValueListNode<K, V>
    implements ListNode<K, V>,
               Sequence<JImmutableMap.Entry<K, V>>
{
    private final MultiValueListNode<K, V> next;
    private final SingleValueListNode<K, V> entry;

    private MultiValueListNode(MultiValueListNode<K, V> next,
                               SingleValueListNode<K, V> entry)
    {
        this.next = next;
        this.entry = entry;
    }

    static <K, V> MultiValueListNode<K, V> of(K key,
                                              V value)
    {
        return new MultiValueListNode<>(null, SingleValueListNode.of(key, value));
    }

    static <K, V> MultiValueListNode<K, V> of(SingleValueListNode<K, V> entry1,
                                              SingleValueListNode<K, V> entry2)
    {
        return new MultiValueListNode<>(new MultiValueListNode<>(null, entry1), entry2);
    }

    static <K, V> MultiValueListNode<K, V> of(SingleValueListNode<K, V> entry1,
                                              SingleValueListNode<K, V> entry2,
                                              SingleValueListNode<K, V> entry3)
    {
        return new MultiValueListNode<>(new MultiValueListNode<>(new MultiValueListNode<>(null, entry1), entry2), entry3);
    }

    @Override
    public V getValueForKey(K key,
                            V defaultValue)
    {
        SingleValueListNode<K, V> answer = getEntryForKeyImpl(key);
        if (answer != null) {
            return answer.getValue();
        } else {
            return defaultValue;
        }
    }

    @Override
    public Holder<V> findValueForKey(K key)
    {
        SingleValueListNode<K, V> answer = getEntryForKeyImpl(key);
        if (answer != null) {
            return answer;
        } else {
            return Holders.of();
        }
    }

    @Override
    public JImmutableMap.Entry<K, V> getEntryForKey(K key)
    {
        return getEntryForKeyImpl(key);
    }

    @Override
    public MultiValueListNode<K, V> setValueForKey(K key,
                                                   V value,
                                                   MutableDelta sizeDelta)
    {
        SingleValueListNode<K, V> entry = getEntryForKeyImpl(key);
        if (entry == null) {
            sizeDelta.add(1);
            return new MultiValueListNode<>(this, SingleValueListNode.of(key, value));
        } else if (entry.getValue() == value) {
            return this;
        } else {
            return new MultiValueListNode<>(removeKeyFromList(key), SingleValueListNode.of(key, value));
        }
    }

    @Override
    public ListNode<K, V> setValueForKey(K key,
                                         Func1<Holder<V>, V> generator,
                                         MutableDelta sizeDelta)
    {
        SingleValueListNode<K, V> entry = getEntryForKeyImpl(key);
        if (entry == null) {
            sizeDelta.add(1);
            return new MultiValueListNode<>(this, SingleValueListNode.of(key, generator.apply(Holders.of())));
        } else {
            final V value = generator.apply(entry);
            if (entry.getValue() == value) {
                return this;
            } else {
                return new MultiValueListNode<>(removeKeyFromList(key), SingleValueListNode.of(key, value));
            }
        }
    }

    @Override
    public ListNode<K, V> deleteValueForKey(K key,
                                            MutableDelta sizeDelta)
    {
        if (getEntryForKey(key) == null) {
            return this;
        }
        MultiValueListNode<K, V> newList = removeKeyFromList(key);
        sizeDelta.subtract(1);
        return (newList != null && newList.next == null) ? newList.entry : newList;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public JImmutableMap.Entry<K, V> getHead()
    {
        return entry;
    }

    @Nonnull
    @Override
    public Sequence<JImmutableMap.Entry<K, V>> getTail()
    {
        if (next == null) {
            return EmptySequence.of();
        } else {
            return next;
        }
    }

    @Override
    @Nonnull
    public Cursor<JImmutableMap.Entry<K, V>> cursor()
    {
        return SequenceCursor.of(this);
    }

    @Nonnull
    @Override
    public SplitableIterator<JImmutableMap.Entry<K, V>> iterator()
    {
        return SequenceIterator.iterator(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MultiValueListNode that = (MultiValueListNode)o;

        if (entry != null ? !entry.equals(that.entry) : that.entry != null) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (next != null ? !next.equals(that.next) : that.next != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = next != null ? next.hashCode() : 0;
        result = 31 * result + (entry != null ? entry.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "MultiValueLeafNode{" +
               "next=" + next +
               ", entry=" + entry +
               '}';
    }

    private MultiValueListNode<K, V> removeKeyFromList(K key)
    {
        MultiValueListNode<K, V> newList = null;
        for (MultiValueListNode<K, V> node = this; node != null; node = node.next) {
            if (!node.keyEquals(key)) {
                newList = new MultiValueListNode<>(newList, node.entry);
            }
        }
        return newList;
    }

    private SingleValueListNode<K, V> getEntryForKeyImpl(K key)
    {
        for (MultiValueListNode<K, V> node = this; node != null; node = node.next) {
            if (node.keyEquals(key)) {
                return node.entry;
            }
        }
        return null;
    }

    private boolean keyEquals(K key)
    {
        return key.equals(entry.getKey());
    }
}
