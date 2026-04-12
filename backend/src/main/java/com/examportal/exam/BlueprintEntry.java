package com.examportal.exam;

import jakarta.persistence.*;
import lombok.*;

/**
 * BlueprintEntry - one row per subject per blueprint.
 * Each entry specifies one subject and how many questions to draw from it.
 * sectionName is optional — if blank, the UI shows a flat question list.
 *
 * subject_id is kept NOT NULL (legacy constraint) and is always set to
 * the single subject ID for this entry. subject_ids (CSV) is kept for
 * display purposes but subject_id drives the actual question fetch.
 */
@Entity
@Table(name = "blueprint_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The subject this entry draws questions from. Always set — satisfies NOT NULL. */
    @Column(name = "subject_id", nullable = false)
    @Builder.Default
    private Long subjectId = 0L;

    /** Optional section label shown in exam UI. Null/blank = no section tabs. */
    @Column(name = "section_name", nullable = true)
    private String sectionName;

    /** CSV kept for reference, not used for question fetching. */
    @Column(name = "subject_ids", nullable = true)
    private String subjectIds;

    @Column(nullable = false)
    private Integer questionCount;

    @Column(nullable = false)
    @Builder.Default
    private Integer marksPerQuestion = 1;

    @Builder.Default
    private Double negativeMarks = 0.25;
}