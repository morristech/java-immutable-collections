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

package org.javimmutable.collections.array;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.Indexed;
import org.javimmutable.collections.JImmutableArray;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.common.AbstractJImmutableArray;
import org.javimmutable.collections.common.MutableDelta;
import org.javimmutable.collections.iterators.IteratorHelper;
import org.javimmutable.collections.iterators.TransformIterator;
import org.javimmutable.collections.serialization.JImmutableArrayProxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Iterator;
import java.util.stream.Collector;

@Immutable
public class JImmutableTrieArray<T>
    extends AbstractJImmutableArray<T>
    implements Serializable
{
    @SuppressWarnings("unchecked")
    private static final JImmutableTrieArray EMPTY = new JImmutableTrieArray(TrieNode.of(), 0);
    private static final long serialVersionUID = -121805;

    private final TrieNode<T> root;
    private final int size;

    private JImmutableTrieArray(TrieNode<T> root,
                                int size)
    {
        this.root = root;
        this.size = size;
    }

    public static <T> Builder<T> builder()
    {
        return new Builder<>();
    }

    @Nonnull
    public static <T> Collector<T, ?, JImmutableArray<T>> collector()
    {
        return Collector.<T, Builder<T>, JImmutableArray<T>>of(() -> new Builder<>(),
                                                               (b, v) -> b.add(v),
                                                               (b1, b2) -> (Builder<T>)b1.add(b2.iterator()),
                                                               b -> b.build());
    }

    @SuppressWarnings("unchecked")
    public static <T> JImmutableTrieArray<T> of()
    {
        return EMPTY;
    }

    /**
     * Efficiently constructs a TrieArray containing the objects from source (in the specified range).
     * In the constructed TrieArray objects will have array indexes starting at 0 (i.e. indexes
     * from the source are not carried over) so if offset is 10 then source.get(10) will map to
     * array.get(0).
     *
     * @deprecated use builder() instead
     */
    @Deprecated
    public static <T> JImmutableArray<T> of(Indexed<? extends T> source,
                                            int offset,
                                            int limit)
    {
        return JImmutableTrieArray.<T>builder().add(source, offset, limit).build();
    }

    @Override
    @Nullable
    public T getValueOr(int index,
                        @Nullable T defaultValue)
    {
        if (root.getShift() < TrieNode.shiftForIndex(index)) {
            return defaultValue;
        } else {
            return root.getValueOr(root.getShift(), index, defaultValue);
        }
    }

    @Nonnull
    @Override
    public Holder<T> find(int index)
    {
        if (root.getShift() < TrieNode.shiftForIndex(index)) {
            return Holders.of();
        } else {
            return root.find(root.getShift(), index);
        }
    }

    @Nonnull
    @Override
    public JImmutableTrieArray<T> assign(int index,
                                         @Nullable T value)
    {
        MutableDelta sizeDelta = new MutableDelta();
        TrieNode<T> newRoot = root.paddedToMinimumDepthForShift(TrieNode.shiftForIndex(index));
        newRoot = newRoot.assign(newRoot.getShift(), index, value, sizeDelta);
        return (newRoot == root) ? this : new JImmutableTrieArray<>(newRoot, size + sizeDelta.getValue());
    }

    @Nonnull
    @Override
    public JImmutableTrieArray<T> delete(int index)
    {
        if (root.getShift() < TrieNode.shiftForIndex(index)) {
            return this;
        } else {
            MutableDelta sizeDelta = new MutableDelta();
            final TrieNode<T> newRoot = root.delete(root.getShift(), index, sizeDelta).trimmedToMinimumDepth();
            return (newRoot == root) ? this : new JImmutableTrieArray<>(newRoot, size + sizeDelta.getValue());
        }
    }

    @Override
    public int size()
    {
        return size;
    }

    @Nonnull
    @Override
    public JImmutableTrieArray<T> deleteAll()
    {
        return of();
    }

    @Override
    @Nonnull
    public Cursor<JImmutableMap.Entry<Integer, T>> cursor()
    {
        return root.cursor();
    }

    @Nonnull
    @Override
    public SplitableIterator<JImmutableMap.Entry<Integer, T>> iterator()
    {
        return root.iterator();
    }

    @Override
    public void checkInvariants()
    {
        root.checkInvariants();
    }

    @Override
    public boolean equals(Object o)
    {
        return (o == this) || ((o instanceof JImmutableArray) && IteratorHelper.iteratorEquals(iterator(), ((JImmutableArray)o).iterator()));
    }

    @Override
    public int hashCode()
    {
        return IteratorHelper.iteratorHashCode(iterator());
    }

    @Override
    public String toString()
    {
        return IteratorHelper.iteratorToString(iterator());
    }

    private Object writeReplace()
    {
        return new JImmutableArrayProxy(this);
    }

    public static class Builder<T>
        implements JImmutableArray.Builder<T>
    {
        private final TrieArrayBuilder<T> builder;

        private Builder()
        {
            builder = new TrieArrayBuilder<>();
        }

        @Override
        public int size()
        {
            return builder.size();
        }

        @Nonnull
        @Override
        public Builder<T> add(T value)
        {
            builder.add(value);
            return this;
        }

        @Nonnull
        @Override
        public JImmutableTrieArray<T> build()
        {
            return builder.size() == 0 ? of() : new JImmutableTrieArray<>(builder.build(), builder.size());
        }

        @Nonnull
        private Iterator<T> iterator()
        {
            return TransformIterator.of(builder.build().iterator(), e -> e.getValue());
        }
    }
}
