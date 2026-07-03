package com.cliniccam.controller;

import com.cliniccam.dto.Dto.*;
import com.cliniccam.entity.User;
import com.cliniccam.service.AuthService;
import com.cliniccam.service.DashboardService;
import com.cliniccam.service.MedecinService;
import com.cliniccam.service.RendezVousService;
//import com.cliniccam.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ================================================================
 * CONTROLLER AUTHENTIFICATION
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/v1/auth/register */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.register(request),
                        "Compte créé avec succès ! Bienvenue sur ClinicCam 🏥"));
    }

    /** POST /api/v1/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Connexion réussie"));
    }

    /** GET /api/v1/auth/me */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromUser(user), "Profil récupéré"));
    }
}


/**
 * ================================================================
 * CONTROLLER MÉDECINS
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/medecins")
@RequiredArgsConstructor
class MedecinController {

    private final MedecinService medecinService;

    /**
     * GET /api/v1/medecins
     * Liste tous les médecins (PUBLIC — page d'accueil)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllMedecins(
            @RequestParam(required = false) String specialite
    ) {
        List<UserResponse> medecins = specialite != null
                ? medecinService.getMedecinsBySpecialite(specialite)
                : medecinService.getAllMedecins();
        return ResponseEntity.ok(ApiResponse.success(medecins, medecins.size() + " médecin(s) trouvé(s)"));
    }

    /**
     * GET /api/v1/medecins/{id}
     * Fiche d'un médecin (PUBLIC)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getMedecin(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(medecinService.getMedecinById(id), "Médecin trouvé"));
    }

    /**
     * PUT /api/v1/medecins/{id}
     * Mise à jour du profil (médecin lui-même ou admin)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateMedecin(
            @PathVariable Long id,
            @RequestBody UpdateMedecinRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                medecinService.updateMedecin(id, request, currentUser),
                "Profil mis à jour"
        ));
    }

    /**
     * GET /api/v1/medecins/{id}/rdv-du-jour
     * Rendez-vous du jour (médecin connecté)
     */
    @GetMapping("/{id}/rdv-du-jour")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RendezVousResponse>>> getRdvDuJour(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                medecinService.getRdvDuJour(id),
                "RDV du jour récupérés"
        ));
    }

    /**
     * GET /api/v1/medecins/{id}/agenda?debut=2024-12-01&fin=2024-12-31
     * Agenda d'un médecin sur une période
     */
    @GetMapping("/{id}/agenda")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<RendezVousResponse>>> getAgenda(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                medecinService.getAgendaMedecin(id, debut, fin),
                "Agenda récupéré"
        ));
    }
}


/**
 * ================================================================
 * CONTROLLER RENDEZ-VOUS
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/rendez-vous")
@RequiredArgsConstructor
class RendezVousController {

    private final RendezVousService rendezVousService;

    /**
     * POST /api/v1/rendez-vous
     * Prendre un rendez-vous (PATIENT connecté)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RendezVousResponse>> prendreRendezVous(
            @Valid @RequestBody CreateRendezVousRequest request,
            @AuthenticationPrincipal User patient
    ) {
        RendezVousResponse rdv = rendezVousService.prendreRendezVous(request, patient);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(rdv, "Rendez-vous pris avec succès ! En attente de confirmation."));
    }

    /**
     * GET /api/v1/rendez-vous/mes-rdv?page=0&size=10
     * Mes rendez-vous (patient connecté) — paginés
     *
     * @RequestParam page  : numéro de page (défaut 0)
     * @RequestParam size  : taille de la page (défaut 10)
     */
    @GetMapping("/mes-rdv")
    public ResponseEntity<ApiResponse<PageResponse<RendezVousResponse>>> getMesRendezVous(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                rendezVousService.getMesRendezVous(patient.getId(), page, size),
                "Rendez-vous récupérés"
        ));
    }

    /**
     * PUT /api/v1/rendez-vous/{id}
     * Mettre à jour un RDV (MEDECIN ou ADMIN)
     * Ex : confirmer, ajouter notes, ordonnance, terminer
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<RendezVousResponse>> updateRendezVous(
            @PathVariable Long id,
            @RequestBody UpdateRendezVousRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                rendezVousService.updateRendezVous(id, request, currentUser),
                "Rendez-vous mis à jour"
        ));
    }

    /**
     * PUT /api/v1/rendez-vous/{id}/annuler
     * Annuler un RDV (par le patient propriétaire)
     */
    @PutMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<RendezVousResponse>> annuler(
            @PathVariable Long id,
            @RequestParam(required = false) String raison,
            @AuthenticationPrincipal User patient
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                rendezVousService.annulerParPatient(id, raison, patient),
                "Rendez-vous annulé"
        ));
    }
}


/**
 * ================================================================
 * CONTROLLER ADMIN & DASHBOARD
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class AdminController {

    private final MedecinService medecinService;
    private final RendezVousService rendezVousService;
    private final DashboardService dashboardService;

    /**
     * GET /api/v1/dashboard/stats
     * Statistiques complètes pour le dashboard (ADMIN)
     * Utilisé par les graphiques Recharts sur le frontend
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStats>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getDashboardStats(),
                "Statistiques récupérées"
        ));
    }

    /**
     * GET /api/v1/specialites
     * Toutes les spécialités disponibles (PUBLIC)
     */
    @GetMapping("/specialites")
    public ResponseEntity<ApiResponse<List<String>>> getSpecialites() {
        return ResponseEntity.ok(ApiResponse.success(medecinService.getSpecialites(), "Spécialités récupérées"));
    }

    /**
     * GET /api/v1/admin/patients?page=0&size=10&search=kamga
     * Liste paginée des patients (ADMIN)
     */
    @GetMapping("/admin/patients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                medecinService.getPatientsPagines(page, size, search),
                "Patients récupérés"
        ));
    }

    /**
     * GET /api/v1/admin/rendez-vous?page=0&size=10
     * Tous les RDV paginés (ADMIN)
     */
    @GetMapping("/admin/rendez-vous")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<RendezVousResponse>>> getAllRdv(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                rendezVousService.getAllRendezVous(page, size),
                "Rendez-vous récupérés"
        ));
    }
}
