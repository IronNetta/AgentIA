package org.seba.agentcli.tool.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for GitTool to ensure command injection is prevented
 */
class GitToolTest {

    @Mock
    private CliService cliService;

    private GitTool gitTool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gitTool = new GitTool(cliService);
    }

    /**
     * Only run if we're in a git repository
     */
    static boolean isGitRepository() {
        return Files.exists(Paths.get(".git"));
    }

    private ProjectContext createTestContext() {
        Path rootPath = Paths.get(".").toAbsolutePath().normalize();
        return new ProjectContext(rootPath, ProjectType.JAVA_MAVEN);
    }

    @Test
    @EnabledIf("isGitRepository")
    void testStatusCommand_NoInjection() {
        // Normal status command should work
        ProjectContext context = createTestContext();
        String result = gitTool.execute("status", context);

        assertNotNull(result);
        assertFalse(result.contains("error"), "Status command should not error");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testDiffCommand_WithNormalFile() {
        // Normal diff command with a file should work
        ProjectContext context = createTestContext();
        String result = gitTool.execute("diff README.md", context);

        assertNotNull(result);
        // Should either show diff or "no changes"
        assertTrue(result.contains("DIFF") || result.contains("No changes") || result.contains("GIT"),
            "Diff command should return formatted output");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testBlameCommand_WithMaliciousInput() {
        // Attempt command injection via blame command
        ProjectContext context = createTestContext();

        // Try to inject a command - this should fail gracefully
        String maliciousFile = "README.md; echo INJECTION_SUCCESS";
        String result = gitTool.execute("blame " + maliciousFile, context);

        assertNotNull(result);
        // The important check: git should report "file not found" or similar error
        // NOT execute the echo command. The parameterized ProcessBuilder prevents
        // shell interpretation, so the whole string is treated as a filename.
        assertTrue(
            result.toLowerCase().contains("not found") ||
            result.toLowerCase().contains("error") ||
            result.toLowerCase().contains("failed"),
            "Should fail with file not found error, not execute injected command"
        );
    }

    @Test
    @EnabledIf("isGitRepository")
    void testDiffCommand_WithMaliciousInput() {
        // Attempt command injection via diff command
        ProjectContext context = createTestContext();

        // Try various injection patterns
        String[] maliciousInputs = {
            "; rm -rf /tmp/test",
            "| cat /etc/passwd",
            "&& echo HACKED",
            "$(whoami)",
            "`id`"
        };

        for (String maliciousInput : maliciousInputs) {
            String result = gitTool.execute("diff " + maliciousInput, context);

            assertNotNull(result);
            // Should not execute injected commands
            assertFalse(result.contains("HACKED"), "Injection attempt should fail: " + maliciousInput);
            assertFalse(result.contains("root:"), "Should not read /etc/passwd: " + maliciousInput);
        }
    }

    @Test
    void testInvalidCommand() {
        // Invalid command should return error
        ProjectContext context = createTestContext();
        String result = gitTool.execute("invalid_command", context);

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("unknown") || result.toLowerCase().contains("error"),
            "Invalid command should return error message");
    }

    @Test
    void testEmptyCommand() {
        // Empty command should return error
        ProjectContext context = createTestContext();
        String result = gitTool.execute("", context);

        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error") || result.toLowerCase().contains("no command"),
            "Empty command should return error message");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testLogCommand_WithNumericLimit() {
        // Log command with numeric limit should work
        ProjectContext context = createTestContext();
        String result = gitTool.execute("log 5", context);

        assertNotNull(result);
        assertTrue(result.contains("COMMIT HISTORY") || result.contains("error"),
            "Log command should return formatted output or error");
    }

    @Test
    @EnabledIf("isGitRepository")
    void testLogCommand_WithMaliciousLimit() {
        // Attempt injection via log limit parameter
        ProjectContext context = createTestContext();
        String result = gitTool.execute("log 5; echo INJECTED", context);

        assertNotNull(result);
        // The semicolon and extra text will cause NumberFormatException when parsing limit
        // Since we split by space, "5;" becomes the limit, which should fail to parse
        assertTrue(
            result.toLowerCase().contains("invalid") ||
            result.toLowerCase().contains("error") ||
            result.toLowerCase().contains("commit history"),
            "Should either fail to parse or show log (not execute injection)"
        );
    }
}
