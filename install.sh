#!/bin/bash
# Installation script for Agent CLI

set -e

echo "🚀 Installation d'Agent CLI..."

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé. Installez Java 21+ d'abord."
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "⚠️  Maven n'est pas installé. Utilisation du wrapper Maven..."
    MAVEN_CMD="./mvnw"
else
    MAVEN_CMD="mvn"
fi

# Build the project
echo "📦 Compilation du projet..."
$MAVEN_CMD clean package -DskipTests

# Create bin directory in home
BIN_DIR="$HOME/.local/bin"
mkdir -p "$BIN_DIR"

# Copy JAR
INSTALL_DIR="$HOME/.agentcli"
mkdir -p "$INSTALL_DIR"

echo "📋 Copie des fichiers..."
cp target/agent-cli-*.jar "$INSTALL_DIR/agentcli.jar"

# Create executable script
cat > "$BIN_DIR/agentcli" << 'EOF'
#!/bin/bash
# Agent CLI Launcher

AGENTCLI_JAR="$HOME/.agentcli/agentcli.jar"

if [ ! -f "$AGENTCLI_JAR" ]; then
    echo "❌ Agent CLI n'est pas installé correctement."
    echo "Réinstallez avec: ./install.sh"
    exit 1
fi

# Run from current directory
java -jar "$AGENTCLI_JAR" "$@"
EOF

chmod +x "$BIN_DIR/agentcli"

echo ""
echo "✅ Agent CLI installé avec succès!"
echo ""
echo "📍 Installation dans: $INSTALL_DIR"
echo "🔗 Exécutable: $BIN_DIR/agentcli"
echo ""

# Function to add PATH to shell config
add_to_path() {
    local config_file="$1"
    local path_line='export PATH="$HOME/.local/bin:$PATH"'

    # Check if line already exists
    if grep -q "/.local/bin" "$config_file" 2>/dev/null; then
        return 1  # Already exists
    fi

    # Add to config file
    echo "" >> "$config_file"
    echo "# Agent CLI - Added by install script" >> "$config_file"
    echo "$path_line" >> "$config_file"
    return 0
}

# Check if ~/.local/bin is in PATH
if [[ ":$PATH:" != *":$HOME/.local/bin:"* ]]; then
    echo "🔧 Configuration du PATH..."
    echo ""

    # Detect shell and add to appropriate config file
    SHELL_NAME=$(basename "$SHELL")
    CONFIG_UPDATED=false

    case "$SHELL_NAME" in
        bash)
            if [ -f "$HOME/.bashrc" ]; then
                if add_to_path "$HOME/.bashrc"; then
                    echo "✅ PATH ajouté à ~/.bashrc"
                    CONFIG_UPDATED=true
                    CONFIG_FILE="~/.bashrc"
                else
                    echo "ℹ️  PATH déjà configuré dans ~/.bashrc"
                fi
            fi
            ;;
        zsh)
            if [ -f "$HOME/.zshrc" ]; then
                if add_to_path "$HOME/.zshrc"; then
                    echo "✅ PATH ajouté à ~/.zshrc"
                    CONFIG_UPDATED=true
                    CONFIG_FILE="~/.zshrc"
                else
                    echo "ℹ️  PATH déjà configuré dans ~/.zshrc"
                fi
            fi
            ;;
        fish)
            FISH_CONFIG="$HOME/.config/fish/config.fish"
            if [ -f "$FISH_CONFIG" ]; then
                if ! grep -q "/.local/bin" "$FISH_CONFIG" 2>/dev/null; then
                    echo "" >> "$FISH_CONFIG"
                    echo "# Agent CLI - Added by install script" >> "$FISH_CONFIG"
                    echo 'set -gx PATH $HOME/.local/bin $PATH' >> "$FISH_CONFIG"
                    echo "✅ PATH ajouté à ~/.config/fish/config.fish"
                    CONFIG_UPDATED=true
                    CONFIG_FILE="~/.config/fish/config.fish"
                else
                    echo "ℹ️  PATH déjà configuré dans config.fish"
                fi
            fi
            ;;
        *)
            echo "⚠️  Shell non reconnu: $SHELL_NAME"
            echo ""
            echo "Ajoutez manuellement à votre fichier de config:"
            echo '  export PATH="$HOME/.local/bin:$PATH"'
            echo ""
            ;;
    esac

    if [ "$CONFIG_UPDATED" = true ]; then
        echo ""
        echo "🔄 Pour activer immédiatement, exécutez:"
        echo "  source $CONFIG_FILE"
        echo ""
        echo "Ou redémarrez votre terminal."
        echo ""

        # Ask user if they want to reload
        read -p "Voulez-vous recharger la configuration maintenant? [O/n]: " -n 1 -r
        echo
        if [[ $REPLY =~ ^[OoYy]$ ]] || [[ -z $REPLY ]]; then
            # Try to reload in current shell
            case "$SHELL_NAME" in
                bash|zsh)
                    export PATH="$HOME/.local/bin:$PATH"
                    echo "✅ Configuration rechargée!"
                    echo ""
                    echo "Note: Pour les nouveaux terminaux, ça fonctionnera automatiquement."
                    ;;
            esac
        fi
    fi
else
    echo "✅ PATH déjà configuré correctement"
fi

echo ""
echo "🎯 Utilisation:"
echo "  cd votre-projet"
echo "  agentcli"
echo ""
echo "Pour tester immédiatement (sans recharger le shell):"
echo "  $BIN_DIR/agentcli"
echo ""
echo "Pour désinstaller:"
echo "  rm -rf $INSTALL_DIR"
echo "  rm $BIN_DIR/agentcli"
echo "  # Puis supprimez la ligne PATH de votre fichier de config"
echo ""
