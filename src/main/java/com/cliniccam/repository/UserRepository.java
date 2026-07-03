package com.cliniccam.repository;

import com.cliniccam.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ================================================================
 * REPOSITORY UTILISATEUR
 * ================================================================
 *
 * Nouveau par rapport à BusCam : utilisation de Pageable
 * pour la PAGINATION côté backend.
 *
 * Page<User> = objet qui contient :
 *   - La liste des éléments (content)
 *   - Le numéro de page, la taille, le total
 *   - Si c'est la première/dernière page
 * ================================================================
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Liste tous les médecins (pour la page publique)
     */
    List<User> findByRoleOrderByLastNameAsc(User.Role role);

    /**
     * Médecins disponibles par spécialité
     */
    List<User> findByRoleAndSpecialiteIgnoreCaseAndDisponibleTrue(
            User.Role role, String specialite
    );

    /**
     * PAGINATION : liste des patients avec recherche par nom
     * Très utile pour l'admin qui gère des centaines de patients
     *
     * Page<User> avec Pageable = résultats paginés automatiquement
     * @Query JPQL : recherche insensible à la casse dans prénom OU nom
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
            AND (
                LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            ORDER BY u.createdAt DESC
            """)
    Page<User> findByRoleAndSearch(
            @Param("role") User.Role role,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Compte le nombre de patients (pour le dashboard)
     */
    long countByRole(User.Role role);

    /**
     * Toutes les spécialités distinctes (pour le filtre de recherche)
     */
    @Query("SELECT DISTINCT u.specialite FROM User u WHERE u.role = 'MEDECIN' AND u.specialite IS NOT NULL ORDER BY u.specialite")
    List<String> findAllSpecialites();
}
