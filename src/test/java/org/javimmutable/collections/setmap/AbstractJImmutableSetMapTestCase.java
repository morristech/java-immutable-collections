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

package org.javimmutable.collections.setmap;

import junit.framework.TestCase;
import org.javimmutable.collections.Func1;
import org.javimmutable.collections.JImmutableList;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.JImmutableSet;
import org.javimmutable.collections.JImmutableSetMap;
import org.javimmutable.collections.MapEntry;
import org.javimmutable.collections.cursors.IterableCursor;
import org.javimmutable.collections.cursors.StandardCursorTest;
import org.javimmutable.collections.hash.JImmutableHashSet;
import org.javimmutable.collections.util.JImmutables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractJImmutableSetMapTestCase
    extends TestCase
{
    public enum Ordering
    {
        INORDER,
        REVERSED,
        HASH
    }

    public JImmutableSetMap<Integer, Integer> verifyOperations(JImmutableSetMap<Integer, Integer> map,
                                                               Ordering ordering)
    {
        verifySetOperations(map);
        verifyContains(map);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(1));
        assertEquals(0, map.getSet(1).size());

        map = map.insert(1, 100);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertSame(map.getSet(1), map.get(1));
        assertEquals(1, map.getSet(1).size());

        map = map.insert(1, 18);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals(new HashSet<>(Arrays.asList(100, 18)), map.getSet(1).getSet());
        assertSame(map.getSet(1), map.get(1));
        assertEquals(2, map.getSet(1).size());

        map = map.insert(MapEntry.of(3, 87));
        map = map.insert(MapEntry.of(2, 87));
        map = map.insert(MapEntry.of(1, 87));
        map = map.insert(MapEntry.of(1, 87));
        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertEquals(new HashSet<>(Arrays.asList(100, 18, 87)), map.getSet(1).getSet());
        assertEquals(3, map.getSet(1).size());
        assertSame(map.getSet(1), map.get(1));
        assertEquals(new HashSet<>(Arrays.asList(87)), map.getSet(2).getSet());
        assertSame(map.getSet(2), map.get(2));
        assertEquals(new HashSet<>(Arrays.asList(87)), map.getSet(3).getSet());
        assertSame(map.getSet(3), map.get(3));

        map = map.assign(3, map.getSet(Integer.MIN_VALUE).insert(300).insert(7).insert(7).insert(14));
        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertEquals(new HashSet<>(Arrays.asList(100, 18, 87)), map.getSet(1).getSet());
        assertSame(map.getSet(1), map.get(1));
        assertEquals(new HashSet<>(Arrays.asList(87)), map.getSet(2).getSet());
        assertSame(map.getSet(2), map.get(2));
        assertEquals(new HashSet<>(Arrays.asList(300, 7, 14)), map.getSet(3).getSet());
        assertSame(map.getSet(3), map.get(3));
        assertEquals(map.getSet(3), map.values(3).stream().collect(Collectors.toSet()));

        final JImmutableSet<Integer> defaultValue = JImmutableHashSet.<Integer>of().insert(17);
        assertTrue(map.find(8).isEmpty());
        assertNull(map.get(8));
        assertNull(map.getValueOr(8, null));
        assertSame(defaultValue, map.getValueOr(8, defaultValue));
        assertSame(map.get(3), map.find(3).getValue());
        assertSame(map.get(3), map.getValueOr(3, defaultValue));
        assertTrue(map.deleteAll().isEmpty());
        assertTrue(map.delete(3).delete(2).delete(1).delete(0).isEmpty());

        if (ordering == Ordering.HASH) {
            StandardCursorTest.listCursorTest(Arrays.asList(100, 18, 87), map.valuesCursor(1));
            StandardCursorTest.listCursorTest(Arrays.asList(87), map.valuesCursor(2));
            StandardCursorTest.listCursorTest(Arrays.asList(7, 300, 14), map.valuesCursor(3));
            StandardCursorTest.listCursorTest(Collections.emptyList(), map.valuesCursor(4));
        } else if (ordering == Ordering.REVERSED) {
            StandardCursorTest.listCursorTest(Arrays.asList(100, 87, 18), map.valuesCursor(1));
            StandardCursorTest.listCursorTest(Arrays.asList(87), map.valuesCursor(2));
            StandardCursorTest.listCursorTest(Arrays.asList(300, 14, 7), map.valuesCursor(3));
            StandardCursorTest.listCursorTest(Collections.emptyList(), map.valuesCursor(4));
        } else {
            StandardCursorTest.listCursorTest(Arrays.asList(18, 87, 100), map.valuesCursor(1));
            StandardCursorTest.listCursorTest(Arrays.asList(87), map.valuesCursor(2));
            StandardCursorTest.listCursorTest(Arrays.asList(7, 14, 300), map.valuesCursor(3));
            StandardCursorTest.listCursorTest(Collections.emptyList(), map.valuesCursor(4));
        }

        verifyTransform(map);
        verifyCollector(map.insert(-10, -20).insert(-45, 90));
        
        return map;
    }


    private void verifyTransform(JImmutableSetMap<Integer, Integer> map)
    {
        final Func1<JImmutableSet<Integer>, JImmutableSet<Integer>> removeAll = set -> set.deleteAll();
        final Func1<JImmutableSet<Integer>, JImmutableSet<Integer>> removeLarge = set -> set.reject(x -> x >= 10);
        final Func1<JImmutableSet<Integer>, JImmutableSet<Integer>> removeEven = set -> set.reject(x -> x % 2 == 0);

        final int goodKey = 1;
        final int badKey = 2;

        final JImmutableSetMap<Integer, Integer> start = map.deleteAll().insertAll(goodKey, Arrays.asList(1, 2, 3, 4, 5, 6));
        final JImmutableSet<Integer> oddOnly = start.getSet(goodKey).reject(x -> x % 2 == 0);

        assertSame(start, start.transform(goodKey, removeLarge));
        assertEquals(start.assign(goodKey, oddOnly), start.transform(goodKey, removeEven));
        assertSame(start, start.transform(badKey, removeLarge));

        assertSame(start, start.transformIfPresent(goodKey, removeLarge));
        assertEquals(start.assign(goodKey, oddOnly), start.transformIfPresent(goodKey, removeEven));
        assertSame(start, start.transform(badKey, removeLarge));
        assertSame(start, start.transformIfPresent(badKey, removeAll));
        assertSame(start, start.transformIfPresent(badKey, removeAll));
    }

    private void verifyContains(JImmutableSetMap<Integer, Integer> emptyMap)
    {
        JImmutableList<Integer> values = JImmutables.list();
        JImmutableSetMap<Integer, Integer> jetMap = emptyMap.insertAll(1, JImmutables.set(1, 2, 4));

        //empty with empty
        assertEquals(false, emptyMap.contains(1));
        assertEquals(false, emptyMap.contains(1, null));
        assertEquals(false, emptyMap.containsAll(1, values));
        assertEquals(false, emptyMap.containsAll(1, values.getList()));
        assertEquals(false, emptyMap.containsAny(1, values));
        assertEquals(false, emptyMap.containsAny(1, values.getList()));

        //values with empty
        assertEquals(true, jetMap.contains(1));
        assertEquals(false, jetMap.contains(1, null));
        assertEquals(true, jetMap.containsAll(1, values));
        assertEquals(true, jetMap.containsAll(1, values.getList()));
        assertEquals(false, jetMap.containsAny(1, values));
        assertEquals(false, jetMap.containsAny(1, values.getList()));

        //empty with values
        values = values.insert(1).insert(1).insert(2);
        assertEquals(false, emptyMap.contains(1, 1));
        assertEquals(false, emptyMap.containsAll(1, values));
        assertEquals(false, emptyMap.containsAll(1, values.getList()));
        assertEquals(false, emptyMap.containsAny(1, values));
        assertEquals(false, emptyMap.containsAny(1, values.getList()));

        //values with values
        //smaller values
        assertEquals(true, jetMap.contains(1, 1));
        assertEquals(true, jetMap.containsAll(1, values));
        assertEquals(true, jetMap.containsAll(1, values.getList()));
        assertEquals(true, jetMap.containsAny(1, values));
        assertEquals(true, jetMap.containsAny(1, values.getList()));

        //extra values
        values = values.insert(5);
        assertEquals(false, jetMap.containsAll(1, values));
        assertEquals(false, jetMap.containsAll(1, values.getList()));
        assertEquals(true, jetMap.containsAny(1, values));
        assertEquals(true, jetMap.containsAny(1, values.getList()));

        //different values
        values = values.deleteAll().insert(3).insert(5);
        assertEquals(false, jetMap.containsAll(1, values));
        assertEquals(false, jetMap.containsAll(1, values.getList()));
        assertEquals(false, jetMap.containsAny(1, values));
        assertEquals(false, jetMap.containsAny(1, values.getList()));
    }

    private void verifySetOperations(JImmutableSetMap<Integer, Integer> emptyMap)
    {
        JImmutableSet<Integer> emptyJet = JImmutableHashSet.of();
        final Set<Integer> emptySet = Collections.emptySet();

        assertEquals(0, emptyMap.size());
        assertEquals(true, emptyMap.isEmpty());
        assertEquals(emptyMap.getSet(0).getSet(), new HashSet<Integer>());

        JImmutableSetMap<Integer, Integer> jetMap = emptyMap;
        assertEquals(false, jetMap.contains(1, 10));
        jetMap = jetMap.insert(1, 10).insert(1, 20);
        assertEquals(true, jetMap != emptyMap);
        assertEquals(false, jetMap.isEmpty());
        assertEquals(true, jetMap.contains(1, 10));
        assertEquals(true, jetMap.contains(1, 20));
        assertEquals(true, jetMap.containsAll(1, Arrays.asList(10, 20)));

        jetMap = jetMap.delete(1);
        assertEquals(0, jetMap.size());
        assertEquals(false, jetMap.contains(1, 10));
        assertEquals(false, jetMap.contains(1, 20));

        final Set<Integer> values = asSet(1, 2, 3, 4);
        HashMap<Integer, Set<Integer>> expected = new HashMap<>();
        expected.put(1, values);
        verifyExpected(expected, 1, values);
        verifyContents(jetMap.union(1, values), expected);
        verifyContents(jetMap.union(1, IterableCursor.of(values)), expected);
        verifyContents(jetMap.union(1, values.iterator()), expected);

        //intersect with larger set
        jetMap = emptyMap.union(1, values);
        final Set<Integer> withExtra = asSet(0, 1, 2, 3, 4, 5);
        Set<Integer> intersectionSet = new HashSet<>(withExtra);
        JImmutableSet<Integer> intersectionJet = emptyJet.union(withExtra);
        verifyContents(jetMap.intersection(1, asIterable(withExtra)), expected);
        verifyContents(jetMap.intersection(1, withExtra), expected);
        verifyContents(jetMap.intersection(1, IterableCursor.of(withExtra)), expected);
        verifyContents(jetMap.intersection(1, withExtra.iterator()), expected);
        verifyContents(jetMap.intersection(1, intersectionJet), expected);
        verifyContents(jetMap.intersection(1, intersectionSet), expected);

        //intersect with smaller set
        jetMap = emptyMap.union(1, withExtra);
        intersectionSet = new HashSet<>(values);
        intersectionJet = emptyJet.union(values);
        verifyContents(jetMap.intersection(1, asIterable(values)), expected);
        verifyContents(jetMap.intersection(1, values), expected);
        verifyContents(jetMap.intersection(1, IterableCursor.of(values)), expected);
        verifyContents(jetMap.intersection(1, values.iterator()), expected);
        verifyContents(jetMap.intersection(1, intersectionJet), expected);
        verifyContents(jetMap.intersection(1, intersectionSet), expected);

        //empty set intersection with non-empty set
        expected.put(1, emptySet);
        verifyExpected(expected, 1, emptySet);
        verifyExpected(expected, 1, emptySet);
        verifyContents(emptyMap.intersection(1, asIterable(withExtra)), expected);
        verifyContents(emptyMap.intersection(1, withExtra), expected);
        verifyContents(emptyMap.intersection(1, IterableCursor.of(withExtra)), expected);
        verifyContents(emptyMap.intersection(1, withExtra.iterator()), expected);
        verifyContents(emptyMap.intersection(1, intersectionJet), expected);
        verifyContents(emptyMap.intersection(1, intersectionSet), expected);

        //non-empty set intersection with empty set
        intersectionSet = new HashSet<>();
        intersectionJet = emptyJet;
        verifyContents(jetMap.intersection(1, asIterable(emptySet)), expected);
        verifyContents(jetMap.intersection(1, emptySet), expected);
        verifyContents(jetMap.intersection(1, IterableCursor.of(emptySet)), expected);
        verifyContents(jetMap.intersection(1, emptySet.iterator()), expected);
        verifyContents(jetMap.intersection(1, intersectionJet), expected);
        verifyContents(jetMap.intersection(1, intersectionSet), expected);

        //deleteAll from smaller set
        expected.put(1, asSet(0, 5));
        verifyExpected(expected, 1, asSet(0, 5));
        jetMap = emptyMap.union(1, withExtra);
        verifyContents(jetMap.deleteAll(1, values), expected);
        verifyContents(jetMap.deleteAll(1, IterableCursor.of(values)), expected);
        verifyContents(jetMap.deleteAll(1, values.iterator()), expected);

        //deleteAll from larger set
        jetMap = emptyMap.union(1, values);
        expected.put(1, emptySet);
        verifyExpected(expected, 1, emptySet);
        verifyContents(jetMap.deleteAll(1, withExtra), expected);
        verifyContents(jetMap.deleteAll(1, IterableCursor.of(withExtra)), expected);
        verifyContents(jetMap.deleteAll(1, withExtra.iterator()), expected);

        //insertAll to empty set
        jetMap = emptyMap;
        expected.put(1, values);
        verifyExpected(expected, 1, values);
        verifyContents(jetMap.insertAll(1, values), expected);
        verifyContents(jetMap.insertAll(1, IterableCursor.of(values)), expected);
        verifyContents(jetMap.insertAll(1, values.iterator()), expected);

        //insertAll to non-empty set
        jetMap = emptyMap.union(1, values);
        expected.put(1, withExtra);
        verifyExpected(expected, 1, withExtra);
        verifyContents(jetMap.insertAll(1, withExtra), expected);
        verifyContents(jetMap.insertAll(1, IterableCursor.of(withExtra)), expected);
        verifyContents(jetMap.insertAll(1, withExtra.iterator()), expected);

        //insertAll to smaller set
        jetMap = emptyMap.union(1, Arrays.asList(4, 5, 6, 7));
        expected.put(1, asSet(0, 1, 2, 3, 4, 5, 6, 7));
        verifyExpected(expected, 1, asSet(0, 1, 2, 3, 4, 5, 6, 7));
        verifyContents(jetMap.insertAll(1, withExtra), expected);
        verifyContents(jetMap.insertAll(1, IterableCursor.of(withExtra)), expected);
        verifyContents(jetMap.insertAll(1, withExtra.iterator()), expected);
    }

    public void verifyContents(JImmutableSetMap<Integer, Integer> jetMap,
                               Map<Integer, Set<Integer>> expected)
    {
        assertEquals(expected.isEmpty(), jetMap.isEmpty());
        assertEquals(expected.size(), jetMap.size());
        for (Map.Entry<Integer, Set<Integer>> expectedEntry : expected.entrySet()) {
            Integer key = expectedEntry.getKey();
            Set<Integer> values = expectedEntry.getValue();
            for (Integer v : values) {
                assertEquals(true, jetMap.contains(key, v));
            }
            assertEquals(true, jetMap.containsAll(key, values));
            assertEquals(true, jetMap.containsAll(key, IterableCursor.of(values)));
            assertEquals(true, jetMap.containsAll(key, values.iterator()));

            assertEquals(!values.isEmpty(), jetMap.containsAny(key, values));
            assertEquals(!values.isEmpty(), jetMap.containsAny(key, IterableCursor.of(values)));
            assertEquals(!values.isEmpty(), jetMap.containsAny(key, values.iterator()));

            if (!values.isEmpty()) {
                List<Integer> subset = Arrays.asList(values.iterator().next());
                assertEquals(true, jetMap.containsAll(key, subset));
                assertEquals(true, jetMap.containsAll(key, IterableCursor.of(subset)));
                assertEquals(true, jetMap.containsAll(key, subset.iterator()));

                assertEquals(true, jetMap.containsAny(key, subset));
                assertEquals(true, jetMap.containsAny(key, IterableCursor.of(subset)));
                assertEquals(true, jetMap.containsAny(key, subset.iterator()));
            }
        }
    }

    public void verifyRandom(JImmutableSetMap<Integer, Integer> emptyJetMap,
                             Map<Integer, Set<Integer>> expected)
    {
        Random random = new Random(2500L);
        for (int i = 0; i < 50; ++i) {
            int size = 1 + random.nextInt(20000);

            JImmutableSetMap<Integer, Integer> jetMap = emptyJetMap;

            for (int loops = 0; loops < (4 * size); ++loops) {
                Integer key = random.nextInt(size);
                Set<Integer> set = new HashSet<>();
                for (int n = random.nextInt(3); n > 0; --n) {
                    set.add(random.nextInt(size));
                }
                int command = random.nextInt(10);
                switch (command) {
                case 0:
                    jetMap = jetMap.insert(key, key);
                    if (expected.containsKey(key)) {
                        expected.get(key).add(key);
                    } else {
                        expected.put(key, asSet(key));
                    }
                    verifyExpected(expected, key, asSet(key));
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 1:
                    jetMap = jetMap.insertAll(key, set);
                    if (expected.containsKey(key)) {
                        expected.get(key).addAll(set);
                    } else {
                        expected.put(key, set);
                    }
                    verifyExpected(expected, key, set);
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 2:
                    jetMap = jetMap.delete(key);
                    if (expected.containsKey(key)) {
                        expected.remove(key);
                    }
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 3:
                    jetMap = jetMap.deleteAll(key, set);
                    if (expected.containsKey(key)) {
                        expected.get(key).removeAll(set);
                    }
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 4:
                    if (expected.containsKey(key)) {
                        assertEquals(jetMap.contains(key, key), expected.get(key).contains(key));
                    }
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 5:
                    if (expected.containsKey(key)) {
                        assertEquals(jetMap.containsAll(key, set), expected.get(key).containsAll(set));
                    }
                    break;
                case 6:
                    jetMap = jetMap.union(key, set);
                    if (expected.containsKey(key)) {
                        expected.get(key).addAll(set);
                    } else {
                        expected.put(key, set);
                    }
                    verifyExpected(expected, key, set);
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 7:
                    jetMap = jetMap.intersection(key, set);
                    if (expected.containsKey(key)) {
                        expected.get(key).retainAll(set);
                    } else {
                        expected.put(key, new HashSet<>());
                    }
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 8:
                    for (Integer value : set) {
                        jetMap = jetMap.delete(key, value);
                    }
                    if (expected.containsKey(key)) {
                        expected.get(key).removeAll(set);
                    }
                    assertEquals(expected.size(), jetMap.size());
                    break;
                case 9:
                    if (expected.containsKey(key)) {
                        Iterator<Integer> values = expected.get(key).iterator();
                        if (values.hasNext()) {
                            Integer value = values.next();
                            jetMap = jetMap.delete(key, value);
                            values.remove();
                        }
                    }
                    assertEquals(expected.size(), jetMap.size());
                    break;
                }
                assertEquals(expected.size(), jetMap.size());
            }
            jetMap.checkInvariants();
            verifyContents(jetMap, expected);
            for (JImmutableMap.Entry<Integer, JImmutableSet<Integer>> e : jetMap) {
                jetMap = jetMap.delete(e.getKey());
                expected.remove(e.getKey());
            }
            jetMap.checkInvariants();
            assertEquals(0, jetMap.size());
            assertEquals(true, jetMap.isEmpty());
            assertEquals(expected.size(), jetMap.size());
        }
    }

    protected Set<Integer> asSet(int... args)
    {
        Set<Integer> set = new HashSet<>();
        for (int arg : args) {
            set.add(arg);
        }
        return set;
    }

    // forces java type system to use Iterable version of method
    protected Iterable<Integer> asIterable(Collection<Integer> values)
    {
        return values;
    }

    protected void verifyExpected(Map<Integer, Set<Integer>> expected,
                                  int key,
                                  Set<Integer> values)
    {
        assertEquals(true, expected.containsKey(key));
        assertEquals(true, expected.get(key).containsAll(values));
    }

    private static void verifyCollector(JImmutableSetMap<Integer, Integer> template)
    {
        Collection<JImmutableMap.Entry<Integer, Integer>> values = new ArrayList<>();
        for (int i = 1; i <= 500; ++i) {
            values.add(MapEntry.of(i, i));
            if (i % 2 == 0) {
                values.add(MapEntry.of(i, -i));
            }
        }

        JImmutableSetMap<Integer, Integer> expected = template.insertAll(values);
        JImmutableSetMap<Integer, Integer> actual = values.parallelStream().collect(template.setMapCollector());
        assertEquals(expected, actual);
    }
}