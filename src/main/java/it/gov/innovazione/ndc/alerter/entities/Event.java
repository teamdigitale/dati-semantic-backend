package it.gov.innovazione.ndc.alerter.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "occurredAt"})})
public class Event implements Nameable {
    @Id
    @GeneratedValue(generator = "uuid")
    @UuidGenerator
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String context;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private String createdBy;
}
