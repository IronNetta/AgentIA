package org.seba.agentcli.files;

import org.seba.agentcli.io.AnsiColors;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Visualiseur de différences - 100% homemade!
 * Affiche les diffs entre deux versions d'un fichier avec coloration
 */
@Component
public class DiffViewer {

    /**
     * Génère un diff entre deux contenus
     */
    public String generateDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");

        List<DiffLine> diffLines = computeDiff(oldLines, newLines);

        return formatDiff(diffLines);
    }

    /**
     * Calcule les différences ligne par ligne (algorithme simple)
     */
    private List<DiffLine> computeDiff(String[] oldLines, String[] newLines) {
        List<DiffLine> result = new ArrayList<>();

        int oldIndex = 0;
        int newIndex = 0;

        while (oldIndex < oldLines.length || newIndex < newLines.length) {
            if (oldIndex >= oldLines.length) {
                // Plus de lignes anciennes, tout le reste est ajouté
                while (newIndex < newLines.length) {
                    result.add(new DiffLine(DiffType.ADDED, newLines[newIndex], newIndex + 1));
                    newIndex++;
                }
            } else if (newIndex >= newLines.length) {
                // Plus de nouvelles lignes, tout le reste est supprimé
                while (oldIndex < oldLines.length) {
                    result.add(new DiffLine(DiffType.REMOVED, oldLines[oldIndex], oldIndex + 1));
                    oldIndex++;
                }
            } else if (oldLines[oldIndex].equals(newLines[newIndex])) {
                // Lignes identiques
                result.add(new DiffLine(DiffType.UNCHANGED, oldLines[oldIndex], oldIndex + 1));
                oldIndex++;
                newIndex++;
            } else {
                // Lignes différentes
                // Chercher si la ligne nouvelle existe plus loin dans l'ancien
                int foundInOld = findInArray(newLines[newIndex], oldLines, oldIndex + 1);
                int foundInNew = findInArray(oldLines[oldIndex], newLines, newIndex + 1);

                if (foundInOld != -1 && (foundInNew == -1 || foundInOld - oldIndex <= foundInNew - newIndex)) {
                    // Ligne supprimée
                    result.add(new DiffLine(DiffType.REMOVED, oldLines[oldIndex], oldIndex + 1));
                    oldIndex++;
                } else if (foundInNew != -1) {
                    // Ligne ajoutée
                    result.add(new DiffLine(DiffType.ADDED, newLines[newIndex], newIndex + 1));
                    newIndex++;
                } else {
                    // Ligne modifiée
                    result.add(new DiffLine(DiffType.REMOVED, oldLines[oldIndex], oldIndex + 1));
                    result.add(new DiffLine(DiffType.ADDED, newLines[newIndex], newIndex + 1));
                    oldIndex++;
                    newIndex++;
                }
            }
        }

        return result;
    }

    /**
     * Cherche une ligne dans un tableau
     */
    private int findInArray(String line, String[] array, int startIndex) {
        for (int i = startIndex; i < Math.min(array.length, startIndex + 5); i++) {
            if (array[i].equals(line)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Formate le diff avec coloration
     */
    private String formatDiff(List<DiffLine> diffLines) {
        StringBuilder output = new StringBuilder();

        output.append(AnsiColors.colorize("╭─ DIFFÉRENCES ", AnsiColors.CYAN))
              .append(AnsiColors.colorize("─".repeat(54), AnsiColors.CYAN))
              .append(AnsiColors.colorize("╮", AnsiColors.CYAN))
              .append("\n");

        // Afficher uniquement les lignes modifiées + contexte
        List<DiffLine> relevantLines = getRelevantLines(diffLines);

        for (DiffLine line : relevantLines) {
            output.append(formatDiffLine(line)).append("\n");
        }

        output.append(AnsiColors.colorize("╰" + "─".repeat(69) + "╯", AnsiColors.CYAN));

        return output.toString();
    }

    /**
     * Filtre pour n'afficher que les lignes pertinentes avec contexte
     */
    private List<DiffLine> getRelevantLines(List<DiffLine> diffLines) {
        List<DiffLine> result = new ArrayList<>();
        int contextLines = 3;

        for (int i = 0; i < diffLines.size(); i++) {
            DiffLine line = diffLines.get(i);

            if (line.type != DiffType.UNCHANGED) {
                // Ajouter le contexte avant
                int start = Math.max(0, i - contextLines);
                for (int j = start; j < i; j++) {
                    if (!result.contains(diffLines.get(j))) {
                        result.add(diffLines.get(j));
                    }
                }

                // Ajouter la ligne modifiée
                result.add(line);

                // Ajouter le contexte après
                int end = Math.min(diffLines.size(), i + contextLines + 1);
                for (int j = i + 1; j < end; j++) {
                    if (!result.contains(diffLines.get(j))) {
                        result.add(diffLines.get(j));
                    }
                }
            }
        }

        // Si trop de lignes, ne garder que les changements
        if (result.size() > 50) {
            result = diffLines.stream()
                    .filter(line -> line.type != DiffType.UNCHANGED)
                    .toList();
        }

        return result;
    }

    /**
     * Formate une ligne de diff
     */
    private String formatDiffLine(DiffLine line) {
        String prefix;
        String color;
        String lineContent = truncate(line.content, 65);

        switch (line.type) {
            case ADDED:
                prefix = "+ ";
                color = AnsiColors.GREEN;
                return AnsiColors.colorize(prefix, color) +
                       AnsiColors.colorize(lineContent, color);

            case REMOVED:
                prefix = "- ";
                color = AnsiColors.RED;
                return AnsiColors.colorize(prefix, color) +
                       AnsiColors.colorize(lineContent, AnsiColors.DIM + color);

            case UNCHANGED:
                prefix = "  ";
                return AnsiColors.dim(prefix + lineContent);

            default:
                return lineContent;
        }
    }

    /**
     * Génère un diff compact (statistiques)
     */
    public String generateDiffStats(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");

        List<DiffLine> diffLines = computeDiff(oldLines, newLines);

        long added = diffLines.stream().filter(l -> l.type == DiffType.ADDED).count();
        long removed = diffLines.stream().filter(l -> l.type == DiffType.REMOVED).count();
        long unchanged = diffLines.stream().filter(l -> l.type == DiffType.UNCHANGED).count();

        StringBuilder stats = new StringBuilder();
        stats.append(AnsiColors.colorize("+", AnsiColors.GREEN))
             .append(AnsiColors.colorize(String.valueOf(added), AnsiColors.BOLD_GREEN))
             .append(" ")
             .append(AnsiColors.colorize("-", AnsiColors.RED))
             .append(AnsiColors.colorize(String.valueOf(removed), AnsiColors.BOLD_RED))
             .append(" ")
             .append(AnsiColors.dim("(~" + unchanged + " inchangées)"));

        return stats.toString();
    }

    /**
     * Génère un diff side-by-side (côte à côte)
     */
    public String generateSideBySideDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");

        int maxLines = Math.max(oldLines.length, newLines.length);
        StringBuilder output = new StringBuilder();

        output.append(AnsiColors.colorize("╭─ ANCIEN ", AnsiColors.RED))
              .append(AnsiColors.colorize("─".repeat(23), AnsiColors.BRIGHT_BLACK))
              .append("┬")
              .append(AnsiColors.colorize("─".repeat(23), AnsiColors.BRIGHT_BLACK))
              .append(AnsiColors.colorize(" NOUVEAU ─╮", AnsiColors.GREEN))
              .append("\n");

        for (int i = 0; i < maxLines; i++) {
            String oldLine = i < oldLines.length ? truncate(oldLines[i], 30) : "";
            String newLine = i < newLines.length ? truncate(newLines[i], 30) : "";

            String oldColor = AnsiColors.RED;
            String newColor = AnsiColors.GREEN;

            if (i < oldLines.length && i < newLines.length && oldLines[i].equals(newLines[i])) {
                oldColor = AnsiColors.BRIGHT_BLACK;
                newColor = AnsiColors.BRIGHT_BLACK;
            }

            output.append("│ ")
                  .append(AnsiColors.colorize(String.format("%-30s", oldLine), oldColor))
                  .append(" │ ")
                  .append(AnsiColors.colorize(String.format("%-30s", newLine), newColor))
                  .append(" │\n");
        }

        output.append(AnsiColors.colorize("╰" + "─".repeat(32) + "┴" + "─".repeat(32) + "╯", AnsiColors.BRIGHT_BLACK));

        return output.toString();
    }

    /**
     * Tronque une chaîne si trop longue
     */
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Type de différence
     */
    private enum DiffType {
        ADDED,
        REMOVED,
        UNCHANGED
    }

    /**
     * Ligne de diff
     */
    private static class DiffLine {
        final DiffType type;
        final String content;
        final int lineNumber;

        DiffLine(DiffType type, String content, int lineNumber) {
            this.type = type;
            this.content = content;
            this.lineNumber = lineNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DiffLine)) return false;
            DiffLine diffLine = (DiffLine) o;
            return lineNumber == diffLine.lineNumber &&
                   type == diffLine.type &&
                   Objects.equals(content, diffLine.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, content, lineNumber);
        }
    }
}
