package com.examportal.reporting;

import com.examportal.common.FeatureDisabledException;
import com.examportal.config.FeatureFlags;
import com.examportal.exam.Exam;
import com.examportal.exam.ExamRepository;
import com.examportal.user.User;
import com.examportal.user.UserRepository;
import com.examportal.evaluation.EvaluationRepository;
import com.examportal.evaluation.EvaluationResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.geom.PageSize;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;

import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final FeatureFlags featureFlags;
    private final EvaluationRepository evaluationRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ObjectMapper objectMapper;

    public byte[] generateResultPdf(Long attemptId) {

        if (!featureFlags.isPdf()) {
            throw new FeatureDisabledException("pdf");
        }

        try {

            EvaluationResult result = evaluationRepository.findByAttemptId(attemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Result not found"));

            User student = userRepository.findById(result.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found"));

            Exam exam = examRepository.findById(result.getExamId())
                    .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

            Map<String, Object> breakdown = objectMapper.readValue(
                    result.getSubjectWiseBreakdown(),
                    Map.class
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Title
            document.add(new Paragraph("EXAMINATION RESULT REPORT")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(exam.getTitle())
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Student info table
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                    .useAllAvailableWidth();

            infoTable.addCell("Student Name:");
            infoTable.addCell(student.getFullName());

            infoTable.addCell("Email:");
            infoTable.addCell(student.getEmail());

            infoTable.addCell("Total Score:");
            infoTable.addCell(String.valueOf(result.getTotalScore()));

            infoTable.addCell("Evaluated At:");
            infoTable.addCell(String.valueOf(result.getEvaluatedAt()));

            document.add(infoTable);

            document.add(new Paragraph("\nSubject Breakdown\n").setBold());

            // Subject breakdown table
            Table bdTable = new Table(6).useAllAvailableWidth();

            String[] headers = {
                    "Subject", "Total", "Correct", "Wrong", "Unattempted", "Score"
            };

            for (String h : headers) {
                bdTable.addHeaderCell(new Cell().add(new Paragraph(h).setBold()));
            }

            for (Map.Entry<String, Object> entry : breakdown.entrySet()) {

                Map<String, Object> data = (Map<String, Object>) entry.getValue();

                bdTable.addCell(entry.getKey());
                bdTable.addCell(String.valueOf(data.get("total")));
                bdTable.addCell(String.valueOf(data.get("correct")));
                bdTable.addCell(String.valueOf(data.get("wrong")));
                bdTable.addCell(String.valueOf(data.get("unattempted")));
                bdTable.addCell(String.valueOf(data.get("score")));
            }

            document.add(bdTable);

            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}