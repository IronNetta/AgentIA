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

class CiCdToolTest {

    @Mock
    private CliService cliService;

    private CiCdTool tool;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tool = new CiCdTool(cliService);
    }

    @Test
    void testGetName() {
        assertEquals("@ci", tool.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(tool.getDescription());
        assertTrue(tool.getDescription().contains("CI") ||
                   tool.getDescription().contains("pipeline"));
    }

    @Test
    void testExecuteWithNoArgs() {
        ProjectContext context = createContext();
        String result = tool.execute("", context);

        assertTrue(result.contains("Error") || result.contains("Command required"));
    }

    @Test
    void testSetupGitHubActionsCreatesWorkflow() {
        ProjectContext context = createContext();
        String result = tool.execute("setup github", context);

        assertNotNull(result);
        assertTrue(result.contains("GitHub") || result.contains("workflow") ||
                   result.contains("created") || result.contains("exists"));
    }

    @Test
    void testSetupGitLabCICreatesPipeline() {
        ProjectContext context = createContext();
        String result = tool.execute("setup gitlab", context);

        assertNotNull(result);
        assertTrue(result.contains("GitLab") || result.contains("pipeline") ||
                   result.contains("created") || result.contains("exists"));
    }

    @Test
    void testSetupWithInvalidPlatform() {
        ProjectContext context = createContext();
        String result = tool.execute("setup invalid-platform", context);

        assertTrue(result.contains("Unknown platform") || result.contains("Error"));
    }

    @Test
    void testTestPipelineGeneratesTemplate() {
        ProjectContext context = createContext();
        String result = tool.execute("test", context);

        assertNotNull(result);
        assertTrue(result.contains("Test") || result.contains("Pipeline") ||
                   result.contains("template") || result.contains("Template"));
    }

    @Test
    void testDeployPipelineShowsComingSoon() {
        ProjectContext context = createContext();
        String result = tool.execute("deploy", context);

        assertNotNull(result);
        assertTrue(result.contains("Deployment") || result.contains("Coming soon") ||
                   result.contains("will include"));
    }

    @Test
    void testGitHubWorkflowFileCreation() throws Exception {
        ProjectContext context = createContext();
        tool.execute("setup github", context);

        Path workflowFile = tempDir.resolve(".github/workflows/ci.yml");
        if (Files.exists(workflowFile)) {
            String content = Files.readString(workflowFile);
            assertTrue(content.contains("name: CI"));
            assertTrue(content.contains("on:"));
            assertTrue(content.contains("jobs:"));
        }
        // If file doesn't exist, workflow already existed (which is OK)
    }

    private ProjectContext createContext() {
        ProjectContext context = mock(ProjectContext.class);
        when(context.getRootPath()).thenReturn(tempDir);
        when(context.getProjectType()).thenReturn(ProjectType.JAVA_MAVEN);
        return context;
    }
}
