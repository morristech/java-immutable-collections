package org.javimmutable.collections.list;

import java.util.ArrayList;
import java.util.List;

public class ListAppendTimingComparison
{
    public static void main(String[] argv)
    {
        Mode mode = (argv.length == 0) ? Mode.OLD : Mode.valueOf(argv[0]);
        final long startMillis = System.currentTimeMillis();
        runTest(mode);
        final long elapsedMillis = System.currentTimeMillis() - startMillis;
        System.out.printf("%s  %d%n", mode, elapsedMillis);
    }

    private enum Mode
    {
        OLD,
        NEW
    }

    private static void runTest(Mode mode)
    {
        for (int loop = 1; loop <= 10_000; ++loop) {
            JImmutableArrayList<Integer> list = JImmutableArrayList.of();
            List<Integer> extras = new ArrayList<>();
            for (int length = 1; length <= 250; ++length) {
                extras.add(length);
                if (mode == Mode.OLD) {
                    list = list.insertAllLast(extras.iterator());
                } else {
                    list = list.insertAllLast(extras);
                }
            }
        }
    }
}
