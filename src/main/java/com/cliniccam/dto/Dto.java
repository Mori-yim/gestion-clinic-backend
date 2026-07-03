package com.cliniccam.dto;

import com.cliniccam.entity.RendezVous;
import com.cliniccam.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * DTOs CLINICCAM — Data Transfer Objects
 * ================================================================
 *
 * Organisés en classes statiques imbriquées dans ce fichier unique.
 *
 * NOUVEAUTÉS vs BusCam :
 *   - PageResponse<T> : enveloppe pour les résultats paginés
 *   - DashboardStats  : objet complexe pour les graphiques
 *   - MedecinResponse : enrichi avec spécialité, biographie
 * ================================================================
 */
public class Dto {

    // ============================================================
    // AUTH DTOs
    // ============================================================

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Le prénom est obligatoire")
        private String firstName;

        @NotBlank(message = "Le nom est obligatoire")
        private String lastName;

        @NotBlank @Email(message = "Email invalide")
        private String email;

        @NotBlank
        @Size(min = 6, message = "Minimum 6 caractères")
        private String password;

        @Pattern(regexp = "^(\\+237)?[6][5-9][0-9]{7}$",
                message = "Numéro camerounais invalide")
        private String phone;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateNaissance;

        @Pattern(regexp = "^[MF]$", message = "Genre : M ou F")
        private String genre;

        /** Rôle demandé : PATIENT par défaut, MEDECIN si inscription pro */
        private User.Role role;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String tokenType;
        private UserResponse user;

        public static AuthResponse of(String token, User user) {
            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(UserResponse.fromUser(user))
                    .build();
        }
    }

    // ============================================================
    // USER / MÉDECIN DTOs
    // ============================================================

    /**
     * Réponse utilisateur générique (sans mot de passe)
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String fullName;
        private String titre;        // "Dr. X" pour médecins
        private String email;
        private String phone;
        private String role;
        private String genre;
        private String dateNaissance;
        private String photoUrl;
        private String createdAt;

        // Champs médecin (null si PATIENT/ADMIN)
        private String specialite;
        private String biographie;
        private Integer tarifConsultation;
        private Boolean disponible;
        private Integer dureeConsultationMinutes;

        public static UserResponse fromUser(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .fullName(user.getFullName())
                    .titre(user.getTitre())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .genre(user.getGenre())
                    .dateNaissance(user.getDateNaissance() != null
                            ? user.getDateNaissance().toString() : null)
                    .photoUrl(user.getPhotoUrl())
                    .createdAt(user.getCreatedAt().toString())
                    .specialite(user.getSpecialite())
                    .biographie(user.getBiographie())
                    .tarifConsultation(user.getTarifConsultation())
                    .disponible(user.getDisponible())
                    //.dureeConsultationMinutes(user.getDureeeconsultationMinutes())
                    .build();
        }
    }

    /**
     * Mise à jour du profil médecin (par lui-même ou l'admin)
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateMedecinRequest {
        private String phone;
        private String biographie;
        private Integer tarifConsultation;
        private Integer dureeConsultationMinutes;
        private Boolean disponible;
        private String photoUrl;
    }

    // ============================================================
    // RENDEZ-VOUS DTOs
    // ============================================================

    /**
     * Prise de rendez-vous par le patient
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateRendezVousRequest {

        @NotNull(message = "L'ID du médecin est obligatoire")
        private Long medecinId;

        @NotNull(message = "La date et l'heure sont obligatoires")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateHeure;

        @NotBlank(message = "Le motif de consultation est obligatoire")
        @Size(max = 500, message = "Motif trop long (500 caractères max)")
        private String motif;
    }

    /**
     * Mise à jour par le médecin (confirmation, notes, ordonnance)
     */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRendezVousRequest {
        private RendezVous.Statut statut;
        private String notesMedecin;
        private String ordonnance;
        private String raisonAnnulation;
    }

    /**
     * Réponse complète d'un rendez-vous
     * Inclut les infos du patient et du médecin (sans mdp)
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RendezVousResponse {
        private Long id;
        private UserResponse patient;
        private UserResponse medecin;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateHeure;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateHeureFin;

        private Integer dureeMinutes;
        private String motif;
        private String notesMedecin;
        private String ordonnance;
        private Integer tarif;
        private String statut;
        private String raisonAnnulation;
        private String createdAt;

        public static RendezVousResponse fromRendezVous(RendezVous rv) {
            return RendezVousResponse.builder()
                    .id(rv.getId())
                    .patient(UserResponse.fromUser(rv.getPatient()))
                    .medecin(UserResponse.fromUser(rv.getMedecin()))
                    .dateHeure(rv.getDateHeure())
                    .dateHeureFin(rv.getDateHeureFin())
                    .dureeMinutes(rv.getDureeMinutes())
                    .motif(rv.getMotif())
                    .notesMedecin(rv.getNotesMedecin())
                    .ordonnance(rv.getOrdonnance())
                    .tarif(rv.getTarif())
                    .statut(rv.getStatut().name())
                    .raisonAnnulation(rv.getRaisonAnnulation())
                    .createdAt(rv.getCreatedAt().toString())
                    .build();
        }
    }

    // ============================================================
    // DASHBOARD STATS DTOs
    // ============================================================

    /**
     * Objet principal envoyé au dashboard React.
     * React Query le met en cache, Recharts l'utilise directement.
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardStats {

        /** KPIs globaux (cartes en haut du dashboard) */
        private long totalPatients;
        private long totalMedecins;
        private long totalRdv;
        private long rdvAujourdhui;
        private long rdvEnAttente;
        private long rdvTermines;
        private long revenuTotal;       // FCFA
        private long revenu30Jours;     // FCFA (30 derniers jours)

        /**
         * Données pour le graphique linéaire "RDV des 7 derniers jours"
         * Format : [{ "date": "2024-12-20", "rdv": 8 }, ...]
         * Compatible directement avec Recharts <LineChart>
         */
        private List<Map<String, Object>> rdvParJour;

        /**
         * Données pour le graphique camembert "RDV par spécialité"
         * Format : [{ "specialite": "Cardiologie", "value": 15 }, ...]
         * Compatible avec Recharts <PieChart>
         */
        private List<Map<String, Object>> rdvParSpecialite;

        /**
         * Données pour le graphique barres "Statuts des RDV"
         * Format : [{ "statut": "CONFIRME", "count": 12 }, ...]
         * Compatible avec Recharts <BarChart>
         */
        private List<Map<String, Object>> rdvParStatut;

        /**
         * Top 5 médecins par nombre de consultations terminées
         */
        private List<TopMedecinDto> topMedecins;
    }

    /** Médecin dans le classement du dashboard */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopMedecinDto {
        private Long id;
        private String nom;
        private String specialite;
        private long consultations;
    }

    // ============================================================
    // PAGINATION
    // ============================================================

    /**
     * Enveloppe générique pour les réponses paginées.
     *
     * Le frontend React peut ainsi afficher la pagination :
     *   "Page 1 / 10 — 47 résultats au total"
     *
     * Compatible avec le composant Pagination React.
     */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PageResponse<T> {
        private List<T> content;         // Données de la page courante
        private int page;                // Numéro de page (0-indexé)
        private int size;                // Taille de la page
        private long totalElements;      // Total des enregistrements
        private int totalPages;          // Nombre total de pages
        private boolean first;           // true si c'est la première page
        private boolean last;            // true si c'est la dernière page
    }

    // ============================================================
    // RÉPONSE API GÉNÉRIQUE
    // ============================================================

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
