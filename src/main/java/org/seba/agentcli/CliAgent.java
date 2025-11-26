package org.seba.agentcli;

import org.seba.agentcli.io.*;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.ui.LLMSelector;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.List;

/**
 * Agent CLI - 100% Homemade, Zero External Dependencies!
 * Beautiful, powerful, and fully controlled.
 */
@SpringBootApplication
public class CliAgent implements CommandLineRunner {

    private final CliService cliService;
    private final CommandProcessor commandProcessor;
    private final LLMSelector llmSelector;
    private final ConsoleReader consoleReader;
    private final AdvancedPrompt advancedPrompt;
    private final CommandHistory commandHistory;
    private final InputReader inputReader;
    private final CommandSuggester commandSuggester;

    private static final int BOX_WIDTH = 70;

    public CliAgent(CliService cliService,
                          CommandProcessor commandProcessor,
                          LLMSelector llmSelector,
                          ConsoleReader consoleReader,
                          AdvancedPrompt advancedPrompt,
                          CommandHistory commandHistory,
                          InputReader inputReader,
                          CommandSuggester commandSuggester) {
        this.cliService = cliService;
        this.commandProcessor = commandProcessor;
        this.llmSelector = llmSelector;
        this.consoleReader = consoleReader;
        this.advancedPrompt = advancedPrompt;
        this.commandHistory = commandHistory;
        this.inputReader = inputReader;
        this.commandSuggester = commandSuggester;
    }

    public static void main(String[] args) {
        // Disable Spring Boot banner
        System.setProperty("spring.main.banner-mode", "off");
        SpringApplication.run(CliAgent.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // Check for --configure flag or if first time
            boolean forceConfig = Arrays.asList(args).contains("--configure");

            if (forceConfig || shouldShowLLMSelector()) {
                llmSelector.showSelectionMenu(consoleReader);
            } else {
                llmSelector.loadSavedConfiguration();
            }

            // Initialize the conversation context with the current working directory
            String currentWorkingDir = System.getProperty("user.dir");
            cliService.initializeContext(currentWorkingDir);

            // Clear screen for a fresh start
            AnsiColors.clearScreen();

            // Get project context for prompt
            ProjectContext projectContext = cliService.getProjectContext();

            // Print beautiful welcome with new banner
            AdvancedPrompt.showWelcomeBanner(projectContext);

            // Show completion hints
            inputReader.showCompletionHints();

            while (true) {
                // Build context-aware prompt
                String prompt = advancedPrompt.buildPrompt(projectContext);

                String line;
                try {
                    line = inputReader.readLine(prompt);
                } catch (Exception e) {
                    line = consoleReader.readLine(prompt);
                }

                // Handle null (EOF) or empty input
                if (line == null) {
                    printGoodbye();
                    break;
                }

                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                // Add to history
                commandHistory.add(line);

                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    printGoodbye();
                    break;
                }

                if ("clear".equalsIgnoreCase(line)) {
                    cliService.clearContext();
                    cliService.clearConversationContext();
                    System.out.println();
                    System.out.println(AnsiColors.success(Symbols.CHECK + " Context cleared successfully"));
                    System.out.println();
                    continue;
                }

                // Handle history command
                if (line.equalsIgnoreCase("history")) {
                    System.out.println();
                    System.out.println(commandHistory.formatHistory(20));
                    System.out.println();
                    continue;
                }

                // Process the command with loading animation
                processCommandWithLoading(line);

                // Refresh branch cache after git commands
                if (line.toLowerCase().startsWith("@git")) {
                    advancedPrompt.clearBranchCache();
                }
            }

        } catch (Exception e) {
            System.out.println();
            System.out.println(BoxDrawer.drawErrorBox(
                "Erreur: " + e.getMessage(),
                BOX_WIDTH - 4));
            // Debug mode only - don't print stack traces in normal operation
            // e.printStackTrace();
        }
    }

    private void processCommandWithLoading(String command) {
        System.out.println();

        // Show loading spinner
        LoadingSpinner spinner = new LoadingSpinner(
            "Génération de la réponse...",
            LoadingSpinner.SpinnerStyle.FANCY,
            AnsiColors.CYAN
        );

        spinner.start();

        try {
            // Process the command
            String response = commandProcessor.processCommand(command);

            // Stop spinner
            spinner.stop();

            // Print response in a nice box
            printResponse(response);

        } catch (Exception e) {
            spinner.stopWithError("Erreur lors du traitement");
            System.out.println();
            System.out.println(BoxDrawer.drawErrorBox(e.getMessage(), BOX_WIDTH - 4));
        }

        System.out.println();
    }

    private void printResponse(String response) {
        // Split response into lines for box
        String[] lines = response.split("\n");

        // Print response header
        System.out.println();
        System.out.println(AnsiColors.colorize(
            Symbols.BOX_HORIZONTAL_DOUBLE.repeat(3) + " RESPONSE " + Symbols.BOX_HORIZONTAL_DOUBLE.repeat(BOX_WIDTH - 14),
            AnsiColors.GREEN
        ));
        System.out.println();

        // Print each line with nice formatting
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                System.out.println();
            } else {
                // Wrap long lines
                List<String> wrappedLines = wrapText(line, BOX_WIDTH - 4);
                for (String wrappedLine : wrappedLines) {
                    System.out.println(AnsiColors.colorize("│ ", AnsiColors.BRIGHT_BLACK) + wrappedLine);
                }
            }
        }

        System.out.println();
        System.out.println(AnsiColors.colorize(Symbols.BOX_HORIZONTAL_DOUBLE.repeat(BOX_WIDTH), AnsiColors.GREEN));
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();

        if (text.length() <= maxWidth) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }

            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private boolean shouldShowLLMSelector() {
        String home = System.getProperty("user.home");
        java.nio.file.Path configPath = java.nio.file.Paths.get(home, ".agentcli", "llm-config.yml");
        return !java.nio.file.Files.exists(configPath);
    }

    private void printWelcome() {
        System.out.println();

        // Beautiful header with gradient effect
        System.out.println(BoxDrawer.drawHeader(
            "AGENT CLI - Multi-Language Development Assistant",
            "Powered by LLM | 100% Homemade - Zero External Dependencies",
            BOX_WIDTH
        ));

        System.out.println();

        // Quick info
        List<String> features = Arrays.asList(
            AnsiColors.colorize(Symbols.CHECK, AnsiColors.GREEN) + " Multi-Language: Java, Python, JS/TS, Go, Rust, C#, PHP, Ruby",
            AnsiColors.colorize(Symbols.CHECK, AnsiColors.GREEN) + " Framework Detection: Spring Boot, Django, React, Vue, etc.",
            AnsiColors.colorize(Symbols.CHECK, AnsiColors.GREEN) + " 10 Powerful Tools: Analyze, Search, Generate, Refactor & More",
            AnsiColors.colorize(Symbols.CHECK, AnsiColors.GREEN) + " Smart Context Management: Persistent Conversations"
        );

        System.out.println(BoxDrawer.drawInfoBox("FEATURES", features, BOX_WIDTH));
        System.out.println();

        // Commands menu
        List<String> commands = Arrays.asList(
            AnsiColors.colorize("@help", AnsiColors.CYAN) + "              " + Symbols.ARROW_RIGHT + " List all available commands",
            AnsiColors.colorize("@llm info", AnsiColors.CYAN) + "          " + Symbols.ARROW_RIGHT + " Show LLM configuration",
            AnsiColors.colorize("@file <path>", AnsiColors.CYAN) + "       " + Symbols.ARROW_RIGHT + " Analyze a specific file",
            AnsiColors.colorize("@search <term>", AnsiColors.CYAN) + "     " + Symbols.ARROW_RIGHT + " Search across the project",
            AnsiColors.colorize("@analyze-project", AnsiColors.CYAN) + "   " + Symbols.ARROW_RIGHT + " Full project analysis",
            AnsiColors.colorize("@execute [action]", AnsiColors.CYAN) + "  " + Symbols.ARROW_RIGHT + " Run build/test/run commands",
            "",
            AnsiColors.colorize("clear", AnsiColors.YELLOW) + "              " + Symbols.ARROW_RIGHT + " Clear conversation context",
            AnsiColors.colorize("exit", AnsiColors.RED) + "               " + Symbols.ARROW_RIGHT + " Exit the application"
        );

        System.out.println(BoxDrawer.drawInfoBox("COMMANDS", commands, BOX_WIDTH));
        System.out.println();

        // Tip
        System.out.println(AnsiColors.colorize(
            "TIP: ", AnsiColors.YELLOW) +
            "Type " + AnsiColors.colorize("@help", AnsiColors.CYAN) +
            " for detailed command usage or ask any question naturally!"
        );
        System.out.println();

        System.out.println(BoxDrawer.drawSeparator("START TYPING", BOX_WIDTH, AnsiColors.PURPLE));
        System.out.println();
    }

    private void printGoodbye() {
        AnsiColors.clearScreen();
        System.out.println();
        System.out.println();

        String[] messages = {
            "Au revoir !",
            "Merci d'avoir utilisé Agent CLI",
            "À bientôt pour de nouveaux projets !"
        };

        for (String msg : messages) {
            System.out.println(AnsiColors.colorize(
                centerText(msg, BOX_WIDTH),
                AnsiColors.CYAN
            ));
        }

        System.out.println();
        System.out.println(BoxDrawer.drawSeparator("", BOX_WIDTH, AnsiColors.CYAN));
        System.out.println();
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}
