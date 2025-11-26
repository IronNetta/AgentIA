package org.seba.agentcli.tool.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiFileRefactorToolTest {

    @Mock
    private CliService cliService;

    private MultiFileRefactorTool tool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new MultiFileRefactorTool(cliService);
    }

    @Test
    void testGetName() {
        assertEquals("@refactor", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().contains("refactoring"));
    }

    @Test
    void testExecuteWithNoArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("", context);

        assertTrue(result.contains("Error"));
        assertTrue(result.contains("Operation required"));
    }

    @Test
    void testExecuteWithInvalidOperation() {
        ProjectContext context = createContext();
        // Need to provide an argument after the operation to reach the switch statement
        String result = tool.execute("invalid-operation somearg", context);

        assertTrue(result.contains("Unknown operation"));
    }

    @Test
    void testRenameClassWithMissingArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("rename-class", context);

        assertTrue(result.contains("Error") || result.contains("required"));
    }

    @Test
    void testRenameMethodWithMissingArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("rename-method", context);

        assertTrue(result.contains("Error") || result.contains("Usage"));
    }

    @Test
    void testRenameVariableWithMissingArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("rename-variable", context);

        assertTrue(result.contains("Error") || result.contains("Usage"));
    }

    @Test
    void testRenameClassWithNoReferences() throws Exception {
        // Create a simple Java file without the class to rename
        Path javaFile = tempDir.resolve("TestFile.java");
        Files.writeString(javaFile, "public class TestFile { }");

        ProjectContext context = createContext();
        String result = tool.execute("rename-class NonExistent NewName", context);

        assertTrue(result.contains("No references") || result.contains("not found"));
    }

    private ProjectContext createContext() {
        ProjectContext context = mock(ProjectContext.class);
        when(context.getRootPath()).thenReturn(tempDir);
        when(context.getProjectType()).thenReturn(ProjectType.JAVA_MAVEN);
        return context;
    }
}
