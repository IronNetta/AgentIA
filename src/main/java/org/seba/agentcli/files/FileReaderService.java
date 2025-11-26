package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.io.BoxDrawer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de lecture de fichiers - 100% homemade!
 * Lecture avec numérotation de lignes, highlighting, et preview
 */
@Service
public class FileReaderService {

    private static final int MAX_LINE_LENGTH = 120;
    private static final String LINE_NUMBER_FORMAT = "%6d→";

    /**
     * Lit un fichier complet avec numérotation de lignes
     */
    public FileContent readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        if (Files.isDirectory(path)) {
            throw new IOException("C'est un répertoire, pas un fichier: " + path);
        }

        List<String> lines = Files.readAllLines(path);
        return new FileContent(path, lines);
    }

    /**
     * Lit une partie d'un fichier (avec offset et limit)
     */
    public FileContent readFileRange(String filePath, int offset, int limit) throws IOException {
        FileContent content = readFile(filePath);

        int start = Math.max(0, offset);
        int end = Math.min(content.getLines().size(), offset + limit);

        List<String> subLines = content.getLines().subList(start, end);

        return new FileContent(content.getPath(), subLines, start);
    }

    /**
     * Affiche un fichier avec format stylisé (comme cat -n)
     */
    public String displayFile(FileContent content) {
        StringBuilder output = new StringBuilder();

        // Header
        output.append(BoxDrawer.drawSeparator(
                "📄 " + content.getPath().getFileName(),
                70,
                AnsiColors.CYAN
        ));
        output.append("\n\n");

        // Lignes numérotées
        int lineNumber = content.getStartLine() + 1;
        for (String line : content.getLines()) {
            String lineNum = AnsiColors.colorize(
                    String.format(LINE_NUMBER_FORMAT, lineNumber),
                    AnsiColors.BRIGHT_BLACK
            );

            String contentLine = truncateLine(line, MAX_LINE_LENGTH);
            output.append(lineNum).append(contentLine).append("\n");

            lineNumber++;
        }

        output.append("\n");
        output.append(BoxDrawer.drawSeparator(
                String.format("%d lignes", content.getLines().size()),
                70,
                AnsiColors.CYAN
        ));

        return output.toString();
    }

    /**
     * Affiche un preview (premières lignes seulement)
     */
    public String displayPreview(String filePath, int maxLines) throws IOException {
        FileContent content = readFileRange(filePath, 0, maxLines);
        return displayFile(content);
    }

    /**
     * Recherche une chaîne dans un fichier
     */
    public List<LineMatch> searchInFile(String filePath, String searchTerm) throws IOException {
        FileContent content = readFile(filePath);
        List<LineMatch> matches = new ArrayList<>();

        int lineNumber = 1;
        for (String line : content.getLines()) {
            if (line.contains(searchTerm)) {
                matches.add(new LineMatch(lineNumber, line, searchTerm));
            }
            lineNumber++;
        }

        return matches;
    }

    /**
     * Affiche les résultats de recherche
     */
    public String displaySearchResults(String filePath, List<LineMatch> matches) {
        if (matches.isEmpty()) {
            return AnsiColors.warning("Aucun résultat trouvé");
        }

        StringBuilder output = new StringBuilder();

        output.append(BoxDrawer.drawSeparator(
                String.format("🔍 %d résultat(s) dans %s", matches.size(), Paths.get(filePath).getFileName()),
                70,
                AnsiColors.YELLOW
        ));
        output.append("\n\n");

        for (LineMatch match : matches) {
            String lineNum = AnsiColors.colorize(
                    String.format(LINE_NUMBER_FORMAT, match.getLineNumber()),
                    AnsiColors.BRIGHT_BLACK
            );

            String highlighted = highlightTerm(match.getLine(), match.getSearchTerm());
            output.append(lineNum).append(highlighted).append("\n");
        }

        return output.toString();
    }

    /**
     * Tronque une ligne si trop longue
     */
    private String truncateLine(String line, int maxLength) {
        if (line.length() <= maxLength) {
            return line;
        }
        return line.substring(0, maxLength - 3) + AnsiColors.colorize("...", AnsiColors.BRIGHT_BLACK);
    }

    /**
     * Surligne un terme dans une ligne
     */
    private String highlightTerm(String line, String term) {
        return line.replace(term, AnsiColors.colorize(term, AnsiColors.BOLD_YELLOW));
    }

    /**
     * Vérifie si un fichier existe
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Obtient les informations sur un fichier
     */
    public FileInfo getFileInfo(String filePath) throws IOException {
        Path path = Paths.get(filePath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new IOException("Fichier inexistant: " + path);
        }

        long size = Files.size(path);
        int lines = (int) Files.lines(path).count();
        String extension = getFileExtension(path);

        return new FileInfo(path, size, lines, extension);
    }

    /**
     * Extrait l'extension d'un fichier
     */
    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * Classe représentant le contenu d'un fichier
     */
    public static class FileContent {
        private final Path path;
        private final List<String> lines;
        private final int startLine;

        public FileContent(Path path, List<String> lines) {
            this(path, lines, 0);
        }

        public FileContent(Path path, List<String> lines, int startLine) {
            this.path = path;
            this.lines = lines;
            this.startLine = startLine;
        }

        public Path getPath() {
            return path;
        }

        public List<String> lines() {
            return lines;
        }

        public int getStartLine() {
            return startLine;
        }

        public String getContent() {
            return String.join("\n", lines);
        }

        public List<String> getLines() {
            return lines;
        }
    }

    /**
     * Classe représentant un match de recherche
     */
    public static class LineMatch {
        private final int lineNumber;
        private final String line;
        private final String searchTerm;

        public LineMatch(int lineNumber, String line, String searchTerm) {
            this.lineNumber = lineNumber;
            this.line = line;
            this.searchTerm = searchTerm;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLine() {
            return line;
        }

        public String getSearchTerm() {
            return searchTerm;
        }
    }

    /**
     * Classe représentant les infos d'un fichier
     */
    public static class FileInfo {
        private final Path path;
        private final long size;
        private final int lines;
        private final String extension;

        public FileInfo(Path path, long size, int lines, String extension) {
            this.path = path;
            this.size = size;
            this.lines = lines;
            this.extension = extension;
        }

        public Path getPath() {
            return path;
        }

        public long getSize() {
            return size;
        }

        public int getLines() {
            return lines;
        }

        public String getExtension() {
            return extension;
        }

        public String getSizeFormatted() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}
