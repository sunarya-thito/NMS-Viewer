package net.minecraft.util.profiling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodProfilerResultsFilled implements MethodProfilerResults {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final MethodProfilerResult EMPTY = new MethodProfilerResult() {
        @Override
        public long a() {
            return 0L;
        }

        @Override
        public long b() {
            return 0L;
        }

        @Override
        public long c() {
            return 0L;
        }

        @Override
        public Object2LongMap<String> d() {
            return Object2LongMaps.emptyMap();
        }
    };
    private static final Splitter SPLITTER = Splitter.on('\u001e');
    private static final Comparator<Entry<String, MethodProfilerResultsFilled.a>> COUNTER_ENTRY_COMPARATOR = Entry.comparingByValue(Comparator.comparingLong((methodprofilerresultsfilled_a) -> {
        return methodprofilerresultsfilled_a.totalValue;
    })).reversed();
    private final Map<String, ? extends MethodProfilerResult> entries;
    private final long startTimeNano;
    private final int startTimeTicks;
    private final long endTimeNano;
    private final int endTimeTicks;
    private final int tickDuration;

    public MethodProfilerResultsFilled(Map<String, ? extends MethodProfilerResult> map, long i, int j, long k, int l) {
        this.entries = map;
        this.startTimeNano = i;
        this.startTimeTicks = j;
        this.endTimeNano = k;
        this.endTimeTicks = l;
        this.tickDuration = l - j;
    }

    private MethodProfilerResult c(String s) {
        MethodProfilerResult methodprofilerresult = (MethodProfilerResult) this.entries.get(s);

        return methodprofilerresult != null ? methodprofilerresult : MethodProfilerResultsFilled.EMPTY;
    }

    @Override
    public List<MethodProfilerResultsField> a(String s) {
        MethodProfilerResult methodprofilerresult = this.c("root");
        long i = methodprofilerresult.a();
        MethodProfilerResult methodprofilerresult1 = this.c(s);
        long j = methodprofilerresult1.a();
        long k = methodprofilerresult1.c();
        List<MethodProfilerResultsField> list = Lists.newArrayList();

        if (!s.isEmpty()) {
            s = s + "\u001e";
        }

        long l = 0L;
        Iterator iterator = this.entries.keySet().iterator();

        while (iterator.hasNext()) {
            String s1 = (String) iterator.next();

            if (a(s, s1)) {
                l += this.c(s1).a();
            }
        }

        float f = (float) l;

        if (l < j) {
            l = j;
        }

        if (i < l) {
            i = l;
        }

        Iterator iterator1 = this.entries.keySet().iterator();

        while (iterator1.hasNext()) {
            String s2 = (String) iterator1.next();

            if (a(s, s2)) {
                MethodProfilerResult methodprofilerresult2 = this.c(s2);
                long i1 = methodprofilerresult2.a();
                double d0 = (double) i1 * 100.0D / (double) l;
                double d1 = (double) i1 * 100.0D / (double) i;
                String s3 = s2.substring(s.length());

                list.add(new MethodProfilerResultsField(s3, d0, d1, methodprofilerresult2.c()));
            }
        }

        if ((float) l > f) {
            list.add(new MethodProfilerResultsField("unspecified", (double) ((float) l - f) * 100.0D / (double) l, (double) ((float) l - f) * 100.0D / (double) i, k));
        }

        Collections.sort(list);
        list.add(0, new MethodProfilerResultsField(s, 100.0D, (double) l * 100.0D / (double) i, k));
        return list;
    }

    private static boolean a(String s, String s1) {
        return s1.length() > s.length() && s1.startsWith(s) && s1.indexOf(30, s.length() + 1) < 0;
    }

    private Map<String, MethodProfilerResultsFilled.a> h() {
        Map<String, MethodProfilerResultsFilled.a> map = Maps.newTreeMap();

        this.entries.forEach((s, methodprofilerresult) -> {
            Object2LongMap<String> object2longmap = methodprofilerresult.d();

            if (!object2longmap.isEmpty()) {
                List<String> list = MethodProfilerResultsFilled.SPLITTER.splitToList(s);

                object2longmap.forEach((s1, olong) -> {
                    ((MethodProfilerResultsFilled.a) map.computeIfAbsent(s1, (s2) -> {
                        return new MethodProfilerResultsFilled.a();
                    })).a(list.iterator(), olong);
                });
            }

        });
        return map;
    }

    @Override
    public long a() {
        return this.startTimeNano;
    }

    @Override
    public int b() {
        return this.startTimeTicks;
    }

    @Override
    public long c() {
        return this.endTimeNano;
    }

    @Override
    public int d() {
        return this.endTimeTicks;
    }

    @Override
    public boolean a(Path path) {
        BufferedWriter bufferedwriter = null;

        boolean flag;

        try {
            Files.createDirectories(path.getParent());
            bufferedwriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            bufferedwriter.write(this.a(this.g(), this.f()));
            boolean flag1 = true;

            return flag1;
        } catch (Throwable throwable) {
            MethodProfilerResultsFilled.LOGGER.error("Could not save profiler results to {}", path, throwable);
            flag = false;
        } finally {
            IOUtils.closeQuietly(bufferedwriter);
        }

        return flag;
    }

    protected String a(long i, int j) {
        StringBuilder stringbuilder = new StringBuilder();

        stringbuilder.append("---- Minecraft Profiler Results ----\n");
        stringbuilder.append("// ");
        stringbuilder.append(i());
        stringbuilder.append("\n\n");
        stringbuilder.append("Version: ").append(SharedConstants.getGameVersion().getId()).append('\n');
        stringbuilder.append("Time span: ").append(i / 1000000L).append(" ms\n");
        stringbuilder.append("Tick span: ").append(j).append(" ticks\n");
        stringbuilder.append("// This is approximately ").append(String.format(Locale.ROOT, "%.2f", (float) j / ((float) i / 1.0E9F))).append(" ticks per second. It should be ").append(20).append(" ticks per second\n\n");
        stringbuilder.append("--- BEGIN PROFILE DUMP ---\n\n");
        this.a(0, "root", stringbuilder);
        stringbuilder.append("--- END PROFILE DUMP ---\n\n");
        Map<String, MethodProfilerResultsFilled.a> map = this.h();

        if (!map.isEmpty()) {
            stringbuilder.append("--- BEGIN COUNTER DUMP ---\n\n");
            this.a(map, stringbuilder, j);
            stringbuilder.append("--- END COUNTER DUMP ---\n\n");
        }

        return stringbuilder.toString();
    }

    @Override
    public String e() {
        StringBuilder stringbuilder = new StringBuilder();

        this.a(0, "root", stringbuilder);
        return stringbuilder.toString();
    }

    private static StringBuilder a(StringBuilder stringbuilder, int i) {
        stringbuilder.append(String.format("[%02d] ", i));

        for (int j = 0; j < i; ++j) {
            stringbuilder.append("|   ");
        }

        return stringbuilder;
    }

    private void a(int i, String s, StringBuilder stringbuilder) {
        List<MethodProfilerResultsField> list = this.a(s);
        Object2LongMap<String> object2longmap = ((MethodProfilerResult) ObjectUtils.firstNonNull(new MethodProfilerResult[]{(MethodProfilerResult) this.entries.get(s), MethodProfilerResultsFilled.EMPTY})).d();

        object2longmap.forEach((s1, olong) -> {
            a(stringbuilder, i).append('#').append(s1).append(' ').append(olong).append('/').append(olong / (long) this.tickDuration).append('\n');
        });
        if (list.size() >= 3) {
            for (int j = 1; j < list.size(); ++j) {
                MethodProfilerResultsField methodprofilerresultsfield = (MethodProfilerResultsField) list.get(j);

                a(stringbuilder, i).append(methodprofilerresultsfield.name).append('(').append(methodprofilerresultsfield.count).append('/').append(String.format(Locale.ROOT, "%.0f", (float) methodprofilerresultsfield.count / (float) this.tickDuration)).append(')').append(" - ").append(String.format(Locale.ROOT, "%.2f", methodprofilerresultsfield.percentage)).append("%/").append(String.format(Locale.ROOT, "%.2f", methodprofilerresultsfield.globalPercentage)).append("%\n");
                if (!"unspecified".equals(methodprofilerresultsfield.name)) {
                    try {
                        this.a(i + 1, s + "\u001e" + methodprofilerresultsfield.name, stringbuilder);
                    } catch (Exception exception) {
                        stringbuilder.append("[[ EXCEPTION ").append(exception).append(" ]]");
                    }
                }
            }

        }
    }

    private void a(int i, String s, MethodProfilerResultsFilled.a methodprofilerresultsfilled_a, int j, StringBuilder stringbuilder) {
        a(stringbuilder, i).append(s).append(" total:").append(methodprofilerresultsfilled_a.selfValue).append('/').append(methodprofilerresultsfilled_a.totalValue).append(" average: ").append(methodprofilerresultsfilled_a.selfValue / (long) j).append('/').append(methodprofilerresultsfilled_a.totalValue / (long) j).append('\n');
        methodprofilerresultsfilled_a.children.entrySet().stream().sorted(MethodProfilerResultsFilled.COUNTER_ENTRY_COMPARATOR).forEach((entry) -> {
            this.a(i + 1, (String) entry.getKey(), (MethodProfilerResultsFilled.a) entry.getValue(), j, stringbuilder);
        });
    }

    private void a(Map<String, MethodProfilerResultsFilled.a> map, StringBuilder stringbuilder, int i) {
        map.forEach((s, methodprofilerresultsfilled_a) -> {
            stringbuilder.append("-- Counter: ").append(s).append(" --\n");
            this.a(0, "root", (MethodProfilerResultsFilled.a) methodprofilerresultsfilled_a.children.get("root"), i, stringbuilder);
            stringbuilder.append("\n\n");
        });
    }

    private static String i() {
        String[] astring = new String[]{"Shiny numbers!", "Am I not running fast enough? :(", "I'm working as hard as I can!", "Will I ever be good enough for you? :(", "Speedy. Zoooooom!", "Hello world", "40% better than a crash report.", "Now with extra numbers", "Now with less numbers", "Now with the same numbers", "You should add flames to things, it makes them go faster!", "Do you feel the need for... optimization?", "*cracks redstone whip*", "Maybe if you treated it better then it'll have more motivation to work faster! Poor server."};

        try {
            return astring[(int) (SystemUtils.getMonotonicNanos() % (long) astring.length)];
        } catch (Throwable throwable) {
            return "Witty comment unavailable :(";
        }
    }

    @Override
    public int f() {
        return this.tickDuration;
    }

    private static class a {

        long selfValue;
        long totalValue;
        final Map<String, MethodProfilerResultsFilled.a> children = Maps.newHashMap();

        a() {}

        public void a(Iterator<String> iterator, long i) {
            this.totalValue += i;
            if (!iterator.hasNext()) {
                this.selfValue += i;
            } else {
                ((MethodProfilerResultsFilled.a) this.children.computeIfAbsent((String) iterator.next(), (s) -> {
                    return new MethodProfilerResultsFilled.a();
                })).a(iterator, i);
            }

        }
    }
}
