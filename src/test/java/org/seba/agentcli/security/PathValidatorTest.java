package org.seba.agentcli.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PathValidator security checks
 */
class PathValidatorTest {

    @Mock
    private SecurityLogger securityLogger;

    private PathValidator pathValidator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pathValidator = new PathValidator(securityLogger);
    }

    @Test
    void testValidPathInCurrentDirectory() {
        // Valid path in current directory
        PathValidator.ValidationResult result = pathValidator.validatePath("README.md");
        assertTrue(result.isValid(), "README.md should be valid");
    }

    @Test
    void testValidRelativePath() {
        // Valid relative path
        PathValidator.ValidationResult result = pathValidator.validatePath("src/main/java/Main.java");
        assertTrue(result.isValid(), "Relative path in project should be valid");
    }

    @Test
    void testPathTraversalAttack() {
        // Path traversal attempt - accessing parent directory
        PathValidator.ValidationResult result = pathValidator.validatePath("../../../etc/passwd");
        assertFalse(result.isValid(), "Path traversal should be blocked");
        assertTrue(result.getErrorMessage().contains("within project directory"),
            "Error message should mention project directory restriction");
    }

    @Test
    void testAccessToGitDirectory() {
        // Attempt to access .git directory
        PathValidator.ValidationResult result = pathValidator.validatePath(".git/config");
        assertFalse(result.isValid(), ".git directory access should be blocked");
        assertTrue(result.getErrorMessage().contains(".git"),
            "Error message should mention .git");
    }

    @Test
    void testAccessToEnvFile() {
        // Attempt to access .env file
        PathValidator.ValidationResult result = pathValidator.validatePath(".env");
        assertFalse(result.isValid(), ".env file access should be blocked");
    }

    @Test
    void testAccessToSshDirectory() {
        // Attempt to access SSH keys
        PathValidator.ValidationResult result = pathValidator.validatePath("../.ssh/id_rsa");
        assertFalse(result.isValid(), "SSH directory access should be blocked");
    }

    @Test
    void testAbsolutePathOutsideProject() {
        // Absolute path outside project
        PathValidator.ValidationResult result = pathValidator.validatePath("/etc/passwd");
        assertFalse(result.isValid(), "Absolute path outside project should be blocked");
    }

    @Test
    void testEmptyPath() {
        // Empty path
        PathValidator.ValidationResult result = pathValidator.validatePath("");
        assertFalse(result.isValid(), "Empty path should be invalid");
    }

    @Test
    void testNullPath() {
        // Null path
        PathValidator.ValidationResult result = pathValidator.validatePath((String) null);
        assertFalse(result.isValid(), "Null path should be invalid");
    }

    @Test
    void testValidateAndNormalize_ValidPath() {
        // Should return normalized path for valid input
        assertDoesNotThrow(() -> {
            Path result = pathValidator.validateAndNormalize("README.md");
            assertNotNull(result);
            assertTrue(result.isAbsolute());
        });
    }

    @Test
    void testValidateAndNormalize_InvalidPath() {
        // Should throw SecurityException for invalid input
        assertThrows(SecurityException.class, () -> {
            pathValidator.validateAndNormalize("../../../etc/passwd");
        });
    }

    @Test
    void testValidateAndNormalize_GitDirectory() {
        // Should throw SecurityException for .git access
        assertThrows(SecurityException.class, () -> {
            pathValidator.validateAndNormalize(".git/config");
        });
    }

    @Test
    void testPathWithDotDotInMiddle() {
        // Path with .. in the middle (should be normalized and validated)
        PathValidator.ValidationResult result = pathValidator.validatePath("src/../README.md");
        // This should be valid as it normalizes to README.md which is in project
        assertTrue(result.isValid(), "Normalized path within project should be valid");
    }

    @Test
    void testAccessToCredentialsFile() {
        // Attempt to access credentials
        PathValidator.ValidationResult result = pathValidator.validatePath("credentials.json");
        assertFalse(result.isValid(), "Credentials file access should be blocked");
    }

    @Test
    void testAccessToSecretsFile() {
        // Attempt to access secrets
        PathValidator.ValidationResult result = pathValidator.validatePath("secrets.yaml");
        assertFalse(result.isValid(), "Secrets file access should be blocked");
    }
}
