package com.biobite.controller;

import com.biobite.config.JwtUtil;
import com.biobite.model.HealthReport;
import com.biobite.model.User;
import com.biobite.repository.HealthReportRepository;
import com.biobite.repository.UserRepository;
import com.biobite.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class HealthReportController {

    @Autowired private HealthReportRepository reportRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> getReports(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = jwtUtil.extractUserId(authHeader.substring(7));
            List<HealthReport> reports = reportRepository.findByUserIdOrderByUploadedAtDesc(userId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/extract")
    public ResponseEntity<?> extractFromText(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            Long userId = jwtUtil.extractUserId(authHeader.substring(7));
            User user   = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            String reportText = request.get("text");
            String reportName = request.getOrDefault("reportName", "Health Report");
            String reportType = request.getOrDefault("reportType", "General");

            String prompt = """
You are a medical report analyzer. Extract key health values from this report and provide a summary.

REPORT TEXT:
""" + reportText + """

RESPOND ONLY WITH THIS JSON (no markdown, no explanation outside JSON):
{
  "extractedValues": {
    "parameter_name": { "value": "actual value", "unit": "unit", "status": "Normal/High/Low/Critical", "referenceRange": "normal range" }
  },
  "summary": "2-3 sentence plain English summary of what this report indicates about the person's health",
  "keyFindings": ["finding 1", "finding 2", "finding 3"],
  "dietaryImplications": ["implication 1", "implication 2"]
}
""";
            String aiResponse = geminiService.getPersonalizedRecommendation(user, prompt);
            String clean      = aiResponse.replace("```json", "").replace("```", "").trim();

            Map parsed = objectMapper.readValue(clean, Map.class);

            HealthReport report = new HealthReport();
            report.setUser(user);
            report.setReportName(reportName);
            report.setReportType(reportType);
            report.setRawText(reportText);
            report.setExtractedValues(objectMapper.writeValueAsString(parsed.get("extractedValues")));
            report.setAiSummary(objectMapper.writeValueAsString(parsed));
            reportRepository.save(report);

            return ResponseEntity.ok(Map.of("report", report, "parsed", parsed));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/manual")
    public ResponseEntity<?> saveManual(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            Long userId = jwtUtil.extractUserId(authHeader.substring(7));
            User user   = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            HealthReport report = new HealthReport();
            report.setUser(user);
            report.setReportName((String) request.getOrDefault("reportName", "Manual Entry"));
            report.setReportType((String) request.getOrDefault("reportType", "Manual"));
            report.setExtractedValues(objectMapper.writeValueAsString(request.get("values")));
            report.setAiSummary(objectMapper.writeValueAsString(Map.of(
                "summary", request.getOrDefault("notes", ""),
                "keyFindings", List.of(),
                "dietaryImplications", List.of()
            )));
            reportRepository.save(report);
            return ResponseEntity.ok(Map.of("message", "Report saved!", "report", report));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> deleteReport(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long reportId) {
        try {
            Long userId = jwtUtil.extractUserId(authHeader.substring(7));
            reportRepository.deleteByIdAndUserId(reportId, userId);
            return ResponseEntity.ok(Map.of("message", "Report deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
