package org.seba.agentcli.tool.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.model.ProjectType;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DependencyManagerToolTest {

    @Mock
    private CliService cliService;

    private DependencyManagerTool tool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new DependencyManagerTool(cliService);
    }

    @Test
    void testGetName() {
        assertEquals("@deps", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().contains("dependency") ||
                   tool.getDescription().contains("dependencies"));
    }

    @Test
    void testExecuteWithNoArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("", context);

        assertTrue(result.contains("Error") || result.contains("Command required"));
    }

    @Test
    void testExecuteWithInvalidCommand() {
        ProjectContext context = createContext();
        String result = tool.execute("invalid-command", context);

        assertTrue(result.contains("Unknown command"));
    }

    @Test
    void testCheckDependenciesUnsupportedProject() {
        ProjectContext context = mock(ProjectContext.class);
        when(context.getRootPath()).thenReturn(tempDir);
        when(context.getProjectType()).thenReturn(ProjectType.UNKNOWN);

        String result = tool.execute("check", context);

        assertTrue(result.contains("not supported") || result.contains("Error"));
    }

    @Test
    void testOutdatedDependenciesUnsupportedProject() {
        ProjectContext context = mock(ProjectContext.class);
        when(context.getRootPath()).thenReturn(tempDir);
        when(context.getProjectType()).thenReturn(ProjectType.UNKNOWN);

        String result = tool.execute("outdated", context);

        assertTrue(result.contains("Not supported") || result.contains("not supported"));
    }

    @Test
    void testSecurityScanUnsupportedProject() {
        ProjectContext context = mock(ProjectContext.class);
        when(context.getRootPath()).thenReturn(tempDir);
        when(context.getProjectType()).thenReturn(ProjectType.UNKNOWN);

        String result = tool.execute("security", context);

        assertTrue(result.contains("not supported") || result.contains("Error"));
    }

    @Test
    void testUpdateDependencyWithNoArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("update", context);

        assertTrue(result.contains("Error") || result.contains("required"));
    }

    private ProjectContext createContext() {
        ProjectContext context = mock(ProjectContext.class);
        when(context.getRootPath()).thenReturn(tempDir);
        when(context.getProjectType()).thenReturn(ProjectType.JAVA_MAVEN);
        return context;
    }
}
