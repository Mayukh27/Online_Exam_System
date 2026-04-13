package com.examportal.question;

import com.examportal.subject.Subject;
import com.examportal.subject.SubjectService;
import com.examportal.user.User;
import com.examportal.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QuestionService - Question bank management.
 * Questions are uploaded by teachers. Active questions feed the blueprint engine.
 * includeAnswer=false strips correctOptionIndex before sending to students.
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SubjectService subjectService;
    private final UserService userService;

    /**
     * Upload a single question via JSON API
     */
    public QuestionDTO upload(QuestionDTO dto, String uploaderEmail) {

        Subject subject = subjectService.getEntityById(dto.getSubjectId());
        User uploader = userService.findByEmail(uploaderEmail);

        Question question = Question.builder()
                .subject(subject)
                .uploadedBy(uploader)
                .questionText(dto.getQuestionText())
                .options(dto.getOptions())
                .correctOptionIndex(dto.getCorrectOptionIndex())
                .difficulty(dto.getDifficulty() != null ? dto.getDifficulty() : Difficulty.MEDIUM)
                .marks(dto.getMarks() != null ? dto.getMarks() : 1)
                .negativeMarks(dto.getNegativeMarks() != null ? dto.getNegativeMarks() : 0.25)
                .build();

        return toDTO(questionRepository.save(question), true);
    }

    /**
     * Upload questions using Excel file
     */
    /**
     * Upload questions from an Excel file (.xlsx).
     *
     * Expected column order (row 1 = header, skipped):
     *   A (0) subject_code     - code of the subject
     *   B (1) question_text    - full question text
     *   C (2) option_a         - option A
     *   D (3) option_b         - option B
     *   E (4) option_c         - option C
     *   F (5) option_d         - option D
     *   G (6) correct_option   - 0=A, 1=B, 2=C, 3=D
     *   H (7) difficulty       - optional: EASY/MEDIUM/HARD (default MEDIUM)
     *   I (8) marks            - optional: positive marks (default 1)
     *   J (9) negative_marks   - optional: negative marks (default 0.25)
     *
     * Rows with empty col A are silently skipped.
     */
    public void uploadQuestionsFromExcel(MultipartFile file, String uploaderEmail) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided. Please select an .xlsx file.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid file type. Only .xlsx files are supported.");
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            User uploader = userService.findByEmail(uploaderEmail);
            List<Question> questions = new ArrayList<>();
            int rowsProcessed = 0;

            for (Row row : sheet) {

                if (row.getRowNum() == 0) continue; // skip header row

                // Skip rows where column A is empty or null
                Cell subjectCell = row.getCell(0);
                if (subjectCell == null || subjectCell.toString().isBlank()) continue;

                try {
                    String subjectCode = subjectCell.toString().trim();

                    if (subjectCode.isBlank()) {
                        throw new IllegalArgumentException(
                                "Row " + (row.getRowNum()+1) + ": Subject code is empty");
                    }
                    Subject subject = subjectService.getEntityByCode(subjectCode);

                    Cell qTextCell = row.getCell(1);
                    if (qTextCell == null || qTextCell.toString().isBlank()) continue;
                    String questionText = qTextCell.toString().trim();

                    // Options A-D (cols 2-5)
                    List<String> options = new java.util.ArrayList<>();
                    for (int col = 2; col <= 5; col++) {
                        Cell c = row.getCell(col);
                        options.add(c != null ? c.toString().trim() : "");
                    }

                    Cell correctCell = row.getCell(6);
                    if (correctCell == null) continue;
                    int correctIndex = (int) correctCell.getNumericCellValue();
                    if (correctIndex < 0 || correctIndex > 3) {
                        throw new IllegalArgumentException(
                                "Row " + (row.getRowNum()+1) + ": correct_option must be 0,1,2 or 3. Got: " + correctIndex);
                    }

                    // Optional columns
                    Difficulty difficulty = Difficulty.MEDIUM;
                    Cell diffCell = row.getCell(7);
                    if (diffCell != null && !diffCell.toString().isBlank()) {
                        try { difficulty = Difficulty.valueOf(diffCell.toString().trim().toUpperCase()); }
                        catch (Exception ignored) {} // keep MEDIUM if value is unrecognised
                    }

                    int marks = 1;
                    Cell marksCell = row.getCell(8);
                    if (marksCell != null && marksCell.getCellType() == CellType.NUMERIC) {
                        marks = Math.max(1, (int) marksCell.getNumericCellValue());
                    }

                    double negativeMarks = 0.25;
                    Cell negCell = row.getCell(9);
                    if (negCell != null && negCell.getCellType() == CellType.NUMERIC) {
                        negativeMarks = Math.max(0, negCell.getNumericCellValue());
                    }

                    questions.add(Question.builder()
                            .subject(subject)
                            .uploadedBy(uploader)
                            .questionText(questionText)
                            .options(options)
                            .correctOptionIndex(correctIndex)
                            .difficulty(difficulty)
                            .marks(marks)
                            .negativeMarks(negativeMarks)
                            .build());

                    rowsProcessed++;

                } catch (IllegalArgumentException ex) {
                    throw ex; // re-throw validation errors directly
                } catch (Exception rowEx) {
                    throw new IllegalArgumentException(
                            "Error on row " + (row.getRowNum()+1) + ": " + rowEx.getMessage());
                }
            }

            if (questions.isEmpty()) {
                throw new IllegalArgumentException(
                        "No valid questions found in the file. Check that column A contains a valid subject code.");
            }

            questionRepository.saveAll(questions);

        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex.getMessage()); // surface clean message to API
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Excel upload failed: " + e.getMessage());
        }
    }
    public List<QuestionDTO> findBySubject(Long subjectId) {
        return questionRepository.findBySubjectIdAndActive(subjectId, true)
                .stream()
                .map(q -> toDTO(q, true))
                .collect(Collectors.toList());
    }

    /**
     * Fetch N random questions from a subject.
     * Called by ExamService during exam publishing to populate the master paper.
     */
    public List<Question> fetchRandom(Long subjectId, int count) {
        return questionRepository.findRandomBySubjectId(subjectId, PageRequest.of(0, count));
    }

    /**
     * CHANGE: Fetch all active questions for a subject.
     * Used by ExamService.publish() to pool questions from multiple subjects per section,
     * then shuffle and take the required count.
     */
    public List<Question> fetchAllActive(Long subjectId) {
        return questionRepository.findBySubjectIdAndActive(subjectId, true);
    }

    public List<QuestionDTO> findByIds(List<Long> ids, boolean includeAnswer) {
        // findAllById is the correct Spring Data JPA method (findByIdIn does not exist)
        return questionRepository.findAllById(ids)
                .stream()
                .map(q -> toDTO(q, includeAnswer))
                .toList();
    }

    public Question getEntityById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    }

    public QuestionDTO toDTO(Question q, boolean includeAnswer) {
        return QuestionDTO.builder()
                .id(q.getId())
                .subjectId(q.getSubject().getId())
                .subjectName(q.getSubject().getName())
                .questionText(q.getQuestionText())
                .options(q.getOptions())
                .correctOptionIndex(includeAnswer ? q.getCorrectOptionIndex() : null)
                .difficulty(q.getDifficulty())
                .marks(q.getMarks())
                .negativeMarks(q.getNegativeMarks())
                .build();
    }
}