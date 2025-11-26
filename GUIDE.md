# 📖 Guide Complet - Agent CLI v3.0.0

Guide d'utilisation complet d'Agent CLI, l'assistant de développement IA intelligent multi-langage.

---

## 📑 Table des Matières

1. [Installation](#-installation)
2. [Configuration LLM](#-configuration-llm)
3. [Menu Interactif](#-menu-interactif)
4. [Les 28 Outils](#-les-28-outils)
5. [Fonctionnalités Intelligentes](#-fonctionnalités-intelligentes)
6. [Support Multi-Langage](#-support-multi-langage)
7. [Configuration Avancée](#-configuration-avancée)
8. [Cas d'Usage](#-cas-dusage)
9. [Dépannage](#-dépannage)
10. [Architecture](#-architecture)

---

## 🚀 Installation

### Linux / macOS

#### Installation Automatique

```bash
cd AgentCLI
chmod +x install.sh
./install.sh
```

Le script va :
- ✅ Compiler le projet avec Maven
- ✅ Créer `~/.agentcli/` avec le JAR
- ✅ Créer l'exécutable `~/.local/bin/agentcli`
- ✅ Vérifier votre PATH

#### Configuration du PATH

Si `~/.local/bin` n'est pas dans votre PATH, ajoutez à `~/.bashrc` ou `~/.zshrc` :

```bash
export PATH="$HOME/.local/bin:$PATH"
```

Puis rechargez :
```bash
source ~/.bashrc  # ou source ~/.zshrc
```

### Windows

#### Installation Automatique

```batch
cd AgentCLI
install.bat
```

Le script va :
- ✅ Compiler avec Maven
- ✅ Créer `%USERPROFILE%\.agentcli\`
- ✅ Créer `agentcli.bat` dans le PATH

#### Ajouter au PATH (si nécessaire)

1. Ouvrir "Variables d'environnement"
2. Éditer la variable `Path`
3. Ajouter : `%USERPROFILE%\.agentcli\bin`

### Installation Manuelle

```bash
# Compiler le projet
mvn clean package
# ou
./mvnw clean package

# Copier le JAR
mkdir -p ~/.agentcli
cp target/agent-cli-3.0.0-SNAPSHOT.jar ~/.agentcli/agentcli.jar

# Créer un script d'exécution
mkdir -p ~/.local/bin
echo '#!/bin/bash' > ~/.local/bin/agentcli
echo 'java -jar ~/.agentcli/agentcli.jar "$@"' >> ~/.local/bin/agentcli
chmod +x ~/.local/bin/agentcli
```

### Vérification

```bash
agentcli
```

Vous devriez voir le menu de configuration ou l'interface Agent CLI.

---

## 🤖 Configuration LLM

Agent CLI supporte **5 providers LLM** différents. Choisissez celui qui vous convient !

### Providers Disponibles

| Provider | Type | Avantages | Inconvénients |
|----------|------|-----------|---------------|
| **Ollama Local** | Local | Gratuit, Privé, Rapide, Sans limites | Nécessite installation |
| **Ollama Cloud** | Cloud | Puissant, Modèles énormes, Pas d'install | Nécessite clé API |
| **LLM Studio** | Local | GUI facile, Gratuit | Moins de modèles |
| **OpenAI** | Cloud | Très performant, GPT-4 | Payant |
| **Custom** | Variable | Flexible | Configuration manuelle |

---

### Option 1 : Ollama Local (Recommandé)

#### Installation d'Ollama

**Linux / macOS :**
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

**Windows :**
Téléchargez depuis https://ollama.ai/

#### Démarrer Ollama

```bash
ollama serve
```

#### Télécharger un Modèle

```bash
# Modèles recommandés pour le code :
ollama pull qwen2.5-coder:7b      # Recommandé - bon équilibre
ollama pull deepseek-coder:6.7b   # Rapide
ollama pull codellama:13b         # Précis mais lent
ollama pull deepseek-coder:1.3b   # Très rapide, pour tests
```

#### Configuration

Au premier lancement d'`agentcli`, choisissez **1 (Ollama Local)** dans le menu.

Ou configurez manuellement dans `~/.agentcli/llm-config.yml` :

```yaml
llm:
  provider: OLLAMA_LOCAL
  endpoint: http://localhost:11434
  model: qwen2.5-coder:7b
  timeout: 120
```

---

### Option 2 : Ollama Cloud

#### Créer un Compte

1. Allez sur https://ollama.com
2. Créez un compte

#### Obtenir une Clé API

1. Allez sur https://ollama.com/settings/keys
2. Cliquez sur "Create API Key"
3. Copiez la clé

#### Configuration

Au premier lancement d'`agentcli`, choisissez **3 (Ollama Cloud)** :

```
Votre choix [1-5] (défaut: 1): 3

🔧 Configuration de Ollama Cloud

Endpoint [https://ollama.com]: ⏎           (Appuyez sur Entrée)
Modèle [llama3.1:latest]: ⏎                (Ou tapez votre modèle préféré)
API Key: ****************************      (Collez votre clé API)

Sauvegarder cette configuration ? [O/n]: o
```

**Modèles disponibles sur Ollama Cloud :**
- `llama3.1:latest` (par défaut)
- `qwen2.5-coder:480b` (très puissant)
- `mixtral:8x7b`
- Et bien d'autres sur https://ollama.com/library

---

### Option 3 : LLM Studio

#### Installation

1. Téléchargez depuis https://lmstudio.ai/
2. Installez l'application
3. Téléchargez un modèle dans l'interface
4. Démarrez le serveur local (bouton "Start Server")

#### Configuration

Choisissez **2 (LLM Studio)** dans le menu interactif :

```
Endpoint [http://localhost:1234]: ⏎
Modèle [votre-modele-ici]: qwen2.5-coder-7b-instruct
```

---

### Option 4 : OpenAI

#### Obtenir une Clé API

1. Créez un compte sur https://platform.openai.com/
2. Allez dans API Keys
3. Créez une nouvelle clé

#### Configuration

Choisissez **4 (OpenAI)** dans le menu :

```
Endpoint [https://api.openai.com/v1]: ⏎
Modèle [gpt-4]: gpt-4-turbo         (ou gpt-3.5-turbo)
API Key: sk-...                      (Votre clé OpenAI)
```

**⚠️ Attention : OpenAI est payant à l'usage**

---

### Option 5 : Custom (Service Personnalisé)

Pour utiliser votre propre service compatible avec l'API Ollama ou OpenAI :

```
Votre choix [1-5]: 5

Endpoint: http://votre-serveur:port
Modèle: votre-modele
API Key: (optionnel)
```

---

### Commande @llm

Gérez votre configuration LLM depuis Agent CLI :

```
>>> @llm info
🤖 Configuration LLM actuelle

Provider: Ollama Local
Endpoint: http://localhost:11434
Modèle: qwen2.5-coder:7b
Timeout: 120s

>>> @llm test
🔍 Test de connexion au LLM...
✓ Connexion réussie
✓ Modèle disponible
✓ Temps de réponse: 234ms

>>> @llm providers
📋 Providers LLM disponibles:
1. OLLAMA_LOCAL    - http://localhost:11434
2. LLM_STUDIO      - http://localhost:1234
3. OLLAMA_CLOUD    - https://ollama.com
4. OPENAI          - https://api.openai.com/v1
5. CUSTOM          - Configuration personnalisée

>>> @llm reload
🔄 Rechargement de la configuration LLM...
✓ Configuration rechargée depuis ~/.agentcli/llm-config.yml
```

---

## 🎯 Menu Interactif

Au premier lancement, Agent CLI affiche un menu interactif pour configurer votre provider LLM.

### Fonctionnement

```
╔═══════════════════════════════════════════════════════════╗
║           🤖 Configuration LLM - Agent CLI               ║
╚═══════════════════════════════════════════════════════════╝

📋 Sélectionnez votre provider LLM:

1. Ollama Local    (Gratuit, Privé, Recommandé)
2. LLM Studio      (GUI, Facile à utiliser)
3. Ollama Cloud    (Puissant, Modèles énormes)
4. OpenAI          (GPT-4, Payant)
5. Custom          (Votre propre service)

Votre choix [1-5] (défaut: 1):
```

### Sauvegarde Automatique

La configuration est automatiquement sauvegardée dans :
- Linux/macOS : `~/.agentcli/llm-config.yml`
- Windows : `%USERPROFILE%\.agentcli\llm-config.yml`

### Forcer le Menu de Configuration

Pour afficher le menu même si une configuration existe :

```bash
agentcli --configure
```

### Changer de Provider

```bash
# Supprimer la config sauvegardée
rm ~/.agentcli/llm-config.yml

# Relancer
agentcli
```

Ou utiliser `--configure`.

---

## 🛠️ Les 28 Outils

Agent CLI v3.0.0 inclut 28 outils puissants.

### Outils Fichiers & Projet

#### @file - Analyse de Fichier

Lit et analyse un fichier avec l'IA.

**Syntaxe :**
```
@file <chemin/vers/fichier>
```

**Exemples :**
```
>>> @file src/main/java/UserService.java
>>> @file package.json
>>> @file README.md
```

---

#### @search - Recherche Avancée

Recherche intelligente avec regex, filtres et contexte.

**Syntaxe :**
```
@search <terme> [options]
```

**Options :**
- `--regex` ou `-r` - Utiliser les expressions régulières
- `--case` ou `-c` - Recherche sensible à la casse
- `--ext <exts>` - Filtrer par extensions (ex: `--ext java,py,js`)
- `--context N` - Afficher N lignes de contexte (défaut: 2)
- `--limit N` - Limiter les résultats (défaut: 50)
- `--files` - Rechercher uniquement dans les noms de fichiers

**Exemples :**
```
>>> @search "UserService"
Trouvé dans 5 fichiers avec highlighting

>>> @search "class.*Service" --regex --ext java
Recherche avec regex dans les fichiers Java uniquement

>>> @search "authentication" --context 5
Affiche 5 lignes de contexte autour des correspondances

>>> @search "config" --files
Recherche uniquement dans les noms de fichiers
```

---

#### @websearch - Recherche Web

Recherche sur le web via DuckDuckGo avec résumé optionnel par l'IA.

**Syntaxe :**
```
@websearch "query" [options]
```

**Options :**
- `--summarize` - Obtenir un résumé généré par l'IA
- `--limit N` - Limiter le nombre de résultats (défaut: 5, max: 10)

**Exemples :**
```
>>> @websearch "Java 21 features"
Recherche web et affiche les résultats bruts

>>> @websearch "Spring Boot best practices" --summarize
Recherche et génère un résumé IA des résultats

>>> @websearch "machine learning" --limit 3 --summarize
Limite à 3 résultats et génère un résumé
```

**Utilisation typique :**
- Rechercher des documentations récentes
- Trouver des solutions à des problèmes
- Se tenir informé des dernières technologies
- Vérifier les best practices actuelles

---

#### @tree - Arborescence du Projet

Affiche l'arborescence du projet.

**Syntaxe :**
```
@tree [profondeur]
```

**Exemples :**
```
>>> @tree
>>> @tree 2
>>> @tree 3
```

---

#### @analyze-project - Analyse Complète

Analyse complète du projet avec détection automatique.

**Syntaxe :**
```
@analyze-project
```

---

### Outils Qualité de Code

#### @review - Revue de Code Automatisée

Revue de code automatisée pour qualité et meilleures pratiques.

**Syntaxe :**
```
@review <fichier_ou_pattern>
```

**Exemples :**
```
>>> @review src/Main.java
Réviser un seul fichier

>>> @review src/**/*.java
Réviser tous les fichiers Java

>>> @review .
Réviser tous les fichiers sources du projet
```

**Vérifie :**
- Code smells et anti-patterns (20+ règles)
- Violations des meilleures pratiques
- Bugs potentiels
- Complexité du code
- Problèmes de sécurité
- Style et formatage

---

#### @refactor - Suggestions de Refactoring

Analyse un fichier et propose des améliorations.

**Syntaxe :**
```
@refactor <fichier>
```

---

#### @generate-test - Génération de Tests

Génère des tests unitaires pour un fichier.

**Syntaxe :**
```
@generate-test <fichier>
```

**Adaptations par langage :**
- Java : JUnit 5 + Mockito
- Python : pytest + mock
- JavaScript/TypeScript : Jest
- Go : testing package
- Rust : cargo test

---

### Outils d'Exécution

#### @execute - Exécuter des Commandes

Exécute des commandes adaptées au type de projet.

**Syntaxe :**
```
@execute [test|build|run]
```

**Adaptations par projet :**
- Java Maven : `mvn test`, `mvn clean package`
- Java Gradle : `./gradlew test`, `./gradlew build`
- Python : `pytest`, `python setup.py build`
- Node.js : `npm test`, `npm run build`
- Go : `go test ./...`, `go build`

---

### Intégration Git

#### @git - Opérations Git Natives

Intégration Git complète sans quitter le CLI.

**Syntaxe :**
```
@git <commande>
```

**Commandes disponibles :**
- `status` - État complet du repo avec statistiques
- `diff` - Différences avec coloration syntaxique
- `log` - Historique des commits (graphique)
- `branch` - Liste des branches
- `staged` - Changements staged
- `unstaged` - Changements non-staged
- `files` - Liste des fichiers trackés
- `blame <file>` - Git blame pour un fichier

---

### Planification & Exécution

#### @plan - Planification Multi-Étapes

Crée et exécute des plans multi-étapes.

**Syntaxe :**
```
@plan [create|show|clear|execute]
```

**Commandes :**
- `create` - Créer un nouveau plan
- `show` - Afficher le plan actuel
- `execute` - Exécuter automatiquement le plan
- `clear` - Effacer le plan actuel

**Fonctionnalités :**
- Exécution automatique de chaque tâche
- Intégration LLM pour chaque étape
- Gestion interactive des erreurs
- Suivi de progression en temps réel
- Support de l'interruption Ctrl+C

---

### Gestion des Erreurs

#### @errors - Historique & Apprentissage

Affiche l'historique des erreurs et les insights d'apprentissage.

**Syntaxe :**
```
@errors [command]
```

**Commandes :**
- `list [n]` - Affiche les n dernières erreurs (défaut: 10)
- `stats` - Statistiques d'erreurs par type
- `insights` - Insights d'apprentissage des erreurs passées
- `clear` - Efface l'historique
- `clearlearn` - Efface les patterns appris

---

### Outils de Configuration

#### @config - Gestion de Configuration

Gère la configuration du projet.

**Syntaxe :**
```
@config [init|show|example]
```

---

#### @llm - Gestion LLM

Gère la configuration LLM (voir section [Configuration LLM](#-configuration-llm)).

---

### Outils d'Aide

#### @help - Aide

Affiche l'aide pour toutes les commandes ou une commande spécifique.

**Syntaxe :**
```
@help [commande]
```

---

## 🧠 Fonctionnalités Intelligentes

Agent CLI v3.0.0 inclut plusieurs systèmes intelligents qui fonctionnent automatiquement.

### 1. Chargement de Contexte Incrémental

**Problème :** Charger tout le projet consomme trop de tokens et est lent.

**Solution :** Système intelligent qui charge uniquement les fichiers pertinents.

**Comment ça marche :**
- Détecte les fichiers explicitement mentionnés
- Analyse les imports et dépendances
- Utilise un cache LRU (50 fichiers)
- Limites : 10 fichiers max, 100KB total

**Performance :**
- 10x plus rapide qu'avant
- 90% moins de tokens utilisés
- Chargement en <100ms

---

### 2. Récupération & Apprentissage des Erreurs

**Problème :** Les erreurs se produisent mais le système n'apprend pas.

**Solution :** Système d'apprentissage persistant.

**Comment ça marche :**
- Enregistre toutes les erreurs avec contexte
- Détecte 8+ patterns d'erreurs
- Apprend les résolutions réussies
- Calcule la confiance (% de succès)
- Stockage dans `.agentcli/error-knowledge.json`

---

### 3. Validation de Code

**Problème :** Écrire du code qui ne compile pas fait perdre du temps.

**Solution :** Système de validation pré-écriture.

**Support :**
- Java (via Compiler API)
- Python (vérification syntaxe)
- JavaScript (vérification syntaxe)
- JSON, XML, YAML (parsing)

---

### 4. Lanceur de Tests

**Problème :** Oublier de lancer les tests après modifications.

**Solution :** Suggestion automatique de tests.

**Support :**
- Maven, Gradle
- pytest, npm test
- go test, cargo test

---

### 5. Analyse de Complexité

**Problème :** Ne pas savoir quand créer un plan.

**Solution :** Détection automatique de complexité des tâches.

**Algorithme de scoring :**
- Mots-clés : "implement", "create", "refactor" (+2)
- Fichiers multiples (+3)
- Changements framework/database (+2)
- Tests requis (+1)
- Étapes multiples (+1 par étape)

**Seuil :** Score >= 6 → suggère un plan

---

### 6. Injection de Contexte Intelligente

**Problème :** Le LLM manque de contexte projet.

**Solution :** Injection automatique de contexte.

**Ce qui est injecté :**
- Type et framework du projet
- Plan actuel (si présent)
- Structure des fichiers (parties pertinentes)
- Historique des erreurs (récentes)
- Configuration

---

## 🌍 Support Multi-Langage

Agent CLI détecte automatiquement le type de projet et adapte son comportement.

### Langages Supportés

| Langage | Détection | Build Tool | Test Tool |
|---------|-----------|------------|-----------|
| Java | pom.xml, build.gradle | Maven, Gradle | JUnit, TestNG |
| Python | requirements.txt, setup.py | pip, poetry | pytest, unittest |
| JavaScript | package.json | npm, yarn | Jest, Mocha |
| TypeScript | tsconfig.json | npm, yarn | Jest |
| Go | go.mod | go | go test |
| Rust | Cargo.toml | cargo | cargo test |
| C# | *.csproj | dotnet | dotnet test |
| PHP | composer.json | composer | PHPUnit |
| Ruby | Gemfile | bundler | RSpec |

---

## ⚙️ Configuration Avancée

### Fichier .agentcli.yml

Créez un fichier `.agentcli.yml` à la racine de votre projet :

```yaml
# Nom du projet
projectName: "Mon Super Projet"

# Type de projet (auto, java, python, node, go, etc.)
projectType: "auto"

# Chemins à ignorer
ignorePaths:
  - ".git"
  - "node_modules"
  - "target"
  - "build"
  - "dist"
  - "__pycache__"
  - ".venv"

# Chemins spécifiques à inclure
includePaths:
  - "src"
  - "lib"
  - "app"

# Taille maximale de fichier (en octets)
maxFileSize: 1000000  # 1 MB

# Commandes personnalisées (aliases)
customCommands:
  "@t": "@execute test"
  "@b": "@execute build"
  "@r": "@execute run"
  "@a": "@analyze-project"

# Paramètres par outil
toolSettings:
  search:
    maxResults: 50
    caseSensitive: false
    defaultContext: 2
  tree:
    defaultDepth: 3
    showHidden: false
  analyze:
    includeMetrics: true
  review:
    strictMode: false
    maxFindings: 100
```

### Génération Automatique

```
>>> @config init
```

---

## 🎯 Cas d'Usage

### 1. Onboarding sur Nouveau Projet

```bash
cd nouveau-projet
agentcli
```

```
>>> @analyze-project
>>> @tree
>>> @search "main"
>>> @file src/index.js
>>> Explique-moi l'architecture de ce projet
```

### 2. Développement de Fonctionnalité

```
>>> @plan create
>>> Ce que je veux construire: Ajouter l'authentification JWT
>>> @plan execute
>>> @execute test
>>> @review src/security/
```

### 3. Debugging

```
>>> @search "NullPointerException"
>>> @file src/problematic/File.java
>>> Pourquoi ce code génère une NPE?
>>> @refactor File.java
>>> @execute test
```

### 4. Revue de Code

```
>>> @review .
>>> @errors insights
>>> @search "TODO"
>>> @refactor src/service/PaymentService.java
>>> Quelles sont les failles de sécurité potentielles?
```

### 5. Refactoring

```
>>> @review src/legacy/
>>> @refactor src/legacy/OldCode.java
>>> @generate-test OldCode.java
>>> @execute test
>>> @git status
>>> @git diff
```

---

## 🐛 Dépannage

### `agentcli: command not found`

**Cause :** `~/.local/bin` n'est pas dans le PATH.

**Solution :**
```bash
# Linux / macOS - Ajoutez à ~/.bashrc ou ~/.zshrc
export PATH="$HOME/.local/bin:$PATH"

# Rechargez
source ~/.bashrc
```

---

### `Connection refused` (Ollama)

**Cause :** Ollama n'est pas démarré.

**Solution :**
```bash
ollama serve
```

---

### `Model not found`

**Cause :** Le modèle n'est pas téléchargé.

**Solution :**
```bash
ollama list  # Voir les modèles installés
ollama pull qwen2.5-coder:7b  # Télécharger un modèle
```

---

### Réponses Lentes

**Cause :** Modèle trop gros ou machine insuffisante.

**Solution :**
Utilisez un modèle plus petit :
```bash
ollama pull deepseek-coder:1.3b
```

Puis changez la configuration :
```bash
agentcli --configure
```

---

## 🏗️ Architecture

### Structure du Projet

```
src/main/java/org/seba/agentcli/
├── CliAgent.java                # Point d'entrée
├── CliService.java              # Service LLM (multi-provider)
├── CommandProcessor.java        # Traitement des commandes
│
├── config/
│   ├── LLMConfig.java           # Configuration LLM
│   ├── AgentConfig.java         # Configuration projet
│   └── ConfigLoader.java        # Chargeur YAML
│
├── context/
│   ├── IncrementalContextLoader.java  # Chargement de contexte intelligent
│   ├── PlanExecutor.java              # Moteur d'exécution de plans
│   └── PlanManager.java               # Gestion des plans
│
├── detector/
│   └── ProjectDetector.java     # Détection multi-langage
│
├── model/
│   ├── ProjectType.java         # Enum des types de projets
│   ├── ProjectContext.java      # Contexte du projet
│   ├── LLMProvider.java         # Enum des providers
│   └── TaskPlan.java            # Représentation des plans
│
├── recovery/
│   ├── ErrorRecoveryManager.java    # Gestion des erreurs
│   └── ErrorLearningSystem.java     # Apprentissage des erreurs
│
├── review/
│   └── CodeReviewService.java   # Revue de code (20+ règles)
│
├── validation/
│   ├── CodeValidator.java       # Validation pré-écriture
│   └── TestRunner.java          # Exécution de tests
│
├── io/
│   ├── AnsiColors.java          # Couleurs terminal
│   ├── BoxDrawer.java           # Boîtes UI
│   ├── AdvancedPrompt.java      # Prompt contextuel
│   ├── CommandHistory.java      # Historique persistant
│   ├── InputReader.java         # Lecture améliorée
│   └── CommandSuggester.java    # Suggestions
│
├── ui/
│   └── LLMSelector.java         # Menu interactif
│
└── tool/
    ├── Tool.java                # Interface
    ├── AbstractTool.java        # Classe de base
    ├── ToolRegistry.java        # Registre
    │
    └── impl/                    # 20 implémentations
        ├── FileTool.java
        ├── SearchTool.java
        ├── TreeTool.java
        ├── AnalyzeProjectTool.java
        ├── GenerateTestTool.java
        ├── RefactorTool.java
        ├── ExecuteTool.java
        ├── ConfigTool.java
        ├── LLMTool.java
        ├── HelpTool.java
        ├── GitTool.java          # NOUVEAU v3.0
        ├── PlanTool.java         # NOUVEAU v3.0
        ├── ErrorsTool.java       # NOUVEAU v3.0
        └── ReviewTool.java       # NOUVEAU v3.0
```

### Système d'Outils Extensible

Créez facilement de nouveaux outils :

```java
@Component
public class MyCustomTool extends AbstractTool {

    public MyCustomTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@mycmd";
    }

    @Override
    public String getDescription() {
        return "Ma commande personnalisée";
    }

    @Override
    public String getUsage() {
        return "@mycmd <args>";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        // Votre logique ici
        return "Résultat de ma commande";
    }
}
```

Spring Boot enregistrera automatiquement votre outil ! ✨

### Technologies Utilisées

- **Spring Boot 3.2.0** - Framework principal
- **WebFlux** - Client HTTP réactif pour les API LLM
- **Jackson** - Parsing YAML et JSON
- **Maven** - Build et dépendances
- **Java Console/BufferedReader** - I/O terminal (100% homemade, pas de bibliothèque CLI)

### Patterns de Conception Clés

- **Injection de Dépendances** - Tous les composants via Spring
- **Pattern Strategy** - Système d'outils et providers LLM
- **Pattern Observer** - Système d'apprentissage des erreurs
- **Template Method** - Classe de base AbstractTool
- **Pattern Registry** - ToolRegistry pour la gestion des outils
- **Pattern Builder** - Objets complexes (Plans, Contextes)

---

## 📊 Statistiques v3.0.0

**Code :**
- 67 fichiers Java (+4 depuis refactoring)
- ~12,500 lignes de code
- 28 outils

**Fonctionnalités :**
- 7 systèmes intelligents
- 5 providers LLM
- 9+ langages de programmation
- 20+ règles de revue de code

**Performance :**
- Chargement de contexte : 10x plus rapide
- Utilisation de tokens : -90%
- Temps de réponse : Considérablement réduit

---

## 🤝 Contribution

Pour contribuer au projet :

1. **Fork** le repository
2. Créez une **branche** : `git checkout -b feature/MaFeature`
3. **Codez** votre feature
4. **Testez** : `mvn test`
5. **Commit** : `git commit -m 'Add: Ma nouvelle feature'`
6. **Push** : `git push origin feature/MaFeature`
7. Ouvrez une **Pull Request**

### Guidelines

- Suivez les conventions de code existantes
- Ajoutez des tests pour les nouvelles fonctionnalités
- Mettez à jour la documentation
- Utilisez des messages de commit clairs
- Lancez `@review` avant de soumettre

---

## 🔮 Roadmap v3.1+

**Priorité Haute :**
- [ ] Refactoring atomique multi-fichiers
- [ ] Gestionnaire de dépendances intelligent
- [ ] Intelligence de couverture de tests
- [ ] Bot de revue de PR
- [ ] Intégration Docker

**Priorité Moyenne :**
- [ ] Générateur de pipeline CI/CD
- [ ] Profileur de performance
- [ ] Scanner de sécurité
- [ ] Résolveur de merge intelligent
- [ ] Auto-documentation

**Priorité Basse :**
- [ ] Commandes vocales
- [ ] Plugins IDE (VSCode, IntelliJ)
- [ ] Tableau de bord web
- [ ] Apprentissage d'équipe
- [ ] Moteur de règles personnalisées

---

## 📚 Ressources

- **Documentation Ollama** : https://ollama.ai/
- **Documentation Spring Boot** : https://spring.io/projects/spring-boot
- **Ollama Cloud** : https://docs.ollama.com/cloud

---

**Retour au README principal : [README.md](README.md)**

**Historique des versions : [CHANGELOG.md](CHANGELOG.md)**
