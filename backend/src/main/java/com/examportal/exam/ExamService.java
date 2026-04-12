package com.examportal.exam;

import com.examportal.common.FeatureDisabledException;
import com.examportal.config.FeatureFlags;
import com.examportal.question.Question;
import com.examportal.question.QuestionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final BlueprintRepository blueprintRepository;
    private final QuestionService questionService;
    private final FeatureFlags featureFlags;
    private final ObjectMapper objectMapper;

    public ExamDTO create(ExamDTO dto) {
        if (!featureFlags.isBlueprint()) throw new FeatureDisabledException("blueprint");
        ExamBlueprint blueprint = blueprintRepository.findById(dto.getBlueprintId())
                .orElseThrow(() -> new IllegalArgumentException("Blueprint not found: " + dto.getBlueprintId()));
        Exam exam = Exam.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .blueprint(blueprint)
                .scheduledStart(dto.getScheduledStart())
                .scheduledEnd(dto.getScheduledEnd())
                .durationMinutes(dto.getDurationMinutes() != null
                        ? dto.getDurationMinutes() : blueprint.getDurationMinutes())
                .build();
        return toDTO(examRepository.save(exam));
    }

    public BlueprintDTO createBlueprint(BlueprintDTO dto) {
        List<BlueprintEntry> entries = dto.getEntries().stream()
                .map(e -> BlueprintEntry.builder()
                        .subjectId(e.getSubjectId())
                        .sectionName(e.getSectionName())  // null/blank is fine
                        .subjectIds(String.valueOf(e.getSubjectId()))  // keep CSV in sync
                        .questionCount(e.getQuestionCount())
                        .marksPerQuestion(e.getMarksPerQuestion() != null ? e.getMarksPerQuestion() : 1)
                        .negativeMarks(e.getNegativeMarks() != null ? e.getNegativeMarks() : 0.25)
                        .build())
                .toList();

        ExamBlueprint blueprint = ExamBlueprint.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .durationMinutes(dto.getDurationMinutes())
                .totalMarks(dto.getTotalMarks())
                .entries(entries)
                .build();
        blueprint = blueprintRepository.save(blueprint);
        return toBlueprintDTO(blueprint);
    }

    public List<BlueprintDTO> getAllBlueprints() {
        return blueprintRepository.findAll().stream().map(this::toBlueprintDTO).toList();
    }

    @Transactional
    public void deleteBlueprint(Long blueprintId) {
        ExamBlueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new IllegalArgumentException("Blueprint not found: " + blueprintId));
        List<Exam> using = examRepository.findByBlueprintId(blueprintId);
        if (!using.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete blueprint: " + using.size() + " exam(s) reference it.");
        }
        blueprintRepository.delete(blueprint);
    }

    /**
     * Publish: draw random questions from each entry's subject.
     * masterSectionMap groups question IDs by section name (if provided).
     * If no entry has a sectionName, masterSectionMap will be empty and
     * the exam UI shows a flat question list.
     */
    @Transactional
    public ExamDTO publish(Long examId) {
        Exam exam = getEntity(examId);
        if (exam.getStatus() != ExamStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT exams can be published");
        if (exam.getScheduledStart().isAfter(exam.getScheduledEnd()))
            throw new IllegalStateException("Start time must be before end time");

        List<Long> allMasterIds = new ArrayList<>();
        Map<String, List<Long>> sectionMap = new LinkedHashMap<>();

        for (BlueprintEntry entry : exam.getBlueprint().getEntries()) {
            List<Question> selected = questionService.fetchRandom(entry.getSubjectId(), entry.getQuestionCount());
            if (selected.size() < entry.getQuestionCount()) {
                throw new IllegalStateException(
                        "Insufficient questions for subject ID " + entry.getSubjectId() +
                                ". Required: " + entry.getQuestionCount() +
                                ", Available: " + selected.size());
            }
            List<Long> ids = selected.stream().map(Question::getId).collect(Collectors.toList());
            allMasterIds.addAll(ids);

            // Only build section map if this entry has a non-blank sectionName
            String sec = entry.getSectionName();
            if (sec != null && !sec.isBlank()) {
                sectionMap.computeIfAbsent(sec, k -> new ArrayList<>()).addAll(ids);
            }
        }

        try {
            exam.setMasterQuestionIds(objectMapper.writeValueAsString(allMasterIds));
            exam.setMasterSectionMap(objectMapper.writeValueAsString(sectionMap));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize question list");
        }

        exam.setStatus(ExamStatus.PUBLISHED);
        return toDTO(examRepository.save(exam));
    }

    public List<ExamDTO> findActiveExams() {
        return examRepository.findActiveExams(LocalDateTime.now())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ExamDTO> findUpcomingExams() {
        return examRepository.findUpcomingExams(LocalDateTime.now())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ExamDTO> findAll() {
        return examRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ExamDTO findById(Long id) { return toDTO(getEntity(id)); }

    public Exam getEntity(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found: " + id));
    }

    public List<Long> getMasterQuestionIds(Exam exam) {
        try {
            List<Long> ids = objectMapper.readValue(
                    exam.getMasterQuestionIds(), new TypeReference<List<Long>>() {});
            Collections.shuffle(ids);
            return ids;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse master question list for exam: " + exam.getId());
        }
    }

    public Map<String, List<Long>> getMasterSectionMap(Exam exam) {
        try {
            if (exam.getMasterSectionMap() == null || exam.getMasterSectionMap().isBlank())
                return Map.of();
            return objectMapper.readValue(exam.getMasterSectionMap(),
                    new TypeReference<LinkedHashMap<String, List<Long>>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireFinishedExams() {
        List<Exam> expired = examRepository.findExpiredExams(LocalDateTime.now().minusSeconds(30));
        expired.forEach(e -> e.setStatus(ExamStatus.COMPLETED));
        if (!expired.isEmpty()) examRepository.saveAll(expired);
    }

    private ExamDTO toDTO(Exam e) {
        return ExamDTO.builder()
                .id(e.getId()).title(e.getTitle()).description(e.getDescription())
                .blueprintId(e.getBlueprint() != null ? e.getBlueprint().getId() : null)
                .status(e.getStatus()).scheduledStart(e.getScheduledStart())
                .scheduledEnd(e.getScheduledEnd()).durationMinutes(e.getDurationMinutes())
                .build();
    }

    private BlueprintDTO toBlueprintDTO(ExamBlueprint b) {
        List<BlueprintEntryDTO> entries = b.getEntries().stream()
                .map(e -> BlueprintEntryDTO.builder()
                        .subjectId(e.getSubjectId())
                        .sectionName(e.getSectionName())
                        .questionCount(e.getQuestionCount())
                        .marksPerQuestion(e.getMarksPerQuestion())
                        .negativeMarks(e.getNegativeMarks())
                        .build())
                .toList();
        return BlueprintDTO.builder()
                .id(b.getId()).name(b.getName()).description(b.getDescription())
                .durationMinutes(b.getDurationMinutes()).totalMarks(b.getTotalMarks())
                .entries(entries).build();
    }
}