# 🤖 Agent CLI - AI-Powered Development Assistant

> Un assistant de développement intelligent, autonome et apprenant, qui transforme votre workflow de développement.

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Tools](https://img.shields.io/badge/Tools-28-blue.svg)](#-les-28-outils)

---

## 🚀 Pourquoi Agent CLI ?

**Agent CLI n'est pas juste un "wrapper" LLM.** C'est un véritable assistant qui :

- ✅ **Apprend de ses erreurs** et mémorise les solutions qui fonctionnent
- ✅ **Exécute des plans multi-étapes** automatiquement avec gestion d'erreurs
- ✅ **Review votre code** avant chaque commit avec 20+ règles de qualité
- ✅ **Charge intelligemment** uniquement le contexte pertinent (10x plus rapide)
- ✅ **Intègre Git nativement** (status, diff, log, branches)
- ✅ **S'adapte à 10+ langages** et détecte automatiquement votre projet

**Comparé à Claude Code, Cursor ou autres :**
- 💎 **Learning persistant** : Mémorise réellement les solutions (unique)
- 🤖 **Exécution automatique** : Plans qui s'exécutent seuls
- 🔍 **Git natif** : Plus besoin de quitter le CLI
- 🎯 **Context intelligent** : Charge uniquement ce qui est nécessaire
- 🔒 **100% local possible** : Vos données restent chez vous

---

## ✨ Les 28 Outils

### 📁 **Gestion de Fichiers**
```bash
@read <file>              # Lecture intelligente
@write <file>             # Écriture avec validation
@edit <file>              # Édition search/replace
@file <path>              # Navigation et analyse
@tree [depth]             # Vue arborescente
@undo                     # Annulation d'opérations
```

### 🔍 **Recherche & Navigation**
```bash
@search <pattern>         # Recherche avancée avec regex
  --regex                 # Mode regex
  --case                  # Case-sensitive
  --ext java,py           # Filtrer par extensions
  --context 3             # Lignes de contexte
  --files                 # Chercher dans les noms

@websearch "query"        # Recherche web (DuckDuckGo)
  --summarize             # Résumé par l'IA
  --limit N               # Limiter résultats (max: 10)
```

### 🔀 **Git Integration**
```bash
@git status               # État du repo
@git diff [file]          # Différences
@git log [n]              # Historique
@git branch               # Branches
@git staged               # Changements staged
@git unstaged             # Changements non-staged
@git files                # Fichiers trackés
@git blame <file>         # Git blame
```

### 📋 **Planification & Exécution**
```bash
@plan create <goal>       # Créer un plan avec l'IA
@plan show                # Afficher le plan actuel
@plan execute             # ⚡ EXÉCUTION AUTOMATIQUE
@plan start <n>           # Démarrer tâche n
@plan complete <n>        # Marquer complétée
@plan fail <n>            # Marquer échouée
@plan clear               # Effacer le plan
```

### 🔧 **Qualité & Review**
```bash
@review <file|pattern>    # Code review automatique
  Score 0-100             # Score de qualité
  20+ règles              # Détection de problèmes
  Best practices          # Vérifications
```

### ❌ **Erreurs & Apprentissage**
```bash
@errors list [n]          # Historique des erreurs
@errors stats             # Statistiques
@errors insights          # 🧠 Ce que l'IA a appris
@errors clear             # Effacer l'historique
@errors clearlearn        # Effacer la mémoire
```

### 🔬 **Analyse & Refactoring**
```bash
@analyze-project          # Analyse architecture

@refactor rename-class OldName NewName
  # 🔥 Refactoring atomique multi-fichiers
  # Analyse toutes les références
  # Preview + confirmation
  # Rollback automatique si erreur

@refactor rename-method old new [--class Class]
@refactor rename-variable old new [--scope file.java]

@generate-test <file>     # Génération de tests
@todo                     # Gestion de TODOs
```

### 📦 **Dependencies & Coverage**
```bash
@deps check               # Analyser les dépendances
@deps outdated            # Trouver versions obsolètes
@deps security            # Scan de vulnérabilités
@deps unused              # Dépendances inutilisées

@coverage analyze         # Analyser le coverage
@coverage gaps            # Zones non testées
@coverage generate        # Générer tests manquants
```

### 🤖 **PR & Docker**
```bash
@pr review                # Review automatique
@pr checklist             # Vérifier checklist
@pr suggest               # Suggestions IA
@pr ready                 # Valider ready to merge

@docker init              # Générer Dockerfile
@docker compose           # docker-compose.yml
@docker optimize          # Optimiser l'image
@docker security          # Scan de sécurité
```

### ⚙️ **CI/CD, Performance & Security**
```bash
@ci setup github          # GitHub Actions workflow
@ci setup gitlab          # GitLab CI pipeline
@ci test                  # Pipeline de tests
@ci deploy                # Pipeline de déploiement

@perf analyze             # Détecter bottlenecks
@perf suggest             # Optimisations IA
@perf benchmark           # Lancer benchmarks

@security scan            # Scan complet
@security secrets         # Détecter secrets exposés
@security owasp           # Check OWASP Top 10
```

### ⚙️ **Configuration & Exécution**
```bash
@config init              # Créer config projet
@execute [test|build]     # Exécuter build/tests
@llm info                 # Gestion LLM
@help                     # Aide complète
```

---

## 🎯 Quick Start (3 minutes)

### 1️⃣ Installer un Provider LLM

**Option recommandée : Ollama (gratuit, local, privé)**

```bash
# Linux / macOS
curl -fsSL https://ollama.ai/install.sh | sh
ollama serve
ollama pull qwen2.5-coder:7b

# Windows
# Télécharger depuis https://ollama.ai/download
```

**Autres options :**
- **LLM Studio** : https://lmstudio.ai/ (GUI facile)
- **OpenAI** : https://platform.openai.com/ (payant)
- **Claude API** : https://www.anthropic.com/api

### 2️⃣ Installer Agent CLI

```bash
cd AgentCLI

# Linux / macOS
./install.sh

# Windows
install.bat
```

Le script installe automatiquement dans `~/.agentcli/` et crée la commande `agentcli`.

### 3️⃣ Premier Lancement

```bash
cd ~/mes-projets/mon-projet
agentcli
```

Un menu interactif vous guide pour la configuration LLM au premier lancement.

---

## 💡 Exemples Concrets

### Workflow Typique : Ajouter une Feature

```bash
# 1. Tâche complexe détectée automatiquement
>>> Ajoute l'authentification JWT

🔍 COMPLEX TASK DETECTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
This task requires multiple steps.

Would you like me to:
  a) Create a plan first (recommended)
  b) Proceed directly without plan

Your choice [a/b]: a

# 2. Plan généré automatiquement
>>> @plan create Add JWT authentication

✓ Plan créé avec 5 tâches:
1. Add JWT dependencies to pom.xml
2. Create JwtTokenProvider class
3. Create JwtAuthenticationFilter
4. Configure Spring Security
5. Add authentication endpoints

# 3. Exécution automatique !
>>> @plan execute

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  AUTOMATIC PLAN EXECUTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Task #1: Add JWT dependencies to pom.xml
✓ Task completed successfully

Task #2: Create JwtTokenProvider class
✓ Task completed successfully

Task #3: Create JwtAuthenticationFilter
✗ Task failed: Compilation error

What would you like to do?
  r) Retry this task
  s) Skip and continue to next task
  q) Stop execution

Your choice [r/s/q]: r

# L'IA apprend de l'erreur et propose une solution
🧠 Learned Solutions (confidence: 87%):
  • Check for missing imports
  • Verify class name matches file name

✓ Task completed on retry

# Continue automatiquement...
✓ All tasks completed!

# 4. Review avant commit
>>> @review src/**/*.java

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  CODE REVIEW REPORT
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Files Reviewed: 8
Total Findings: 3
Average Score: 89.2/100

✓ Code quality is good. Ready to commit.

# 5. Git workflow
>>> @git status
>>> @git diff
>>> git commit -m "feat: add JWT authentication"
```

### Explorer un Nouveau Projet

```bash
# Analyse complète
>>> @analyze-project

🔍 Project Analysis
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Type: Java Maven
Framework: Spring Boot 3.2.0
Files: 156 source files
Tests: JUnit 5 + Mockito

Architecture:
├── Controller Layer (12 files) - REST API
├── Service Layer (24 files) - Business logic
├── Repository Layer (8 files) - JPA/Hibernate
└── Configuration (6 files) - Security, JWT, CORS

# Recherche intelligente
>>> @search "authentication" --ext java --context 2

📄 src/main/java/config/SecurityConfig.java
────────────────────────────────────────────
  45│     @Bean
  46│     public SecurityFilterChain filterChain(HttpSecurity http) {
  47│         http.authorizeRequests()
  48│             .requestMatchers("/api/auth/**").permitAll()
  49│             .anyRequest().authenticated();

# Review de qualité
>>> @review src/main/java/service/UserService.java

Score: 92/100 ✓

Warnings:
  ⚠ Line 45: [Long Method] Method has 52 lines (max: 50)
  ℹ Line 78: [TODO Comment] TODO: Add caching
```

### Debugging Assisté

```bash
>>> Pourquoi mon test UserServiceTest échoue ?

🔍 Analysing test file and error logs...

Found issue in UserServiceTest.java:
- Line 34: Mock not initialized properly
- Missing @ExtendWith(MockitoExtension.class)

Suggested fix:
[Shows exact code change needed]

>>> Apply the fix
✓ Fix applied to UserServiceTest.java

>>> @execute test
✓ All tests passed (42/42)
```

---

## 🧠 Fonctionnalités Intelligentes

### 1. **Learning from Errors** 🎓

L'agent **apprend** de chaque erreur résolue et stocke les solutions dans `.agentcli/error-knowledge.json`.

```bash
>>> @errors insights

═══════════════════════════════════════════════════
  ERROR LEARNING INSIGHTS
═══════════════════════════════════════════════════

Total Patterns Learned: 47
Successful Resolutions: 123
Failed Attempts: 18

Most Resolved Patterns:
1. IOException (34 successes, 94% success rate)
2. NullPointerException (28 successes, 87%)
3. CompilationError (19 successes, 90%)

Top Solutions:
• "Check file permissions" (used 12 times, 100% success)
• "Add null checks" (used 8 times, 87% success)
• "Import missing classes" (used 15 times, 93% success)
```

**Résultat** : Les erreurs récurrentes sont résolues automatiquement grâce à l'historique.

### 2. **Incremental Context Loading** ⚡

**Avant** : Chargeait 100+ fichiers à chaque requête (lent, lourd)
**Maintenant** : Charge intelligemment 5-10 fichiers pertinents

```
Requête: "Explique le système d'authentification"

🔍 Loading relevant context...
✓ Loaded 7 files (23KB):
  • SecurityConfig.java
  • JwtTokenProvider.java
  • AuthenticationController.java
  • UserDetailsServiceImpl.java
  [+ 3 related files]

⚡ 10x plus rapide | 90% moins de tokens utilisés
```

### 3. **Smart Plan Execution** 🤖

Les plans ne sont plus des listes passives. Ils **s'exécutent** automatiquement :

- ✅ Exécute chaque tâche avec le LLM
- ✅ Gère les erreurs (retry/skip/stop)
- ✅ Met à jour le progress en temps réel
- ✅ Apprend des échecs pour améliorer

### 4. **Code Review Automatique** 🔍

20+ règles de qualité pour Java, Python, JavaScript :

**Détecte :**
- Empty catch blocks
- Hardcoded credentials
- Debug statements (System.out, console.log)
- Long methods (>50 lines)
- Deep nesting (>4 levels)
- Generic exception catching
- Resource leaks
- Security issues
- Best practice violations

**Score de qualité** : 0-100 par fichier

---

## 🌐 Support Multi-Langage

### Langages Détectés Automatiquement

| Langage | Frameworks | Build Tools |
|---------|-----------|-------------|
| **Java** | Spring Boot, Quarkus, Micronaut | Maven, Gradle |
| **Python** | Django, Flask, FastAPI | pip, poetry |
| **JavaScript** | React, Vue, Angular, Express | npm, yarn, pnpm |
| **TypeScript** | Next.js, Nest.js, Angular | npm, yarn |
| **Go** | Gin, Echo, Fiber | go modules |
| **Rust** | Actix, Rocket, Axum | cargo |
| **C#** | .NET, ASP.NET | dotnet |
| **PHP** | Laravel, Symfony | composer |
| **Ruby** | Rails, Sinatra | bundler |
| **C++** | - | cmake, make |

---

## ⚙️ Configuration Avancée

### Configuration Globale

`~/.agentcli/llm-config.yml` (créé au premier lancement)

```yaml
provider: OLLAMA_LOCAL
endpoint: http://localhost:11434
model: qwen2.5-coder:7b
```

### Configuration Par Projet

`.agentcli/config.yml` à la racine de votre projet :

```yaml
projectName: "My Awesome Project"
projectType: "auto"  # Détection auto

# Fichiers à ignorer
ignorePaths:
  - ".git"
  - "node_modules"
  - "target"
  - "build"
  - ".agentcli"

# Raccourcis personnalisés
customCommands:
  "@test": "@execute test"
  "@build": "@execute build"
  "@lint": "@review src/"

# Paramètres des outils
toolSettings:
  search:
    maxResults: 50
    defaultContext: 2

  review:
    minQualityScore: 80
    failOnErrors: true

  plan:
    autoExecute: false
    confirmEachStep: true

# Règles de code personnalisées
codeRules:
  - pattern: "System\\.out"
    severity: error
    message: "Use logger instead"
    exclude: ["**/test/**"]
```

Générer automatiquement :
```bash
>>> @config init
```

---

## 🏗️ Architecture Technique

```
src/main/java/org/seba/agentcli/
├── config/              # Configuration LLM multi-provider
├── context/             # Gestion du contexte & plans
│   ├── ContextManager
│   ├── IncrementalContextLoader  ⚡ Nouveau
│   ├── PlanManager
│   └── PlanExecutor              ⚡ Nouveau
├── detector/            # Détection type de projet
├── files/               # Opérations sur fichiers
│   ├── FileReaderService
│   ├── FileWriterService
│   ├── FileEditorService
│   ├── BackupManager
│   ├── CodeValidator             ⚡ Nouveau
│   └── TestRunner                ⚡ Nouveau
├── io/                  # Interface CLI (BufferedReader/Console)
├── model/               # Modèles de données
├── recovery/            # Gestion des erreurs
│   ├── ErrorRecoveryManager      ⚡ Nouveau
│   └── ErrorLearningSystem       ⚡ Nouveau
├── review/              # Code review
│   └── CodeReviewService         ⚡ Nouveau
├── tool/                # Architecture des outils
│   ├── Tool (interface)
│   ├── AbstractTool
│   └── impl/            # 28 outils
│       ├── GitTool               ⚡ Nouveau
│       ├── SearchTool            ⚡ Amélioré
│       ├── ReviewTool            ⚡ Nouveau
│       ├── ErrorsTool            ⚡ Nouveau
│       ├── PlanTool              ⚡ Amélioré
│       └── [15 autres...]
├── CliService           # Communication avec LLM
└── CliAgent             # Application principale
```

### Extensibilité

**Ajouter un nouvel outil :**

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
        return "My custom tool";
    }

    @Override
    public String getUsage() {
        return "@mycmd <args>";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        // Votre logique ici
        return "Result";
    }
}
```

Spring Boot l'enregistre automatiquement ! ✨

---

## 🚀 Build & Installation

### Prérequis

- **Java 21+**
- **Maven 3.6+** (ou utiliser `./mvnw`)
- **Un provider LLM** (Ollama recommandé)

### Build Manuel

```bash
# Compiler
./mvnw clean package

# Le JAR est créé dans target/
# agent-cli-3.0.0-SNAPSHOT.jar

# Exécuter directement
java -jar target/agent-cli-3.0.0-SNAPSHOT.jar

# Ou utiliser le script d'installation
./install.sh
```

### Installation Système

```bash
# Linux / macOS
sudo ./install.sh

# Windows (PowerShell en admin)
.\install.bat

# Vérifie l'installation
agentcli --version
```

---

## 🐛 Troubleshooting

### `agentcli: command not found`

```bash
# Ajouter au PATH
export PATH="$HOME/.local/bin:$PATH"

# Rendre permanent (ajouter à ~/.bashrc ou ~/.zshrc)
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc
```

### `Connection refused` (Ollama)

```bash
# Démarrer Ollama
ollama serve

# Vérifier qu'il tourne
curl http://localhost:11434/api/tags
```

### Réponses Lentes

```bash
# Utiliser un modèle plus rapide
ollama pull deepseek-coder:1.3b

# Ou
ollama pull qwen2.5-coder:3b
```

### Problèmes de Mémoire

```bash
# Augmenter la heap Java
export JAVA_OPTS="-Xmx2G"
agentcli
```

### Reset Complet

```bash
# Supprimer toute la config
rm -rf ~/.agentcli/

# Relancer (recrée la config)
agentcli --configure
```

---

## 📚 Documentation

- **[CHANGELOG.md](CHANGELOG.md)** - Historique des versions
- 📖 Ce README contient toute la documentation nécessaire pour démarrer

---

## 🤝 Contribution

Les contributions sont les bienvenues ! 🎉

### Comment Contribuer

1. **Fork** le projet
2. **Créer** une branche (`git checkout -b feature/AmazingFeature`)
3. **Commit** vos changements (`git commit -m 'feat: add AmazingFeature'`)
4. **Push** vers la branche (`git push origin feature/AmazingFeature`)
5. **Ouvrir** une Pull Request

### Roadmap

Fonctionnalités prévues (voir issue #roadmap) :

- [ ] Multi-file refactoring atomique
- [ ] Smart dependency manager
- [ ] Docker integration
- [ ] CI/CD pipeline generator
- [ ] Voice commands
- [ ] IDE plugins (VSCode, IntelliJ)
- [ ] Web dashboard
- [ ] Team learning (knowledge sharing)

**Votez pour vos fonctionnalités préférées dans les issues !**

---

## 📊 Statistiques du Projet

- **59 fichiers** Java
- **~12,000 lignes** de code
- **28 outils** fonctionnels
- **10+ langages** supportés
- **3 systèmes** d'apprentissage
- **20+ règles** de code review

---

## 🌟 Remerciements

- [Ollama](https://ollama.ai/) - Infrastructure LLM locale
- [Spring Boot](https://spring.io/) - Framework Java
- [Jackson](https://github.com/FasterXML/jackson) - Sérialisation JSON/YAML

---

## 📝 License

Ce projet est sous licence **MIT License**. Voir [LICENSE](LICENSE) pour plus de détails.

---

## 💬 Support & Contact

- 📖 **Documentation** : Voir [GUIDE.md](GUIDE.md) pour le guide complet
- 🐛 **Bug Reports** : Ouvrez une issue sur GitHub
- 💡 **Feature Requests** : Proposez vos idées via une issue

---

<div align="center">


**Prêt à transformer votre workflow ?**

```bash
cd votre-projet && agentcli
```

⭐ **N'oubliez pas de star le projet si vous l'aimez !** ⭐

</div>
