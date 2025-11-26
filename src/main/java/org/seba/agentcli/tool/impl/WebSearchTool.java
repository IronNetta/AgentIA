package org.seba.agentcli.tool.impl;

import org.seba.agentcli.io.AnsiColors;
import org.seba.agentcli.CliService;
import org.seba.agentcli.model.ProjectContext;
import org.seba.agentcli.tool.AbstractTool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web search tool for searching the web and getting summarized results.
 * Commands:
 *   @websearch "query"              - Search and display raw results
 *   @websearch "query" --summarize  - Search and get LLM summary
 *   @websearch "query" --limit N    - Limit number of results (default: 5)
 */
@Component
public class WebSearchTool extends AbstractTool {

    private final HttpClient httpClient;
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    // DuckDuckGo HTML search endpoint (doesn't require API key)
    private static final String SEARCH_URL = "https://html.duckduckgo.com/html/";

    public WebSearchTool(CliService cliService) {
        super(cliService);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getName() {
        return "websearch";
    }

    @Override
    public String getDescription() {
        return "Search the web and optionally get LLM-summarized results";
    }

    @Override
    public String getUsage() {
        return "@websearch \"query\" [--summarize] [--limit N]\n\n" +
               "Options:\n" +
               "  --summarize    Get LLM summary of results\n" +
               "  --limit N      Limit number of results (default: 5, max: 10)\n\n" +
               "Examples:\n" +
               "  @websearch \"Java 17 features\"\n" +
               "  @websearch \"Spring Boot best practices\" --summarize\n" +
               "  @websearch \"machine learning\" --limit 3 --summarize";
    }

    @Override
    public String execute(String args, ProjectContext context) {
        if (args == null || args.trim().isEmpty()) {
            return AnsiColors.colorize("Error: Please provide a search query", AnsiColors.RED) + "\n" +
                   AnsiColors.colorize("Usage: " + getUsage(), AnsiColors.YELLOW);
        }

        try {
            // Parse arguments
            SearchArgs searchArgs = parseArgs(args);

            if (searchArgs.query == null || searchArgs.query.isEmpty()) {
                return AnsiColors.colorize("Error: Search query is required", AnsiColors.RED);
            }

            System.out.println(AnsiColors.colorize("🔍 Searching for: " + searchArgs.query, AnsiColors.CYAN));

            // Perform web search
            List<SearchResult> results = performSearch(searchArgs.query, searchArgs.limit);

            if (results.isEmpty()) {
                return AnsiColors.colorize("No results found for: " + searchArgs.query, AnsiColors.YELLOW);
            }

            // Format results
            StringBuilder output = new StringBuilder();
            output.append(AnsiColors.colorize("\n📊 Found " + results.size() + " results:\n\n", AnsiColors.GREEN));

            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                output.append(AnsiColors.colorize("[" + (i + 1) + "] ", AnsiColors.BRIGHT_BLACK));
                output.append(AnsiColors.colorize(result.title, AnsiColors.BOLD_CYAN));
                output.append("\n");
                output.append(AnsiColors.colorize("    🔗 ", AnsiColors.BRIGHT_BLACK));
                output.append(AnsiColors.colorize(result.url, AnsiColors.BLUE));
                output.append("\n");
                if (result.snippet != null && !result.snippet.isEmpty()) {
                    output.append(AnsiColors.colorize("    ", AnsiColors.BRIGHT_BLACK));
                    output.append(result.snippet);
                    output.append("\n");
                }
                output.append("\n");
            }

            // If summarize is requested, use LLM to summarize results
            if (searchArgs.summarize) {
                output.append(AnsiColors.colorize("🤖 Generating summary...\n\n", AnsiColors.PURPLE));
                String summary = summarizeResults(searchArgs.query, results);
                output.append(AnsiColors.colorize("Summary:\n", AnsiColors.BOLD_CYAN));
                output.append(summary);
                output.append("\n");
            }

            return output.toString();

        } catch (IOException e) {
            return AnsiColors.colorize("Error performing web search: " + e.getMessage(), AnsiColors.RED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AnsiColors.colorize("Search interrupted", AnsiColors.RED);
        } catch (Exception e) {
            return AnsiColors.colorize("Unexpected error: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Parse command arguments
     */
    private SearchArgs parseArgs(String args) {
        SearchArgs searchArgs = new SearchArgs();
        searchArgs.limit = DEFAULT_LIMIT;
        searchArgs.summarize = false;

        // Extract query (text in quotes or first non-flag argument)
        Pattern queryPattern = Pattern.compile("\"([^\"]+)\"");
        Matcher queryMatcher = queryPattern.matcher(args);

        if (queryMatcher.find()) {
            searchArgs.query = queryMatcher.group(1);
        } else {
            // No quotes, take everything before first flag
            String[] parts = args.split("\\s+--");
            searchArgs.query = parts[0].trim();
        }

        // Check for --summarize flag
        if (args.contains("--summarize")) {
            searchArgs.summarize = true;
        }

        // Check for --limit flag
        Pattern limitPattern = Pattern.compile("--limit\\s+(\\d+)");
        Matcher limitMatcher = limitPattern.matcher(args);
        if (limitMatcher.find()) {
            int limit = Integer.parseInt(limitMatcher.group(1));
            searchArgs.limit = Math.min(limit, MAX_LIMIT);
        }

        return searchArgs;
    }

    /**
     * Perform web search using DuckDuckGo HTML
     */
    private List<SearchResult> performSearch(String query, int limit) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        // DuckDuckGo HTML search (POST request)
        String requestBody = "q=" + encodedQuery;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SEARCH_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Search failed with status code: " + response.statusCode());
        }

        return parseSearchResults(response.body(), limit);
    }

    /**
     * Parse HTML search results from DuckDuckGo
     */
    private List<SearchResult> parseSearchResults(String html, int limit) {
        List<SearchResult> results = new ArrayList<>();

        // Pattern to match search results in DuckDuckGo HTML
        // This is a simplified parser - production code should use a proper HTML parser
        Pattern resultPattern = Pattern.compile(
            "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
            Pattern.DOTALL
        );

        Pattern snippetPattern = Pattern.compile(
            "<a class=\"result__snippet\"[^>]*>([^<]+)</a>",
            Pattern.DOTALL
        );

        Matcher resultMatcher = resultPattern.matcher(html);
        Matcher snippetMatcher = snippetPattern.matcher(html);

        while (resultMatcher.find() && results.size() < limit) {
            SearchResult result = new SearchResult();
            result.url = cleanUrl(resultMatcher.group(1));
            result.title = cleanText(resultMatcher.group(2));

            // Try to find corresponding snippet
            if (snippetMatcher.find(resultMatcher.start())) {
                result.snippet = cleanText(snippetMatcher.group(1));
            }

            results.add(result);
        }

        return results;
    }

    /**
     * Clean URL from DuckDuckGo redirect
     */
    private String cleanUrl(String url) {
        // DuckDuckGo uses redirect URLs like: //duckduckgo.com/l/?uddg=https%3A%2F%2F...
        if (url.startsWith("//duckduckgo.com/l/?")) {
            Pattern uddgPattern = Pattern.compile("uddg=([^&]+)");
            Matcher matcher = uddgPattern.matcher(url);
            if (matcher.find()) {
                try {
                    return java.net.URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return url;
                }
            }
        }
        return url;
    }

    /**
     * Clean HTML text (remove tags, decode entities)
     */
    private String cleanText(String text) {
        if (text == null) return "";

        // Remove HTML tags
        text = text.replaceAll("<[^>]+>", "");

        // Decode common HTML entities
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'")
                   .replace("&nbsp;", " ");

        // Clean up whitespace
        text = text.trim().replaceAll("\\s+", " ");

        return text;
    }

    /**
     * Use LLM to summarize search results
     */
    private String summarizeResults(String query, List<SearchResult> results) {
        StringBuilder context = new StringBuilder();
        context.append("User searched for: \"").append(query).append("\"\n\n");
        context.append("Search results:\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append((i + 1)).append(". ").append(result.title).append("\n");
            context.append("   URL: ").append(result.url).append("\n");
            if (result.snippet != null && !result.snippet.isEmpty()) {
                context.append("   Snippet: ").append(result.snippet).append("\n");
            }
            context.append("\n");
        }

        String prompt = context.toString() +
                       "\nPlease provide a concise summary of these search results. " +
                       "Highlight the most relevant information related to the user's query. " +
                       "Keep the summary professional and informative.";

        try {
            return cliService.query(prompt);
        } catch (Exception e) {
            return AnsiColors.colorize("Error generating summary: " + e.getMessage(), AnsiColors.RED);
        }
    }

    /**
     * Inner class for search arguments
     */
    private static class SearchArgs {
        String query;
        int limit;
        boolean summarize;
    }

    /**
     * Inner class for search results
     */
    private static class SearchResult {
        String title;
        String url;
        String snippet;
    }
}
