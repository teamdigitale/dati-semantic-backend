package it.gov.innovazione.ndc.alerter.entities;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile implements Nameable {
    @Id
    @GeneratedValue(generator = "uuid")
    @UuidGenerator
    private String id;
    @Column(unique = true, nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = EventCategory.class)
    private List<EventCategory> eventCategories;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity minSeverity;
    @Column(nullable = false)
    private Long aggregationTime;
    @Column
    @Builder.Default
    private Instant lastAlertedAt = Instant.now();
}
