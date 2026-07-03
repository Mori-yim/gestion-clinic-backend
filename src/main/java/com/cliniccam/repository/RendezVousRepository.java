package com.cliniccam.repository;

import com.cliniccam.entity.RendezVous;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ================================================================
 * REPOSITORY RENDEZ-VOUS
 * ================================================================
 *
 * Contient des requêtes JPQL avancées pour :
 *   - Les statistiques du dashboard (groupBy, count, sum)
 *   - La vérification des conflits de créneaux
 *   - La pagination des RDV par médecin/patient
 * ================================================================
 */
@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    // ============================================================
    // REQUÊTES DE BASE
    // ============================================================

    /**
     * RDV d'un patient (paginé + trié par date décroissante)
     */
    Page<RendezVous> findByPatientIdOrderByDateHeureDesc(Long patientId, Pageable pageable);

    /**
     * RDV d'un médecin pour une période donnée (son agenda)
     */
    List<RendezVous> findByMedecinIdAndDateHeureBetweenOrderByDateHeureAsc(
            Long medecinId,
            LocalDateTime debut,
            LocalDateTime fin
    );

    /**
     * RDV d'un médecin aujourd'hui (tri par heure)
     */
    @Query("""
            SELECT rv FROM RendezVous rv
            WHERE rv.medecin.id = :medecinId
            AND rv.dateHeure BETWEEN :debutJournee AND :finJournee
            AND rv.statut NOT IN ('ANNULE')
            ORDER BY rv.dateHeure ASC
            """)
    List<RendezVous> findRdvDuJour(
            @Param("medecinId") Long medecinId,
            @Param("debutJournee") LocalDateTime debutJournee,
            @Param("finJournee") LocalDateTime finJournee
    );

    // ============================================================
    // VÉRIFICATION DES CONFLITS DE CRÉNEAUX
    // ============================================================

    /**
     * Vérifie si un médecin a déjà un RDV qui chevauche le créneau demandé.
     *
     * Deux RDV se chevauchent si :
     *   existant.debut < nouveau.fin ET existant.fin > nouveau.debut
     *
     * On exclut les RDV annulés et terminés.
     */
    @Query("""
            SELECT COUNT(rv) > 0 FROM RendezVous rv
            WHERE rv.medecin.id = :medecinId
            AND rv.statut NOT IN ('ANNULE', 'TERMINE')
            AND rv.dateHeure < :heureFin
            AND (rv.dateHeure + rv.dureeMinutes * 1 MINUTE) > :heureDebut
            """)
    boolean existsConflitCreneaux(
            @Param("medecinId") Long medecinId,
            @Param("heureDebut") LocalDateTime heureDebut,
            @Param("heureFin") LocalDateTime heureFin
    );

    // ============================================================
    // STATISTIQUES POUR LE DASHBOARD
    // ============================================================

    /**
     * Compte les RDV par statut (pour la vue globale)
     * Retourne : [["EN_ATTENTE", 12], ["CONFIRME", 8], ...]
     */
    @Query("SELECT rv.statut, COUNT(rv) FROM RendezVous rv GROUP BY rv.statut")
    List<Object[]> countByStatut();

    /**
     * Nombre de RDV par jour sur les 7 derniers jours
     * Utilisé pour le graphique linéaire du dashboard
     *
     * FUNCTION('DATE', ...) extrait la date sans l'heure en PostgreSQL
     */
    @Query("""
            SELECT CAST(rv.dateHeure AS DATE), COUNT(rv)
            FROM RendezVous rv
            WHERE rv.dateHeure >= :depuis
            GROUP BY CAST(rv.dateHeure AS DATE)
            ORDER BY CAST(rv.dateHeure AS DATE) ASC
            """)
    List<Object[]> countRdvParJour(@Param("depuis") LocalDateTime depuis);

    /**
     * RDV par spécialité médicale (pour le graphique camembert)
     * Retourne : [["Cardiologie", 15], ["Pédiatrie", 23], ...]
     */
    @Query("""
            SELECT rv.medecin.specialite, COUNT(rv)
            FROM RendezVous rv
            WHERE rv.statut != 'ANNULE'
            GROUP BY rv.medecin.specialite
            ORDER BY COUNT(rv) DESC
            """)
    List<Object[]> countRdvParSpecialite();

    /**
     * Top 5 des médecins les plus consultés
     * Retourne : [[medecinId, nom, specialite, count], ...]
     */
    @Query("""
            SELECT rv.medecin.id, rv.medecin.firstName, rv.medecin.lastName,
                   rv.medecin.specialite, COUNT(rv)
            FROM RendezVous rv
            WHERE rv.statut = 'TERMINE'
            GROUP BY rv.medecin.id, rv.medecin.firstName, rv.medecin.lastName, rv.medecin.specialite
            ORDER BY COUNT(rv) DESC
            """)
    List<Object[]> topMedecins(Pageable pageable);

    /**
     * Revenu total des consultations terminées
     */
    @Query("SELECT COALESCE(SUM(rv.tarif), 0) FROM RendezVous rv WHERE rv.statut = 'TERMINE'")
    Long revenuTotal();

    /**
     * Revenu des 30 derniers jours
     */
    @Query("""
            SELECT COALESCE(SUM(rv.tarif), 0) FROM RendezVous rv
            WHERE rv.statut = 'TERMINE' AND rv.dateHeure >= :depuis
            """)
    Long revenuPeriode(@Param("depuis") LocalDateTime depuis);

    /**
     * Tous les RDV récents (admin) avec pagination
     */
    Page<RendezVous> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Compte total des RDV selon leur statut
     */
    long countByStatut(RendezVous.Statut statut);
}
