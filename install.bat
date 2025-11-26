@echo off
REM Installation script for Agent CLI (Windows)

echo ========================================
echo   Installation d'Agent CLI
echo ========================================
echo.

REM Check Java
echo [1/4] Verification de Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERREUR] Java n'est pas installe.
    echo Installez Java 21+ depuis: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java trouve

REM Build with Maven
echo.
echo [2/4] Compilation du projet...
if exist mvnw.cmd (
    call mvnw.cmd clean package -DskipTests
) else (
    call mvn clean package -DskipTests
)

if errorlevel 1 (
    echo [ERREUR] Erreur lors de la compilation
    pause
    exit /b 1
)

REM Create installation directory
set INSTALL_DIR=%USERPROFILE%\.agentcli
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
set BIN_DIR=%INSTALL_DIR%\bin
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

REM Copy JAR
echo.
echo [3/4] Copie des fichiers...
for %%f in (target\agent-cli-*.jar) do (
    copy "%%f" "%INSTALL_DIR%\agentcli.jar" >nul
)

REM Create launcher batch file
echo @echo off > "%BIN_DIR%\agentcli.bat"
echo REM Agent CLI Launcher for Windows >> "%BIN_DIR%\agentcli.bat"
echo. >> "%BIN_DIR%\agentcli.bat"
echo set "AGENTCLI_JAR=%%USERPROFILE%%\.agentcli\agentcli.jar" >> "%BIN_DIR%\agentcli.bat"
echo. >> "%BIN_DIR%\agentcli.bat"
echo if not exist "%%AGENTCLI_JAR%%" ( >> "%BIN_DIR%\agentcli.bat"
echo     echo Agent CLI n'est pas installe correctement. >> "%BIN_DIR%\agentcli.bat"
echo     echo Reinstallez avec: install.bat >> "%BIN_DIR%\agentcli.bat"
echo     pause >> "%BIN_DIR%\agentcli.bat"
echo     exit /b 1 >> "%BIN_DIR%\agentcli.bat"
echo ) >> "%BIN_DIR%\agentcli.bat"
echo. >> "%BIN_DIR%\agentcli.bat"
echo REM Run directly - user should execute this from a proper command prompt >> "%BIN_DIR%\agentcli.bat"
echo java -jar "%%AGENTCLI_JAR%%" %%* >> "%BIN_DIR%\agentcli.bat"

echo [OK] Fichiers copies

REM Check and add to PATH
echo.
echo [4/4] Configuration du PATH...

REM Check if already in PATH
echo %PATH% | find /i "%BIN_DIR%" >nul
if %errorlevel% equ 0 (
    echo [OK] PATH deja configure
    goto :path_done
)

REM Add to user PATH
echo.
echo Ajout de %BIN_DIR% au PATH...
echo.

REM Get current user PATH
for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v PATH 2^>nul') do set "CURRENT_PATH=%%b"

REM Check if path already exists in registry
echo %CURRENT_PATH% | find /i "%BIN_DIR%" >nul
if %errorlevel% equ 0 (
    echo [INFO] PATH deja configure dans le registre
    goto :path_done
)

REM Add to PATH using setx
if "%CURRENT_PATH%"=="" (
    setx PATH "%BIN_DIR%"
) else (
    setx PATH "%BIN_DIR%;%CURRENT_PATH%"
)

if errorlevel 1 (
    echo [ATTENTION] Impossible d'ajouter automatiquement au PATH.
    echo Ajoutez manuellement: %BIN_DIR%
    echo.
    echo Instructions:
    echo 1. Ouvrir: Parametres systeme avances ^> Variables d'environnement
    echo 2. Dans "Variables utilisateur", modifier "Path"
    echo 3. Ajouter: %BIN_DIR%
    goto :path_done
)

echo [OK] PATH mis a jour dans le registre

:path_done

echo.
echo ========================================
echo   Installation terminee avec succes!
echo ========================================
echo.
echo Installation: %INSTALL_DIR%
echo Executable:   %BIN_DIR%\agentcli.bat
echo.
echo IMPORTANT: Redemarrez votre terminal pour que les
echo changements de PATH prennent effet.
echo.
echo ========================================
echo   Utilisation
echo ========================================
echo.
echo Apres avoir redemarre le terminal:
echo   cd votre-projet
echo   agentcli
echo.
echo Pour tester immediatement (sans redemarrer):
echo   "%BIN_DIR%\agentcli.bat"
echo.
echo ========================================
echo   Desinstallation
echo ========================================
echo.
echo Pour desinstaller:
echo   rmdir /s /q "%INSTALL_DIR%"
echo   Puis retirez %BIN_DIR% de votre PATH
echo.
pause
