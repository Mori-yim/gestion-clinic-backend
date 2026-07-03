package com.cliniccam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 * POINT D'ENTRÉE - CLINICCAM
 * ================================================================
 * Plateforme de gestion de clinique médicale :
 *   - Gestion des patients, médecins, spécialités
 *   - Prise et suivi des rendez-vous
 *   - Dashboard avec statistiques en temps réel
 *   - 3 rôles : PATIENT, MEDECIN, ADMIN
 * ================================================================
 */
@SpringBootApplication
public class ClinicCamApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicCamApplication.class, args);
        System.out.println("""
                ================================================
                🏥  ClinicCam API démarrée !
                📍  http://localhost:8081/api/v1
                ================================================
                """);
    }
}
