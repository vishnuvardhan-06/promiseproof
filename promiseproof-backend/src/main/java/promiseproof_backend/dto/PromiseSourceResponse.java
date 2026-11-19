package promiseproof_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PromiseSourceResponse(

        Long id,
        String manifestoName,
        String sourceUrl,
        Integer pageNumber,
        String exactQuotation,
        String sourceType,
        LocalDate publishedDate,
        LocalDateTime createdAt
) {
}
