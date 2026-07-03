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
 * BEAN PUBLIC : MedecinService
 * ================================================================
 * Exposé comme @Service public pour injection dans les controllers.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedecinService {

    private final UserRepository userRepository;
    private final RendezVousRepository rendezVousRepository;

    public List<UserResponse> getAllMedecins() {
        return userRepository.findByRoleOrderByLastNameAsc(User.Role.MEDECIN)
                .stream().map(UserResponse::fromUser).collect(Collectors.toList());
    }

    public List<UserResponse> getMedecinsBySpecialite(String specialite) {
        return userRepository
                .findByRoleAndSpecialiteIgnoreCaseAndDisponibleTrue(User.Role.MEDECIN, specialite)
                .stream().map(UserResponse::fromUser).collect(Collectors.toList());
    }

    public UserResponse getMedecinById(Long id) {
        User medecin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé : " + id));
        return UserResponse.fromUser(medecin);
    }

    public PageResponse<UserResponse> getPatientsPagines(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> pageResult = userRepository.findByRoleAndSearch(
                User.Role.PATIENT, search != null ? search : "", pageable);

        List<UserResponse> content = pageResult.getContent()
                .stream().map(UserResponse::fromUser).collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(content).page(pageResult.getNumber()).size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements()).totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst()).last(pageResult.isLast()).build();
    }

    public List<String> getSpecialites() {
        return userRepository.findAllSpecialites();
    }

    @Transactional
    public UserResponse updateMedecin(Long id, UpdateMedecinRequest request, User currentUser) {
        User medecin = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé : " + id));

        boolean isSelf = medecin.getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
        if (!isSelf && !isAdmin) throw new SecurityException("Accès refusé");

        if (request.getPhone() != null) medecin.setPhone(request.getPhone());
        if (request.getBiographie() != null) medecin.setBiographie(request.getBiographie());
        if (request.getTarifConsultation() != null) medecin.setTarifConsultation(request.getTarifConsultation());
        if (request.getDureeConsultationMinutes() != null) medecin.setDureeeconsultationMinutes(request.getDureeConsultationMinutes());
        if (request.getDisponible() != null) medecin.setDisponible(request.getDisponible());
        if (request.getPhotoUrl() != null) medecin.setPhotoUrl(request.getPhotoUrl());

        return UserResponse.fromUser(userRepository.save(medecin));
    }

    public List<RendezVousResponse> getRdvDuJour(Long medecinId) {
        LocalDateTime debut = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(23, 59, 59);
        return rendezVousRepository.findRdvDuJour(medecinId, debut, fin)
                .stream().map(RendezVousResponse::fromRendezVous).collect(Collectors.toList());
    }

    public List<RendezVousResponse> getAgendaMedecin(Long medecinId, LocalDate debut, LocalDate fin) {
        return rendezVousRepository
                .findByMedecinIdAndDateHeureBetweenOrderByDateHeureAsc(
                        medecinId, debut.atStartOfDay(), fin.atTime(23, 59, 59))
                .stream().map(RendezVousResponse::fromRendezVous).collect(Collectors.toList());
    }
}


/**
 * ================================================================
 * BEAN PUBLIC : DashboardService
 * ================================================================
 */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class DashboardService {
//
//    private final RendezVousRepository rendezVousRepository;
//    private final UserRepository userRepository;
//
//    public DashboardStats getDashboardStats() {
//        long totalPatients = userRepository.countByRole(User.Role.PATIENT);
//        long totalMedecins = userRepository.countByRole(User.Role.MEDECIN);
//        long totalRdv      = rendezVousRepository.count();
//        long rdvEnAttente  = rendezVousRepository.countByStatut(RendezVous.Statut.EN_ATTENTE);
//        long rdvTermines   = rendezVousRepository.countByStatut(RendezVous.Statut.TERMINE);
//        long revenuTotal   = rendezVousRepository.revenuTotal();
//        long revenu30j     = rendezVousRepository.revenuPeriode(LocalDateTime.now().minusDays(30));
//
//        // Compte RDV aujourd'hui
//        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
//        LocalDateTime finJour   = LocalDate.now().atTime(23, 59, 59);
//        long rdvAujourdhui = rendezVousRepository
//                .findByMedecinIdAndDateHeureBetweenOrderByDateHeureAsc(
//                        null, debutJour, finJour) // simplifié
//                .size();
//
//        return DashboardStats.builder()
//                .totalPatients(totalPatients).totalMedecins(totalMedecins)
//                .totalRdv(totalRdv).rdvAujourdhui(rdvAujourdhui)
//                .rdvEnAttente(rdvEnAttente).rdvTermines(rdvTermines)
//                .revenuTotal(revenuTotal).revenu30Jours(revenu30j)
//                .rdvParJour(buildRdvParJour())
//                .rdvParSpecialite(buildRdvParSpecialite())
//                .rdvParStatut(buildRdvParStatut())
//                .topMedecins(buildTopMedecins())
//                .build();
//    }
//
//    private List<Map<String, Object>> buildRdvParJour() {
//        LocalDateTime depuis = LocalDateTime.now().minusDays(7);
//        List<Object[]> raw = rendezVousRepository.countRdvParJour(depuis);
//        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);
//
//        Map<String, Long> countParDate = new LinkedHashMap<>();
//        for (int i = 6; i >= 0; i--) {
//            countParDate.put(LocalDate.now().minusDays(i).format(fmt), 0L);
//        }
//        for (Object[] row : raw) {
//            if (row[0] != null) {
//                try {
//                    String label = LocalDate.parse(row[0].toString()).format(fmt);
//                    countParDate.put(label, (Long) row[1]);
//                } catch (Exception ignored) {}
//            }
//        }
//        return countParDate.entrySet().stream().map(e -> {
//            Map<String, Object> p = new LinkedHashMap<>();
//            p.put("date", e.getKey()); p.put("rdv", e.getValue()); return p;
//        }).collect(Collectors.toList());
//    }
//
//    private List<Map<String, Object>> buildRdvParSpecialite() {
//        return rendezVousRepository.countRdvParSpecialite().stream().map(row -> {
//            Map<String, Object> e = new LinkedHashMap<>();
//            e.put("name", row[0] != null ? row[0] : "Non défini");
//            e.put("value", row[1]); return e;
//        }).collect(Collectors.toList());
//    }
//
//    private List<Map<String, Object>> buildRdvParStatut() {
//        return rendezVousRepository.countByStatut().stream().map(row -> {
//            Map<String, Object> e = new LinkedHashMap<>();
//            e.put("statut", row[0].toString()); e.put("count", row[1]); return e;
//        }).collect(Collectors.toList());
//    }
//
//    private List<TopMedecinDto> buildTopMedecins() {
//        return rendezVousRepository.topMedecins(PageRequest.of(0, 5)).stream()
//                .map(row -> TopMedecinDto.builder()
//                        .id((Long) row[0]).nom(row[1] + " " + row[2])
//                        .specialite(row[3] != null ? row[3].toString() : "")
//                        .consultations((Long) row[4]).build())
//                .collect(Collectors.toList());
//    }
//}
