package com.redhat.devtools.lsp4ij.features.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PathPatternMatcherTest {

    @Test
    @DisplayName("Pattern with Windows absolute path and glob")
    void testWindowsAbsolutePathWithGlob() {
        String pattern = "C:\\Users\\XXX\\IdeaProjects\\test-rust/**/*.rs";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.rs", matcher.getPattern());
        assertNotNull(matcher.getBasePath());
        assertEquals(Paths.get("C:/Users/XXX/IdeaProjects/test-rust"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with Unix absolute path and glob")
    void testUnixAbsolutePathWithGlob() {
        String pattern = "/home/user/project/**/*.java";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.java", matcher.getPattern());
        assertNotNull(matcher.getBasePath());
        assertEquals(Paths.get("/home/user/project"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with relative path and glob")
    void testRelativePathWithGlob() {
        String pattern = "src/main/java/**/*.kt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.kt", matcher.getPattern());
        assertNotNull(matcher.getBasePath());
        assertEquals(Paths.get("src/main/java"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern starting with ** (no base path)")
    void testPatternStartingWithGlob() {
        String pattern = "**/*.txt";
        Path defaultBase = Paths.get("/default/path");
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, defaultBase);
        
        assertEquals("**/*.txt", matcher.getPattern());
        assertEquals(defaultBase, matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern starting with * (no base path)")
    void testPatternStartingWithAsterisk() {
        String pattern = "*.rs";
        Path defaultBase = Paths.get("/default");
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, defaultBase);
        
        assertEquals("*.rs", matcher.getPattern());
        assertEquals(defaultBase, matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with single level wildcard")
    void testSingleLevelWildcard() {
        String pattern = "/usr/local/bin/*.sh";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("*.sh", matcher.getPattern());
        assertEquals(Paths.get("/usr/local/bin"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with question mark wildcard")
    void testQuestionMarkWildcard() {
        String pattern = "C:\\test\\folder\\file?.txt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("file?.txt", matcher.getPattern());
        assertEquals(Paths.get("C:/test/folder"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with bracket wildcard")
    void testBracketWildcard() {
        String pattern = "/var/log/app[123].log";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("app[123].log", matcher.getPattern());
        assertEquals(Paths.get("/var/log"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with brace wildcard")
    void testBraceWildcard() {
        String pattern = "/etc/config/{prod,dev,test}.yml";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("{prod,dev,test}.yml", matcher.getPattern());
        assertEquals(Paths.get("/etc/config"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with multiple glob types")
    void testMultipleGlobTypes() {
        String pattern = "C:\\projects\\myapp\\**/*.{java,kt,rs}";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.{java,kt,rs}", matcher.getPattern());
        assertEquals(Paths.get("C:/projects/myapp"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with mixed separators (backslash and forward slash)")
    void testMixedSeparators() {
        String pattern = "C:\\Users/XXX/project\\src/**/*.java";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.java", matcher.getPattern());
        assertNotNull(matcher.getBasePath());
        // Should handle mixed separators
    }

    @Test
    @DisplayName("Pattern with no glob characters")
    void testNoGlobCharacters() {
        String pattern = "/home/user/file.txt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("/home/user/file.txt", matcher.getPattern());
        assertNull(matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with glob at the very beginning")
    void testGlobAtBeginning() {
        String pattern = "**/src/main/**/*.java";
        Path defaultBase = Paths.get("/project");
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, defaultBase);
        
        assertEquals("**/src/main/**/*.java", matcher.getPattern());
        assertEquals(defaultBase, matcher.getBasePath());
    }

    @Test
    @DisplayName("Empty pattern")
    void testEmptyPattern() {
        String pattern = "";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("", matcher.getPattern());
        assertNull(matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with only separator and glob")
    void testOnlySeparatorAndGlob() {
        String pattern = "/*.txt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("*.txt", matcher.getPattern());
        // Root path "/" is extracted but might be null depending on implementation
    }

    @Test
    @DisplayName("Deep nested path with glob")
    void testDeepNestedPath() {
        String pattern = "/a/b/c/d/e/f/g/h/**/*.xml";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.xml", matcher.getPattern());
        assertEquals(Paths.get("/a/b/c/d/e/f/g/h"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Windows drive letter only")
    void testWindowsDriveLetterOnly() {
        String pattern = "C:/**/*.dll";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.dll", matcher.getPattern());
        // C: might result in special handling
    }

    @Test
    @DisplayName("Pattern with trailing slash before glob")
    void testTrailingSlashBeforeGlob() {
        String pattern = "/home/user/project//**/*.js";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.js", matcher.getPattern());
        assertEquals(Paths.get("/home/user/project"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with UNC path (Windows network)")
    void testUNCPath() {
        String pattern = "\\\\server\\share\\folder/**/*.doc";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.doc", matcher.getPattern());
        assertNotNull(matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with only filename glob")
    void testOnlyFilenameGlob() {
        String pattern = "test*.java";
        Path defaultBase = Paths.get("/src");
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, defaultBase);
        
        assertEquals("test*.java", matcher.getPattern());
        assertEquals(defaultBase, matcher.getBasePath());
    }

    @Test
    @DisplayName("Default base path is used when pattern has no path component")
    void testDefaultBasePathUsed() {
        String pattern = "*.rs";
        Path defaultBase = Paths.get("/my/default/path");
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, defaultBase);
        
        assertNotNull(matcher.getBasePath());
        assertEquals(defaultBase, matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with consecutive asterisks")
    void testConsecutiveAsterisks() {
        String pattern = "/path/to/***/*.txt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("***/*.txt", matcher.getPattern());
        assertEquals(Paths.get("/path/to"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with dot in directory name")
    void testDotInDirectoryName() {
        String pattern = "/home/user/.config/**/*.conf";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.conf", matcher.getPattern());
        assertEquals(Paths.get("/home/user/.config"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with spaces in path")
    void testSpacesInPath() {
        String pattern = "/home/my documents/project/**/*.pdf";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.pdf", matcher.getPattern());
        assertEquals(Paths.get("/home/my documents/project"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with Unicode characters in path")
    void testUnicodeInPath() {
        String pattern = "/home/utilisateur/projét/**/*.txt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.txt", matcher.getPattern());
        assertEquals(Paths.get("/home/utilisateur/projét"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Complex real-world pattern - Rust project")
    void testRealWorldRustProject() {
        String pattern = "C:\\Users\\Developer\\RustProjects\\my-app\\src/**/*.rs";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.rs", matcher.getPattern());
        assertEquals(Paths.get("C:/Users/Developer/RustProjects/my-app/src"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Complex real-world pattern - Java Maven project")
    void testRealWorldJavaMavenProject() {
        String pattern = "/home/dev/projects/my-service/src/main/java/**/*.java";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.java", matcher.getPattern());
        assertEquals(Paths.get("/home/dev/projects/my-service/src/main/java"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Complex real-world pattern - Node.js project")
    void testRealWorldNodeJsProject() {
        String pattern = "D:\\workspace\\my-node-app\\src\\**\\*.{ts,js}";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**\\*.{ts,js}", matcher.getPattern());
        assertEquals(Paths.get("D:/workspace/my-node-app/src"), matcher.getBasePath());
    }

    @Test
    @DisplayName("Pattern with multiple consecutive separators")
    void testMultipleConsecutiveSeparators() {
        String pattern = "/home//user///project/**/*.txt";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.txt", matcher.getPattern());
        // Should handle multiple separators gracefully
    }

    @Test
    @DisplayName("Null default base path")
    void testNullDefaultBasePath() {
        String pattern = "**/*.java";
        PathPatternMatcher matcher = PathPatternMatcher.fromPattern(pattern, null);
        
        assertEquals("**/*.java", matcher.getPattern());
        assertNull(matcher.getBasePath());
    }
}