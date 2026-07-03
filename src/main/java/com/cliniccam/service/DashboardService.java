package com.cliniccam.service;

import com.cliniccam.dto.Dto;
import com.cliniccam.entity.RendezVous;
import com.cliniccam.entity.User;
import com.cliniccam.repository.RendezVousRepository;
import com.cliniccam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors; /**
 * ================================================================
 * SERVICE DASHBOARD — Statistiques pour les graphiques Recharts
 * ================================================================
 *
 * Ce service agrège les données de plusieurs tables
 * et les formate exactement comme Recharts les attend.
 *
 * Chaque méthode retourne des List<Map<String, Object>> car
 * Recharts attend des tableaux d'objets JSON simples.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final RendezVousRepository rendezVousRepository;
    private final UserRepository userRepository;

    /**
     * Construit l'objet DashboardStats complet.
     * Appelé au chargement du dashboard admin.
     */
    public Dto.DashboardStats getDashboardStats() {

        // ── KPIs globaux ──────────────────────────────────────
        long totalPatients = userRepository.countByRole(User.Role.PATIENT);
        long totalMedecins = userRepository.countByRole(User.Role.MEDECIN);
        long totalRdv      = rendezVousRepository.count();
        long rdvEnAttente  = rendezVousRepository.countByStatut(RendezVous.Statut.EN_ATTENTE);
        long rdvTermines   = rendezVousRepository.countByStatut(RendezVous.Statut.TERMINE);
        long revenuTotal   = rendezVousRepository.revenuTotal();
        long revenu30j     = rendezVousRepository.revenuPeriode(LocalDateTime.now().minusDays(30));

        // RDV aujourd'hui
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime finJour   = LocalDate.now().atTime(23, 59, 59);
        long rdvAujourdhui = rendezVousRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, Integer.MAX_VALUE))
                .getContent()
                .stream()
                .filter(rv -> !rv.getDateHeure().isBefore(debutJour)
                        && !rv.getDateHeure().isAfter(finJour))
                .count();

        // ── Graphique 1 : RDV des 7 derniers jours (LineChart) ─
        List<Map<String, Object>> rdvParJour = buildRdvParJour();

        // ── Graphique 2 : Par spécialité (PieChart) ────────────
        List<Map<String, Object>> rdvParSpecialite = buildRdvParSpecialite();

        // ── Graphique 3 : Par statut (BarChart) ────────────────
        List<Map<String, Object>> rdvParStatut = buildRdvParStatut();

        // ── Top médecins ───────────────────────────────────────
        List<Dto.TopMedecinDto> topMedecins = buildTopMedecins();

        return Dto.DashboardStats.builder()
                .totalPatients(totalPatients)
                .totalMedecins(totalMedecins)
                .totalRdv(totalRdv)
                .rdvAujourdhui(rdvAujourdhui)
                .rdvEnAttente(rdvEnAttente)
                .rdvTermines(rdvTermines)
                .revenuTotal(revenuTotal)
                .revenu30Jours(revenu30j)
                .rdvParJour(rdvParJour)
                .rdvParSpecialite(rdvParSpecialite)
                .rdvParStatut(rdvParStatut)
                .topMedecins(topMedecins)
                .build();
    }

    /**
     * RDV par jour sur les 7 derniers jours.
     * Retourne : [{ "date": "20 déc", "rdv": 5 }, ...]
     *
     * Format attendu par Recharts <LineChart dataKey="rdv">
     */
    private List<Map<String, Object>> buildRdvParJour() {
        LocalDateTime depuis = LocalDateTime.now().minusDays(7);
        List<Object[]> raw = rendezVousRepository.countRdvParJour(depuis);

        // Créer une map date → count depuis les résultats BDD
        Map<String, Long> countParDate = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);

        // Initialiser les 7 derniers jours à 0 (pour afficher même les jours sans RDV)
        for (int i = 6; i >= 0; i--) {
            String label = LocalDate.now().minusDays(i).format(fmt);
            countParDate.put(label, 0L);
        }

        // Remplir avec les données réelles
        for (Object[] row : raw) {
            if (row[0] != null) {
                String dateLabel = LocalDate.parse(row[0].toString()).format(fmt);
                countParDate.put(dateLabel, (Long) row[1]);
            }
        }

        return countParDate.entrySet().stream()
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", e.getKey());
                    point.put("rdv", e.getValue());
                    return point;
                })
                .collect(Collectors.toList());
    }

    /**
     * RDV par spécialité médicale.
     * Retourne : [{ "name": "Cardiologie", "value": 15 }, ...]
     *
     * Format attendu par Recharts <PieChart>
     */
    private List<Map<String, Object>> buildRdvParSpecialite() {
        return rendezVousRepository.countRdvParSpecialite()
                .stream()
                .map(row -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("name", row[0] != null ? row[0] : "Non défini");
                    entry.put("value", row[1]);
                    return entry;
                })
                .collect(Collectors.toList());
    }

    /**
     * RDV par statut.
     * Retourne : [{ "statut": "CONFIRME", "count": 12 }, ...]
     *
     * Format attendu par Recharts <BarChart>
     */
    private List<Map<String, Object>> buildRdvParStatut() {
        return rendezVousRepository.countByStatut()
                .stream()
                .map(row -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("statut", row[0].toString());
                    entry.put("count", row[1]);
                    return entry;
                })
                .collect(Collectors.toList());
    }

    /**
     * Top 5 médecins par consultations terminées
     */
    private List<Dto.TopMedecinDto> buildTopMedecins() {
        Pageable top5 = PageRequest.of(0, 5);
        return rendezVousRepository.topMedecins(top5)
                .stream()
                .map(row -> Dto.TopMedecinDto.builder()
                        .id((Long) row[0])
                        .nom(row[1] + " " + row[2])
                        .specialite(row[3] != null ? row[3].toString() : "")
                        .consultations((Long) row[4])
                        .build())
                .collect(Collectors.toList());
    }
}
