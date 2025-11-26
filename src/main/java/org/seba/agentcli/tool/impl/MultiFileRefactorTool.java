package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Multi-file atomic refactoring tool.
 * Supports rename operations across entire codebase with rollback capability.
 *
 * Commands:
 *   @refactor rename-class OldName NewName
 *   @refactor rename-method oldMethod newMethod [--class ClassName]
 *   @refactor rename-variable oldVar newVar [--scope file.java]
 *   @refactor rename-package old.package new.package
 */
@Component
public class MultiFileRefactorTool extends AbstractTool {

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private final Map<Path, String> backups = new HashMap<>();

    public MultiFileRefactorTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@refactor";
    }

    @Override
    public String getDescription() {
        return "Atomic multi-file refactoring with rollback support";
    }

    @Override
    public String getUsage() {
        return "@refactor <operation> <args>\n\n" +
               "Operations:\n" +
               "  rename-class OldName NewName\n" +
               "    Rename a class across all files\n\n" +
               "  rename-method oldMethod newMethod [--class ClassName]\n" +
               "    Rename a method (optionally scoped to a class)\n\n" +
               "  rename-variable oldVar newVar [--scope file.java]\n" +
               "    Rename a variable (optionally scoped to a file)\n\n" +
               "  rename-package old.package new.package\n" +
               "    Rename a package and update all imports\n\n" +
               "Examples:\n" +
               "  @refactor rename-class UserService UserManager\n" +
               "  @refactor rename-method getUser fetchUser --class UserService\n" +
               "  @refactor rename-variable userId uid --scope UserController.java\n" +
               "  @refactor rename-package com.old com.new";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Operation required", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        try {
            String[] parts = args.trim().split("\\s+", 2);
            String operation = parts[0];

            if (parts.length < 2) {
                return AnsiColors.colorize("Error: Arguments required for operation: " + operation, AnsiColors.RED);
            }

            String operationArgs = parts[1];

            return switch (operation) {
                case "rename-class" -> renameClass(operationArgs, context);
                case "rename-method" -> renameMethod(operationArgs, context);
                case "rename-variable" -> renameVariable(operationArgs, context);
                case "rename-package" -> renamePackage(operationArgs, context);
                default -> AnsiColors.colorize("Error: Unknown operation: " + operation, AnsiColors.RED) + "\n" +
                           AnsiColors.colorize("Available: rename-class, rename-method, rename-variable, rename-package", AnsiColors.YELLOW);
            };

        } catch (Exception e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Rename a class across all files
     */
    private String renameClass(String args, ProjectContext context) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            return AnsiColors.colorize("Error: Usage: rename-class OldName NewName", AnsiColors.RED);
        }

        String oldName = parts[0];
        String newName = parts[1];

        System.out.println(AnsiColors.colorize("🔍 Analyzing references to class: " + oldName, AnsiColors.CYAN));

        try {
            // Find all Java files
            List<Path> javaFiles = findJavaFiles(context.getRootPath());

            // Find all files that reference this class
            List<FileReference> references = findClassReferences(javaFiles, oldName);

            if (references.isEmpty()) {
                return AnsiColors.colorize("No references found for class: " + oldName, AnsiColors.YELLOW);
            }

            // Display preview
            StringBuilder preview = new StringBuilder();
            preview.append(AnsiColors.colorize("\n📋 Found " + references.size() + " references in " +
                          references.stream().map(r -> r.file).distinct().count() + " files:\n\n", AnsiColors.GREEN));

            Map<Path, List<FileReference>> byFile = references.stream()
                    .collect(Collectors.groupingBy(r -> r.file));

            for (Map.Entry<Path, List<FileReference>> entry : byFile.entrySet()) {
                Path file = entry.getKey();
                List<FileReference> refs = entry.getValue();
                preview.append(AnsiColors.colorize("  " + context.getRootPath().relativize(file), AnsiColors.CYAN));
                preview.append(AnsiColors.colorize(" (" + refs.size() + " references)\n", AnsiColors.BRIGHT_BLACK));

                for (FileReference ref : refs.stream().limit(3).toList()) {
                    preview.append(AnsiColors.colorize("    Line " + ref.lineNumber + ": ", AnsiColors.BRIGHT_BLACK));
                    preview.append(ref.context.replace(oldName, AnsiColors.colorize(oldName, AnsiColors.YELLOW)));
                    preview.append("\n");
                }
                if (refs.size() > 3) {
                    preview.append(AnsiColors.colorize("    ... and " + (refs.size() - 3) + " more\n", AnsiColors.BRIGHT_BLACK));
                }
                preview.append("\n");
            }

            System.out.print(preview.toString());

            // Ask for confirmation
            System.out.print(AnsiColors.colorize("\n⚠️  Apply refactoring? This will modify " + byFile.size() + " files. [y/N]: ", AnsiColors.YELLOW));
            Scanner scanner = new Scanner(System.in);
            String response = scanner.nextLine().trim().toLowerCase();

            if (!response.equals("y") && !response.equals("yes")) {
                return AnsiColors.colorize("Refactoring cancelled", AnsiColors.YELLOW);
            }

            // Apply refactoring with transaction
            System.out.println(AnsiColors.colorize("\n🔄 Applying refactoring...", AnsiColors.CYAN));

            backups.clear();
            int filesModified = 0;
            int referencesReplaced = 0;

            try {
                for (Map.Entry<Path, List<FileReference>> entry : byFile.entrySet()) {
                    Path file = entry.getKey();

                    // Backup file
                    String originalContent = Files.readString(file);
                    backups.put(file, originalContent);

                    // Replace all occurrences
                    String newContent = replaceClassName(originalContent, oldName, newName);

                    if (!newContent.equals(originalContent)) {
                        Files.writeString(file, newContent);
                        filesModified++;
                        referencesReplaced += entry.getValue().size();
                        System.out.println(AnsiColors.colorize("  ✓ " + context.getRootPath().relativize(file), AnsiColors.GREEN));
                    }
                }

                // Also rename the class file itself if it exists
                Path oldFile = findClassFile(context.getRootPath(), oldName);
                if (oldFile != null) {
                    Path newFile = oldFile.getParent().resolve(newName + ".java");
                    Files.move(oldFile, newFile);
                    System.out.println(AnsiColors.colorize("  ✓ Renamed file: " + oldName + ".java → " + newName + ".java", AnsiColors.GREEN));
                }

                backups.clear(); // Success, clear backups

                return AnsiColors.colorize("\n✅ Refactoring completed successfully!", AnsiColors.BOLD_GREEN) + "\n" +
                       AnsiColors.colorize("   Files modified: " + filesModified, AnsiColors.GREEN) + "\n" +
                       AnsiColors.colorize("   References replaced: " + referencesReplaced, AnsiColors.GREEN);

            } catch (Exception e) {
                // Rollback on error
                System.err.println(AnsiColors.colorize("\n❌ Error during refactoring: " + e.getMessage(), AnsiColors.RED));
                System.err.println(AnsiColors.colorize("🔄 Rolling back changes...", AnsiColors.YELLOW));

                rollback();

                return AnsiColors.colorize("\n✓ Rollback completed. No changes were made.", AnsiColors.YELLOW);
            }

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Rename a method across files
     */
    private String renameMethod(String args, ProjectContext context) {
        // Parse args: oldMethod newMethod [--class ClassName]
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            return AnsiColors.colorize("Error: Usage: rename-method oldMethod newMethod [--class ClassName]", AnsiColors.RED);
        }

        String oldMethod = parts[0];
        String newMethod = parts[1];
        String className = null;

        // Check for --class option
        for (int i = 2; i < parts.length - 1; i++) {
            if (parts[i].equals("--class")) {
                className = parts[i + 1];
                break;
            }
        }

        System.out.println(AnsiColors.colorize("🔍 Analyzing method: " + oldMethod +
                          (className != null ? " in class " + className : ""), AnsiColors.CYAN));

        try {
            List<Path> javaFiles = findJavaFiles(context.getRootPath());
            List<FileReference> references = findMethodReferences(javaFiles, oldMethod, className);

            if (references.isEmpty()) {
                return AnsiColors.colorize("No references found for method: " + oldMethod, AnsiColors.YELLOW);
            }

            // Similar preview and confirmation flow as renameClass
            return executeRefactoring(references, oldMethod, newMethod, context, "method");

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Rename a variable
     */
    private String renameVariable(String args, ProjectContext context) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            return AnsiColors.colorize("Error: Usage: rename-variable oldVar newVar [--scope file.java]", AnsiColors.RED);
        }

        String oldVar = parts[0];
        String newVar = parts[1];
        String scope = null;

        for (int i = 2; i < parts.length - 1; i++) {
            if (parts[i].equals("--scope")) {
                scope = parts[i + 1];
                break;
            }
        }

        System.out.println(AnsiColors.colorize("🔍 Analyzing variable: " + oldVar +
                          (scope != null ? " in " + scope : ""), AnsiColors.CYAN));

        try {
            List<Path> filesToSearch;
            if (scope != null) {
                Path scopeFile = context.getRootPath().resolve(scope);
                if (!Files.exists(scopeFile)) {
                    return AnsiColors.colorize("Error: Scope file not found: " + scope, AnsiColors.RED);
                }
                filesToSearch = List.of(scopeFile);
            } else {
                filesToSearch = findJavaFiles(context.getRootPath());
            }

            List<FileReference> references = findVariableReferences(filesToSearch, oldVar);

            if (references.isEmpty()) {
                return AnsiColors.colorize("No references found for variable: " + oldVar, AnsiColors.YELLOW);
            }

            return executeRefactoring(references, oldVar, newVar, context, "variable");

        } catch (IOException e) {
            return AnsiColors.colorize("Error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Rename a package
     */
    private String renamePackage(String args, ProjectContext context) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            return AnsiColors.colorize("Error: Usage: rename-package old.package new.package", AnsiColors.RED);
        }

        String oldPackage = parts[0];
        String newPackage = parts[1];

        return AnsiColors.colorize("Package refactoring not yet implemented", AnsiColors.YELLOW) + "\n" +
               AnsiColors.colorize("Coming soon!", AnsiColors.BRIGHT_BLACK);
    }

    /**
     * Execute refactoring with preview and confirmation
     */
    private String executeRefactoring(List<FileReference> references, String oldName, String newName,
                                     ProjectContext context, String type) throws IOException {
        // Display preview
        StringBuilder preview = new StringBuilder();
        preview.append(AnsiColors.colorize("\n📋 Found " + references.size() + " references in " +
                      references.stream().map(r -> r.file).distinct().count() + " files:\n\n", AnsiColors.GREEN));

        Map<Path, List<FileReference>> byFile = references.stream()
                .collect(Collectors.groupingBy(r -> r.file));

        for (Map.Entry<Path, List<FileReference>> entry : byFile.entrySet()) {
            Path file = entry.getKey();
            List<FileReference> refs = entry.getValue();
            preview.append(AnsiColors.colorize("  " + context.getRootPath().relativize(file), AnsiColors.CYAN));
            preview.append(AnsiColors.colorize(" (" + refs.size() + " references)\n", AnsiColors.BRIGHT_BLACK));
        }

        System.out.print(preview.toString());

        // Ask for confirmation
        System.out.print(AnsiColors.colorize("\n⚠️  Apply refactoring? [y/N]: ", AnsiColors.YELLOW));
        Scanner scanner = new Scanner(System.in);
        String response = scanner.nextLine().trim().toLowerCase();

        if (!response.equals("y") && !response.equals("yes")) {
            return AnsiColors.colorize("Refactoring cancelled", AnsiColors.YELLOW);
        }

        // Apply changes
        System.out.println(AnsiColors.colorize("\n🔄 Applying refactoring...", AnsiColors.CYAN));

        backups.clear();
        int filesModified = 0;

        try {
            for (Map.Entry<Path, List<FileReference>> entry : byFile.entrySet()) {
                Path file = entry.getKey();

                String originalContent = Files.readString(file);
                backups.put(file, originalContent);

                String newContent = replaceWholeWord(originalContent, oldName, newName);

                if (!newContent.equals(originalContent)) {
                    Files.writeString(file, newContent);
                    filesModified++;
                    System.out.println(AnsiColors.colorize("  ✓ " + context.getRootPath().relativize(file), AnsiColors.GREEN));
                }
            }

            backups.clear();

            return AnsiColors.colorize("\n✅ " + type + " refactoring completed!", AnsiColors.BOLD_GREEN) + "\n" +
                   AnsiColors.colorize("   Files modified: " + filesModified, AnsiColors.GREEN);

        } catch (Exception e) {
            System.err.println(AnsiColors.colorize("\n❌ Error: " + e.getMessage(), AnsiColors.RED));
            System.err.println(AnsiColors.colorize("🔄 Rolling back...", AnsiColors.YELLOW));
            rollback();
            return AnsiColors.colorize("\n✓ Rollback completed.", AnsiColors.YELLOW);
        }
    }

    /**
     * Find all Java files in the project
     */
    private List<Path> findJavaFiles(Path root) throws IOException {
        List<Path> javaFiles = new ArrayList<>();

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java") && attrs.size() < MAX_FILE_SIZE) {
                    // Skip build directories
                    String pathStr = file.toString();
                    if (!pathStr.contains("/target/") && !pathStr.contains("/build/") &&
                        !pathStr.contains("/.git/") && !pathStr.contains("/node_modules/")) {
                        javaFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return javaFiles;
    }

    /**
     * Find all references to a class
     */
    private List<FileReference> findClassReferences(List<Path> files, String className) throws IOException {
        List<FileReference> references = new ArrayList<>();

        // Pattern to match class name (as whole word)
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(className) + "\\b");

        for (Path file : files) {
            String content = Files.readString(file);
            String[] lines = content.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    references.add(new FileReference(file, i + 1, line.trim()));
                }
            }
        }

        return references;
    }

    /**
     * Find all references to a method
     */
    private List<FileReference> findMethodReferences(List<Path> files, String methodName, String className) throws IOException {
        List<FileReference> references = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(");

        for (Path file : files) {
            // If className specified, only search in that file
            if (className != null && !file.getFileName().toString().equals(className + ".java")) {
                continue;
            }

            String content = Files.readString(file);
            String[] lines = content.split("\n");

            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = pattern.matcher(lines[i]);
                if (matcher.find()) {
                    references.add(new FileReference(file, i + 1, lines[i].trim()));
                }
            }
        }

        return references;
    }

    /**
     * Find all references to a variable
     */
    private List<FileReference> findVariableReferences(List<Path> files, String varName) throws IOException {
        List<FileReference> references = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(varName) + "\\b");

        for (Path file : files) {
            String content = Files.readString(file);
            String[] lines = content.split("\n");

            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = pattern.matcher(lines[i]);
                if (matcher.find()) {
                    references.add(new FileReference(file, i + 1, lines[i].trim()));
                }
            }
        }

        return references;
    }

    /**
     * Replace class name intelligently (preserves camelCase in derived names)
     */
    private String replaceClassName(String content, String oldName, String newName) {
        // Replace as whole word
        return content.replaceAll("\\b" + Pattern.quote(oldName) + "\\b", newName);
    }

    /**
     * Replace whole word
     */
    private String replaceWholeWord(String content, String oldName, String newName) {
        return content.replaceAll("\\b" + Pattern.quote(oldName) + "\\b", newName);
    }

    /**
     * Find the file containing a class definition
     */
    private Path findClassFile(Path root, String className) throws IOException {
        List<Path> javaFiles = findJavaFiles(root);

        for (Path file : javaFiles) {
            if (file.getFileName().toString().equals(className + ".java")) {
                return file;
            }
        }

        return null;
    }

    /**
     * Rollback all changes
     */
    private void rollback() {
        for (Map.Entry<Path, String> entry : backups.entrySet()) {
            try {
                Files.writeString(entry.getKey(), entry.getValue());
                System.out.println(AnsiColors.colorize("  ✓ Restored: " + entry.getKey().getFileName(), AnsiColors.GREEN));
            } catch (IOException e) {
                System.err.println(AnsiColors.colorize("  ✗ Failed to restore: " + entry.getKey(), AnsiColors.RED));
            }
        }
        backups.clear();
    }

    /**
     * Reference to a symbol in a file
     */
    private static class FileReference {
        final Path file;
        final int lineNumber;
        final String context;

        FileReference(Path file, int lineNumber, String context) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.context = context;
        }
    }
}
