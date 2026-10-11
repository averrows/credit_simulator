package com.bcadigital.creditsimulator.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal JSON parser for flat objects: the web-service payload and saved sheets.
 *
 * <p>Supports exactly what the loader and sheet store need: one object, or a
 * top-level array of objects, with string keys and string/number/boolean/null
 * values, arbitrary whitespace, and standard string escapes. Anything else (nested
 * values, truncation) fails fast with {@link IllegalArgumentException} so callers
 * can fall back gracefully. No external dependency on purpose: the test mandates no frameworks
 * beyond a web-services library, and the JDK HTTP client plus this parser mean
 * we need none at all.
 */
public final class MiniJson {

    private final String text;
    private int position;

    private MiniJson(final String text) {
        this.text = text;
    }

    /**
     * Parses a JSON object into string values (numbers kept in textual form).
     *
     * @param json the JSON text; never null
     * @return key to raw value, never null
     * @throws IllegalArgumentException when the text is not a flat JSON object
     */
    public static Map<String, String> parseObject(final String json) {
        Objects.requireNonNull(json, "json");
        final MiniJson parser = new MiniJson(json);
        parser.skipWhitespace();
        final Map<String, String> values = parser.readObject();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("JSON tidak valid: teks berlebih di akhir");
        }
        return values;
    }

    /**
     * Parses a JSON array of flat objects, e.g. the saved sheets file.
     *
     * @param json the JSON text; never null
     * @return one key-to-raw-value map per element, in order, never null
     * @throws IllegalArgumentException when the text is not an array of flat objects
     */
    public static List<Map<String, String>> parseObjectArray(final String json) {
        Objects.requireNonNull(json, "json");
        final MiniJson parser = new MiniJson(json);
        parser.skipWhitespace();
        parser.expect('[');
        final List<Map<String, String>> objects = new ArrayList<>();
        parser.skipWhitespace();
        if (parser.peek() == ']') {
            parser.position++;
        } else {
            while (true) {
                parser.skipWhitespace();
                objects.add(parser.readObject());
                parser.skipWhitespace();
                final char next = parser.peek();
                parser.position++;
                if (next == ']') {
                    break;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("JSON tidak valid pada posisi " + (parser.position - 1));
                }
            }
        }
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("JSON tidak valid: teks berlebih di akhir");
        }
        return objects;
    }

    private Map<String, String> readObject() {
        expect('{');
        final Map<String, String> values = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            position++;
            return values;
        }
        while (true) {
            skipWhitespace();
            final String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            values.put(key, readValue());
            skipWhitespace();
            final char next = peek();
            if (next == ',') {
                position++;
            } else if (next == '}') {
                position++;
                return values;
            } else {
                throw new IllegalArgumentException("JSON tidak valid pada posisi " + position);
            }
        }
    }

    private String readValue() {
        final char next = peek();
        if (next == '"') {
            return readString();
        }
        if (next == '-' || (next >= '0' && next <= '9')) {
            return readNumber();
        }
        if (text.startsWith("true", position)) {
            position += 4;
            return "true";
        }
        if (text.startsWith("false", position)) {
            position += 5;
            return "false";
        }
        if (text.startsWith("null", position)) {
            position += 4;
            return "null";
        }
        throw new IllegalArgumentException("JSON tidak valid pada posisi " + position);
    }

    private String readNumber() {
        final int start = position;
        while (!atEnd() && "-+0123456789.eE".indexOf(text.charAt(position)) >= 0) {
            position++;
        }
        if (start == position) {
            throw new IllegalArgumentException("JSON tidak valid pada posisi " + position);
        }
        return text.substring(start, position);
    }

    private String readString() {
        expect('"');
        final StringBuilder value = new StringBuilder();
        while (true) {
            if (atEnd()) {
                throw new IllegalArgumentException("JSON tidak valid: string tak berakhir");
            }
            final char current = text.charAt(position++);
            if (current == '"') {
                return value.toString();
            }
            if (current == '\\') {
                if (atEnd()) {
                    throw new IllegalArgumentException("JSON tidak valid: escape tak berakhir");
                }
                value.append(readEscape());
            } else {
                value.append(current);
            }
        }
    }

    private char readEscape() {
        final char escaped = text.charAt(position++);
        switch (escaped) {
            case '"':
            case '\\':
            case '/':
                return escaped;
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'u':
                if (position + 4 > text.length()) {
                    throw new IllegalArgumentException("JSON tidak valid: escape unicode pendek");
                }
                try {
                    final char code = (char) Integer.parseInt(text.substring(position, position + 4), 16);
                    position += 4;
                    return code;
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("JSON tidak valid: escape unicode salah");
                }
            default:
                throw new IllegalArgumentException("JSON tidak valid: escape tak dikenal \\" + escaped);
        }
    }

    private void skipWhitespace() {
        while (!atEnd()) {
            final char current = text.charAt(position);
            if (current == ' ' || current == '\t' || current == '\n' || current == '\r') {
                position++;
            } else {
                return;
            }
        }
    }

    private void expect(final char wanted) {
        if (atEnd() || text.charAt(position) != wanted) {
            throw new IllegalArgumentException("JSON tidak valid pada posisi " + position);
        }
        position++;
    }

    private char peek() {
        if (atEnd()) {
            throw new IllegalArgumentException("JSON tidak valid: akhir teks tak terduga");
        }
        return text.charAt(position);
    }

    private boolean atEnd() {
        return position >= text.length();
    }
}
