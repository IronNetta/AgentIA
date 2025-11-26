package org.seba.agentcli.io;

/**
 * Beautiful loading spinner - 100% homemade!
 * Shows animated loading indicators while waiting for LLM responses
 */
public class LoadingSpinner {

    // Different spinner styles
    private static final String[] SPINNER_DOTS = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final String[] SPINNER_LINE = {"-", "\\", "|", "/"};
    private static final String[] SPINNER_ARROW = {"←", "↖", "↑", "↗", "→", "↘", "↓", "↙"};
    private static final String[] SPINNER_DOTS_BOUNCE = {"⠁", "⠂", "⠄", "⡀", "⢀", "⠠", "⠐", "⠈"};
    private static final String[] SPINNER_BLOCKS = {"▁", "▂", "▃", "▄", "▅", "▆", "▇", "█", "▇", "▆", "▅", "▄", "▃", "▂"};
    private static final String[] SPINNER_CIRCLE = {"◐", "◓", "◑", "◒"};
    private static final String[] SPINNER_FANCY = {"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};

    public enum SpinnerStyle {
        DOTS(SPINNER_DOTS),
        LINE(SPINNER_LINE),
        ARROW(SPINNER_ARROW),
        BOUNCE(SPINNER_DOTS_BOUNCE),
        BLOCKS(SPINNER_BLOCKS),
        CIRCLE(SPINNER_CIRCLE),
        FANCY(SPINNER_FANCY);

        final String[] frames;

        SpinnerStyle(String[] frames) {
            this.frames = frames;
        }
    }

    private final SpinnerStyle style;
    private final String message;
    private final String color;
    private Thread spinnerThread;
    private volatile boolean running;
    private int currentFrame;

    public LoadingSpinner(String message, SpinnerStyle style, String color) {
        this.message = message;
        this.style = style;
        this.color = color;
        this.currentFrame = 0;
        this.running = false;
    }

    public LoadingSpinner(String message) {
        this(message, SpinnerStyle.FANCY, AnsiColors.CYAN);
    }

    /**
     * Start the spinner animation
     */
    public void start() {
        if (running) return;

        running = true;
        spinnerThread = new Thread(() -> {
            try {
                // Hide cursor
                System.out.print("\033[?25l");

                while (running) {
                    String frame = style.frames[currentFrame];
                    String spinnerText = AnsiColors.colorize(frame, color) + " " +
                                       AnsiColors.colorize(message, AnsiColors.BRIGHT_WHITE);

                    // Print spinner with carriage return (overwrites previous line)
                    System.out.print("\r" + spinnerText);
                    System.out.flush();

                    currentFrame = (currentFrame + 1) % style.frames.length;

                    Thread.sleep(80); // Animation speed
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Show cursor again
                System.out.print("\033[?25h");
            }
        });

        spinnerThread.setDaemon(true);
        spinnerThread.start();
    }

    /**
     * Stop the spinner and clear the line
     */
    public void stop() {
        if (!running) return;

        running = false;

        try {
            if (spinnerThread != null) {
                spinnerThread.join(200);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clear the line
        System.out.print("\r" + " ".repeat(message.length() + 10) + "\r");
        System.out.flush();
    }

    /**
     * Stop with a success message
     */
    public void stopWithSuccess(String successMessage) {
        stop();
        System.out.println(AnsiColors.success("✓ " + successMessage));
    }

    /**
     * Stop with an error message
     */
    public void stopWithError(String errorMessage) {
        stop();
        System.out.println(AnsiColors.error("✗ " + errorMessage));
    }

    /**
     * Stop with custom message
     */
    public void stopWithMessage(String message, String color) {
        stop();
        System.out.println(AnsiColors.colorize(message, color));
    }

    /**
     * Static helper: Quick spinner for a task
     */
    public static void withSpinner(String message, Runnable task) {
        LoadingSpinner spinner = new LoadingSpinner(message);
        spinner.start();

        try {
            task.run();
            spinner.stopWithSuccess("Done!");
        } catch (Exception e) {
            spinner.stopWithError("Failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Static helper: Quick spinner with custom result handling
     */
    public static <T> T withSpinner(String message, SpinnerTask<T> task) throws Exception {
        LoadingSpinner spinner = new LoadingSpinner(message);
        spinner.start();

        try {
            T result = task.run();
            spinner.stop();
            return result;
        } catch (Exception e) {
            spinner.stopWithError("Failed: " + e.getMessage());
            throw e;
        }
    }

    @FunctionalInterface
    public interface SpinnerTask<T> {
        T run() throws Exception;
    }

    /**
     * Demo/Test method
     */
    public static void demo() throws InterruptedException {
        System.out.println("Loading Spinner Demo:\n");

        for (SpinnerStyle style : SpinnerStyle.values()) {
            LoadingSpinner spinner = new LoadingSpinner(
                "Loading with " + style.name() + " style...",
                style,
                AnsiColors.CYAN
            );

            spinner.start();
            Thread.sleep(2000);
            spinner.stopWithSuccess("Completed!");
            Thread.sleep(500);
        }
    }
}
