package net.minecraft.util;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringEscapeUtils;

public class CSVWriter {

    private static final String LINE_SEPARATOR = "\r\n";
    private static final String FIELD_SEPARATOR = ",";
    private final Writer output;
    private final int columnCount;

    CSVWriter(Writer writer, List<String> list) throws IOException {
        this.output = writer;
        this.columnCount = list.size();
        this.a(list.stream());
    }

    public static CSVWriter.a a() {
        return new CSVWriter.a();
    }

    public void a(Object... aobject) throws IOException {
        if (aobject.length != this.columnCount) {
            throw new IllegalArgumentException("Invalid number of columns, expected " + this.columnCount + ", but got " + aobject.length);
        } else {
            this.a(Stream.of(aobject));
        }
    }

    private void a(Stream<?> stream) throws IOException {
        Writer writer = this.output;
        Stream stream1 = stream.map(CSVWriter::a);

        writer.write((String) stream1.collect(Collectors.joining(",")) + "\r\n");
    }

    private static String a(@Nullable Object object) {
        return StringEscapeUtils.escapeCsv(object != null ? object.toString() : "[null]");
    }

    public static class a {

        private final List<String> headers = Lists.newArrayList();

        public a() {}

        public CSVWriter.a a(String s) {
            this.headers.add(s);
            return this;
        }

        public CSVWriter a(Writer writer) throws IOException {
            return new CSVWriter(writer, this.headers);
        }
    }
}
