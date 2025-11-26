package org.seba.agentcli.context;

import org.seba.agentcli.model.ProjectContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads file context incrementally based on relevance
 * Instead of loading all files at startup, loads only what's needed
 */
@Component
public class IncrementalContextLoader {

    private static final int MAX_CONTEXT_SIZE = 100_000; // chars
    private static final int MAX_FILES_IN_CONTEXT = 10;
    private static final int CACHE_SIZE = 50;

    // LRU cache for recently accessed files
    private final LinkedHashMap<String, FileContext> fileCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, FileContext> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    // Patterns to detect file references
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile(
            "([a-zA-Z0-9_/-]+\\.(java|py|js|ts|tsx|jsx|go|rs|cpp|c|cs|xml|json|yaml|yml|md))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^\\s*(?:import|from|require|use)\\s+([a-zA-Z0-9._/]+)",
            Pattern.MULTILINE
    );

    /**
     * Analyzes user query and loads relevant file contexts
     */
    public ContextBundle loadRelevantContext(String userQuery, ProjectContext projectContext) {
        ContextBundle bundle = new ContextBundle();

        if (projectContext == null) {
            return bundle;
        }

        // 1. Extract explicitly mentioned files
        Set<String> mentionedFiles = extractMentionedFiles(userQuery);

        // 2. Find matching files in project
        Set<Path> relevantFiles = new HashSet<>();
        for (String mentioned : mentionedFiles) {
            relevantFiles.addAll(findMatchingFiles(mentioned, projectContext));
        }

        // 3. Analyze query for implicit file needs
        relevantFiles.addAll(findImplicitRelevantFiles(userQuery, projectContext));

        // 4. Load file contents (up to limit)
        int totalSize = 0;
        int fileCount = 0;

        // Prioritize: cache hits first, then newer files
        List<Path> sortedFiles = relevantFiles.stream()
                .sorted((a, b) -> {
                    boolean aInCache = fileCache.containsKey(a.toString());
                    boolean bInCache = fileCache.containsKey(b.toString());
                    if (aInCache && !bInCache) return -1;
                    if (!aInCache && bInCache) return 1;
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());

        for (Path file : sortedFiles) {
            if (fileCount >= MAX_FILES_IN_CONTEXT || totalSize >= MAX_CONTEXT_SIZE) {
                break;
            }

            FileContext context = loadFileContext(file);
            if (context != null && context.content.length() + totalSize <= MAX_CONTEXT_SIZE) {
                bundle.addFile(context);
                totalSize += context.content.length();
                fileCount++;
            }
        }

        // 5. Add related files (imports, dependencies)
        if (fileCount < MAX_FILES_IN_CONTEXT) {
            Set<Path> relatedFiles = findRelatedFiles(relevantFiles, projectContext);
            for (Path file : relatedFiles) {
                if (fileCount >= MAX_FILES_IN_CONTEXT || totalSize >= MAX_CONTEXT_SIZE) {
                    break;
                }

                if (!bundle.hasFile(file)) {
                    FileContext context = loadFileContext(file);
                    if (context != null && context.content.length() + totalSize <= MAX_CONTEXT_SIZE) {
                        bundle.addFile(context);
                        totalSize += context.content.length();
                        fileCount++;
                    }
                }
            }
        }

        return bundle;
    }

    /**
     * Extracts file paths explicitly mentioned in the query
     */
    private Set<String> extractMentionedFiles(String query) {
        Set<String> files = new HashSet<>();
        Matcher matcher = FILE_PATH_PATTERN.matcher(query);

        while (matcher.find()) {
            files.add(matcher.group(1));
        }

        return files;
    }

    /**
     * Finds files in project matching a mentioned name
     */
    private Set<Path> findMatchingFiles(String mentioned, ProjectContext projectContext) {
        Set<Path> matches = new HashSet<>();

        // Try exact match first
        Path exactPath = projectContext.getRootPath().resolve(mentioned);
        if (Files.exists(exactPath)) {
            matches.add(exactPath);
            return matches;
        }

        // Try fuzzy match on source files
        String lowerMentioned = mentioned.toLowerCase();
        for (Path sourceFile : projectContext.getSourceFiles()) {
            String relative = projectContext.getRootPath().relativize(sourceFile).toString();
            if (relative.toLowerCase().contains(lowerMentioned) ||
                sourceFile.getFileName().toString().toLowerCase().contains(lowerMentioned)) {
                matches.add(sourceFile);
            }
        }

        return matches;
    }

    /**
     * Finds files implicitly relevant based on query content
     */
    private Set<Path> findImplicitRelevantFiles(String query, ProjectContext projectContext) {
        Set<Path> relevant = new HashSet<>();
        String lowerQuery = query.toLowerCase();

        // Keywords suggesting what kind of files to load
        Map<String, List<String>> keywordToPatterns = Map.of(
            "test", List.of("test", "spec"),
            "config", List.of("config", "application", "settings"),
            "controller", List.of("controller"),
            "service", List.of("service"),
            "model", List.of("model", "entity", "dto"),
            "main", List.of("main", "app"),
            "pom", List.of("pom.xml"),
            "package.json", List.of("package.json")
        );

        for (Map.Entry<String, List<String>> entry : keywordToPatterns.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                for (String pattern : entry.getValue()) {
                    relevant.addAll(findFilesByPattern(pattern, projectContext));
                }
            }
        }

        return relevant;
    }

    /**
     * Finds files by pattern in their name/path
     */
    private Set<Path> findFilesByPattern(String pattern, ProjectContext projectContext) {
        return projectContext.getSourceFiles().stream()
                .filter(file -> {
                    String path = file.toString().toLowerCase();
                    return path.contains(pattern.toLowerCase());
                })
                .limit(3) // Limit per pattern
                .collect(Collectors.toSet());
    }

    /**
     * Finds files related to the given files (imports, dependencies)
     */
    private Set<Path> findRelatedFiles(Set<Path> files, ProjectContext projectContext) {
        Set<Path> related = new HashSet<>();

        for (Path file : files) {
            try {
                String content = Files.readString(file);
                Set<String> imports = extractImports(content);

                for (String importPath : imports) {
                    Set<Path> matchingFiles = findMatchingFiles(importPath, projectContext);
                    related.addAll(matchingFiles);

                    if (related.size() >= 5) { // Limit related files
                        return related;
                    }
                }
            } catch (IOException e) {
                // Skip files that can't be read
            }
        }

        return related;
    }

    /**
     * Extracts import statements from file content
     */
    private Set<String> extractImports(String content) {
        Set<String> imports = new HashSet<>();
        Matcher matcher = IMPORT_PATTERN.matcher(content);

        while (matcher.find()) {
            String importPath = matcher.group(1);
            // Convert package notation to file path
            importPath = importPath.replace('.', '/');
            imports.add(importPath);
        }

        return imports;
    }

    /**
     * Loads a file's context (with caching)
     */
    private FileContext loadFileContext(Path file) {
        String filePath = file.toString();

        // Check cache first
        if (fileCache.containsKey(filePath)) {
            FileContext cached = fileCache.get(filePath);
            try {
                // Validate cache is still fresh
                if (Files.getLastModifiedTime(file).toMillis() == cached.lastModified) {
                    return cached;
                }
            } catch (IOException e) {
                // File might have been deleted, remove from cache
                fileCache.remove(filePath);
                return null;
            }
        }

        // Load fresh
        try {
            String content = Files.readString(file);
            long lastModified = Files.getLastModifiedTime(file).toMillis();

            FileContext context = new FileContext(file, content, lastModified);
            fileCache.put(filePath, context);

            return context;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Clears the file cache
     */
    public void clearCache() {
        fileCache.clear();
    }

    /**
     * Gets cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(fileCache.size(), CACHE_SIZE);
    }

    /**
     * Represents a file's context
     */
    public static class FileContext {
        public final Path path;
        public final String content;
        public final long lastModified;

        public FileContext(Path path, String content, long lastModified) {
            this.path = path;
            this.content = content;
            this.lastModified = lastModified;
        }

        public String getRelativePath(Path root) {
            return root.relativize(path).toString();
        }
    }

    /**
     * Bundle of file contexts
     */
    public static class ContextBundle {
        private final List<FileContext> files = new ArrayList<>();
        private final Set<String> filePaths = new HashSet<>();

        public void addFile(FileContext context) {
            if (!filePaths.contains(context.path.toString())) {
                files.add(context);
                filePaths.add(context.path.toString());
            }
        }

        public List<FileContext> getFiles() {
            return files;
        }

        public boolean hasFile(Path path) {
            return filePaths.contains(path.toString());
        }

        public int getTotalSize() {
            return files.stream().mapToInt(f -> f.content.length()).sum();
        }

        public String formatForPrompt(Path projectRoot) {
            if (files.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\n=== RELEVANT FILES ===\n\n");

            for (FileContext file : files) {
                sb.append("File: ").append(file.getRelativePath(projectRoot)).append("\n");
                sb.append("─".repeat(60)).append("\n");

                // Show first 100 lines or full content if smaller
                String[] lines = file.content.split("\n");
                int linesToShow = Math.min(100, lines.length);

                for (int i = 0; i < linesToShow; i++) {
                    sb.append(String.format("%4d│ %s\n", i + 1, lines[i]));
                }

                if (lines.length > linesToShow) {
                    sb.append(String.format("... (%d more lines)\n", lines.length - linesToShow));
                }

                sb.append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * Cache statistics
     */
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;

        public CacheStats(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }

        public double getUsagePercentage() {
            return (currentSize * 100.0) / maxSize;
        }
    }
}
