package org.seba.agentcli.io;

/**
 * Symboles ASCII/Unicode compatibles - pas d'emojis !
 * Fonctionne sur tous les terminaux
 */
public class Symbols {

    // Status symbols
    public static final String SUCCESS = "[✓]";
    public static final String ERROR = "[✗]";
    public static final String WARNING = "[!]";
    public static final String INFO = "[i]";
    public static final String QUESTION = "[?]";
    public static final String ARROW_RIGHT = "→";
    public static final String ARROW_LEFT = "←";
    public static final String BULLET = "•";
    public static final String CHECK = "✓";
    public static final String CROSS = "✗";
    public static final String STAR = "★";
    public static final String CIRCLE = "○";
    public static final String DOT = "·";

    // Icons (text-based)
    public static final String FILE = "[FILE]";
    public static final String FOLDER = "[DIR]";
    public static final String CODE = "[CODE]";
    public static final String EDIT = "[EDIT]";
    public static final String DELETE = "[DEL]";
    public static final String SEARCH = "[FIND]";
    public static final String TODO = "[TODO]";
    public static final String CONFIG = "[CFG]";
    public static final String BUILD = "[BLD]";
    public static final String TEST = "[TEST]";
    public static final String ROBOT = "[AI]";
    public static final String BACKUP = "[BKP]";

    // Box drawing - single line
    public static final String BOX_HORIZONTAL = "─";
    public static final String BOX_VERTICAL = "│";
    public static final String BOX_TOP_LEFT = "┌";
    public static final String BOX_TOP_RIGHT = "┐";
    public static final String BOX_BOTTOM_LEFT = "└";
    public static final String BOX_BOTTOM_RIGHT = "┘";
    public static final String BOX_CROSS = "┼";
    public static final String BOX_T_DOWN = "┬";
    public static final String BOX_T_UP = "┴";
    public static final String BOX_T_RIGHT = "├";
    public static final String BOX_T_LEFT = "┤";

    // Box drawing - double line
    public static final String BOX_HORIZONTAL_DOUBLE = "═";
    public static final String BOX_VERTICAL_DOUBLE = "║";
    public static final String BOX_TOP_LEFT_DOUBLE = "╔";
    public static final String BOX_TOP_RIGHT_DOUBLE = "╗";
    public static final String BOX_BOTTOM_LEFT_DOUBLE = "╚";
    public static final String BOX_BOTTOM_RIGHT_DOUBLE = "╝";

    // Box drawing - rounded
    public static final String BOX_TOP_LEFT_ROUND = "╭";
    public static final String BOX_TOP_RIGHT_ROUND = "╮";
    public static final String BOX_BOTTOM_LEFT_ROUND = "╰";
    public static final String BOX_BOTTOM_RIGHT_ROUND = "╯";

    // Box drawing - bold
    public static final String BOX_HORIZONTAL_BOLD = "━";
    public static final String BOX_VERTICAL_BOLD = "┃";
    public static final String BOX_TOP_LEFT_BOLD = "┏";
    public static final String BOX_TOP_RIGHT_BOLD = "┓";
    public static final String BOX_BOTTOM_LEFT_BOLD = "┗";
    public static final String BOX_BOTTOM_RIGHT_BOLD = "┛";

    // Progress/Loading
    public static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    public static final String[] SPINNER_SIMPLE = {"|", "/", "-", "\\"};
    public static final String BLOCK_FULL = "█";
    public static final String BLOCK_LIGHT = "░";
    public static final String BLOCK_MEDIUM = "▒";
    public static final String BLOCK_DARK = "▓";

    // Arrows
    public static final String ARROW_UP = "↑";
    public static final String ARROW_DOWN = "↓";
    public static final String ARROW_DOUBLE_RIGHT = "»";
    public static final String ARROW_DOUBLE_LEFT = "«";

    /**
     * Crée un titre stylisé
     */
    public static String title(String text) {
        return BOX_HORIZONTAL_DOUBLE.repeat(3) + " " + text + " " + BOX_HORIZONTAL_DOUBLE.repeat(3);
    }

    /**
     * Crée une ligne de séparation
     */
    public static String separator(int width) {
        return BOX_HORIZONTAL.repeat(width);
    }

    /**
     * Crée une ligne de séparation double
     */
    public static String separatorDouble(int width) {
        return BOX_HORIZONTAL_DOUBLE.repeat(width);
    }

    /**
     * Crée une barre de progression
     */
    public static String progressBar(int percentage, int width) {
        int filled = (percentage * width) / 100;
        int empty = width - filled;
        return BLOCK_FULL.repeat(filled) + BLOCK_LIGHT.repeat(empty);
    }

    /**
     * Formate un message de succès
     */
    public static String success(String message) {
        return SUCCESS + " " + message;
    }

    /**
     * Formate un message d'erreur
     */
    public static String error(String message) {
        return ERROR + " " + message;
    }

    /**
     * Formate un message d'avertissement
     */
    public static String warning(String message) {
        return WARNING + " " + message;
    }

    /**
     * Formate un message d'info
     */
    public static String info(String message) {
        return INFO + " " + message;
    }

    /**
     * Crée un bandeau
     */
    public static String banner(String text, int width) {
        int padding = (width - text.length() - 4) / 2;
        String pad = " ".repeat(Math.max(0, padding));
        return BOX_HORIZONTAL_DOUBLE.repeat(width) + "\n" +
               BOX_VERTICAL_DOUBLE + pad + text + pad + BOX_VERTICAL_DOUBLE + "\n" +
               BOX_HORIZONTAL_DOUBLE.repeat(width);
    }

    /**
     * Liste à puces
     */
    public static String bullet(String text) {
        return BULLET + " " + text;
    }

    /**
     * Élément numéroté
     */
    public static String numbered(int number, String text) {
        return String.format("[%d] %s", number, text);
    }

    /**
     * Indentation
     */
    public static String indent(int level) {
        return "  ".repeat(level);
    }

    /**
     * Box simple
     */
    public static String box(String content, int width) {
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();

        // Top
        result.append(BOX_TOP_LEFT).append(BOX_HORIZONTAL.repeat(width - 2)).append(BOX_TOP_RIGHT).append("\n");

        // Content
        for (String line : lines) {
            String padded = String.format("%-" + (width - 4) + "s", line);
            result.append(BOX_VERTICAL).append(" ").append(padded).append(" ").append(BOX_VERTICAL).append("\n");
        }

        // Bottom
        result.append(BOX_BOTTOM_LEFT).append(BOX_HORIZONTAL.repeat(width - 2)).append(BOX_BOTTOM_RIGHT);

        return result.toString();
    }
}
