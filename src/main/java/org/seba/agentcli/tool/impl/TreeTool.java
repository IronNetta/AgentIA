package org.seba.agentcli.tool.impl;

import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Component
public class TreeTool extends AbstractTool {

    private static final Set<String> IGNORED_DIRS = Set.of(
        ".git", ".svn", "node_modules", "target", "build",
        "dist", "out", ".idea", ".vscode", "__pycache__"
    );

    public TreeTool(CliService cliService) {
        super(cliService);
    }

    @Override
    public String getName() {
        return "@tree";
    }

    @Override
    public String getDescription() {
        return "Affiche l'arborescence du projet";
    }

    @Override
    public String getUsage() {
        return "@tree [profondeur_max]";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        int maxDepth = 3; // Default depth

        if (!args.isEmpty()) {
            try {
                maxDepth = Integer.parseInt(args.trim());
                if (maxDepth < 1 || maxDepth > 10) {
                    return formatError("Profondeur doit être entre 1 et 10");
                }
            } catch (NumberFormatException e) {
                return formatError("Profondeur invalide: " + args);
            }
        }

        try {
            StringBuilder tree = new StringBuilder();
            tree.append("📁 ").append(context.getRootPath().getFileName()).append("\n");
            buildTree(context.getRootPath(), "", true, 0, maxDepth, tree);
            return tree.toString();

        } catch (Exception e) {
            return formatError("Erreur lors de la création de l'arbre: " + e.getMessage());
        }
    }

    private void buildTree(Path path, String prefix, boolean isLast,
                          int currentDepth, int maxDepth, StringBuilder output) throws IOException {

        if (currentDepth >= maxDepth) {
            return;
        }

        try (Stream<Path> paths = Files.list(path)) {
            List<Path> children = paths
                .filter(p -> !IGNORED_DIRS.contains(p.getFileName().toString()))
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();

            for (int i = 0; i < children.size(); i++) {
                Path child = children.get(i);
                boolean childIsLast = (i == children.size() - 1);

                String connector = childIsLast ? "└── " : "├── ";
                String icon = Files.isDirectory(child) ? "📁 " : "📄 ";

                output.append(prefix)
                      .append(connector)
                      .append(icon)
                      .append(child.getFileName())
                      .append("\n");

                if (Files.isDirectory(child)) {
                    String newPrefix = prefix + (childIsLast ? "    " : "│   ");
                    buildTree(child, newPrefix, childIsLast, currentDepth + 1, maxDepth, output);
                }
            }
        }
    }
}
