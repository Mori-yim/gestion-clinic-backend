package com.cliniccam.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ================================================================
 * ENTITÉ UTILISATEUR — CLINICCAM
 * ================================================================
 *
 * Un seul modèle User pour les 3 rôles :
 *   - PATIENT : prend des rendez-vous, voit son historique
 *   - MEDECIN : gère son agenda, voit ses patients du jour
 *   - ADMIN   : gère tout (users, médecins, stats globales)
 *
 * Les champs spécifiques au médecin (spécialite, numeroOrdre)
 * sont nullable pour les autres rôles.
 * ================================================================
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /** Email = identifiant de connexion */
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    /** Téléphone camerounais */
    @Column(length = 20)
    private String phone;

    /** Date de naissance (utile pour le dossier médical) */
    private LocalDate dateNaissance;

    /**
     * Genre : M (Masculin), F (Féminin)
     * Stocké comme String pour éviter les problèmes d'enum Postgres
     */
    @Column(length = 1)
    private String genre;

    // ============================================================
    // CHAMPS SPÉCIFIQUES AU MÉDECIN (null si PATIENT/ADMIN)
    // ============================================================

    /**
     * Spécialité médicale (ex: "Cardiologie", "Pédiatrie")
     * Null si l'utilisateur n'est pas MEDECIN
     */
    private String specialite;

    /**
     * Numéro d'ordre du médecin au Cameroun
     * Identifiant officiel de la profession médicale
     */
    private String numeroOrdre;

    /**
     * Durée standard d'une consultation (en minutes)
     * Utilisé pour bloquer le créneau dans l'agenda
     */
    @Builder.Default
    private Integer dureeconsultationMinutes = 30;

    /**
     * Photo de profil (URL stockée, ex: Cloudinary ou Supabase Storage)
     */
    private String photoUrl;

    /**
     * Biographie du médecin (affiché sur sa fiche publique)
     */
    @Column(columnDefinition = "TEXT")
    private String biographie;

    /** Tarif de consultation en FCFA */
    private Integer tarifConsultation;

    /** true = disponible pour de nouveaux patients */
    @Builder.Default
    private Boolean disponible = true;

    // ============================================================
    // CHAMPS COMMUNS
    // ============================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.PATIENT;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ============================================================
    // UserDetails (Spring Security)
    // ============================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return true; }

    /** Nom complet */
    public String getFullName() { return firstName + " " + lastName; }

    /** Nom d'affichage médecin avec titre */
    public String getTitre() {
        return role == Role.MEDECIN ? "Dr. " + getFullName() : getFullName();
    }

    public void setDureeeconsultationMinutes(Integer dureeConsultationMinutes) {
    }

    public int getDureeeconsultationMinutes() {
        return this.dureeconsultationMinutes;
    }

    // ============================================================
    // ENUMS
    // ============================================================

    public enum Role {
        PATIENT,  // Peut prendre des RDV
        MEDECIN,  // Gère son agenda
        ADMIN     // Gestion complète
    }
}
