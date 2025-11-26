@echo off
REM Agent CLI Launcher for Windows
REM Direct execution of Agent CLI JAR

set AGENTCLI_JAR=%~dp0target\agent-cli-3.0.0-SNAPSHOT.jar

if not exist "%AGENTCLI_JAR%" (
    echo JAR file not found: %AGENTCLI_JAR%
    echo Please run 'mvn clean package' first to build the project.
    pause
    exit /b 1
)

REM Run the application with proper parameters
java -jar "%AGENTCLI_JAR%" %*