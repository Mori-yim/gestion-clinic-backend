package com.cliniccam.config;

import com.cliniccam.entity.RendezVous;
import com.cliniccam.entity.User;
import com.cliniccam.repository.RendezVousRepository;
import com.cliniccam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * DATA LOADER — Données de démonstration ClinicCam
 * Crée des données réalistes pour impressionner lors d'une démo :
 *   - 1 admin, 6 médecins (spécialités variées), 5 patients
 *   - ~20 rendez-vous avec statuts variés (pour les graphiques)
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(
            UserRepository userRepository,
            RendezVousRepository rendezVousRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Données déjà présentes — skip DataLoader");
                return;
            }

            log.info(" Chargement des données de démonstration ClinicCam...");

            // ── ADMIN 
            User admin = userRepository.save(User.builder()
                    .firstName("Directeur").lastName("Clinique")
                    .email("admin@cliniccam.cm")
                    .password(passwordEncoder.encode("Admin123!"))
                    .phone("+237690000001").role(User.Role.ADMIN)
                    .build());

            // ── MÉDECINS 
            User cardio = userRepository.save(User.builder()
                    .firstName("Pierre").lastName("Mbarga")
                    .email("dr.mbarga@cliniccam.cm")
                    .password(passwordEncoder.encode("Medecin123!"))
                    .phone("+237677111222").role(User.Role.MEDECIN)
                    .specialite("Cardiologie").numeroOrdre("CMR-CARD-0012")
                    .dureeconsultationMinutes(45).tarifConsultation(25000)
                    .biographie("Cardiologue avec 15 ans d'expérience. Formé à Paris et Yaoundé. Spécialisé dans les maladies coronariennes.")
                    .photoUrl("https://i.pravatar.cc/150?img=11").disponible(true)
                    .genre("M").dateNaissance(LocalDate.of(1975, 3, 14))
                    .build());

            User pediatre = userRepository.save(User.builder()
                    .firstName("Marie").lastName("Fono")
                    .email("dr.fono@cliniccam.cm")
                    .password(passwordEncoder.encode("Medecin123!"))
                    .phone("+237677222333").role(User.Role.MEDECIN)
                    .specialite("Pédiatrie").numeroOrdre("CMR-PED-0034")
                    .dureeconsultationMinutes(30).tarifConsultation(15000)
                    .biographie("Pédiatre passionnée par la santé de l'enfant. Consultations en français et en anglais.")
                    .photoUrl("https://i.pravatar.cc/150?img=5").disponible(true)
                    .genre("F").dateNaissance(LocalDate.of(1982, 7, 22))
                    .build());

            User generaliste = userRepository.save(User.builder()
                    .firstName("Jean").lastName("Kouam")
                    .email("dr.kouam@cliniccam.cm")
                    .password(passwordEncoder.encode("Medecin123!"))
                    .phone("+237677333444").role(User.Role.MEDECIN)
                    .specialite("Médecine Générale").numeroOrdre("CMR-GEN-0056")
                    .dureeconsultationMinutes(20).tarifConsultation(10000)
                    .biographie("Médecin généraliste de proximité. Consultations rapides et efficaces.")
                    .photoUrl("https://i.pravatar.cc/150?img=12").disponible(true)
                    .genre("M").dateNaissance(LocalDate.of(1978, 11, 5))
                    .build());

            User dermato = userRepository.save(User.builder()
                    .firstName("Sophie").lastName("Njike")
                    .email("dr.njike@cliniccam.cm")
                    .password(passwordEncoder.encode("Medecin123!"))
                    .phone("+237677444555").role(User.Role.MEDECIN)
                    .specialite("Dermatologie").numeroOrdre("CMR-DERM-0078")
                    .dureeconsultationMinutes(30).tarifConsultation(18000)
                    .biographie("Dermatologue spécialisée dans les affections cutanées tropicales.")
                    .photoUrl("https://i.pravatar.cc/150?img=9").disponible(true)
                    .genre("F").dateNaissance(LocalDate.of(1985, 4, 18))
                    .build());

            User gyneco = userRepository.save(User.builder()
                    .firstName("Paul").lastName("Tchinda")
                    .email("dr.tchinda@cliniccam.cm")
                    .password(passwordEncoder.encode("Medecin123!"))
                    .phone("+237677555666").role(User.Role.MEDECIN)
                    .specialite("Gynécologie").numeroOrdre("CMR-GYN-0090")
                    .dureeconsultationMinutes(40).tarifConsultation(20000)
                    .biographie("Gynécologue-obstétricien avec expertise en grossesses à risque.")
                    .photoUrl("https://i.pravatar.cc/150?img=15").disponible(true)
                    .genre("M").dateNaissance(LocalDate.of(1970, 9, 3))
                    .build());

            User ophtalmo = userRepository.save(User.builder()
                    .firstName("Cécile").lastName("Ateba")
                    .email("dr.ateba@cliniccam.cm")
                    .password(passwordEncoder.encode("Medecin123!"))
                    .phone("+237677666777").role(User.Role.MEDECIN)
                    .specialite("Ophtalmologie").numeroOrdre("CMR-OPH-0102")
                    .dureeconsultationMinutes(25).tarifConsultation(16000)
                    .biographie("Ophtalmologue. Chirurgie de la cataracte et traitement du glaucome.")
                    .photoUrl("https://i.pravatar.cc/150?img=20").disponible(true)
                    .genre("F").dateNaissance(LocalDate.of(1980, 12, 30))
                    .build());

            // ── PATIENTS 
            User patient1 = userRepository.save(User.builder()
                    .firstName("Alain").lastName("Talla")
                    .email("alain.talla@gmail.com")
                    .password(passwordEncoder.encode("Patient123!"))
                    .phone("+237677100200").role(User.Role.PATIENT)
                    .genre("M").dateNaissance(LocalDate.of(1990, 6, 12))
                    .build());

            User patient2 = userRepository.save(User.builder()
                    .firstName("Fatima").lastName("Bello")
                    .email("fatima.bello@gmail.com")
                    .password(passwordEncoder.encode("Patient123!"))
                    .phone("+237677200300").role(User.Role.PATIENT)
                    .genre("F").dateNaissance(LocalDate.of(1988, 2, 28))
                    .build());

            User patient3 = userRepository.save(User.builder()
                    .firstName("Eric").lastName("Nkeng")
                    .email("eric.nkeng@gmail.com")
                    .password(passwordEncoder.encode("Patient123!"))
                    .phone("+237677300400").role(User.Role.PATIENT)
                    .genre("M").dateNaissance(LocalDate.of(1995, 8, 15))
                    .build());

            User patient4 = userRepository.save(User.builder()
                    .firstName("Carine").lastName("Essomba")
                    .email("carine.essomba@gmail.com")
                    .password(passwordEncoder.encode("Patient123!"))
                    .phone("+237677400500").role(User.Role.PATIENT)
                    .genre("F").dateNaissance(LocalDate.of(1992, 4, 7))
                    .build());

            User patient5 = userRepository.save(User.builder()
                    .firstName("Boris").lastName("Wamba")
                    .email("boris.wamba@gmail.com")
                    .password(passwordEncoder.encode("Patient123!"))
                    .phone("+237677500600").role(User.Role.PATIENT)
                    .genre("M").dateNaissance(LocalDate.of(1985, 11, 20))
                    .build());

            //  RENDEZ-VOUS (données pour les graphiques) 
            LocalDateTime base = LocalDateTime.now();

            // RDV passés TERMINÉS (pour les stats de revenu)
            sauvegarderRdv(rendezVousRepository, patient1, cardio,
                    base.minusDays(5).withHour(9).withMinute(0),
                    "Douleurs thoraciques récurrentes", RendezVous.Statut.TERMINE,
                    "Bilan cardiaque normal. Stress professionnel.", "Magnésium 300mg x2/jour");

            sauvegarderRdv(rendezVousRepository, patient2, pediatre,
                    base.minusDays(4).withHour(10).withMinute(30),
                    "Fièvre persistante chez enfant 5 ans", RendezVous.Statut.TERMINE,
                    "Rhinopharyngite virale. Repos recommandé.", "Paracétamol 250mg, Vitamine C");

            sauvegarderRdv(rendezVousRepository, patient3, generaliste,
                    base.minusDays(4).withHour(14).withMinute(0),
                    "Consultation de routine", RendezVous.Statut.TERMINE,
                    "Patient en bonne santé générale.", null);

            sauvegarderRdv(rendezVousRepository, patient4, dermato,
                    base.minusDays(3).withHour(11).withMinute(0),
                    "Éruption cutanée inexpliquée", RendezVous.Statut.TERMINE,
                    "Dermatite atopique. Éviter les irritants.", "Hydrocortisone crème 1%");

            sauvegarderRdv(rendezVousRepository, patient5, gyneco,
                    base.minusDays(3).withHour(15).withMinute(0),
                    "Suivi grossesse 6 mois", RendezVous.Statut.TERMINE,
                    "Grossesse évolutive normale. Prochain RDV dans 4 semaines.", "Fer + Acide folique");

            sauvegarderRdv(rendezVousRepository, patient1, ophtalmo,
                    base.minusDays(2).withHour(9).withMinute(30),
                    "Baisse de vision depuis 3 mois", RendezVous.Statut.TERMINE,
                    "Myopie évolutive. Correction nécessaire.", "Prescription lunettes -2.5 OD");

            sauvegarderRdv(rendezVousRepository, patient2, generaliste,
                    base.minusDays(2).withHour(16).withMinute(0),
                    "Maux de tête fréquents", RendezVous.Statut.TERMINE,
                    "Céphalées de tension. Hydratation insuffisante.", null);

            sauvegarderRdv(rendezVousRepository, patient3, cardio,
                    base.minusDays(1).withHour(10).withMinute(0),
                    "Palpitations cardiaques", RendezVous.Statut.CONFIRME,
                    null, null);

            // RDV du JOUR (pour les stats "aujourd'hui")
            sauvegarderRdv(rendezVousRepository, patient4, pediatre,
                    base.withHour(9).withMinute(0),
                    "Vaccination rappel 18 mois", RendezVous.Statut.CONFIRME,
                    null, null);

            sauvegarderRdv(rendezVousRepository, patient5, dermato,
                    base.withHour(11).withMinute(0),
                    "Acné persistante", RendezVous.Statut.EN_ATTENTE,
                    null, null);

            sauvegarderRdv(rendezVousRepository, patient1, generaliste,
                    base.withHour(14).withMinute(30),
                    "Renouvellement ordonnance diabète", RendezVous.Statut.CONFIRME,
                    null, null);

            // RDV FUTURS
            sauvegarderRdv(rendezVousRepository, patient2, cardio,
                    base.plusDays(1).withHour(9).withMinute(0),
                    "Contrôle tension artérielle", RendezVous.Statut.EN_ATTENTE,
                    null, null);

            sauvegarderRdv(rendezVousRepository, patient3, gyneco,
                    base.plusDays(2).withHour(10).withMinute(0),
                    "Consultation prénatale", RendezVous.Statut.EN_ATTENTE,
                    null, null);

            sauvegarderRdv(rendezVousRepository, patient4, ophtalmo,
                    base.plusDays(3).withHour(14).withMinute(0),
                    "Examen de la vue", RendezVous.Statut.EN_ATTENTE,
                    null, null);

            // RDV ANNULÉ
            sauvegarderRdv(rendezVousRepository, patient5, pediatre,
                    base.minusDays(1).withHour(15).withMinute(0),
                    "Consultation urgente", RendezVous.Statut.ANNULE,
                    null, null);

            log.info("""
                     CLINICCAM — Données de démo chargées !
                    
                    👔 ADMIN   : admin@cliniccam.cm / Admin123!
                    🩺 MÉDECIN : dr.mbarga@cliniccam.cm / Medecin123!
                    👤 PATIENT : alain.talla@gmail.com / Patient123!
                    
                     6 médecins | 5 patients | 15 rendez-vous
                    """);
        };
    }

    /* Helper : crée et sauvegarde un RDV rapidement */
    private void sauvegarderRdv(
            RendezVousRepository repo,
            User patient, User medecin,
            LocalDateTime dateHeure, String motif,
            RendezVous.Statut statut,
            String notes, String ordonnance
    ) {
        RendezVous rdv = RendezVous.builder()
                .patient(patient).medecin(medecin)
                .dateHeure(dateHeure)
                .dureeMinutes(medecin.getDureeeconsultationMinutes() /*!= null
                        ? medecin.getDureeeconsultationMinutes() : 30*/)
                .motif(motif).statut(statut)
                .tarif(medecin.getTarifConsultation())
                .notesMedecin(notes).ordonnance(ordonnance)
                .build();
        repo.save(rdv);
    }
}
