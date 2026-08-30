package com.ororura.analyzer.resume.ai;

import java.util.List;

import com.ororura.analyzer.resume.error.ResumeAnalysisException;
import com.ororura.analyzer.resume.error.ResumeErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LlmResponseParser {

    private static final int MAX_SURROUNDING_TEXT = 200;
    private final ObjectMapper objectMapper;

    public LlmResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LlmResumeAnalysisResponse parse(String content) {
        String json = extractSingleObject(content);
        if (json == null) throw invalid();
        try {
            JsonNode root = objectMapper.readTree(json);
            validateRequiredShape(root);
            return objectMapper.readerFor(LlmResumeAnalysisResponse.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw invalid(exception);
        }
    }

    String extractSingleObject(String content) {
        if (content == null || content.isBlank()) return null;
        String candidate = stripFence(content.trim());
        if (candidate == null) return null;
        int start = candidate.indexOf('{');
        if (start < 0 || start > MAX_SURROUNDING_TEXT) return null;
        int end = balancedObjectEnd(candidate, start);
        if (end < 0 || candidate.length() - end - 1 > MAX_SURROUNDING_TEXT) return null;
        String suffix = candidate.substring(end + 1);
        if (suffix.indexOf('{') >= 0 || suffix.indexOf('}') >= 0) return null;
        return candidate.substring(start, end + 1);
    }

    private static String stripFence(String value) {
        if (!value.startsWith("```")) return value;
        if (!value.endsWith("```") || value.length() < 6) return null;
        int newline = value.indexOf('\n');
        if (newline < 0) return null;
        String language = value.substring(3, newline).trim();
        if (!language.isEmpty() && !language.equalsIgnoreCase("json")) return null;
        return value.substring(newline + 1, value.length() - 3).trim();
    }

    private static int balancedObjectEnd(String value, int start) {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char character = value.charAt(index);
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
                continue;
            }
            if (character == '"') quoted = true;
            else if (character == '{') depth++;
            else if (character == '}' && --depth == 0) return index;
        }
        return -1;
    }

    private static void validateRequiredShape(JsonNode root) {
        required(root, "semanticScores", "experienceAssessment", "resumeAssessment", "skills",
                "employmentPeriods", "strengths", "weaknesses", "atsIssues", "recommendations", "warnings");
        required(root.path("experienceAssessment"), "commercialRelevance", "experienceDescriptionQuality",
                "responsibilityLevel");
        required(root.path("resumeAssessment"), "resumeQuality", "atsReadability");
        required(root.path("skills"), "confirmed", "weakEvidence", "missing");
        if (!root.path("semanticScores").isArray()) throw new IllegalArgumentException("Expected semanticScores array");
        for (JsonNode assessment : root.path("semanticScores")) {
            required(assessment, "criterion", "score", "evidence");
        }
        requireScores(root.path("experienceAssessment"), "commercialRelevance", "experienceDescriptionQuality",
                "responsibilityLevel");
        requireScores(root.path("resumeAssessment"), "resumeQuality", "atsReadability");
        for (JsonNode period : root.path("employmentPeriods")) {
            required(period, "company", "position", "startYear", "startMonth", "endYear", "endMonth", "current");
        }
    }

    private static void requireScores(JsonNode assessment, String... fields) {
        for (String field : fields) {
            required(assessment.path(field), "score", "evidence");
        }
    }

    private static void required(JsonNode node, String... fields) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("Expected object");
        for (String field : fields) {
            if (!node.has(field)) throw new IllegalArgumentException("Missing field: " + field);
        }
    }

    private static ResumeAnalysisException invalid() {
        return new ResumeAnalysisException(ResumeErrorCode.AI_INVALID_RESPONSE,
                "AI response does not match the required schema");
    }

    private static ResumeAnalysisException invalid(Exception cause) {
        return new ResumeAnalysisException(ResumeErrorCode.AI_INVALID_RESPONSE,
                "AI response does not match the required schema", cause);
    }
}
