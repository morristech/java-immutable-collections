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

package org.javimmutable.collections.hash;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Func1;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.MapEntry;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.common.AbstractJImmutableMap;
import org.javimmutable.collections.common.MutableDelta;
import org.javimmutable.collections.hash.collision_map.CollisionMap;
import org.javimmutable.collections.hash.collision_map.ListCollisionMap;
import org.javimmutable.collections.hash.collision_map.TreeCollisionMap;
import org.javimmutable.collections.hash.hamt.HamtEmptyNode;
import org.javimmutable.collections.hash.hamt.HamtNode;
import org.javimmutable.collections.serialization.JImmutableHashMapProxy;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

@Immutable
public class JImmutableHashMap<T, K, V>
    extends AbstractJImmutableMap<K, V>
    implements Serializable
{
    // we only need one instance of the transformations object
    static final ListCollisionMap LIST_COLLISION_MAP = new ListCollisionMap();

    // we only need one instance of the transformations object
    static final TreeCollisionMap TREE_COLLISION_MAP = new TreeCollisionMap();

    // this is safe since the transformations object works for any possible K and V
    @SuppressWarnings("unchecked")
    static final JImmutableHashMap LIST_EMPTY = new JImmutableHashMap(HamtEmptyNode.of(), 0, LIST_COLLISION_MAP);

    // this is safe since the transformations object works for any possible K and V
    @SuppressWarnings("unchecked")
    static final JImmutableHashMap TREE_EMPTY = new JImmutableHashMap(HamtEmptyNode.of(), 0, TREE_COLLISION_MAP);

    private static final long serialVersionUID = -121805;

    private final HamtNode<T, K, V> root;
    private final int size;
    private final CollisionMap<T, K, V> collisionMap;

    private JImmutableHashMap(HamtNode<T, K, V> root,
                              int size,
                              CollisionMap<T, K, V> collisionMap)
    {
        this.root = root;
        this.size = size;
        this.collisionMap = collisionMap;
    }

    /**
     * Returns an empty hash map.  The empty map will automatically select a collision handling strategy
     * on the first call to assign() based on the key for that call.  For this reason all keys used for a
     * given map must either implement or not implement Comparable.  If some keys implement it and some do
     * not the collision handling code will likely fail due to a class cast exception or a method
     * not defined exception.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> EmptyHashMap<K, V> of()
    {
        return EmptyHashMap.INSTANCE;
    }

    /**
     * Returns an empty map using the appropriate collision handling strategy for keys of the given
     * class.  All keys used with that map should derive from the specified class to avoid runtime
     * problems with incompatible keys.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> JImmutableMap<K, V> of(Class<K> klass)
    {
        return klass.isAssignableFrom(Comparable.class) ? TREE_EMPTY : LIST_EMPTY;
    }

    /**
     * Returns an empty map using the appropriate collision handling strategy for the given key's
     * class.  All keys used with that map should derive from the specified key's class to avoid runtime
     * problems with incompatible keys.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> JImmutableMap<K, V> forKey(K key)
    {
        return (key instanceof Comparable) ? TREE_EMPTY : LIST_EMPTY;
    }

    /**
     * Returns an empty map using linked lists for handling hash code collisions.  This is safe
     * for any type of key but is slower when many keys have the same hash code.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> JImmutableMap<K, V> usingList()
    {
        return (JImmutableMap<K, V>)LIST_EMPTY;
    }

    /**
     * Returns an empty map using linked lists for handling hash code collisions.  This is faster
     * than the list based collision handling but depends on all keys implementing Comparable and
     * being able to compare themselves to all other keys.
     */
    @SuppressWarnings("unchecked")
    public static <K extends Comparable<K>, V> JImmutableMap<K, V> usingTree()
    {
        return (JImmutableMap<K, V>)TREE_EMPTY;
    }

    @Override
    public V getValueOr(K key,
                        V defaultValue)
    {
        return root.getValueOr(collisionMap, key.hashCode(), key, defaultValue);
    }

    @Nonnull
    @Override
    public Holder<V> find(@Nonnull K key)
    {
        return root.find(collisionMap, key.hashCode(), key);
    }

    @Nonnull
    @Override
    public Holder<Entry<K, V>> findEntry(@Nonnull K key)
    {
        Holder<V> value = find(key);
        if (value.isEmpty()) {
            return Holders.of();
        } else {
            return Holders.of(MapEntry.of(key, value.getValue()));
        }
    }

    @Nonnull
    @Override
    public JImmutableMap<K, V> assign(@Nonnull K key,
                                      V value)
    {
        MutableDelta sizeDelta = new MutableDelta();
        HamtNode<T, K, V> newRoot = root.assign(collisionMap, key.hashCode(), key, value, sizeDelta);
        if (newRoot == root) {
            return this;
        } else {
            return new JImmutableHashMap<>(newRoot, size + sizeDelta.getValue(), collisionMap);
        }
    }

    @Nonnull
    @Override
    public JImmutableMap<K, V> update(@Nonnull K key,
                                      @Nonnull Func1<Holder<V>, V> generator)
    {
        MutableDelta sizeDelta = new MutableDelta();
        HamtNode<T, K, V> newRoot = root.update(collisionMap, key.hashCode(), key, generator, sizeDelta);
        if (newRoot == root) {
            return this;
        } else {
            return new JImmutableHashMap<>(newRoot, size + sizeDelta.getValue(), collisionMap);
        }
    }

    @Nonnull
    @Override
    public JImmutableMap<K, V> delete(@Nonnull K key)
    {
        MutableDelta sizeDelta = new MutableDelta();
        HamtNode<T, K, V> newRoot = root.delete(collisionMap, key.hashCode(), key, sizeDelta);
        if (newRoot == root) {
            return this;
        } else if (newRoot.isEmpty()) {
            return of();
        } else {
            return new JImmutableHashMap<>(newRoot, size + sizeDelta.getValue(), collisionMap);
        }
    }

    @Override
    public int size()
    {
        return size;
    }

    @Nonnull
    @Override
    public JImmutableMap<K, V> deleteAll()
    {
        return of();
    }

    @Override
    @Nonnull
    public Cursor<Entry<K, V>> cursor()
    {
        return root.cursor(collisionMap);
    }

    @Nonnull
    @Override
    public SplitableIterator<Entry<K, V>> iterator()
    {
        return root.iterator(collisionMap);
    }

    @Override
    public void checkInvariants()
    {
        root.checkInvariants();
    }

    // for unit test to verify proper transforms selected
    CollisionMap getCollisionMap()
    {
        return collisionMap;
    }

    private Object writeReplace()
    {
        return new JImmutableHashMapProxy(this);
    }
}
