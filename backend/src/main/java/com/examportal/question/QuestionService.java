package com.examportal.question;

import com.examportal.subject.Subject;
import com.examportal.subject.SubjectService;
import com.examportal.user.User;
import com.examportal.user.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * QuestionService - Question bank management.
 *
 * IMAGE SUPPORT:
 *   Single upload  : multipart/form-data endpoint receives optional questionImage
 *                    and up to 4 optionImage_0 … optionImage_3 files. Raw bytes are
 *                    stored as BYTEA in PostgreSQL via QuestionOptionImage rows.
 *
 *   Excel upload   : Supports both original 10-column text layout and extended layout
 *                    with optional image path columns:
 *                    K question_image, L option1_image, M option2_image,
 *                    N option3_image, O option4_image.
 *                    If image paths are used, an optional ZIP can be posted as imageZip.
 *
 *   Image serving  : GET /api/questions/{id}/image          → question body image
 *                    GET /api/questions/{id}/option-image/{i} → option i image
 *
 * Questions are uploaded by teachers. Active questions feed the blueprint engine.
 * includeAnswer=false strips correctOptionIndex before sending to students.
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionImageRepository questionImageRepository;
    private final QuestionOptionImageRepository optionImageRepository;
    private final SubjectService subjectService;
    private final UserService userService;

    // ─────────────────────────────────────────────────────────────────────
    // SINGLE QUESTION UPLOAD  (multipart/form-data)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Upload a single question with optional images.
     *
     * @param dto            Question metadata and text options.
     * @param questionImage  Optional image for the question body (may be null/empty).
     * @param optionImages   Array of 4 slots; each element may be null if that option
     *                       has no image.
     * @param uploaderEmail  Authenticated teacher's email.
     */
    @Transactional
    public QuestionDTO upload(QuestionDTO dto,
                              MultipartFile questionImage,
                              MultipartFile[] optionImages,
                              String uploaderEmail) {

        Subject subject = subjectService.getEntityById(dto.getSubjectId());
        User uploader = userService.findByEmail(uploaderEmail);

        // ── Build question entity ─────────────────────────────────────────
        Question.QuestionBuilder builder = Question.builder()
                .subject(subject)
                .uploadedBy(uploader)
                .questionText(dto.getQuestionText() != null ? dto.getQuestionText() : "")
                .options(dto.getOptions() != null ? dto.getOptions() : List.of("", "", "", ""))
                .correctOptionIndex(dto.getCorrectOptionIndex())
                .difficulty(dto.getDifficulty() != null ? dto.getDifficulty() : Difficulty.MEDIUM)
                .marks(dto.getMarks() != null ? dto.getMarks() : 1)
                .negativeMarks(dto.getNegativeMarks() != null ? dto.getNegativeMarks() : 0.25);

        byte[] questionImageBytes = null;
        String questionImageType = null;

        // ── Capture question image (stored in question_images table) ─────
        if (questionImage != null && !questionImage.isEmpty()) {
            try {
                questionImageBytes = questionImage.getBytes();
                questionImageType = questionImage.getContentType();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to read question image: " + e.getMessage());
            }
        }

        Question question = questionRepository.save(builder.build());

        if (questionImageBytes != null) {
            upsertQuestionImage(question, questionImageBytes, questionImageType, null, null, false);
        }

        // ── Attach option images ──────────────────────────────────────────
        if (optionImages != null) {
            List<QuestionOptionImage> imgRows = new ArrayList<>();
            for (int i = 0; i < optionImages.length && i < 4; i++) {
                MultipartFile f = optionImages[i];
                if (f != null && !f.isEmpty()) {
                    try {
                        imgRows.add(QuestionOptionImage.builder()
                                .question(question)
                                .optionIndex(i)
                                .imageData(f.getBytes())
                                .imageType(f.getContentType())
                                .build());
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                "Failed to read option image " + i + ": " + e.getMessage());
                    }
                }
            }
            optionImageRepository.saveAll(imgRows);
        }

        return toDTO(question, true);
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXCEL BULK UPLOAD  (text-only OR text + image ZIP)
    // ─────────────────────────────────────────────────────────────────────

    @Transactional
    public void uploadQuestionsFromExcel(MultipartFile file, String uploaderEmail) {
        uploadQuestionsFromExcel(file, null, uploaderEmail);
    }

    /**
     * Upload questions from an Excel file (.xlsx) and optionally bind images from a ZIP.
     *
     * Base columns (A..J) remain unchanged and backward compatible.
     * Optional image path columns:
     *   K (10) question_image, L..O (11..14) option1..option4 image paths,
     *   P (15) combined_option_image (single image containing all options)
     */
    @Transactional
    public void uploadQuestionsFromExcel(MultipartFile file, MultipartFile imageZip, String uploaderEmail) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided. Please select an .xlsx file.");
        }

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase(Locale.ROOT)
                : "";

        byte[] excelBytes;
        Map<String, ZipImageData> zipImages;

        if (filename.endsWith(".zip")) {
            if (imageZip != null && !imageZip.isEmpty()) {
                throw new IllegalArgumentException("When primary file is a ZIP bundle, do not provide imageZip separately.");
            }
            BundlePayload bundle = loadBundleZip(file);
            excelBytes = bundle.excelBytes();
            zipImages = bundle.images();
        } else if (filename.endsWith(".xlsx")) {
            try {
                excelBytes = file.getBytes();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to read Excel file: " + e.getMessage());
            }
            zipImages = loadZipImages(imageZip);
        } else {
            throw new IllegalArgumentException("Invalid file type. Upload .xlsx or a .zip bundle containing one .xlsx.");
        }

        try (InputStream is = new ByteArrayInputStream(excelBytes);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headers = buildHeaderIndexMap(headerRow);
            Map<String, ZipImageData> embeddedImages = extractEmbeddedImages(sheet);
            User uploader = userService.findByEmail(uploaderEmail);
            int uploaded = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // skip header

                Cell subjectCell = getCellByHeaders(row, headers, "subject_code", "subject_id");
                if (subjectCell == null || subjectCell.toString().isBlank()) continue;

                try {
                    Subject subject = resolveSubject(subjectCell);

                    Cell qTextCell = getCellByHeaders(row, headers, "question", "question_text");
                    String questionText = getCellString(qTextCell).trim();
                    boolean hasImageInput = hasAnyImageInput(row, headers, embeddedImages);
                    if (questionText.isBlank() && !hasImageInput) continue;

                    List<String> options = new ArrayList<>();
                    options.add(getCellString(getCellByHeaders(row, headers, "option1", "option_a")));
                    options.add(getCellString(getCellByHeaders(row, headers, "option2", "option_b")));
                    options.add(getCellString(getCellByHeaders(row, headers, "option3", "option_c")));
                    options.add(getCellString(getCellByHeaders(row, headers, "option4", "option_d")));

                    Cell correctCell = getCellByHeaders(row, headers, "correct", "correct_option");
                    if (correctCell == null) continue;
                        int correctIndex = parseCorrectOption(correctCell, row.getRowNum());
                    if (correctIndex < 0 || correctIndex > 3) {
                        throw new IllegalArgumentException(
                                "Row " + (row.getRowNum() + 1) +
                                        ": correct_option must be 0,1,2 or 3. Got: " + correctIndex);
                    }

                    Difficulty difficulty = Difficulty.MEDIUM;
                    Cell diffCell = getCellByHeaders(row, headers, "difficulty");
                    if (diffCell != null && !diffCell.toString().isBlank()) {
                        try { difficulty = Difficulty.valueOf(diffCell.toString().trim().toUpperCase()); }
                        catch (Exception ignored) {}
                    }

                    int marks = 1;
                    Cell marksCell = getCellByHeaders(row, headers, "marks");
                    if (marksCell != null && !marksCell.toString().isBlank()) {
                        marks = Math.max(1, (int) Math.round(parseNumericCell(marksCell, "marks", row.getRowNum())));
                    }

                    double negativeMarks = 0.25;
                    Cell negCell = getCellByHeaders(row, headers, "negative", "negative_marks");
                    if (negCell != null && !negCell.toString().isBlank()) {
                        negativeMarks = Math.max(0, parseNumericCell(negCell, "negative", row.getRowNum()));
                    }

                    Question question = questionRepository.save(Question.builder()
                            .subject(subject)
                            .uploadedBy(uploader)
                            .questionText(questionText)
                            .options(options)
                            .correctOptionIndex(correctIndex)
                            .difficulty(difficulty)
                            .marks(marks)
                            .negativeMarks(negativeMarks)
                            .build());

                    attachImagesFromRow(row, headers, zipImages, embeddedImages, question);
                    uploaded++;

                } catch (IllegalArgumentException ex) {
                    throw ex;
                } catch (Exception rowEx) {
                    throw new IllegalArgumentException(
                            "Error on row " + (row.getRowNum() + 1) + ": " + rowEx.getMessage());
                }
            }

            if (uploaded == 0) {
                throw new IllegalArgumentException(
                        "No valid questions found in the file. " +
                                "Check that column A contains a valid subject ID/code.");
            }

        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Excel upload failed: " + e.getMessage());
        }
    }

    private Subject resolveSubject(Cell subjectCell) {
        if (subjectCell.getCellType() == CellType.NUMERIC) {
            Long subjectId = (long) subjectCell.getNumericCellValue();
            return subjectService.getEntityById(subjectId);
        }

        String subjectCode = subjectCell.toString().trim();
        if (subjectCode.isBlank()) {
            throw new IllegalArgumentException("Subject value cannot be blank");
        }

        // Backward-compatible: allow either numeric internal id or subject code.
        try {
            return subjectService.getEntityById(Long.parseLong(subjectCode));
        } catch (NumberFormatException ignored) {
            return subjectService.getEntityByCode(subjectCode);
        }
    }

    private void attachImagesFromRow(
            Row row,
            Map<String, Integer> headers,
            Map<String, ZipImageData> zipImages,
            Map<String, ZipImageData> embeddedImages,
            Question question
    ) {
        String qPath = getCellString(getCellByHeaders(row, headers, "question_image"));
        String combinedOptPath = getCellString(getCellByHeaders(row, headers, "combined_option_image"));

        ZipImageData qImg = null;
        ZipImageData combinedImg = null;

        qImg = findEmbeddedImage(embeddedImages, row.getRowNum(), headers, "question_image");
        if (qImg == null && !qPath.isBlank()) {
            qImg = findZipImage(zipImages, qPath, row.getRowNum());
        }

        // combined_option_image is now stored separately and rendered in exam page
        // below the question text.
        combinedImg = findEmbeddedImage(embeddedImages, row.getRowNum(), headers, "combined_option_image");
        if (combinedImg == null && !combinedOptPath.isBlank()) {
            combinedImg = findZipImage(zipImages, combinedOptPath, row.getRowNum());
        }

        if (qImg != null || combinedImg != null) {
            upsertQuestionImage(
                    question,
                    qImg != null ? qImg.data() : null,
                    qImg != null ? qImg.mimeType() : null,
                    combinedImg != null ? combinedImg.data() : null,
                    combinedImg != null ? combinedImg.mimeType() : null,
                    true
            );
        }

        List<QuestionOptionImage> optionImages = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String optPath = getCellString(getCellByHeaders(row, headers, "option" + (i + 1) + "_image"));
            ZipImageData optImg = null;
            optImg = findEmbeddedImage(embeddedImages, row.getRowNum(), headers, "option" + (i + 1) + "_image");
            if (optImg == null && !optPath.isBlank()) {
                optImg = findZipImage(zipImages, optPath, row.getRowNum());
            }
            if (optImg == null) continue;
            optionImages.add(QuestionOptionImage.builder()
                    .question(question)
                    .optionIndex(i)
                    .imageData(optImg.data())
                    .imageType(optImg.mimeType())
                    .build());
        }
        if (!optionImages.isEmpty()) {
            optionImageRepository.saveAll(optionImages);
        }
    }

    private boolean hasAnyImageInput(
            Row row,
            Map<String, Integer> headers,
            Map<String, ZipImageData> embeddedImages
    ) {
        String[] imageHeaders = {
                "question_image",
                "combined_option_image",
                "option1_image",
                "option2_image",
                "option3_image",
                "option4_image"
        };

        for (String header : imageHeaders) {
            if (findEmbeddedImage(embeddedImages, row.getRowNum(), headers, header) != null) {
                return true;
            }
            String path = getCellString(getCellByHeaders(row, headers, header));
            if (!path.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private Map<String, ZipImageData> loadZipImages(MultipartFile imageZip) {
        Map<String, ZipImageData> images = new HashMap<>();
        if (imageZip == null || imageZip.isEmpty()) return images;

        String filename = imageZip.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("imageZip must be a .zip file");
        }

        try (ZipInputStream zis = new ZipInputStream(imageZip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String key = normalizeZipPath(entry.getName());
                if (key.isBlank()) continue;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zis.read(buffer)) > 0) {
                    bos.write(buffer, 0, n);
                }
                images.put(key, new ZipImageData(bos.toByteArray(), detectMimeType(key)));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read imageZip: " + e.getMessage());
        }

        return images;
    }

    private ZipImageData findZipImage(Map<String, ZipImageData> zipImages, String imagePath, int rowNum) {
        if (zipImages.isEmpty()) {
            throw new IllegalArgumentException("Row " + (rowNum + 1) + ": image path provided but imageZip is missing");
        }

        String key = normalizeZipPath(imagePath);
        ZipImageData data = zipImages.get(key);

        // Fallback: if Excel provides only file name, resolve uniquely across nested folders.
        if (data == null) {
            String fileName = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
            List<ZipImageData> matches = zipImages.entrySet().stream()
                    .filter(e -> e.getKey().equals(fileName) || e.getKey().endsWith("/" + fileName))
                    .map(Map.Entry::getValue)
                    .toList();
            if (matches.size() == 1) {
                data = matches.get(0);
            } else if (matches.size() > 1) {
                throw new IllegalArgumentException("Row " + (rowNum + 1) + ": ambiguous image file name in ZIP: " + imagePath);
            }
        }

        if (data == null) {
            throw new IllegalArgumentException("Row " + (rowNum + 1) + ": image not found in ZIP: " + imagePath);
        }
        return data;
    }

    private BundlePayload loadBundleZip(MultipartFile bundleZip) {
        Map<String, ZipImageData> images = new HashMap<>();
        byte[] excel = null;

        try (ZipInputStream zis = new ZipInputStream(bundleZip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String key = normalizeZipPath(entry.getName());
                if (key.isBlank()) continue;

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = zis.read(buffer)) > 0) {
                    bos.write(buffer, 0, n);
                }
                byte[] content = bos.toByteArray();

                if (key.endsWith(".xlsx")) {
                    if (excel != null) {
                        throw new IllegalArgumentException("ZIP bundle must contain only one .xlsx file.");
                    }
                    excel = content;
                    continue;
                }

                images.put(key, new ZipImageData(content, detectMimeType(key)));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read ZIP bundle: " + e.getMessage());
        }

        if (excel == null) {
            throw new IllegalArgumentException("ZIP bundle must contain one .xlsx file at root or inside folders.");
        }

        return new BundlePayload(excel, images);
    }

    private String getCellString(Cell cell) {
        return cell == null ? "" : cell.toString().trim();
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel header row is missing");
        }

        Map<String, Integer> headers = new HashMap<>();
        for (Cell cell : headerRow) {
            String key = normalizeHeader(cell.toString());
            if (!key.isBlank()) headers.put(key, cell.getColumnIndex());
        }

        if (!headers.containsKey("question") && !headers.containsKey("question_text")) {
            throw new IllegalArgumentException("Excel header must contain 'question' or 'question_text'");
        }
        if (!headers.containsKey("subject_code") && !headers.containsKey("subject_id")) {
            throw new IllegalArgumentException("Excel header must contain 'subject_code' or 'subject_id'");
        }
        if (!headers.containsKey("correct") && !headers.containsKey("correct_option")) {
            throw new IllegalArgumentException("Excel header must contain 'correct' or 'correct_option'");
        }

        return headers;
    }

    private Cell getCellByHeaders(Row row, Map<String, Integer> headers, String... headerNames) {
        for (String name : headerNames) {
            Integer idx = headers.get(normalizeHeader(name));
            if (idx != null) {
                return row.getCell(idx);
            }
        }
        return null;
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT);
    }

    private int parseCorrectOption(Cell cell, int rowNum) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(cell.getNumericCellValue());
        }

        String raw = cell.toString().trim().toUpperCase(Locale.ROOT);
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Row " + (rowNum + 1) + ": correct_option is blank");
        }
        if (raw.matches("^[0-3]$")) {
            return Integer.parseInt(raw);
        }
        return switch (raw) {
            case "A" -> 0;
            case "B" -> 1;
            case "C" -> 2;
            case "D" -> 3;
            default -> throw new IllegalArgumentException(
                    "Row " + (rowNum + 1) + ": correct_option must be 0/1/2/3 or A/B/C/D. Got: " + raw);
        };
    }

    private double parseNumericCell(Cell cell, String fieldName, int rowNum) {
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        String raw = cell.toString().trim();
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Row " + (rowNum + 1) + ": " + fieldName + " is blank");
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Row " + (rowNum + 1) + ": invalid numeric value for " + fieldName + ": " + raw);
        }
    }

    private String normalizeZipPath(String raw) {
        if (raw == null) return "";
        String normalized = raw.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String embeddedKey(int row, int col) {
        return row + ":" + col;
    }

    private Map<String, ZipImageData> extractEmbeddedImages(Sheet sheet) {
        Map<String, ZipImageData> embedded = new HashMap<>();
        if (!(sheet instanceof XSSFSheet xssfSheet)) return embedded;

        XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
        if (drawing == null) return embedded;

        for (XSSFShape shape : drawing.getShapes()) {
            if (!(shape instanceof XSSFPicture picture)) continue;
            if (!(picture.getAnchor() instanceof XSSFClientAnchor anchor)) continue;

            int row = anchor.getRow1();
            int col = anchor.getCol1();
            if (row < 1 || col < 0) continue; // ignore header-row/unanchored artifacts

            XSSFPictureData data = picture.getPictureData();
            if (data == null || data.getData() == null || data.getData().length == 0) continue;

            String ext = data.suggestFileExtension();
            String mime = detectMimeType(ext == null ? "" : ("file." + ext));
            embedded.putIfAbsent(embeddedKey(row, col), new ZipImageData(data.getData(), mime));
        }

        return embedded;
    }

    private ZipImageData findEmbeddedImage(
            Map<String, ZipImageData> embeddedImages,
            int rowNum,
            Map<String, Integer> headers,
            String headerName
    ) {
        Integer col = headers.get(normalizeHeader(headerName));
        if (col == null) return null;
        return embeddedImages.get(embeddedKey(rowNum, col));
    }

    private String detectMimeType(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }

    private record ZipImageData(byte[] data, String mimeType) {}

    private record BundlePayload(byte[] excelBytes, Map<String, ZipImageData> images) {}

    // ─────────────────────────────────────────────────────────────────────
    // IMAGE SERVING
    // ─────────────────────────────────────────────────────────────────────

    /** Returns raw bytes + MIME type for the question's body image. */
    public byte[] getQuestionImage(Long questionId) {
        QuestionImage img = questionImageRepository.findByQuestionId(questionId).orElse(null);
        if (img != null && img.getQuestionImage() != null) {
            return img.getQuestionImage();
        }

        throw new IllegalArgumentException("Question " + questionId + " has no image.");
    }

    public String getQuestionImageType(Long questionId) {
        QuestionImage img = questionImageRepository.findByQuestionId(questionId).orElse(null);
        if (img != null && img.getQuestionImage() != null) {
            return img.getQuestionImageType() != null ? img.getQuestionImageType() : "application/octet-stream";
        }

        return "application/octet-stream";
    }

    public byte[] getCombinedOptionImage(Long questionId) {
        QuestionImage img = questionImageRepository.findByQuestionId(questionId).orElse(null);
        if (img != null && img.getCombinedOptionImage() != null) {
            return img.getCombinedOptionImage();
        }

        throw new IllegalArgumentException("Question " + questionId + " has no combined option image.");
    }

    public String getCombinedOptionImageType(Long questionId) {
        QuestionImage img = questionImageRepository.findByQuestionId(questionId).orElse(null);
        if (img != null && img.getCombinedOptionImage() != null) {
            return img.getCombinedOptionImageType() != null
                    ? img.getCombinedOptionImageType()
                    : "application/octet-stream";
        }

        return "application/octet-stream";
    }

    /** Returns raw bytes + MIME type for the image of a specific option slot. */
    public QuestionOptionImage getOptionImage(Long questionId, int optionIndex) {
        return optionImageRepository.findByQuestionIdOrderByOptionIndex(questionId)
                .stream()
                .filter(img -> img.getOptionIndex() == optionIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No image for question " + questionId + " option " + optionIndex));
    }

    // ─────────────────────────────────────────────────────────────────────
    // QUERY HELPERS
    // ─────────────────────────────────────────────────────────────────────

    public List<QuestionDTO> findBySubject(Long subjectId) {
        return questionRepository.findBySubjectIdAndActive(subjectId, true)
                .stream()
                .map(q -> toDTO(q, true))
                .collect(Collectors.toList());
    }

    public List<Question> fetchRandom(Long subjectId, int count) {
        return questionRepository.findRandomBySubjectId(subjectId, PageRequest.of(0, count));
    }

    public List<Question> fetchRandom(Long subjectId, int count, Integer marks, Double negativeMarks) {
        int effectiveMarks = marks != null ? marks : 1;
        double effectiveNegative = negativeMarks != null ? negativeMarks : 0.25;
        return questionRepository.findRandomBySubjectIdAndMarks(
                subjectId,
                effectiveMarks,
                effectiveNegative,
                PageRequest.of(0, count)
        );
    }

    public List<Question> fetchAllActive(Long subjectId) {
        return questionRepository.findBySubjectIdAndActive(subjectId, true);
    }

    public List<QuestionDTO> findByIds(List<Long> ids, boolean includeAnswer) {
        return questionRepository.findAllById(ids)
                .stream()
                .map(q -> toDTO(q, includeAnswer))
                .toList();
    }

    public Question getEntityById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    }

    // ─────────────────────────────────────────────────────────────────────
    // DTO CONVERSION
    // ─────────────────────────────────────────────────────────────────────

    public QuestionDTO toDTO(Question q, boolean includeAnswer) {
        // Build per-option image flags
        List<QuestionOptionImage> imgs =
                q.getOptionImages() != null ? q.getOptionImages() : Collections.emptyList();

        int optCount = q.getOptions() != null ? q.getOptions().size() : 4;
        List<Boolean> hasImg = new ArrayList<>(Collections.nCopies(optCount, false));
        for (QuestionOptionImage img : imgs) {
            int idx = img.getOptionIndex();
            if (idx >= 0 && idx < optCount) hasImg.set(idx, true);
        }

        QuestionImage qImg = questionImageRepository.findByQuestionId(q.getId()).orElse(null);
        boolean hasQuestionImage = qImg != null && qImg.getQuestionImage() != null;
        boolean hasCombinedOptionImage = qImg != null && qImg.getCombinedOptionImage() != null;

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
                .hasQuestionImage(hasQuestionImage)
                .hasCombinedOptionImage(hasCombinedOptionImage)
                .optionHasImage(hasImg)
                .build();
    }

    private void upsertQuestionImage(
            Question question,
            byte[] questionBytes,
            String questionMime,
            byte[] combinedBytes,
            String combinedMime,
            boolean copyCombinedToQuestionWhenMissing
    ) {
        QuestionImage image = questionImageRepository.findByQuestionId(question.getId())
                .orElseGet(() -> QuestionImage.builder().question(question).build());

        if (questionBytes != null) {
            image.setQuestionImage(questionBytes);
            image.setQuestionImageType(questionMime);
        }

        if (combinedBytes != null) {
            image.setCombinedOptionImage(combinedBytes);
            image.setCombinedOptionImageType(combinedMime);

            if (copyCombinedToQuestionWhenMissing && image.getQuestionImage() == null) {
                image.setQuestionImage(combinedBytes);
                image.setQuestionImageType(combinedMime);
            }
        }

        questionImageRepository.save(image);
    }
}