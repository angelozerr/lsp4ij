// File: src/test/java/com/example/json5/Json5SanitizerTest.java
package com.redhat.devtools.lsp4ij.internal.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.redhat.devtools.lsp4ij.templates.json.Json5Sanitizer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class Json5SanitizerTest {

    private JsonElement parse(String content) throws IOException {
        Path tmp = Files.createTempFile("test", ".json5");
        Files.writeString(tmp, content);
        JsonElement element = Json5Sanitizer.parse(tmp);
        Files.delete(tmp);
        return element;
    }

    @Test
    void testSimpleObject() throws IOException {
        String json5 = """
            {
              // commentaire ligne
              "key": "value", // trailing comment
            }
        """;
        JsonObject obj = parse(json5).getAsJsonObject();
        assertEquals("value", obj.get("key").getAsString());
    }

    @Test
    void testArrayWithTrailingCommaAndComments() throws IOException {
        String json5 = """
            [
              1, // comment
              2,
              3, 
            ]
        """;
        JsonArray arr = parse(json5).getAsJsonArray();
        assertEquals(3, arr.size());
        assertEquals(1, arr.get(0).getAsInt());
        assertEquals(2, arr.get(1).getAsInt());
        assertEquals(3, arr.get(2).getAsInt());
    }

    @Test
    void testMultilineComment() throws IOException {
        String json5 = """
            {
              /* comment */
              "foo": 42 /* another */
            }
        """;
        JsonObject obj = parse(json5).getAsJsonObject();
        assertEquals(42, obj.get("foo").getAsInt());
    }

    @Test
    void testStringWithCommentLikeContent() throws IOException {
        String json5 = """
            {
              "text": "this is not // a comment",
              "more": "nor is /* this */"
            }
        """;
        JsonObject obj = parse(json5).getAsJsonObject();
        assertEquals("this is not // a comment", obj.get("text").getAsString());
        assertEquals("nor is /* this */", obj.get("more").getAsString());
    }

    @Test
    void testLiteralRootValue() throws IOException {
        assertTrue(parse("123").isJsonPrimitive());
        assertTrue(parse("true").getAsBoolean());
        assertEquals("hello", parse("\"hello\"").getAsString());
    }

    @Test
    void testEmptyObjectWithComment() throws IOException {
        String json5 = """
            {
              // empty object with comment
            }
        """;
        JsonObject obj = parse(json5).getAsJsonObject();
        assertTrue(obj.entrySet().isEmpty());
    }

    @Test
    void testCommaBeforeEndOfObjectIsRemoved() throws IOException {
        String json5 = """
            {
              "a": 1,
              "b": 2,
            }
        """;
        JsonObject obj = parse(json5).getAsJsonObject();
        assertEquals(2, obj.get("b").getAsInt());
    }

    @Test
    void testCommaBeforeEndOfArrayIsRemoved() throws IOException {
        String json5 = """
            ["one", "two", ]
        """;
        JsonArray arr = parse(json5).getAsJsonArray();
        assertEquals(2, arr.size());
    }

    @Test
    void testEscapedQuotesInString() throws IOException {
        String json5 = """
            {
              "quote": "\"quoted\" text"
            }
        """;
        JsonObject obj = parse(json5).getAsJsonObject();
        assertEquals("\"quoted\" text", obj.get("quote").getAsString());
    }
}
