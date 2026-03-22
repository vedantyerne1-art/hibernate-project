package com.trustid.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustid.document.dto.OcrExtractionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final ObjectMapper objectMapper;

    @Value("${app.ocr.provider:heuristic}")
    private String provider;

    @Value("${app.ocr.google-vision-api-key:}")
    private String googleVisionApiKey;

    public OcrExtractionResult extract(MultipartFile file) {
        String rawText = "";
        try {
            if ("google-vision".equalsIgnoreCase(provider) && googleVisionApiKey != null && !googleVisionApiKey.isBlank()) {
                rawText = extractWithGoogleVision(file);
            }
        } catch (Exception ignored) {
            // OCR should never block upload. Heuristic fallback still runs.
        }

        if (rawText == null || rawText.isBlank()) {
            rawText = "";
        }

        String normalized = rawText.replaceAll("\\s+", " ").trim();
        String number = detectDocumentNumber(normalized);
        LocalDate dob = detectDob(normalized);
        String name = detectName(normalized);

        return OcrExtractionResult.builder()
                .rawText(rawText)
                .extractedDocumentNumber(number)
                .extractedDob(dob)
                .extractedName(name)
                .build();
    }

    private String extractWithGoogleVision(MultipartFile file) throws Exception {
        String endpoint = "https://vision.googleapis.com/v1/images:annotate?key=" + googleVisionApiKey;
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String payload = "{\"requests\":[{\"image\":{\"content\":\"" + base64 + "\"},\"features\":[{\"type\":\"TEXT_DETECTION\"}]}]}";

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.postForObject(endpoint, payload, String.class);
        JsonNode root = objectMapper.readTree(response);
        JsonNode text = root.path("responses").path(0).path("fullTextAnnotation").path("text");
        return text.isMissingNode() ? "" : text.asText("");
    }

    private String detectDocumentNumber(String text) {
        if (text == null || text.isBlank()) return null;
        Pattern pattern = Pattern.compile("([A-Z]{3,5}[0-9]{4,8}|[0-9]{4}\\s?[0-9]{4}\\s?[0-9]{4})");
        Matcher matcher = pattern.matcher(text.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", "") : null;
    }

    private LocalDate detectDob(String text) {
        if (text == null || text.isBlank()) return null;
        Pattern pattern = Pattern.compile("(\\d{2}[-/]\\d{2}[-/]\\d{4}|\\d{4}[-/]\\d{2}[-/]\\d{2})");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        String value = matcher.group(1);
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        }) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String detectName(String text) {
        if (text == null || text.isBlank()) return null;
        Pattern pattern = Pattern.compile("(?:NAME|Name|name)[:\\s]+([A-Za-z][A-Za-z\\s]{2,60})");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
