package com.cliniccam.service;

import com.cliniccam.dto.Dto.*;
import com.cliniccam.entity.RendezVous;
import com.cliniccam.entity.User;
import com.cliniccam.repository.RendezVousRepository;
import com.cliniccam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ================================================================
 * SERVICE RENDEZ-VOUS
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RendezVousService {

    private final RendezVousRepository rendezVousRepository;
    private final UserRepository userRepository;

    /**
     * Prendre un rendez-vous (par un patient)
     *
     * Vérifie :
     *   1. Le médecin existe et est disponible
     *   2. Le créneau demandé n'est pas déjà pris (pas de conflit)
     *   3. La date est dans le futur
     */
    @Transactional
    public RendezVousResponse prendreRendezVous(CreateRendezVousRequest request, User patient) {

        // 1. Récupérer le médecin
        User medecin = userRepository.findById(request.getMedecinId())
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));

        if (medecin.getRole() != User.Role.MEDECIN) {
            throw new IllegalArgumentException("L'utilisateur sélectionné n'est pas un médecin");
        }

        if (!Boolean.TRUE.equals(medecin.getDisponible())) {
            throw new IllegalStateException("Ce médecin n'est plus disponible pour de nouveaux patients");
        }

        // 2. Vérifier que la date est dans le futur
        if (request.getDateHeure().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Impossible de prendre un RDV dans le passé");
        }

        // 3. Calculer l'heure de fin du créneau demandé
        int duree = medecin.getDureeeconsultationMinutes(); /*!= null
                ? medecin.getDureeeconsultationMinutes() : 30;*/
        LocalDateTime heureFin = request.getDateHeure().plusMinutes(duree);

        // 4. Vérifier les conflits de créneaux
        // (cette requête JPQL vérifie les chevauchements d'horaires)
        boolean conflit = rendezVousRepository.existsConflitCreneaux(
                medecin.getId(),
                request.getDateHeure(),
                heureFin
        );

        if (conflit) {
            throw new IllegalStateException(
                    "Ce créneau est déjà pris. Veuillez choisir un autre horaire."
            );
        }

        // 5. Créer le RDV
        RendezVous rdv = RendezVous.builder()
                .patient(patient)
                .medecin(medecin)
                .dateHeure(request.getDateHeure())
                .dureeMinutes(duree)
                .motif(request.getMotif())
                .tarif(medecin.getTarifConsultation())
                .statut(RendezVous.Statut.EN_ATTENTE)
                .build();

        RendezVous saved = rendezVousRepository.save(rdv);
        log.info("RDV créé : patient={} médecin={} date={}",
                patient.getEmail(), medecin.getEmail(), request.getDateHeure());

        return RendezVousResponse.fromRendezVous(saved);
    }

    /**
     * Mes rendez-vous (patient) — paginés
     */
    public PageResponse<RendezVousResponse> getMesRendezVous(Long patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RendezVous> pageResult =
                rendezVousRepository.findByPatientIdOrderByDateHeureDesc(patientId, pageable);

        return buildPageResponse(pageResult);
    }

    /**
     * Agenda du médecin (pour la semaine en cours)
     */
    public List<RendezVousResponse> getAgendaMedecin(Long medecinId, LocalDate debut, LocalDate fin) {
        return rendezVousRepository
                .findByMedecinIdAndDateHeureBetweenOrderByDateHeureAsc(
                        medecinId,
                        debut.atStartOfDay(),
                        fin.atTime(23, 59, 59)
                )
                .stream()
                .map(RendezVousResponse::fromRendezVous)
                .collect(Collectors.toList());
    }

    /**
     * RDV du jour pour un médecin
     */
    public List<RendezVousResponse> getRdvDuJour(Long medecinId) {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);
        return rendezVousRepository.findRdvDuJour(medecinId, debut, fin)
                .stream()
                .map(RendezVousResponse::fromRendezVous)
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour un RDV (par le médecin ou l'admin)
     * Ex : confirmer, ajouter notes, mettre TERMINE, annuler
     */
    @Transactional
    public RendezVousResponse updateRendezVous(Long rdvId, UpdateRendezVousRequest request, User currentUser) {
        RendezVous rdv = rendezVousRepository.findById(rdvId)
                .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé : " + rdvId));

        // Vérification des droits : médecin concerné ou admin
        boolean isMedecinConcerne = rdv.getMedecin().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isMedecinConcerne && !isAdmin) {
            throw new SecurityException("Vous n'êtes pas autorisé à modifier ce rendez-vous");
        }

        // Mettre à jour les champs non-null
        if (request.getStatut() != null) {
            // Vérifier la cohérence du changement de statut
            validerTransitionStatut(rdv.getStatut(), request.getStatut());
            rdv.setStatut(request.getStatut());
        }
        if (request.getNotesMedecin() != null) rdv.setNotesMedecin(request.getNotesMedecin());
        if (request.getOrdonnance() != null) rdv.setOrdonnance(request.getOrdonnance());
        if (request.getRaisonAnnulation() != null) rdv.setRaisonAnnulation(request.getRaisonAnnulation());

        RendezVous updated = rendezVousRepository.save(rdv);
        log.info("RDV {} mis à jour → statut: {}", rdvId, updated.getStatut());

        return RendezVousResponse.fromRendezVous(updated);
    }

    /**
     * Annuler un RDV (par le patient)
     */
    @Transactional
    public RendezVousResponse annulerParPatient(Long rdvId, String raison, User patient) {
        RendezVous rdv = rendezVousRepository.findById(rdvId)
                .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé"));

        if (!rdv.getPatient().getId().equals(patient.getId())) {
            throw new SecurityException("Ce rendez-vous ne vous appartient pas");
        }

        if (!rdv.isAnnulable()) {
            throw new IllegalStateException("Ce rendez-vous ne peut plus être annulé");
        }

        rdv.setStatut(RendezVous.Statut.ANNULE);
        rdv.setRaisonAnnulation(raison != null ? raison : "Annulé par le patient");

        return RendezVousResponse.fromRendezVous(rendezVousRepository.save(rdv));
    }

    /**
     * Tous les RDV (admin) avec pagination
     */
    public PageResponse<RendezVousResponse> getAllRendezVous(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RendezVous> pageResult = rendezVousRepository.findAllByOrderByCreatedAtDesc(pageable);
        return buildPageResponse(pageResult);
    }

    // ============================================================
    // MÉTHODES PRIVÉES
    // ============================================================

    /**
     * Valide que la transition de statut est logique.
     * Ex : on ne peut pas passer de TERMINE à EN_ATTENTE
     */
    private void validerTransitionStatut(RendezVous.Statut actuel, RendezVous.Statut nouveau) {
        if (actuel == RendezVous.Statut.ANNULE) {
            throw new IllegalStateException("Impossible de modifier un RDV annulé");
        }
        if (actuel == RendezVous.Statut.TERMINE && nouveau != RendezVous.Statut.TERMINE) {
            throw new IllegalStateException("Impossible de modifier un RDV terminé");
        }
    }

    /** Convertit Page<RendezVous> en PageResponse<RendezVousResponse> */
    private PageResponse<RendezVousResponse> buildPageResponse(Page<RendezVous> pageResult) {
        List<RendezVousResponse> content = pageResult.getContent()
                .stream()
                .map(RendezVousResponse::fromRendezVous)
                .collect(Collectors.toList());

        return PageResponse.<RendezVousResponse>builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }
}


