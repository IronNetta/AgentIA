package org.seba.agentcli.io;

import org.seba.agentcli.context.PlanManager;
import org.seba.agentcli.model.ProjectContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Advanced prompt system with context awareness
 * Shows: project name, git branch, active plan
 */
@Component
public class AdvancedPrompt {

    private final PlanManager planManager;
    private String cachedBranch = null;
    private long lastBranchCheck = 0;
    private static final long BRANCH_CACHE_MS = 5000; // 5 seconds

    public AdvancedPrompt(PlanManager planManager) {
        this.planManager = planManager;
    }

    /**
     * Builds the prompt string with context
     */
    public String buildPrompt(ProjectContext context) {
        StringBuilder prompt = new StringBuilder();

        // Start with bracket
        prompt.append(AnsiColors.colorize("[", AnsiColors.BRIGHT_BLACK));

        // Project name
        if (context != null && context.getRootPath() != null) {
            String projectName = context.getRootPath().getFileName().toString();
            prompt.append(AnsiColors.colorize(projectName, AnsiColors.CYAN));
        } else {
            prompt.append(AnsiColors.colorize("AgentCLI", AnsiColors.CYAN));
        }

        // Git branch (if available)
        String branch = getCurrentBranch(context);
        if (branch != null) {
            prompt.append(AnsiColors.colorize(":", AnsiColors.BRIGHT_BLACK));
            prompt.append(AnsiColors.colorize(branch, AnsiColors.YELLOW));
        }

        // Close bracket
        prompt.append(AnsiColors.colorize("]", AnsiColors.BRIGHT_BLACK));

        // Active plan indicator
        if (planManager.hasPlan()) {
            int completed = planManager.getCurrentPlan().getCompletedCount();
            int total = planManager.getCurrentPlan().getTasks().size();
            prompt.append(" ");
            prompt.append(AnsiColors.colorize("📋", AnsiColors.BLUE));
            prompt.append(" ");
            prompt.append(AnsiColors.colorize(completed + "/" + total, AnsiColors.BLUE));
        }

        // Arrow prompt
        prompt.append(" ");
        prompt.append(AnsiColors.colorize(">>>", AnsiColors.GREEN));
        prompt.append(" ");

        return prompt.toString();
    }

    /**
     * Gets current Git branch (with caching)
     */
    private String getCurrentBranch(ProjectContext context) {
        // Check cache first
        long now = System.currentTimeMillis();
        if (cachedBranch != null && (now - lastBranchCheck) < BRANCH_CACHE_MS) {
            return cachedBranch;
        }

        // Try to get branch
        try {
            String rootPath = context != null ? context.getRootPath().toString() : ".";
            File gitDir = new File(rootPath, ".git");
            if (!gitDir.exists()) {
                return null;
            }

            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            pb.directory(new File(rootPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String branch = reader.readLine();

            int exitCode = process.waitFor();
            if (exitCode == 0 && branch != null && !branch.isEmpty()) {
                cachedBranch = branch.trim();
                lastBranchCheck = now;
                return cachedBranch;
            }
        } catch (Exception e) {
            // Silently fail - not critical
        }

        return null;
    }

    /**
     * Clears the branch cache (call after git operations)
     */
    public void clearBranchCache() {
        cachedBranch = null;
        lastBranchCheck = 0;
    }

    /**
     * Shows a welcome banner
     */
    public static void showWelcomeBanner(ProjectContext context) {
        System.out.println();
        System.out.println(AnsiColors.colorize("╔═══════════════════════════════════════════════════════════╗", AnsiColors.CYAN));
        System.out.println(AnsiColors.colorize("║", AnsiColors.CYAN) +
                          AnsiColors.colorize("              🤖 Agent CLI v3.0.0                        ", AnsiColors.BOLD_WHITE) +
                          AnsiColors.colorize("║", AnsiColors.CYAN));
        System.out.println(AnsiColors.colorize("║", AnsiColors.CYAN) +
                          "         Intelligence Augmentée - Nov 2025               " +
                          AnsiColors.colorize("║", AnsiColors.CYAN));
        System.out.println(AnsiColors.colorize("╚═══════════════════════════════════════════════════════════╝", AnsiColors.CYAN));
        System.out.println();

        if (context != null) {
            String projectName = context.getRootPath().getFileName().toString();
            System.out.println(AnsiColors.colorize("📁 Project: ", AnsiColors.BRIGHT_BLACK) +
                             AnsiColors.colorize(projectName, AnsiColors.CYAN));
            System.out.println(AnsiColors.colorize("📦 Type: ", AnsiColors.BRIGHT_BLACK) +
                             AnsiColors.colorize(context.getProjectType().toString(), AnsiColors.YELLOW));
            System.out.println(AnsiColors.colorize("📍 Root: ", AnsiColors.BRIGHT_BLACK) +
                             context.getRootPath().toString());
            System.out.println();
        }

        System.out.println(AnsiColors.colorize("Type ", AnsiColors.BRIGHT_BLACK) +
                         AnsiColors.colorize("@help", AnsiColors.GREEN) +
                         AnsiColors.colorize(" for commands, ", AnsiColors.BRIGHT_BLACK) +
                         AnsiColors.colorize("exit", AnsiColors.RED) +
                         AnsiColors.colorize(" to quit", AnsiColors.BRIGHT_BLACK));
        System.out.println();
    }

    /**
     * Shows a spinner for long operations
     */
    public static class Spinner {
        private static final String[] FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        private volatile boolean running = false;
        private Thread spinnerThread;
        private final String message;

        public Spinner(String message) {
            this.message = message;
        }

        public void start() {
            running = true;
            spinnerThread = new Thread(() -> {
                int frame = 0;
                while (running) {
                    System.out.print("\r" + AnsiColors.colorize(FRAMES[frame], AnsiColors.CYAN) + " " + message + "  ");
                    frame = (frame + 1) % FRAMES.length;
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                // Clear the line
                System.out.print("\r" + " ".repeat(message.length() + 10) + "\r");
            });
            spinnerThread.setDaemon(true);
            spinnerThread.start();
        }

        public void stop(String finalMessage) {
            running = false;
            if (spinnerThread != null) {
                try {
                    spinnerThread.join(200);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            if (finalMessage != null) {
                System.out.println(finalMessage);
            }
        }

        public void stop() {
            stop(null);
        }
    }

    /**
     * Shows a progress bar
     */
    public static void showProgress(int current, int total, String message) {
        int barLength = 40;
        int filled = (int) ((double) current / total * barLength);
        int percentage = (int) ((double) current / total * 100);

        StringBuilder bar = new StringBuilder();
        bar.append("\r");
        bar.append(message).append(" ");
        bar.append(AnsiColors.colorize("[", AnsiColors.BRIGHT_BLACK));

        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(AnsiColors.colorize("█", AnsiColors.GREEN));
            } else {
                bar.append(AnsiColors.colorize("░", AnsiColors.BRIGHT_BLACK));
            }
        }

        bar.append(AnsiColors.colorize("]", AnsiColors.BRIGHT_BLACK));
        bar.append(" ");
        bar.append(AnsiColors.colorize(percentage + "%", AnsiColors.CYAN));
        bar.append(" ");
        bar.append(AnsiColors.colorize("(" + current + "/" + total + ")", AnsiColors.BRIGHT_BLACK));

        System.out.print(bar.toString());

        if (current == total) {
            System.out.println(); // New line when complete
        }
    }
}
