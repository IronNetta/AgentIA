package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
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
 * Tool pour rechercher et gérer les TODOs dans le code
 */
@Component
public class TodoTool extends AbstractTool {

    private static final Pattern TODO_PATTERN = Pattern.compile(
            ".*(?://|#|/\\*|<!--|\\*)\\s*TODO:?\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> IGNORED_DIRS = Arrays.asList(
            "target", "build", "node_modules", ".git", ".idea", "dist", "out"
    );

    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".java", ".py", ".js", ".ts", ".go", ".rs", ".c", ".cpp", ".h",
            ".cs", ".php", ".rb", ".kt", ".swift", ".scala", ".sh", ".yml", ".yaml"
    );

    public TodoTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@todo";
    }

    @Override
    public String getDescription() {
        return "Recherche et affiche tous les TODOs dans le code";
    }

    @Override
    public String getUsage() {
        return "@todo [--file <path>] [--count] [--stats]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        try {
            String[] parts = args.trim().split("\\s+");

            boolean showCount = args.contains("--count");
            boolean showStats = args.contains("--stats");
            String specificFile = null;

            // Parse --file argument
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("--file") && i + 1 < parts.length) {
                    specificFile = parts[i + 1];
                    break;
                }
            }

            // Scan for TODOs
            List<TodoItem> todos;
            if (specificFile != null) {
                todos = scanFile(Paths.get(specificFile));
            } else {
                todos = scanProject(Paths.get("."));
            }

            if (todos.isEmpty()) {
                return AnsiColors.info("✓ Aucun TODO trouvé !");
            }

            // Display results
            if (showCount) {
                return displayCount(todos);
            } else if (showStats) {
                return displayStats(todos);
            } else {
                return displayTodos(todos);
            }

        } catch (Exception e) {
            return formatError("Erreur lors de la recherche: " + e.getMessage());
        }
    }

    /**
     * Scan un fichier spécifique pour les TODOs
     */
    private List<TodoItem> scanFile(Path file) throws IOException {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return Collections.emptyList();
        }

        List<TodoItem> todos = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);

        for (int i = 0; i < lines.size(); i++) {
            Matcher matcher = TODO_PATTERN.matcher(lines.get(i));
            if (matcher.matches()) {
                String description = matcher.group(1).trim();
                todos.add(new TodoItem(file, i + 1, description, lines.get(i).trim()));
            }
        }

        return todos;
    }

    /**
     * Scan tout le projet pour les TODOs
     */
    private List<TodoItem> scanProject(Path root) throws IOException {
        List<TodoItem> allTodos = new ArrayList<>();

        Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(this::isCodeFile)
                .filter(this::notInIgnoredDir)
                .forEach(file -> {
                    try {
                        allTodos.addAll(scanFile(file));
                    } catch (IOException e) {
                        // Ignore errors
                    }
                });

        return allTodos;
    }

    /**
     * Vérifie si c'est un fichier de code
     */
    private boolean isCodeFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    /**
     * Vérifie que le fichier n'est pas dans un répertoire ignoré
     */
    private boolean notInIgnoredDir(Path path) {
        String pathStr = path.toString();
        return IGNORED_DIRS.stream().noneMatch(dir -> pathStr.contains("/" + dir + "/") || pathStr.contains("\\" + dir + "\\"));
    }

    /**
     * Affiche la liste complète des TODOs
     */
    private String displayTodos(List<TodoItem> todos) {
        StringBuilder output = new StringBuilder();

        // Group by file
        Map<Path, List<TodoItem>> byFile = todos.stream()
                .collect(Collectors.groupingBy(TodoItem::getFile));

        output.append(BoxDrawer.drawSeparator(
                String.format("📝 %d TODO(S) TROUVÉ(S)", todos.size()),
                70,
                AnsiColors.YELLOW
        ));
        output.append("\n\n");

        // Display by file
        byFile.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Path file = entry.getKey();
                    List<TodoItem> fileTodos = entry.getValue();

                    // File header
                    output.append(AnsiColors.colorize(
                            "📄 " + file.toString(),
                            AnsiColors.BOLD_CYAN
                    ));
                    output.append(AnsiColors.dim(" (" + fileTodos.size() + ")"));
                    output.append("\n");

                    // TODOs for this file
                    fileTodos.forEach(todo -> {
                        output.append(AnsiColors.colorize(
                                String.format("   %4d│ ", todo.getLineNumber()),
                                AnsiColors.BRIGHT_BLACK
                        ));
                        output.append(AnsiColors.colorize("TODO: ", AnsiColors.YELLOW));
                        output.append(todo.getDescription());
                        output.append("\n");
                    });

                    output.append("\n");
                });

        output.append(AnsiColors.dim("💡 Utilisez @todo --stats pour voir les statistiques"));

        return output.toString();
    }

    /**
     * Affiche le nombre de TODOs
     */
    private String displayCount(List<TodoItem> todos) {
        return AnsiColors.info(String.format("📝 %d TODO(s) trouvé(s)", todos.size()));
    }

    /**
     * Affiche les statistiques des TODOs
     */
    private String displayStats(List<TodoItem> todos) {
        StringBuilder output = new StringBuilder();

        Map<Path, List<TodoItem>> byFile = todos.stream()
                .collect(Collectors.groupingBy(TodoItem::getFile));

        Map<String, Long> byExtension = todos.stream()
                .collect(Collectors.groupingBy(
                        todo -> getExtension(todo.getFile()),
                        Collectors.counting()
                ));

        output.append(BoxDrawer.drawSeparator(
                "📊 STATISTIQUES DES TODOs",
                70,
                AnsiColors.CYAN
        ));
        output.append("\n\n");

        // Stats générales
        List<String[]> generalStats = Arrays.asList(
                new String[]{"Total TODOs", String.valueOf(todos.size())},
                new String[]{"Fichiers concernés", String.valueOf(byFile.size())},
                new String[]{"Moyenne par fichier", String.format("%.1f", todos.size() / (double) byFile.size())}
        );

        output.append(BoxDrawer.drawInfoPanel("ℹ️ GÉNÉRAL", generalStats, 66));
        output.append("\n\n");

        // Par type de fichier
        output.append(AnsiColors.colorize("📁 Par type de fichier:", AnsiColors.BOLD_WHITE));
        output.append("\n\n");

        byExtension.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String ext = entry.getKey();
                    long count = entry.getValue();
                    double percentage = (count * 100.0) / todos.size();

                    output.append("   ");
                    output.append(AnsiColors.colorize(String.format("%-10s", ext), AnsiColors.CYAN));
                    output.append(" ");
                    output.append(drawBar((int) percentage, 30));
                    output.append(" ");
                    output.append(AnsiColors.highlight(String.format("%d", count)));
                    output.append(AnsiColors.dim(String.format(" (%.1f%%)", percentage)));
                    output.append("\n");
                });

        output.append("\n");

        // Top 5 fichiers avec le plus de TODOs
        output.append(AnsiColors.colorize("🏆 Top 5 fichiers:", AnsiColors.BOLD_WHITE));
        output.append("\n\n");

        byFile.entrySet().stream()
                .sorted(Map.Entry.<Path, List<TodoItem>>comparingByValue(
                        Comparator.comparingInt(List::size)
                ).reversed())
                .limit(5)
                .forEach(entry -> {
                    output.append("   ");
                    output.append(AnsiColors.colorize(
                            String.format("%2d", entry.getValue().size()),
                            AnsiColors.BOLD_YELLOW
                    ));
                    output.append(" → ");
                    output.append(entry.getKey().toString());
                    output.append("\n");
                });

        return output.toString();
    }

    /**
     * Dessine une barre de progression
     */
    private String drawBar(int percentage, int width) {
        int filled = (percentage * width) / 100;
        int empty = width - filled;

        return AnsiColors.colorize("█".repeat(filled), AnsiColors.GREEN) +
               AnsiColors.colorize("░".repeat(empty), AnsiColors.BRIGHT_BLACK);
    }

    /**
     * Extrait l'extension d'un fichier
     */
    private String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "no-ext";
    }

    /**
     * Classe représentant un TODO
     */
    private static class TodoItem {
        private final Path file;
        private final int lineNumber;
        private final String description;
        private final String fullLine;

        public TodoItem(Path file, int lineNumber, String description, String fullLine) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.description = description;
            this.fullLine = fullLine;
        }

        public Path getFile() {
            return file;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getDescription() {
            return description;
        }

        public String getFullLine() {
            return fullLine;
        }
    }
}
