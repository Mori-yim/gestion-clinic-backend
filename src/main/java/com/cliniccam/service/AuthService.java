package com.cliniccam.service;

import com.cliniccam.dto.Dto.*;
import com.cliniccam.entity.User;
import com.cliniccam.repository.UserRepository;
import com.cliniccam.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ================================================================
 * SERVICE AUTHENTIFICATION
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Un compte existe déjà avec l'email : " + request.getEmail()
            );
        }

        // Rôle par défaut : PATIENT (sécurité : on ne peut pas s'auto-promouvoir ADMIN)
        User.Role role = (request.getRole() == User.Role.MEDECIN)
                ? User.Role.MEDECIN
                : User.Role.PATIENT;

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .dateNaissance(request.getDateNaissance())
                .genre(request.getGenre())
                .role(role)
                .build();

        User saved = userRepository.save(user);
        log.info("Nouveau {} inscrit : {}", role, saved.getEmail());

        return AuthResponse.of(jwtService.generateToken(saved), saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        log.info("Connexion : {} ({})", user.getEmail(), user.getRole());
        return AuthResponse.of(jwtService.generateToken(user), user);
    }
}


/**
 * ================================================================
 * SERVICE MÉDECIN
 * ================================================================
 *
 * NOUVEAUTÉ : pagination avec Spring Data + Pageable
 *
 * Pageable permet de :
 *   - Limiter le nombre de résultats par page (ex: 10 par page)
 *   - Naviguer entre les pages (page 0, 1, 2...)
 *   - Trier les résultats (orderBy)
 *
 * Le frontend envoie : GET /api/v1/medecins?page=0&size=10
 * Le backend retourne : { content: [...], totalPages: 5, ... }
 * ================================================================
 */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//class MedecinService {
//
//    private final UserRepository userRepository;
//
//    /**
//     * Liste tous les médecins (page publique)
//     */
//    public List<UserResponse> getAllMedecins() {
//        return userRepository.findByRoleOrderByLastNameAsc(User.Role.MEDECIN)
//                .stream()
//                .map(UserResponse::fromUser)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Médecins par spécialité (filtre sur la page publique)
//     */
//    public List<UserResponse> getMedecinsBySpecialite(String specialite) {
//        return userRepository
//                .findByRoleAndSpecialiteIgnoreCaseAndDisponibleTrue(User.Role.MEDECIN, specialite)
//                .stream()
//                .map(UserResponse::fromUser)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Détail d'un médecin
//     */
//    public UserResponse getMedecinById(Long id) {
//        User medecin = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Médecin non trouvé : " + id));
//        if (medecin.getRole() != User.Role.MEDECIN) {
//            throw new RuntimeException("Cet utilisateur n'est pas un médecin");
//        }
//        return UserResponse.fromUser(medecin);
//    }
//
//    /**
//     * Liste paginée des patients (admin)
//     *
//     * PageRequest.of(page, size) crée l'objet Pageable.
//     * Spring Data JPA gère le LIMIT/OFFSET automatiquement.
//     */
//    public PageResponse<UserResponse> getPatientsPagines(int page, int size, String search) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<User> pageResult = userRepository.findByRoleAndSearch(
//                User.Role.PATIENT,
//                search != null ? search : "",
//                pageable
//        );
//
//        // Convertir Page<User> → PageResponse<UserResponse>
//        List<UserResponse> content = pageResult.getContent()
//                .stream()
//                .map(UserResponse::fromUser)
//                .collect(Collectors.toList());
//
//        return PageResponse.<UserResponse>builder()
//                .content(content)
//                .page(pageResult.getNumber())
//                .size(pageResult.getSize())
//                .totalElements(pageResult.getTotalElements())
//                .totalPages(pageResult.getTotalPages())
//                .first(pageResult.isFirst())
//                .last(pageResult.isLast())
//                .build();
//    }
//
//    /**
//     * Toutes les spécialités disponibles (pour le filtre)
//     */
//    public List<String> getSpecialites() {
//        return userRepository.findAllSpecialites();
//    }
//
//    /**
//     * Mise à jour du profil médecin
//     */
//    @Transactional
//    public UserResponse updateMedecin(Long id, UpdateMedecinRequest request, User currentUser) {
//        User medecin = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Médecin non trouvé : " + id));
//
//        // Un médecin ne peut modifier que son propre profil
//        boolean isSelf = medecin.getId().equals(currentUser.getId());
//        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;
//
//        if (!isSelf && !isAdmin) {
//            throw new SecurityException("Vous ne pouvez pas modifier ce profil");
//        }
//
//        if (request.getPhone() != null) medecin.setPhone(request.getPhone());
//        if (request.getBiographie() != null) medecin.setBiographie(request.getBiographie());
//        if (request.getTarifConsultation() != null) medecin.setTarifConsultation(request.getTarifConsultation());
//        if (request.getDureeConsultationMinutes() != null) medecin.setDureeeconsultationMinutes(request.getDureeConsultationMinutes());
//        if (request.getDisponible() != null) medecin.setDisponible(request.getDisponible());
//        if (request.getPhotoUrl() != null) medecin.setPhotoUrl(request.getPhotoUrl());
//
//        return UserResponse.fromUser(userRepository.save(medecin));
//    }
//}
