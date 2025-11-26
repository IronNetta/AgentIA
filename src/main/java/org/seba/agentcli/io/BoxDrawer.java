package org.seba.agentcli.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Beautiful ASCII box drawer - 100% homemade!
 * Creates professional-looking boxes for terminal output
 */
public class BoxDrawer {

    // Box styles (different Unicode box-drawing characters)
    public enum BoxStyle {
        SINGLE("┌", "─", "┐", "│", "└", "┘"),
        DOUBLE("╔", "═", "╗", "║", "╚", "╝"),
        ROUNDED("╭", "─", "╮", "│", "╰", "╯"),
        BOLD("┏", "━", "┓", "┃", "┗", "┛"),
        CLASSIC("+", "-", "+", "|", "+", "+");

        final String topLeft, horizontal, topRight, vertical, bottomLeft, bottomRight;

        BoxStyle(String tl, String h, String tr, String v, String bl, String br) {
            this.topLeft = tl;
            this.horizontal = h;
            this.topRight = tr;
            this.vertical = v;
            this.bottomLeft = bl;
            this.bottomRight = br;
        }
    }

    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    /**
     * Draw a simple box with title
     */
    public static String drawBox(String title, int width, BoxStyle style, String color) {
        StringBuilder box = new StringBuilder();

        // Top border with title
        box.append(AnsiColors.colorize(style.topLeft, color));
        if (title != null && !title.isEmpty()) {
            box.append(AnsiColors.colorize(" " + title + " ", color));
            int remainingWidth = width - title.length() - 2;
            box.append(AnsiColors.colorize(style.horizontal.repeat(Math.max(0, remainingWidth)), color));
        } else {
            box.append(AnsiColors.colorize(style.horizontal.repeat(width), color));
        }
        box.append(AnsiColors.colorize(style.topRight, color));
        box.append("\n");

        // Bottom border
        box.append(AnsiColors.colorize(style.bottomLeft, color));
        box.append(AnsiColors.colorize(style.horizontal.repeat(width), color));
        box.append(AnsiColors.colorize(style.bottomRight, color));

        return box.toString();
    }

    /**
     * Draw a box with content
     */
    public static String drawBoxWithContent(String title, List<String> lines, int width, BoxStyle style, String color) {
        StringBuilder box = new StringBuilder();

        // Top border with title
        box.append(AnsiColors.colorize(style.topLeft, color));
        if (title != null && !title.isEmpty()) {
            box.append(AnsiColors.colorize(" " + title + " ", AnsiColors.BOLD_WHITE));
            int remainingWidth = width - title.length() - 2;
            box.append(AnsiColors.colorize(style.horizontal.repeat(Math.max(0, remainingWidth)), color));
        } else {
            box.append(AnsiColors.colorize(style.horizontal.repeat(width), color));
        }
        box.append(AnsiColors.colorize(style.topRight, color));
        box.append("\n");

        // Content lines
        for (String line : lines) {
            box.append(AnsiColors.colorize(style.vertical, color));
            box.append(" ");
            box.append(padOrTruncate(line, width));
            box.append(" ");
            box.append(AnsiColors.colorize(style.vertical, color));
            box.append("\n");
        }

        // Bottom border
        box.append(AnsiColors.colorize(style.bottomLeft, color));
        box.append(AnsiColors.colorize(style.horizontal.repeat(width), color));
        box.append(AnsiColors.colorize(style.bottomRight, color));

        return box.toString();
    }

    /**
     * Draw a fancy header box
     */
    public static String drawHeader(String title, String subtitle, int width) {
        StringBuilder box = new StringBuilder();

        // Top border
        box.append(AnsiColors.colorize(Symbols.BOX_TOP_LEFT_DOUBLE, AnsiColors.CYAN));
        box.append(AnsiColors.colorize(Symbols.BOX_HORIZONTAL_DOUBLE.repeat(width - 2), AnsiColors.CYAN));
        box.append(AnsiColors.colorize(Symbols.BOX_TOP_RIGHT_DOUBLE, AnsiColors.CYAN));
        box.append("\n");

        // Empty line
        box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
        box.append(" ".repeat(width - 2));
        box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
        box.append("\n");

        // Title
        box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
        box.append("  ");
        box.append(AnsiColors.colorize(title, AnsiColors.BOLD_WHITE));
        box.append(" ".repeat(width - title.length() - 4));
        box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
        box.append("\n");

        // Subtitle if provided
        if (subtitle != null && !subtitle.isEmpty()) {
            box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
            box.append("  ");
            box.append(AnsiColors.colorize(subtitle, AnsiColors.BRIGHT_CYAN));
            box.append(" ".repeat(width - subtitle.length() - 4));
            box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
            box.append("\n");
        }

        // Empty line
        box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
        box.append(" ".repeat(width - 2));
        box.append(AnsiColors.colorize(Symbols.BOX_VERTICAL_DOUBLE, AnsiColors.CYAN));
        box.append("\n");

        // Bottom border
        box.append(AnsiColors.colorize(Symbols.BOX_BOTTOM_LEFT_DOUBLE, AnsiColors.CYAN));
        box.append(AnsiColors.colorize(Symbols.BOX_HORIZONTAL_DOUBLE.repeat(width - 2), AnsiColors.CYAN));
        box.append(AnsiColors.colorize(Symbols.BOX_BOTTOM_RIGHT_DOUBLE, AnsiColors.CYAN));

        return box.toString();
    }

    /**
     * Draw an info box
     */
    public static String drawInfoBox(String title, List<String> items, int width) {
        return drawBoxWithContent(title, items, width, BoxStyle.SINGLE, AnsiColors.BLUE);
    }

    /**
     * Draw a success box
     */
    public static String drawSuccessBox(String message, int width) {
        List<String> lines = new ArrayList<>();
        lines.add(centerText(message, width));
        return drawBoxWithContent("✓ SUCCESS", lines, width, BoxStyle.BOLD, AnsiColors.GREEN);
    }

    /**
     * Draw an error box
     */
    public static String drawErrorBox(String message, int width) {
        List<String> lines = new ArrayList<>();
        lines.add(centerText(message, width));
        return drawBoxWithContent("✗ ERROR", lines, width, BoxStyle.BOLD, AnsiColors.RED);
    }

    /**
     * Draw a warning box
     */
    public static String drawWarningBox(String message, int width) {
        List<String> lines = new ArrayList<>();
        lines.add(centerText(message, width));
        return drawBoxWithContent("⚠ WARNING", lines, width, BoxStyle.BOLD, AnsiColors.YELLOW);
    }

    /**
     * Draw a section separator
     */
    public static String drawSeparator(String text, int width, String color) {
        int textLen = text != null ? text.length() : 0;
        int sideLen = (width - textLen - 2) / 2;

        StringBuilder sep = new StringBuilder();
        sep.append(AnsiColors.colorize("─".repeat(sideLen), color));
        if (text != null && !text.isEmpty()) {
            sep.append(AnsiColors.colorize(" " + text + " ", AnsiColors.BOLD_WHITE));
        }
        sep.append(AnsiColors.colorize("─".repeat(sideLen), color));

        return sep.toString();
    }

    /**
     * Draw a progress bar
     */
    public static String drawProgressBar(int percentage, int width, String color) {
        int filled = (int) ((percentage / 100.0) * width);
        int empty = width - filled;

        return AnsiColors.colorize("[", color) +
               AnsiColors.colorize("█".repeat(filled), AnsiColors.GREEN) +
               AnsiColors.colorize("░".repeat(empty), AnsiColors.BRIGHT_BLACK) +
               AnsiColors.colorize("]", color) +
               AnsiColors.colorize(" " + percentage + "%", AnsiColors.WHITE);
    }

    /**
     * Helper: Center text in a given width
     */
    private static String centerText(String text, int width) {
        if (text == null) return " ".repeat(width);

        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, width - text.length() - padding));
    }

    /**
     * Helper: Pad or truncate text to exact width
     */
    private static String padOrTruncate(String text, int width) {
        if (text == null) text = "";

        if (text.length() > width) {
            return text.substring(0, width - 3) + "...";
        } else {
            return text + " ".repeat(width - text.length());
        }
    }

    /**
     * Draw a menu box with numbered options
     */
    public static String drawMenu(String title, List<String> options, int width) {
        List<String> menuLines = new ArrayList<>();

        for (int i = 0; i < options.size(); i++) {
            String option = AnsiColors.colorize(String.format("%d.", i + 1), AnsiColors.CYAN) +
                           " " + options.get(i);
            menuLines.add(option);
        }

        return drawBoxWithContent(title, menuLines, width, BoxStyle.ROUNDED, AnsiColors.PURPLE);
    }

    /**
     * Draw a key-value info panel
     */
    public static String drawInfoPanel(String title, List<String[]> keyValuePairs, int width) {
        List<String> lines = new ArrayList<>();

        for (String[] pair : keyValuePairs) {
            if (pair.length >= 2) {
                String line = AnsiColors.colorize(String.format("%-20s", pair[0] + ":"), AnsiColors.CYAN) +
                             " " + pair[1];
                lines.add(line);
            }
        }

        return drawBoxWithContent(title, lines, width, BoxStyle.SINGLE, AnsiColors.BLUE);
    }
}
