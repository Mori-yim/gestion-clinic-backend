# gestion-clinic-backend
# 🏥 ClinicCam Backend — API REST Spring Boot

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-007396?style=flat&logo=java)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=flat&logo=jsonwebtokens)](https://jwt.io/)
[![Deploy](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=flat&logo=railway)](https://railway.app/)

API REST complète pour la plateforme **ClinicCam** — gestion de clinique médicale et prise de rendez-vous en ligne au Cameroun.

---

## 📋 Table des Matières

- [À propos](#-à-propos)
- [Technologies](#-technologies)
- [Architecture](#-architecture)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)
- [Modèle de données](#-modèle-de-données)
- [Déploiement Railway](#-déploiement-railway)
- [Auteur](#-auteur)

---

## 📖 À propos

ClinicCam Backend est une API REST médicale qui gère :
- **3 rôles distincts** : `PATIENT`, `MEDECIN`, `ADMIN` avec redirections différentes
- **Annuaire médecins** public avec filtres par spécialité
- **Prise de rendez-vous** avec vérification des conflits de créneaux
- **Agenda médecin** semaine par semaine
- **Statuts RDV** : `EN_ATTENTE → CONFIRME → EN_COURS → TERMINE / ANNULE`
- **Notes et ordonnances** par le médecin après consultation
- **Dashboard admin** avec statistiques agrégées pour les graphiques Recharts
- **Pagination Spring Data** avec `Pageable` sur toutes les listes

---

## 🛠️ Technologies

| Technologie | Version | Usage |
|------------|---------|-------|
| Spring Boot | 3.2.5 | Framework principal |
| Spring Security | 6.x | Auth JWT + RBAC 3 rôles |
| Spring Data JPA | 3.x | ORM + Pageable pagination |
| JJWT | 0.11.5 | JWT HS256 |
| PostgreSQL | 16 | Base de données (Supabase) |
| Lombok | Latest | Réduction boilerplate |
| Maven | 3.6+ | Gestion des dépendances |

---

## 🏗️ Architecture

```
src/main/java/com/cliniccam/
├── ClinicCamApplication.java
├── config/
│   ├── SecurityConfig.java     ← RBAC 3 rôles, CORS, BCrypt
│   └── DataLoader.java         ← 6 médecins + 5 patients + 15 RDV de démo
├── controller/
│   └── AuthController.java     ← Auth, Médecins, RendezVous, Admin/Dashboard
├── dto/
│   └── Dto.java                ← DTOs + PageResponse<T> + DashboardStats
├── entity/
│   ├── User.java               ← PATIENT/MEDECIN/ADMIN (UserDetails)
│   └── RendezVous.java         ← RDV avec cycle de vie complet
├── exception/
│   └── GlobalExceptionHandler.java
├── repository/
│   ├── UserRepository.java     ← Pagination, recherche, spécialités
│   └── RendezVousRepository.java ← @Query JPQL stats dashboard
├── security/
│   ├── JwtService.java
│   └── JwtAuthFilter.java
└── service/
    ├── AuthService.java
    ├── MedecinService.java     ← Pagination + agenda
    ├── RendezVousService.java  ← Vérif conflits + transitions statut
    └── DashboardService.java   ← Agrégation stats Recharts
```

---

## ✅ Prérequis

- Java 17+, Maven 3.6+
- PostgreSQL local OU compte [Supabase](https://supabase.com)

---

## 🚀 Installation

```bash
git clone https://github.com/Mori-yim/cliniccam-backend.git
cd cliniccam-backend

# Créer la BDD
psql -U postgres -c "CREATE DATABASE cliniccam_db;"

# Lancer (port 8081)
mvn spring-boot:run

# 6 médecins + 5 patients + 15 RDV créés automatiquement
```

---

## ⚙️ Configuration

```properties
server.port=8081

spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/cliniccam_db}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:password}

jwt.secret=${JWT_SECRET:cliniccam-secret-key-256-bits}
jwt.expiration=${JWT_EXPIRATION:86400000}

cors.allowed-origins=${CORS_ORIGINS:http://localhost:5174}
```

---

## 📡 API Endpoints

### Base URL : `http://localhost:8081/api/v1`

#### Authentification

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/auth/register` | Public | Inscription PATIENT ou MEDECIN |
| `POST` | `/auth/login` | Public | Connexion → JWT |
| `GET` | `/auth/me` | Connecté | Profil courant |

#### Médecins

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/medecins` | Public | Liste + filtre `?specialite=Cardiologie` |
| `GET` | `/medecins/{id}` | Public | Fiche médecin |
| `PUT` | `/medecins/{id}` | MEDECIN/ADMIN | Mettre à jour profil |
| `GET` | `/medecins/{id}/rdv-du-jour` | MEDECIN/ADMIN | RDV du jour |
| `GET` | `/medecins/{id}/agenda` | MEDECIN/ADMIN | Agenda `?debut=&fin=` |

#### Rendez-Vous

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `POST` | `/rendez-vous` | PATIENT | Prendre RDV (vérif conflits) |
| `GET` | `/rendez-vous/mes-rdv` | PATIENT | Mes RDV paginés `?page=0&size=10` |
| `PUT` | `/rendez-vous/{id}` | MEDECIN/ADMIN | Confirmer, terminer, noter |
| `PUT` | `/rendez-vous/{id}/annuler` | PATIENT | Annuler son RDV |

#### Admin & Dashboard

| Méthode | Endpoint | Accès | Description |
|---------|----------|-------|-------------|
| `GET` | `/dashboard/stats` | ADMIN | Stats pour graphiques Recharts |
| `GET` | `/specialites` | Public | Liste des spécialités |
| `GET` | `/admin/patients` | ADMIN | Patients paginés `?page=&size=&search=` |
| `GET` | `/admin/rendez-vous` | ADMIN | Tous les RDV paginés |

### Pagination

```bash
# Exemple : patients page 2, 10 par page, recherche "kamga"
GET /api/v1/admin/patients?page=1&size=10&search=kamga

# Réponse :
{
  "content": [...],
  "page": 1,
  "size": 10,
  "totalElements": 47,
  "totalPages": 5,
  "first": false,
  "last": false
}
```

---

## 🗄️ Modèle de données

### Entités

| Entité | Table | Relations |
|--------|-------|-----------|
| `User` | `users` | Rôle : PATIENT / MEDECIN / ADMIN |
| `RendezVous` | `rendez_vous` | `@ManyToOne` patient + médecin |

### Cycle de vie d'un RDV

```
EN_ATTENTE → CONFIRME → EN_COURS → TERMINE
           ↘ ANNULE (avant EN_COURS seulement)
```

### Spécialités médicales disponibles
Cardiologie, Pédiatrie, Médecine Générale, Dermatologie, Gynécologie, Ophtalmologie

---

## 👥 Comptes de démonstration

| Rôle | Email | Mot de passe |
|------|-------|-------------|
| 👔 ADMIN | admin@cliniccam.cm | Admin123! |
| 🩺 MEDECIN | dr.mbarga@cliniccam.cm | Medecin123! |
| 👤 PATIENT | alain.talla@gmail.com | Patient123! |

---

## ☁️ Déploiement Railway

```bash
# Variables d'environnement Railway :
DATABASE_URL=jdbc:postgresql://db.<ref>.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<password>
JWT_SECRET=<openssl rand -base64 64>
CORS_ORIGINS=https://cliniccam.vercel.app
```

---

## 👨‍💻 Auteur

**Mori (YIMFACK MORINO)**
- 🎓 Licence DAP — Université de Douala
- 🐙 GitHub : [@Mori-yim](https://github.com/Mori-yim)
