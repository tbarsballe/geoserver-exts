package com.boundlessgeo.ysld;

import com.boundlessgeo.ysld.parse.ParseException;
import org.yaml.snakeyaml.events.Event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tuple {
    static final Pattern PATTERN = Pattern.compile("\\s*\\(?\\s*(\\d*)\\s*,\\s*(\\d*)\\s*\\)?\\s*");

    public static Tuple parse(String val, Event evt, String msg) {
        Matcher m = Tuple.PATTERN.matcher(val);
        if (!m.matches()) {
            throw new ParseException(msg, evt);
        }

        return new Tuple(!"".equals(m.group(1))?m.group(1):null,
                !"".equals(m.group(2))?m.group(2):null);
    }

    public final String first;
    public final String second;

    public Tuple(String first, String second) {
        this.first = first;
        this.second = second;
    }

    public boolean isNull() {
        return first == null && second == null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        if (first != null) {
            sb.append(first);
        }
        if (second != null) {
            sb.append(",").append(second);
        }
        return sb.append(")").toString();
    }
}
