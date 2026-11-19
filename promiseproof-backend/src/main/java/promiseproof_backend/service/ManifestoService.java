package promiseproof_backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.ExtractedPromiseResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Assisted extraction of promise-like statements from pasted manifesto text.
 *
 * This is a deterministic text heuristic, not a model: it proposes candidate
 * sentences for a human to review, edit and confirm. Nothing is stored here,
 * and the UI labels the result as a suggestion rather than an authoritative
 * reading of the manifesto.
 */
@Service
public class ManifestoService {

    private static final int MAX_TEXT_LENGTH = 50_000;
    private static final int MAX_RESULTS = 40;
    private static final int MIN_LENGTH = 25;
    private static final int TITLE_LENGTH = 90;
    private static final int DESCRIPTION_LENGTH = 1_000;

    private static final Pattern BULLET_PREFIX = Pattern.compile(
            "^\\s*(?:[-\\u2013\\u2014\\u2022*]+|\\d{1,2}[.)]|[a-zA-Z][.)])\\s*"
    );

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final Pattern SENTENCE_BREAK = Pattern.compile(
            "(?<=[.!?])\\s+"
    );

    private static final Pattern SIGNAL_WORDS = Pattern.compile(
            "\\b(will|shall|commit|pledge|promise|ensure|provide|increase|" +
                    "reduce|improve|build|create|launch|implement|establish|" +
                    "expand|introduce|guarantee|deliver|develop|construct|" +
                    "upgrade|allocate|complete|achieve|protect|support|offer|" +
                    "give|bring|generate|fund|prioritise|prioritize|double|" +
                    "triple|aim|target)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> NON_CONTENT_PREFIXES = List.of(
            "table of contents",
            "contents",
            "foreword",
            "preface",
            "introduction",
            "chapter",
            "section",
            "annexure",
            "appendix"
    );

    public List<ExtractedPromiseResponse> extract(String text) {

        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Paste some manifesto text to extract promises"
            );
        }

        if (text.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Manifesto text is too long. Please paste at most " +
                            MAX_TEXT_LENGTH + " characters."
            );
        }

        Map<String, ExtractedPromiseResponse> results =
                new LinkedHashMap<>();

        for (String block : text.split("\\R+")) {

            for (String sentence : splitSentences(block)) {

                if (results.size() >= MAX_RESULTS) {
                    return new ArrayList<>(results.values());
                }

                String cleaned = clean(sentence);

                if (!isPromiseLike(cleaned)) {
                    continue;
                }

                String key = cleaned
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", "");

                if (key.isEmpty() || results.containsKey(key)) {
                    continue;
                }

                results.put(
                        key,
                        new ExtractedPromiseResponse(
                                toTitle(cleaned),
                                truncate(cleaned, DESCRIPTION_LENGTH)
                        )
                );
            }
        }

        return new ArrayList<>(results.values());
    }

    private static List<String> splitSentences(String block) {

        String trimmed = block == null ? "" : block.trim();

        if (trimmed.isEmpty()) {
            return List.of();
        }

        // Short bullet lines stay whole. Longer paragraphs are split so that
        // one line containing several sentences becomes several candidates.
        if (trimmed.length() > 120) {
            return Arrays.asList(SENTENCE_BREAK.split(trimmed));
        }

        return List.of(trimmed);
    }

    private static String clean(String value) {

        String cleaned = BULLET_PREFIX.matcher(value).replaceFirst("");

        return WHITESPACE.matcher(cleaned).replaceAll(" ").trim();
    }

    private static boolean isPromiseLike(String value) {

        if (value.length() < MIN_LENGTH) {
            return false;
        }

        if (value.endsWith(":")) {
            return false;
        }

        if (value.split(" ").length < 4) {
            return false;
        }

        String lower = value.toLowerCase(Locale.ROOT);

        for (String prefix : NON_CONTENT_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return false;
            }
        }

        // Headings are usually all capitals.
        if (value.equals(value.toUpperCase(Locale.ROOT)) &&
                value.length() < 100) {
            return false;
        }

        if (!value.chars().anyMatch(Character::isLetter)) {
            return false;
        }

        return SIGNAL_WORDS.matcher(value).find() || value.length() >= 70;
    }

    private static String toTitle(String value) {

        if (value.length() <= TITLE_LENGTH) {
            return stripTrailingPunctuation(value);
        }

        String cut = value.substring(0, TITLE_LENGTH);

        int lastSpace = cut.lastIndexOf(' ');

        if (lastSpace > 40) {
            cut = cut.substring(0, lastSpace);
        }

        return stripTrailingPunctuation(cut) + "\u2026";
    }

    private static String stripTrailingPunctuation(String value) {

        return value.replaceAll("[\\s,;:.\\-]+$", "");
    }

    private static String truncate(String value, int maxLength) {

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 1).trim() + "\u2026";
    }
}
