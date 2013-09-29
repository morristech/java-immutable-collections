///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2013, Burton Computer Corporation
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

package org.javimmutable.collections.util;

import org.javimmutable.collections.PersistentList;
import org.javimmutable.collections.PersistentMap;
import org.javimmutable.collections.PersistentRandomAccessList;
import org.javimmutable.collections.PersistentSet;
import org.javimmutable.collections.PersistentStack;
import org.javimmutable.collections.Sequence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Test program to run an infinite loop feeding data to a PersistentMap, querying the
 * data, and deleting the data to verify the map always contains what it should.
 */
public class RandomLoop
{
    private class MapFactory
    {
        private int count;

        private PersistentMap<String, String> createMap()
        {
            count += 1;
            if (count % 2 == 0) {
                return Immutables.map();
            } else {
                return Immutables.sortedMap();
            }
        }
    }

    public void execute(String[] filenames)
            throws Exception
    {
        MapFactory factory = new MapFactory();
        long seed = System.currentTimeMillis();
        System.out.printf("Starting with initial seed %d%n", seed);
        Random random = new Random(seed);
        PersistentList<String> tokens = loadTokens(filenames);
        System.out.printf("Loaded %d tokens from %d files%n", tokens.size(), filenames.length);
        //noinspection InfiniteLoopStatement
        while (true) {
            testStack(random);
            testList(random);
            testRandomAccessList(random);
            testSets(tokens, random);
            testMaps(factory, tokens, random);
        }
    }

    public static void main(String[] argv)
            throws Exception
    {
        new RandomLoop().execute(argv);
    }

    private void testStack(Random random)
    {
        PersistentStack<Integer> stack = Immutables.stack();
        LinkedList<Integer> expected = new LinkedList<Integer>();
        int size = random.nextInt(1000);
        System.out.printf("Testing PersistentStack of size %d%n", size);
        for (int i = 0; i < size; ++i) {
            int value = random.nextInt(999999999);
            stack = stack.insert(value);
            expected.add(0, value);
        }
        Sequence<Integer> seq = stack;
        for (Integer value : expected) {
            if (!value.equals(seq.getHead())) {
                throw new RuntimeException(String.format("found mismatch expected %d found %d", value, seq.getHead()));
            }
            seq = seq.getTail();
        }
        if (!seq.isEmpty()) {
            throw new RuntimeException("expected to be at end of stack but found more values");
        }
        System.out.println("PersistentStack test completed without errors");
    }

    private void testList(Random random)
    {
        PersistentList<Integer> list = Immutables.list();
        ArrayList<Integer> expected = new ArrayList<Integer>();
        int size = random.nextInt(10000);
        System.out.printf("Testing PersistentList of size %d%n", size);

        for (int loops = 1; loops <= 6; ++loops) {
            System.out.printf("growing %d%n", list.size());
            for (int i = 0; i < size / 3; ++i) {
                int value = random.nextInt(999999999);
                list = list.insert(value);
                expected.add(value);
            }
            verifyContents(expected, list);
            System.out.printf("shrinking %d%n", list.size());
            for (int i = 0; i < size / 6; ++i) {
                list = list.deleteLast();
                expected.remove(expected.size() - 1);
            }
            verifyContents(expected, list);
        }
        System.out.printf("cleanup %d%n", expected.size());
        while (list.size() > 0) {
            list = list.deleteLast();
            expected.remove(expected.size() - 1);
        }
        verifyContents(expected, list);
        System.out.println("PersistentList test completed without errors");
    }

    private void testRandomAccessList(Random random)
    {
        PersistentRandomAccessList<Integer> list = Immutables.ralist();
        ArrayList<Integer> expected = new ArrayList<Integer>();
        int size = random.nextInt(10000);
        System.out.printf("Testing PersistentRandomAccessList of size %d%n", size);

        for (int loops = 1; loops <= 6; ++loops) {
            System.out.printf("growing %d%n", list.size());
            for (int i = 0; i < size / 3; ++i) {
                int value = random.nextInt(999999999);
                if (list.size() == 0) {
                    list = list.insert(value);
                    expected.add(value);
                } else {
                    int index = random.nextInt(list.size());
                    list = list.insert(index, value);
                    expected.add(index, value);
                }
            }
            verifyContents(expected, list);
            System.out.printf("shrinking %d%n", list.size());
            for (int i = 0; i < size / 6; ++i) {
                if (list.size() == 1) {
                    list = list.deleteLast();
                    expected.remove(expected.size() - 1);
                } else {
                    int index = random.nextInt(list.size());
                    list = list.delete(index);
                    expected.remove(index);
                }
            }
            verifyContents(expected, list);
        }
        System.out.printf("cleanup %d%n", expected.size());
        while (list.size() > 0) {
            list = list.delete(0);
            expected.remove(0);
        }
        verifyContents(expected, list);
        System.out.println("PersistentRandomAccessList test completed without errors");
    }

    private void testSets(PersistentList<String> tokens,
                          Random random)
    {
        PersistentSet<String> hset = Immutables.set();
        PersistentSet<String> tset = Immutables.sortedSet();
        Set<String> expected = new HashSet<String>();
        int size = random.nextInt(100000);
        PersistentRandomAccessList<String> values = Immutables.ralist();

        System.out.printf("Testing PersistentSet of size %d%n", size);

        for (int loops = 1; loops <= 6; ++loops) {
            System.out.printf("growing %d%n", hset.size());
            for (int i = 0; i < size / 3; ++i) {
                String value = makeKey(tokens, random);
                values = values.insert(value);
                hset = hset.insert(value);
                tset = tset.insert(value);
                expected.add(value);
            }
            verifyContents(expected, hset);
            verifyContents(expected, tset);
            System.out.printf("shrinking %d%n", hset.size());
            for (int i = 0; i < size / 6; ++i) {
                int keyIndex = random.nextInt(values.size());
                String key = values.get(keyIndex);
                expected.remove(key);
                hset = hset.delete(key);
                tset = tset.delete(key);
                values = values.delete(keyIndex);
            }
            verifyContents(expected, hset);
            verifyContents(expected, tset);
        }
        System.out.printf("cleanup %d%n", expected.size());
        while (values.size() > 0) {
            String value = values.get(0);
            hset = hset.delete(value);
            tset = tset.delete(value);
            expected.remove(value);
            values = values.delete(0);
        }
        verifyContents(expected, hset);
        verifyContents(expected, tset);
        System.out.println("PersistentSet test completed without errors");
    }

    private void testMaps(MapFactory factory,
                          PersistentList<String> tokens,
                          Random random)
            throws Exception
    {
        final int tokenCount = 1 + random.nextInt(100000);
        final List<String> keys = new ArrayList<String>();
        final Map<String, String> expected = new HashMap<String, String>();
        PersistentMap<String, String> map = factory.createMap();
        PersistentRandomAccessList<String> pkeys = Immutables.ralist();
        System.out.printf("starting %s test with %d tokens and factory %s%n", map.getClass().getSimpleName(), tokenCount, map.getClass().getSimpleName());
        for (int loops = 1; loops <= 6; ++loops) {
            System.out.printf("growing %d%n", map.size());
            for (int i = 0; i < tokenCount / 3; ++i) {
                String key = makeKey(tokens, random);
                keys.add(key);
                pkeys = pkeys.insert(key);
                expected.put(key, key);
                map = map.assign(key, key);
            }
            verifyContents(expected, map);
            System.out.printf("shrinking %d%n", map.size());
            for (int i = 0; i < tokenCount / 6; ++i) {
                int keyIndex = random.nextInt(keys.size());
                String key = pkeys.get(keyIndex);
                expected.remove(key);
                map = map.delete(key);
                keys.remove(keyIndex);
                pkeys = pkeys.delete(keyIndex);
            }
            verifyContents(expected, map);
        }
        if (keys.size() != pkeys.size()) {
            throw new RuntimeException(String.format("key size mismatch - expected %d found %d%n", keys.size(), pkeys.size()));
        }
        System.out.printf("comparing %d keys%n", pkeys.size());
        for (int i = 0; i < pkeys.size(); ++i) {
            String key = keys.get(i);
            String pkey = pkeys.get(i);
            if (!key.equals(pkey)) {
                throw new RuntimeException(String.format("key mismatch - expected %s found %s%n", key, pkey));
            }
        }
        System.out.printf("cleanup %d%n", map.size());
        for (String key : keys) {
            expected.remove(key);
            map = map.delete(key);
        }
        if (map.size() != 0) {
            throw new RuntimeException(String.format("expected map to be empty but it contained %d keys%n", map.size()));
        }
        verifyContents(expected, map);
        System.out.printf("completed %s test without errors%n", map.getClass().getSimpleName());
    }

    private void verifyContents(List<Integer> expected,
                                PersistentList<Integer> list)
    {
        System.out.printf("checking contents with size %d%n", list.size());
        if (list.size() != expected.size()) {
            throw new RuntimeException(String.format("size mismatch - expected %d found %d", expected.size(), list.size()));
        }
        int index = 0;
        for (Integer expectedValue : expected) {
            Integer listValue = list.get(index);
            if (!expectedValue.equals(expectedValue)) {
                throw new RuntimeException(String.format("value mismatch - expected %d found %d%n", expectedValue, listValue));
            }
            index += 1;
        }
        index = 0;
        for (Integer listValue : list) {
            Integer expectedValue = expected.get(index);
            if (!expectedValue.equals(expectedValue)) {
                throw new RuntimeException(String.format("value mismatch - expected %d found %d%n", expectedValue, listValue));
            }
            index += 1;
        }
    }

    private void verifyContents(Set<String> expected,
                                PersistentSet<String> set)
    {
        System.out.printf("checking contents with size %d%n", set.size());
        if (set.size() != expected.size()) {
            throw new RuntimeException(String.format("size mismatch - expected %d found %d", expected.size(), set.size()));
        }
        for (String expectedValue : expected) {
            if (!set.contains(expectedValue)) {
                throw new RuntimeException(String.format("value mismatch - expected %s but not in %s%n", expectedValue, set.getClass().getSimpleName()));
            }
        }
        for (String expectedValue : set) {
            if (!expected.contains(expectedValue)) {
                throw new RuntimeException(String.format("value mismatch - expected %s but not in Set%n", expectedValue));
            }
        }
    }

    private void verifyContents(Map<String, String> expected,
                                PersistentMap<String, String> map)
    {
        System.out.printf("checking contents with size %d%n", map.size());
        if (map.size() != expected.size()) {
            throw new RuntimeException(String.format("size mismatch - expected %d found %d", expected.size(), map.size()));
        }
        for (PersistentMap.Entry<String, String> entry : map) {
            String mapValue = map.find(entry.getKey()).getValueOrNull();
            String expectedValue = expected.get(entry.getKey());
            if (!mapValue.equals(expectedValue)) {
                throw new RuntimeException(String.format("value mismatch - expected %s found %s%n", expectedValue, mapValue));
            }
        }
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String mapValue = map.find(entry.getKey()).getValueOrNull();
            String expectedValue = expected.get(entry.getKey());
            if (!mapValue.equals(expectedValue)) {
                throw new RuntimeException(String.format("value mismatch - expected %s found %s%n", expectedValue, mapValue));
            }
        }
    }

    private String makeKey(PersistentList<String> tokens,
                           Random random)
    {
        int length = 1 + random.nextInt(250);
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append(tokens.get(random.nextInt(tokens.size())));
        }
        return sb.toString();
    }

    private PersistentList<String> loadTokens(String[] filenames)
            throws IOException
    {
        PersistentSet<String> tokens = Immutables.set();
        for (String filename : filenames) {
            tokens = addTokensFromFile(tokens, filename);
        }
        return Immutables.list(tokens);
    }

    private PersistentSet<String> addTokensFromFile(PersistentSet<String> tokens,
                                                    String filename)
            throws IOException
    {
        BufferedReader inp = new BufferedReader(new FileReader(filename));
        try {
            for (String line = inp.readLine(); line != null; line = inp.readLine()) {
                StringTokenizer tokenizer = new StringTokenizer(line);
                while (tokenizer.hasMoreTokens()) {
                    tokens = tokens.insert(tokenizer.nextToken());
                }
            }
        } finally {
            inp.close();
        }
        return tokens;
    }
}