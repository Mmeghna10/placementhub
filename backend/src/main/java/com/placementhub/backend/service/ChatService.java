package com.placementhub.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placementhub.backend.dto.ChatMessageDto;
import com.placementhub.backend.exception.ServiceUnavailableException;
import com.placementhub.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String ASSISTANT_UNAVAILABLE =
            "The assistant is unavailable right now. Please try again in a moment.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    public String getReply(List<ChatMessageDto> messages) {
        String role = SecurityUtils.getCurrentUserRole();
        String systemPrompt = buildSystemPrompt(role);

        // The key goes in a header, not the query string, so it never appears in URLs
        // that proxies or access logs may record.
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent";

        List<Map<String, Object>> contents = messages.stream()
                .map(m -> {
                    Map<String, Object> part = new HashMap<>();
                    part.put("text", m.getContent());

                    Map<String, Object> content = new HashMap<>();
                    content.put("role", "assistant".equals(m.getRole()) ? "model" : "user");
                    content.put("parts", List.of(part));
                    return content;
                })
                .collect(Collectors.toList());

        Map<String, Object> systemInstruction = new HashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", systemPrompt)));

        Map<String, Object> body = new HashMap<>();
        body.put("system_instruction", systemInstruction);
        body.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // Upstream details are logged for us but never returned to the client.
        String rawResponse;
        try {
            // Get the raw response as plain text first — avoids RestTemplate's
            // automatic type conversion, which is where the Jackson conflict occurs
            rawResponse = restTemplate.postForObject(url, request, String.class);
        } catch (HttpStatusCodeException e) {
            log.warn("Gemini API returned {}", e.getStatusCode());
            throw new ServiceUnavailableException(ASSISTANT_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            log.warn("Gemini API unreachable or timed out: {}", e.getMessage());
            throw new ServiceUnavailableException(ASSISTANT_UNAVAILABLE);
        }

        try {
            JsonNode text = objectMapper.readTree(rawResponse)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                log.warn("Gemini response had no candidate text");
                throw new ServiceUnavailableException(ASSISTANT_UNAVAILABLE);
            }
            return text.asText();
        } catch (JsonProcessingException e) {
            log.warn("Could not parse Gemini response: {}", e.getOriginalMessage());
            throw new ServiceUnavailableException(ASSISTANT_UNAVAILABLE);
        }
    }

    private String buildSystemPrompt(String role) {
        String base = "You are the in-app guide for PlacementHub, a campus placement portal built with "
                + "Spring Boot and React. Keep answers short (2-4 sentences), friendly, and practical. "
                + "You can answer general questions too (not just app navigation), but always stay helpful and concise. "
                + "Never invent app features that don't exist — if unsure whether something exists in PlacementHub, say so.\n\n";

        return switch (role) {
            case "STUDENT" -> base + "This user is a STUDENT. In this app: their profile is under 'Profile' via the navbar email (or the 'Profile' nav link), with an Edit button to update fullName, phone, college, degree, branch, graduationYear, cgpa, skills, resumeUrl, linkedinUrl, githubUrl. 'Browse Jobs' shows all approved jobs, with an 'All / Recommended' toggle — Recommended filters by matching their CGPA and branch text against each job's requirements. Clicking a job shows full details with an 'Apply Now' button. 'My Applications' lists everything they've applied to with a status badge; clicking one shows a dated Timeline (APPLIED → SHORTLISTED → INTERVIEW → OFFERED/REJECTED).";

            case "RECRUITER" -> base + "This user is a RECRUITER. In this app: 'My Jobs' in the navbar shows their posted jobs, with a 'Post a Job' button to create new ones (fields: title, description, job type, location, salary range, required skills, minimum CGPA, eligible branches, deadline). New jobs start as PENDING until admin approves them. Each job in My Jobs has edit (pencil icon), delete (trash icon), and view applicants (people icon) actions. Their personal profile and company profile are two separate pages ('Profile' via navbar email, and 'Company' in the navbar).";

            case "ADMIN" -> base + "This user is an ADMIN. In this app: 'Dashboard' shows totals (students, recruiters, companies, jobs, pending jobs, applications). 'Jobs' lists all jobs; jobs marked PENDING have Approve/Reject buttons right there. 'Students' and 'Recruiters' are read-only tables of everyone registered. 'Analytics' shows a bar chart of applications by status and a pie chart of job approval breakdown. Admin accounts don't have an editable profile — just an account info page.";

            default -> base;
        };
    }
}