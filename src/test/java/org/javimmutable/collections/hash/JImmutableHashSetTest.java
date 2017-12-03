///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2017, Burton Computer Corporation
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

import junit.framework.TestCase;
import org.javimmutable.collections.Func1;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.JImmutableStack;
import org.javimmutable.collections.common.StandardIterableStreamableTests;
import org.javimmutable.collections.common.StandardJImmutableSetTests;
import org.javimmutable.collections.common.StandardSerializableTests;
import org.javimmutable.collections.cursors.StandardCursorTest;
import org.javimmutable.collections.list.JImmutableLinkedStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;

public class JImmutableHashSetTest
    extends TestCase
{
    public void testStandard()
    {
        StandardJImmutableSetTests.verifySet(JImmutableHashSet.of());
        StandardCursorTest.emptyCursorTest(JImmutableHashSet.<Integer>of().cursor());
        StandardCursorTest.listCursorTest(asList(1, 2, 3), JImmutableHashSet.<Integer>of().union(asList(1, 2, 3)).cursor());
    }

    public void test()
    {
        JImmutableStack<String> expected = JImmutableLinkedStack.of();
        expected = expected.insert("fred").insert("wilma").insert("betty").insert("barney");

        JImmutableSet<String> set = JImmutableHashSet.of();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(false, set.contains("fred"));
        assertEquals(false, set.contains("wilma"));
        assertEquals(false, set.contains("betty"));
        assertEquals(false, set.contains("barney"));
        assertEquals(false, set.containsAny(expected));
        assertEquals(false, set.containsAll(expected));

        set = set.insert("FRED".toLowerCase());
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(true, set.contains("fred"));
        assertEquals(false, set.contains("wilma"));
        assertEquals(false, set.contains("betty"));
        assertEquals(false, set.contains("barney"));
        assertEquals(true, set.containsAny(expected));
        assertEquals(false, set.containsAll(expected));

        set = set.insert("WILMA".toLowerCase());
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(true, set.contains("fred"));
        assertEquals(true, set.contains("wilma"));
        assertEquals(false, set.contains("betty"));
        assertEquals(false, set.contains("barney"));
        assertEquals(true, set.containsAny(expected));
        assertEquals(false, set.containsAll(expected));

        assertSame(set, set.insert("fred"));
        assertSame(set, set.insert("wilma"));

        JImmutableSet<String> set2 = set.union(expected);
        assertFalse(set2.isEmpty());
        assertEquals(4, set2.size());
        assertEquals(true, set2.contains("fred"));
        assertEquals(true, set2.contains("wilma"));
        assertEquals(true, set2.contains("betty"));
        assertEquals(true, set2.contains("barney"));
        assertEquals(true, set2.containsAny(expected));
        assertEquals(true, set2.containsAll(expected));
        assertEquals(new HashSet<>(asList("fred", "wilma", "betty", "barney")), set2.getSet());

        assertEquals(set, set.intersection(set2));
        assertEquals(set, set2.intersection(set));
        assertEquals(set, set2.delete("betty").delete("barney"));

        set2 = set2.deleteAll(set);
        assertFalse(set2.isEmpty());
        assertEquals(2, set2.size());
        assertEquals(false, set2.contains("fred"));
        assertEquals(false, set2.contains("wilma"));
        assertEquals(true, set2.contains("betty"));
        assertEquals(true, set2.contains("barney"));
        assertEquals(true, set2.containsAny(expected));
        assertEquals(false, set2.containsAny(set));
        assertEquals(false, set2.containsAll(expected));

        JImmutableSet<String> set3 = set.union(expected).insert("homer").insert("marge");
        assertFalse(set3.isEmpty());
        assertEquals(6, set3.size());
        assertEquals(true, set3.contains("fred"));
        assertEquals(true, set3.contains("wilma"));
        assertEquals(true, set3.contains("betty"));
        assertEquals(true, set3.contains("barney"));
        assertEquals(true, set3.contains("homer"));
        assertEquals(true, set3.contains("marge"));
        assertEquals(true, set3.containsAny(expected));
        assertEquals(true, set3.containsAny(set));
        assertEquals(true, set3.containsAny(set2));
        assertEquals(true, set3.containsAll(expected));
        assertEquals(true, set3.containsAll(set));
        assertEquals(true, set3.containsAll(set2));
        assertEquals(new HashSet<>(asList("fred", "wilma", "betty", "barney", "homer", "marge")), set3.getSet());
        assertEquals(set, set3.intersection(set));
        assertEquals(set2, set3.intersection(set2));
        assertEquals(set, set.intersection(set));
        assertEquals(set, set.intersection(set3));
        assertEquals(JImmutableHashSet.<String>of(), set.intersection(set2));
        assertEquals(JImmutableHashSet.<String>of(), set2.intersection(set));
        assertEquals(JImmutableHashSet.<String>of(), set3.deleteAll(set3));
    }

    @SuppressWarnings("unchecked")
    public void testIntersectWithStringComparisonDifference()
    {
        // from hash set standpoint HELLO and Hello are NOT the same String so an intersection
        // should yield an empty set.  However that's not how java.util.Set works
        Set<String> hset = new HashSet<>(asList("Hello"));
        Set<String> tset = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        tset.add("HELLO");
        // membership tests show no overlap
        assertEquals(true, hset.contains("Hello"));
        assertEquals(false, hset.contains("HELLO"));
        assertEquals(false, hset.containsAll(tset));
        // yet retainAll() retains the mis-matched string because TreeSet says they are the same
        hset.retainAll(tset);
        assertEquals(asList("Hello"), iterToList(hset));

        // reversing who receives the retainAll() call results in different answer!
        // TreeSet does not retain the HELLO value because HashSet says it's not a member
        hset = new HashSet<>(asList("Hello"));
        tset = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        tset.add("HELLO");
        // membership tests show complete overlap
        assertEquals(true, tset.contains("Hello"));
        assertEquals(true, tset.contains("HELLO"));
        assertEquals(true, tset.containsAll(hset));
        // yet retainAll() drops the matching String because HashSet says they are not the same
        tset.retainAll(hset);
        assertEquals(asList(), iterToList(tset));

        // JImmutableSet inherits Set's retainAll() behavior in intersection(Set)
        // but NOT in the Collection, Cursorable, Cursor, and Iterator variations.
        // Iterable intersection makes membership decision itself
        // Set intersection defers membership decision to passed in Set (like java.util.Set)
        // Collection intersection uses Iterator internally
        tset = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        tset.add("HELLO");
        JImmutableSet<String> jet = JImmutableHashSet.<String>of().insert("Hello");
        // same as Set
        assertEquals(true, jet.contains("Hello"));
        assertEquals(false, jet.contains("HELLO"));
        assertEquals(false, jet.containsAll(tset));
        // same as Set.retainAll()
        assertEquals(asList("Hello"), iterToList(jet.intersection(tset)));
        // different than Set.retainAll()
        assertEquals(asList(), iterToList(jet.intersection(tset.iterator())));
        assertEquals(asList(), iterToList(jet.intersection((Collection)tset)));
    }

    public void testStreams()
    {
        JImmutableSet<Integer> mset = JImmutableHashSet.<Integer>of().insert(4).insert(3).insert(4).insert(2).insert(1).insert(3);
        StandardIterableStreamableTests.verifyOrderedUsingCollection(asList(1, 2, 3, 4), mset);
    }

    public void testSerialization()
        throws Exception
    {
        final Func1<Object, Iterator> iteratorFactory = a -> ((JImmutableSet)a).iterator();
        final JImmutableSet<String> empty = JImmutableHashSet.of();
        StandardSerializableTests.verifySerializable(iteratorFactory, null, empty,
                                                     "H4sIAAAAAAAAAFvzloG1uIjBMb8oXS8rsSwzN7e0JDEpJ1UvOT8nJzW5JDM/r1ivOLUoMzEnsyoRxNXz8oQp8kgszghOLQkoyq+o/A8C/1SMeRgYKgrKORgYmF8yAEEFADwPF+xhAAAA");
        StandardSerializableTests.verifySerializable(iteratorFactory, null, empty.insert("a"),
                                                     "H4sIAAAAAAAAAFvzloG1uIjBMb8oXS8rsSwzN7e0JDEpJ1UvOT8nJzW5JDM/r1ivOLUoMzEnsyoRxNXz8oQp8kgszghOLQkoyq+o/A8C/1SMeRgYKgrKORgYmF8yMDAwljAwJlYAAIYCjBFlAAAA");
        StandardSerializableTests.verifySerializable(iteratorFactory, null, empty.insertAll(asList("a", "b", "c", "b")),
                                                     "H4sIAAAAAAAAAFvzloG1uIjBMb8oXS8rsSwzN7e0JDEpJ1UvOT8nJzW5JDM/r1ivOLUoMzEnsyoRxNXz8oQp8kgszghOLQkoyq+o/A8C/1SMeRgYKgrKORgYmF8yAIkSBsZEIE4C4uQKAIJXlKptAAAA");
    }

    private List<String> iterToList(Iterable<String> source)
    {
        List<String> answer = new ArrayList<>();
        for (String value : source) {
            answer.add(value);
        }
        return answer;
    }
}
