package org.seba.agentcli.io;

/**
 * ANSI color codes - 100% homemade, no external dependencies!
 * Simple and efficient terminal colors
 */
public class AnsiColors {

    // Reset
    public static final String RESET = "\u001B[0m";

    // Regular Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bold
    public static final String BOLD = "\u001B[1m";
    public static final String BOLD_BLACK = "\u001B[1;30m";
    public static final String BOLD_RED = "\u001B[1;31m";
    public static final String BOLD_GREEN = "\u001B[1;32m";
    public static final String BOLD_YELLOW = "\u001B[1;33m";
    public static final String BOLD_BLUE = "\u001B[1;34m";
    public static final String BOLD_PURPLE = "\u001B[1;35m";
    public static final String BOLD_CYAN = "\u001B[1;36m";
    public static final String BOLD_WHITE = "\u001B[1;37m";

    // Background
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_PURPLE = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";

    // Bright Colors
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Text Styles
    public static final String UNDERLINE = "\u001B[4m";
    public static final String ITALIC = "\u001B[3m";
    public static final String DIM = "\u001B[2m";
    public static final String BLINK = "\u001B[5m";
    public static final String REVERSE = "\u001B[7m";
    public static final String HIDDEN = "\u001B[8m";
    public static final String STRIKETHROUGH = "\u001B[9m";

    // Detect if terminal supports colors
    private static final boolean COLORS_ENABLED = checkColorSupport();

    /**
     * Check if terminal supports ANSI colors
     */
    private static boolean checkColorSupport() {
        // Check if running in a terminal (not redirected)
        if (System.console() == null) {
            return false;
        }

        // Check environment variables
        String term = System.getenv("TERM");
        if (term != null && (term.contains("color") || term.contains("xterm") || term.contains("256"))) {
            return true;
        }

        // Check OS
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux") || os.contains("mac") || os.contains("unix")) {
            return true;
        }

        // Windows 10+ supports ANSI
        if (os.contains("windows")) {
            String version = System.getProperty("os.version");
            return version != null && !version.startsWith("6."); // Windows 10 = version 10.0
        }

        return false;
    }

    /**
     * Colorize text if colors are supported
     */
    public static String colorize(String text, String color) {
        if (COLORS_ENABLED) {
            return color + text + RESET;
        }
        return text;
    }

    /**
     * Helper methods for common use cases
     */
    public static String success(String text) {
        return colorize(text, BOLD_GREEN);
    }

    public static String error(String text) {
        return colorize(text, BOLD_RED);
    }

    public static String warning(String text) {
        return colorize(text, BOLD_YELLOW);
    }

    public static String info(String text) {
        return colorize(text, BOLD_CYAN);
    }

    public static String highlight(String text) {
        return colorize(text, BOLD_WHITE);
    }

    public static String dim(String text) {
        return colorize(text, BRIGHT_BLACK);
    }

    /**
     * Disable colors (useful for testing or non-interactive mode)
     */
    public static boolean isColorsEnabled() {
        return COLORS_ENABLED;
    }

    /**
     * Print colored text
     */
    public static void println(String text, String color) {
        System.out.println(colorize(text, color));
    }

    public static void print(String text, String color) {
        System.out.print(colorize(text, color));
    }

    /**
     * Additional helper methods for styled text
     */
    public static String bold(String text) {
        return colorize(text, BOLD);
    }

    public static String underline(String text) {
        return colorize(text, UNDERLINE);
    }

    public static String italic(String text) {
        return colorize(text, ITALIC);
    }

    public static String rainbow(String text) {
        String[] colors = {RED, YELLOW, GREEN, CYAN, BLUE, PURPLE};
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            result.append(colorize(String.valueOf(text.charAt(i)), colors[i % colors.length]));
        }

        return result.toString();
    }

    /**
     * Gradient text (fade from one color to another)
     */
    public static String gradient(String text, String startColor, String endColor) {
        // Simple implementation: alternate between start and end color
        StringBuilder result = new StringBuilder();
        int half = text.length() / 2;

        for (int i = 0; i < text.length(); i++) {
            String color = i < half ? startColor : endColor;
            result.append(colorize(String.valueOf(text.charAt(i)), color));
        }

        return result.toString();
    }

    /**
     * Clear screen
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Move cursor to position
     */
    public static void moveCursor(int row, int col) {
        System.out.print(String.format("\033[%d;%dH", row, col));
        System.out.flush();
    }

    /**
     * Clear current line
     */
    public static void clearLine() {
        System.out.print("\r\033[K");
        System.out.flush();
    }

    /**
     * Print a fancy banner
     */
    public static void printBanner(String text) {
        int width = 60;
        println("═".repeat(width), CYAN);
        println(centerText(text, width), BOLD_WHITE);
        println("═".repeat(width), CYAN);
    }

    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}
