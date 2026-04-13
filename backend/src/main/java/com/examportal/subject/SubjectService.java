package com.examportal.subject;

import com.examportal.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SubjectService
 * CHANGE: delete() cascades to questions. findByCode() enables Subject Code = Subject ID lookup.
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;

    public SubjectDTO create(SubjectDTO dto) {
        if (subjectRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Subject '" + dto.getName() + "' already exists");
        }
        if (subjectRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Subject code '" + dto.getCode() + "' already exists");
        }
        return toDTO(subjectRepository.save(Subject.builder()
            .name(dto.getName()).description(dto.getDescription()).code(dto.getCode()).build()));
    }

    public List<SubjectDTO> findAll() {
        return subjectRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SubjectDTO findById(Long id) { return toDTO(getEntityById(id)); }

    public Subject getEntityById(Long id) {
        return subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
    }
    public Subject getEntityByCode(String code) {
        return subjectRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject code: " + code));
    }

    public SubjectDTO update(Long id, SubjectDTO dto) {
        Subject s = getEntityById(id);
        s.setName(dto.getName());
        s.setDescription(dto.getDescription());
        s.setCode(dto.getCode());
        return toDTO(subjectRepository.save(s));
    }

    /**
     * CHANGE: Delete subject and all its questions.
     * Questions reference the subject via FK; deleting subject cascades to questions.
     */
    @Transactional
    public void delete(Long id) {
        Subject s = getEntityById(id);
        // Delete all questions for this subject first (FK constraint)
        questionRepository.deleteBySubjectId(id);
        subjectRepository.delete(s);
    }

    public SubjectDTO toDTO(Subject s) {
        return SubjectDTO.builder().id(s.getId()).name(s.getName())
            .description(s.getDescription()).code(s.getCode()).build();
    }
}
