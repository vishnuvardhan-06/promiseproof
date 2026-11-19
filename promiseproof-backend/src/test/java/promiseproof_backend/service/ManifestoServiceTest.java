package promiseproof_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import promiseproof_backend.dto.ExtractedPromiseResponse;

class ManifestoServiceTest {

    private final ManifestoService manifestoService =
            new ManifestoService();

    @Test
    void extractsBulletAndNumberedPromises() {

        String text = """
                OUR VISION

                1. We will build 100 hospitals across the state by 2030.
                - The party shall provide free bus travel for women.
                2. Ensure every household has piped drinking water.
                """;

        List<ExtractedPromiseResponse> results =
                manifestoService.extract(text);

        assertEquals(3, results.size());
        assertEquals(
                "We will build 100 hospitals across the state by 2030",
                results.get(0).title()
        );
        assertEquals(
                "The party shall provide free bus travel for women",
                results.get(1).title()
        );
        assertTrue(
                results.get(2).title().startsWith("Ensure every household")
        );
    }

    @Test
    void skipsHeadingsShortLinesAndFrontMatter() {

        String text = """
                CHAPTER ONE

                Short line.

                Contents

                This section explains the party history in detail.
                """;

        assertTrue(manifestoService.extract(text).isEmpty());
    }

    @Test
    void splitsLongParagraphsIntoSentences() {

        String text = "We will double the education budget over the next " +
                "five years and review every school curriculum carefully. " +
                "We shall also recruit ten thousand new teachers for rural " +
                "districts by 2030, with training places funded centrally.";

        List<ExtractedPromiseResponse> results =
                manifestoService.extract(text);

        assertEquals(2, results.size());
        assertTrue(results.get(0).title().startsWith("We will double"));
        assertTrue(results.get(1).title().startsWith("We shall also"));
    }

    @Test
    void removesDuplicatePromises() {

        String text = """
                We will build 100 hospitals across the state by 2030.
                We will build 100 hospitals across the state by 2030.
                """;

        assertEquals(1, manifestoService.extract(text).size());
    }

    @Test
    void rejectsEmptyText() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> manifestoService.extract("   ")
        );

        assertEquals(400, exception.getStatusCode().value());
    }
}
