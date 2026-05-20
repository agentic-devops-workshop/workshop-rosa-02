package com.sifap.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_entry")
public class AuditEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String entityType;

    @Column(nullable = false, length = 64)
    private String entityId;

    /** IN (inclusão), AL (alteração), EX (exclusão). REQ-AUD-002. */
    @Column(nullable = false, length = 2)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String stateBefore;

    @Column(columnDefinition = "TEXT")
    private String stateAfter;

    @Column(length = 64)
    private String reason;

    /** CPF mascarado XXX.XXX.NNN-NN. REQ-AUD-001. */
    @Column(length = 20)
    private String cpfMasked;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String v) { this.entityType = v; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String v) { this.entityId = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getStateBefore() { return stateBefore; }
    public void setStateBefore(String v) { this.stateBefore = v; }
    public String getStateAfter() { return stateAfter; }
    public void setStateAfter(String v) { this.stateAfter = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public String getCpfMasked() { return cpfMasked; }
    public void setCpfMasked(String v) { this.cpfMasked = v; }
    public Instant getCreatedAt() { return createdAt; }
}
