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

package org.javimmutable.collections.tree;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public class UpdateResult<K, V>
{
    @SuppressWarnings("unchecked")
    private static UpdateResult UNCHANGED = new UpdateResult(Type.UNCHANGED, null, null, 0);

    public enum Type
    {
        UNCHANGED,
        INPLACE,
        SPLIT
    }

    public final Type type;
    public final Node<K, V> newNode;
    public final Node<K, V> extraNode;
    public final int sizeDelta;

    private UpdateResult(Type type,
                         Node<K, V> newNode,
                         Node<K, V> extraNode,
                         int sizeDelta)
    {
        this.type = type;
        this.newNode = newNode;
        this.extraNode = extraNode;
        this.sizeDelta = sizeDelta;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    static <K, V> UpdateResult<K, V> createUnchanged()
    {
        return UNCHANGED;
    }

    @Nonnull
    static <K, V> UpdateResult<K, V> createInPlace(@Nonnull Node<K, V> newNode,
                                                   int sizeDelta)
    {
        return new UpdateResult<>(Type.INPLACE, newNode, null, sizeDelta);
    }

    @Nonnull
    static <K, V> UpdateResult<K, V> createSplit(@Nonnull Node<K, V> newNode,
                                                 @Nonnull Node<K, V> extraNode,
                                                 int sizeDelta)
    {
        return new UpdateResult<>(Type.SPLIT, newNode, extraNode, sizeDelta);
    }

    @Override
    public String toString()
    {
        return String.format("<%s,%s,%s,%d>", type, newNode, extraNode, sizeDelta);
    }
}
