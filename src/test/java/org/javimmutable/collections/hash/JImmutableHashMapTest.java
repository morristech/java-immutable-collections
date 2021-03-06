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

import junit.framework.TestCase;
import org.javimmutable.collections.Func1;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.MapEntry;
import org.javimmutable.collections.common.StandardJImmutableMapTests;
import org.javimmutable.collections.common.StandardSerializableTests;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.javimmutable.collections.common.StandardJImmutableMapTests.verifyEnumeration;
import static org.javimmutable.collections.cursors.StandardCursorTest.*;

public class JImmutableHashMapTest
    extends TestCase
{
    public void test()
    {
        JImmutableMap<Integer, Integer> map = JImmutableHashMap.usingList();
        assertEquals(true, map.find(10).isEmpty());
        assertEquals(0, map.size());
        assertEquals(true, map.isEmpty());
        map = map.assign(10, 20);
        assertEquals(1, map.size());
        assertEquals(false, map.isEmpty());
        assertEquals(false, map.find(10).isEmpty());
        assertEquals(20, (int)map.find(10).getValue());
        assertEquals(20, (int)map.getValueOr(10, -99));
        assertEquals(-99, (int)map.getValueOr(72, -99));
        map = map.delete(10);
        assertEquals(true, map.find(10).isEmpty());
        assertEquals(0, map.size());

        StandardJImmutableMapTests.verifyMiscellaneous(JImmutableHashMap.usingList());
        StandardJImmutableMapTests.verifyMiscellaneous(JImmutableHashMap.usingTree());
    }

    public void testValueIdentity()
    {
        JImmutableMap<Integer, String> map = JImmutableHashMap.usingList();
        map = map.assign(10, "ab");
        assertSame(map, map.assign(10, "ab"));
        for (int i = 100; i <= 15000; ++i) {
            map = map.assign(i, Integer.toString(i));
        }
        map = map.assign(14000, "aaa");
        assertSame(map, map.assign(14000, "aaa"));
    }

    public void testRandom1()
    {
        Random random = new Random(100L);
        for (int maxKeyLoop = 0; maxKeyLoop < 2; ++maxKeyLoop) {
            final int maxKey = (maxKeyLoop == 0) ? 10000 : 99999999;
            for (int loop = 0; loop < 1000; ++loop) {
                HashMap<ManualHashKey, Integer> expected = new HashMap<>();
                JImmutableMap<ManualHashKey, Integer> map = (loop % 2 == 0) ? JImmutableHashMap.usingTree() : JImmutableHashMap.usingList();
                final int size = 250 + random.nextInt(250);
                for (int i = 1; i <= size; ++i) {
                    int command = random.nextInt(6);
                    switch (command) {
                        case 0: {
                            ManualHashKey key = createManualHashKey(maxKey, random);
                            Integer value = random.nextInt(1000000);
                            int merged = value;
                            map = map.update(key, h -> h.isEmpty() ? value : h.getValue() ^ value);
                            if (expected.get(key) != null) {
                                merged = expected.get(key) ^ value;
                            }
                            expected.put(key, merged);
                            break;
                        }
                        case 1: {
                            ManualHashKey key = createManualHashKey(maxKey, random);
                            Integer value = random.nextInt(1000000);
                            expected.put(key, value);
                            map = map.assign(key, value);
                            break;
                        }
                        case 2: {
                            JImmutableMap<ManualHashKey, Integer> col = JImmutableHashMap.usingTree();
                            int times = random.nextInt(3);
                            for (int rep = 0; rep < times; rep++) {
                                ManualHashKey key = createManualHashKey(maxKey, random);
                                Integer value = random.nextInt(1000000);
                                col = col.assign(key, value);
                            }
                            expected.putAll(col.getMap());
                            map = (random.nextBoolean()) ? map.assignAll(col) : map.assignAll(col.getMap());
                            break;
                        }
                        case 3: {
                            ManualHashKey key = createManualHashKey(maxKey, random);
                            expected.remove(key);
                            map = map.delete(key);
                            break;
                        }
                        case 4: {
                            ManualHashKey key = createManualHashKey(maxKey, random);
                            assertEquals(expected.get(key), map.find(key).getValueOrNull());
                            assertEquals(expected.size(), map.size());
                            map.checkInvariants();
                            break;
                        }
                        case 5: {
                            ManualHashKey key = createManualHashKey(maxKey, random);
                            Integer value = random.nextInt(1000000);
                            Integer currentValue = map.get(key);
                            if (currentValue == null) {
                                map = map.update(key, h -> h.isEmpty() ? value : -h.getValue());
                            } else {
                                map = map.update(key, h -> h.isEmpty() ? -value : value);
                            }
                            expected.put(key, value);
                            break;
                        }
                    }
                }

                map.checkInvariants();
                verifyEnumeration(expected, map);

                for (Map.Entry<ManualHashKey, Integer> entry : expected.entrySet()) {
                    Holder<Integer> mapValue = map.find(entry.getKey());
                    assertEquals(true, mapValue.isFilled());
                    assertEquals(entry.getValue(), mapValue.getValue());
                }

                // verify the cursor worked properly
                final List<JImmutableMap.Entry<ManualHashKey, Integer>> entries = new ArrayList<>();
                Map<ManualHashKey, Integer> fromCursor = new HashMap<>();
                for (JImmutableMap.Entry<ManualHashKey, Integer> entry : map) {
                    entries.add(entry);
                    fromCursor.put(entry.getKey(), entry.getValue());
                }
                assertEquals(expected, fromCursor);
                listCursorTest(entries, map.cursor());
                cursorTest(value -> entries.get(value).getKey(), entries.size(), map.keysCursor());
                cursorTest(value -> entries.get(value).getValue(), entries.size(), map.valuesCursor());
                listIteratorTest(entries, map.iterator());
                iteratorTest(value -> entries.get(value).getKey(), entries.size(), map.getMap().keySet().iterator());
                iteratorTest(value -> entries.get(value).getValue(), entries.size(), map.getMap().values().iterator());

                // verify the Map adaptor worked properly
                assertEquals(expected, map.getMap());
                assertEquals(expected.keySet(), map.getMap().keySet());
                assertEquals(expected.entrySet(), map.getMap().entrySet());
                ArrayList<Integer> jvalues = new ArrayList<>(expected.values());
                ArrayList<Integer> pvalues = new ArrayList<>(map.getMap().values());
                Collections.sort(jvalues);
                Collections.sort(pvalues);
                assertEquals(jvalues, pvalues);

                // verify the map can remove all keys
                ArrayList<ManualHashKey> keys = new ArrayList<>(expected.keySet());
                Collections.shuffle(keys, random);
                for (ManualHashKey key : keys) {
                    assertEquals(false, map.find(key).isEmpty());
                    map = map.delete(key);
                    assertEquals(true, map.find(key).isEmpty());
                }
                assertEquals(0, map.size());
            }
        }
    }

    @Nonnull
    private ManualHashKey createManualHashKey(int maxKey,
                                              Random random)
    {
        int keyValue = random.nextInt(maxKey);
        return new ManualHashKey(keyValue % 100, String.valueOf(keyValue));
    }

    public void testEquals()
    {
        JImmutableMap<Integer, Integer> map1 = JImmutableHashMap.<Integer, Integer>usingList().assign(1, 3).assign(2, 4).assign(3, 5);
        JImmutableMap<Integer, Integer> map2 = JImmutableHashMap.<Integer, Integer>usingList().assign(1, 3).assign(2, 4).assign(3, 5);
        assertEquals(map1.hashCode(), map2.hashCode());
        assertEquals(map1, map2);
    }

    public void testDeleteAll()
    {
        JImmutableMap<Integer, Integer> map1 = JImmutableHashMap.<Integer, Integer>usingList().assign(1, 3).assign(2, 4).assign(3, 5);
        assertSame(JImmutableHashMap.of(), map1.deleteAll());
    }

    public void testAssignAll()
    {
        //assignAll(JImmutableMap)
        JImmutableMap<String, Number> empty = JImmutableHashMap.of();
        JImmutableMap<String, Number> map = empty;
        JImmutableMap<String, Integer> expected = JImmutableHashMap.usingList();
        map = map.assignAll(expected);
        assertEquals(expected, map);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());

        expected = expected.assign("a", 10);
        map = map.assignAll(expected);
        assertEquals(expected, map);
        assertEquals(1, map.size());
        assertEquals(10, map.get("a"));
        assertSame(JImmutableHashMap.TREE_COLLISION_MAP, ((JImmutableHashMap)map).getCollisionMap());

        assertEquals(map, map.assignAll(empty));

        expected = expected.assign("a", 8).assign("b", 12).assign("c", 14);
        map = map.assignAll(expected);
        assertEquals(expected, map);
        assertEquals(3, map.size());
        assertEquals(8, map.get("a"));
        assertSame(JImmutableHashMap.TREE_COLLISION_MAP, ((JImmutableHashMap)map).getCollisionMap());

        //assignAll(Map)
        map = empty;
        Map<String, Integer> expectedMutable = new HashMap<>();
        map = map.assignAll(expectedMutable);
        assertEquals(expectedMutable, map.getMap());
        assertEquals(0, map.size());

        expectedMutable.put("a", 10);
        map = map.assignAll(expectedMutable);
        assertEquals(expectedMutable, map.getMap());
        assertEquals(1, map.size());
        assertEquals(10, map.get("a"));
        assertSame(JImmutableHashMap.TREE_COLLISION_MAP, ((JImmutableHashMap)map).getCollisionMap());

        assertEquals(map, map.assignAll(Collections.<String, Integer>emptyMap()));

        expectedMutable.put("a", 8);
        expectedMutable.put("b", 12);
        expectedMutable.put("c", 14);
        map = map.assignAll(expectedMutable);
        assertEquals(expectedMutable, map.getMap());
        assertEquals(3, map.size());
        assertEquals(8, map.get("a"));
        assertSame(JImmutableHashMap.TREE_COLLISION_MAP, ((JImmutableHashMap)map).getCollisionMap());

    }

    public void testEnumeration()
    {
        JImmutableMap<Integer, Integer> map = JImmutableHashMap.usingList();
        HashMap<Integer, Integer> expected = new HashMap<>();
        for (int i = 0; i < 100000; ++i) {
            map = map.assign(i, 2 * i);
            expected.put(i, 2 * i);
            assertEquals(expected.size(), map.size());
        }
        verifyEnumeration(expected, map);
    }

    public void testHashCollisions()
    {
        ManualHashKey key1 = new ManualHashKey(1000, "a");
        ManualHashKey key2 = new ManualHashKey(1000, "b");
        ManualHashKey key3 = new ManualHashKey(1000, "c");
        JImmutableMap<ManualHashKey, String> map = JImmutableHashMap.usingList();
        map = map.assign(key1, "1").assign(key2, "2").assign(key3, "3");
        assertEquals(3, map.size());
        assertEquals("1", map.get(key1));
        assertEquals("2", map.get(key2));
        assertEquals("3", map.get(key3));
        assertEquals("1", map.getValueOr(key1, "X"));
        assertEquals("2", map.getValueOr(key2, "X"));
        assertEquals("3", map.getValueOr(key3, "X"));
        assertEquals("1", map.find(key1).getValueOr("X"));
        assertEquals("2", map.find(key2).getValueOr("X"));
        assertEquals("3", map.find(key3).getValueOr("X"));
        map = map.delete(key2);
        assertEquals(2, map.size());
        assertEquals("1", map.get(key1));
        assertEquals(null, map.get(key2));
        assertEquals("3", map.get(key3));
        assertEquals("1", map.getValueOr(key1, "X"));
        assertEquals("X", map.getValueOr(key2, "X"));
        assertEquals("3", map.getValueOr(key3, "X"));
        assertEquals("1", map.find(key1).getValueOr("X"));
        assertEquals("X", map.find(key2).getValueOr("X"));
        assertEquals("3", map.find(key3).getValueOr("X"));
        map = map.delete(key1);
        assertEquals(1, map.size());
        assertEquals(null, map.get(key1));
        assertEquals(null, map.get(key2));
        assertEquals("3", map.get(key3));
        assertEquals("X", map.getValueOr(key1, "X"));
        assertEquals("X", map.getValueOr(key2, "X"));
        assertEquals("3", map.getValueOr(key3, "X"));
        assertEquals("X", map.find(key1).getValueOr("X"));
        assertEquals("X", map.find(key2).getValueOr("X"));
        assertEquals("3", map.find(key3).getValueOr("X"));
        map = map.delete(key3);
        assertEquals(0, map.size());
        assertEquals(null, map.get(key1));
        assertEquals(null, map.get(key2));
        assertEquals(null, map.get(key3));
        assertEquals("X", map.getValueOr(key1, "X"));
        assertEquals("X", map.getValueOr(key2, "X"));
        assertEquals("X", map.getValueOr(key3, "X"));
        assertEquals("X", map.find(key1).getValueOr("X"));
        assertEquals("X", map.find(key2).getValueOr("X"));
        assertEquals("X", map.find(key3).getValueOr("X"));
        assertSame(JImmutableHashMap.of(), map);
    }

    public void testTransformSelection()
    {
        assertSame(JImmutableHashMap.LIST_EMPTY, JImmutableHashMap.forKey(new Object()));
        assertSame(JImmutableHashMap.TREE_EMPTY, JImmutableHashMap.forKey(100));
        assertSame(JImmutableHashMap.TREE_EMPTY, JImmutableHashMap.forKey("testing"));
    }

    public void testStreams()
    {
        final EmptyHashMap<Integer, Integer> hashMap = JImmutableHashMap.of();
        assertEquals(asList(), hashMap.stream().collect(Collectors.toList()));
        assertEquals(asList(MapEntry.of(1, 10)), hashMap.assign(1, 10).stream().collect(Collectors.toList()));
        assertEquals(asList(MapEntry.of(1, 10), MapEntry.of(4, 40)), hashMap.assign(1, 10).assign(4, 40).stream().collect(Collectors.toList()));

        assertEquals(asList(), hashMap.keys().stream().collect(Collectors.toList()));
        assertEquals(asList(1), hashMap.assign(1, 10).keys().stream().collect(Collectors.toList()));
        assertEquals(asList(1, 4), hashMap.assign(1, 10).assign(4, 40).keys().stream().collect(Collectors.toList()));

        assertEquals(asList(), hashMap.keys().stream().collect(Collectors.toList()));
        assertEquals(asList(10), hashMap.assign(1, 10).values().stream().collect(Collectors.toList()));
        assertEquals(asList(10, 40), hashMap.assign(1, 10).assign(4, 40).values().stream().collect(Collectors.toList()));
    }

    public void testSerialization()
        throws Exception
    {
        final Func1<Object, Iterator> iteratorFactory = a -> ((JImmutableMap)a).iterator();
        final JImmutableMap<Integer, String> empty = JImmutableHashMap.of();
        StandardSerializableTests.verifySerializable(iteratorFactory, null, empty,
                                                     "H4sIAAAAAAAAAFvzloG1uIjBMb8oXS8rsSwzN7e0JDEpJ1UvOT8nJzW5JDM/r1ivOLUoMzEnsyoRxNXz8oQp8kgszvBNLAgoyq+o/A8C/1SMeRgYKooYXEkwzzGpuKQoMbkEYS42MwvKORgYmF8yAEEFAO752S21AAAA");
        StandardSerializableTests.verifySerializable(iteratorFactory, null, empty.insert(MapEntry.of(1, "a")),
                                                     "H4sIAAAAAAAAAFvzloG1uIjBMb8oXS8rsSwzN7e0JDEpJ1UvOT8nJzW5JDM/r1ivOLUoMzEnsyoRxNXz8oQp8kgszvBNLAgoyq+o/A8C/1SMeRgYKooYXEkwzzGpuKQoMbkEYS42MwvKORgYmF8yMDAwAt0rCDQ7US8nMS9dzzOvJDU9tUjo0YIl3xvbLZgYGD0ZWMsSc0pTge4QQKjzK81NSi1qWzNVlnvKg24mkJEgw0oYGBMrAIbHHPIGAQAA");
        StandardSerializableTests.verifySerializable(iteratorFactory, null, empty.insertAll(asList(MapEntry.of(Integer.MIN_VALUE, "a"), MapEntry.of(1, "b"), MapEntry.of(Integer.MAX_VALUE, "c"))),
                                                     "H4sIAAAAAAAAAFvzloG1uIjBMb8oXS8rsSwzN7e0JDEpJ1UvOT8nJzW5JDM/r1ivOLUoMzEnsyoRxNXz8oQp8kgszvBNLAgoyq+o/A8C/1SMeRgYKooYXEkwzzGpuKQoMbkEYS42MwvKORgYmF8yAAmgewWBZifq5STmpet55pWkpqcWCT1asOR7Y7sFEwOjJwNrWWJOaSrQHQIIdX6luUmpRW1rpspyT3nQzQQysgFoWgkDY2JxIUMdAzOQwwjkJUF49UCrgbzkCgB7GrcPIgEAAA==");
    }

    private static class ManualHashKey
        implements Comparable<ManualHashKey>
    {
        private final int hash;
        private final String value;

        private ManualHashKey(int hash,
                              String value)
        {
            this.hash = hash;
            this.value = value;
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof ManualHashKey)) {
                return false;
            }
            ManualHashKey other = (ManualHashKey)o;
            return (other.hash == hash) && other.value.equals(value);
        }

        @Override
        public int compareTo(@Nonnull ManualHashKey o)
        {
            return value.compareTo(o.value);
        }
    }
}
