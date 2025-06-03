// File: src/main/java/com/example/json5/Json5Sanitizer.java
package com.redhat.devtools.lsp4ij.templates.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Json5Sanitizer {

    public static JsonElement parse(Path filePath) throws IOException {
        String raw = Files.readString(filePath);
        String cleaned = stripCommentsAndTrailingCommas(raw);
        return new Gson().fromJson(cleaned, JsonElement.class);
    }

    public static String stripCommentsAndTrailingCommas(String input) {
        StringBuilder output = new StringBuilder();
        boolean inString = false;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        char stringDelimiter = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            char next = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';

            if (inSingleLineComment) {
                if (c == '\n') {
                    inSingleLineComment = false;
                    output.append(c);
                }
                continue;
            }

            if (inMultiLineComment) {
                if (c == '*' && next == '/') {
                    inMultiLineComment = false;
                    i++; // skip '/'
                }
                continue;
            }

            if (inString) {
                output.append(c);
                if (c == '\\') {
                    if (i + 1 < input.length()) {
                        output.append(input.charAt(i + 1));
                        i++;
                    }
                } else if (c == stringDelimiter) {
                    inString = false;
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                stringDelimiter = c;
                output.append(c);
                continue;
            }

            if (c == '/' && next == '/') {
                inSingleLineComment = true;
                i++;
                continue;
            }

            if (c == '/' && next == '*') {
                inMultiLineComment = true;
                i++;
                continue;
            }

            output.append(c);
        }

        return removeTrailingCommas(output.toString());
    }

    private static String removeTrailingCommas(String input) {
        StringBuilder out = new StringBuilder();
        boolean inString = false;
        char stringDelimiter = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inString) {
                out.append(c);
                if (c == '\\') {
                    if (i + 1 < input.length()) {
                        out.append(input.charAt(i + 1));
                        i++;
                    }
                } else if (c == stringDelimiter) {
                    inString = false;
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                inString = true;
                stringDelimiter = c;
                out.append(c);
                continue;
            }

            if (c == ',' && i + 1 < input.length()) {
                int j = i + 1;
                while (j < input.length() && Character.isWhitespace(input.charAt(j))) {
                    j++;
                }
                if (j < input.length()) {
                    char next = input.charAt(j);
                    if (next == '}' || next == ']') {
                        continue; // skip the comma
                    }
                }
            }

            out.append(c);
        }

        return out.toString();
    }
}
