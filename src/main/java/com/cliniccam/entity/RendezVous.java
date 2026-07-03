package com.cliniccam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 * ENTITÉ RENDEZ-VOUS
 * ================================================================
 *
 * Cœur de l'application : relie un Patient à un Médecin
 * pour une consultation à une date/heure donnée.
 *
 * Cycle de vie d'un RDV :
 *   EN_ATTENTE → CONFIRME → EN_COURS → TERMINE
 *                        ↘ ANNULE (à tout moment avant EN_COURS)
 *
 * Relations :
 *   - @ManyToOne patient  : plusieurs RDV pour un patient
 *   - @ManyToOne medecin  : plusieurs RDV pour un médecin
 * ================================================================
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Le patient qui prend le rendez-vous
     * LAZY : ne charge pas tout l'objet User si on n'en a pas besoin
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    /**
     * Le médecin consulté
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private User medecin;

    /**
     * Date et heure du rendez-vous
     * Ex : 2024-12-25T09:30:00
     */
    @Column(nullable = false)
    private LocalDateTime dateHeure;

    /**
     * Durée de la consultation en minutes
     * Snapshot au moment de la réservation (peut changer pour le médecin)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer dureeMinutes = 30;

    /**
     * Motif de la consultation (ex: "Douleur thoracique", "Suivi diabète")
     * Renseigné par le patient lors de la prise de RDV
     */
    @Column(columnDefinition = "TEXT")
    private String motif;

    /**
     * Notes du médecin après la consultation (compte-rendu)
     * Renseigné par le médecin après la consultation
     * Visible uniquement par le médecin et l'admin
     */
    @Column(columnDefinition = "TEXT")
    private String notesMedecin;

    /**
     * Ordonnance (texte libre pour la démo)
     * En production : document PDF lié
     */
    @Column(columnDefinition = "TEXT")
    private String ordonnance;

    /**
     * Tarif facturé (snapshot du tarif du médecin au moment du RDV)
     */
    private Integer tarif;

    /**
     * Statut du rendez-vous
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;

    /**
     * Raison d'annulation (obligatoire si statut = ANNULE)
     */
    private String raisonAnnulation;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================

    /**
     * Calcule l'heure de fin du RDV (début + durée)
     */
    public LocalDateTime getDateHeureFin() {
        return dateHeure.plusMinutes(dureeMinutes);
    }

    /**
     * Vérifie si le RDV peut encore être annulé
     */
    public boolean isAnnulable() {
        return statut == Statut.EN_ATTENTE || statut == Statut.CONFIRME;
    }

    // ============================================================
    // ENUM STATUTS
    // ============================================================

    public enum Statut {
        EN_ATTENTE,   // Patient a pris RDV, médecin doit confirmer
        CONFIRME,     // Médecin a confirmé le créneau
        EN_COURS,     // Consultation en train de se dérouler
        TERMINE,      // Consultation terminée, notes disponibles
        ANNULE        // Annulé (par patient, médecin ou admin)
    }
}
