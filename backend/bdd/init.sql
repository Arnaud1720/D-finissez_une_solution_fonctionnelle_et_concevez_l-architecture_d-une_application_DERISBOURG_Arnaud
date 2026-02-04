-- ============================================================================
-- YOUR CAR YOUR WAY — Script d'initialisation de la base de données
-- ============================================================================
-- Base    : PostgreSQL 15
-- Schéma  : conforme au MPD de l'ADD V3.1 (10 tables)
-- Auteur  : Arnaud DERISBOURG
-- Date    : 04/02/2026
-- ============================================================================
-- USAGE :
--   Ce script crée la structure complète de la BDD.
--   Les tables du PoC (users, conversations, messages) sont alignées
--   avec les entités JPA/Hibernate existantes.
--   Les tables de la solution globale (agencies, vehicles, offers,
--   rentals, payments, invoices) préparent l'évolution post-PoC.
--
--   Pour l'utiliser :
--     1. Passer spring.jpa.hibernate.ddl-auto à "validate" ou "none"
--     2. Monter ce script via docker-compose (voir docker-compose.yml)
--        OU exécuter manuellement : psql -U ycyw_user -d ycyw -f 01_schema.sql
-- ============================================================================

-- Nettoyage (ordre inverse des dépendances FK)
DROP TABLE IF EXISTS invoices          CASCADE;
DROP TABLE IF EXISTS payments          CASCADE;
DROP TABLE IF EXISTS rentals           CASCADE;
DROP TABLE IF EXISTS offers            CASCADE;
DROP TABLE IF EXISTS vehicles          CASCADE;
DROP TABLE IF EXISTS agencies          CASCADE;
DROP TABLE IF EXISTS messages          CASCADE;
DROP TABLE IF EXISTS conversation_participants CASCADE;
DROP TABLE IF EXISTS conversations     CASCADE;
DROP TABLE IF EXISTS users             CASCADE;

-- ============================================================================
-- 1. USERS — Utilisateurs (PoC)
-- ============================================================================
-- Entité JPA : com.arn.ycyw.your_car_your_way.entity.Users
-- Rôles     : USER (client), EMPLOYEE (employé), ADMIN (administrateur)
-- Réf. BR   : BR-AUTH-01, BR-AUTH-02
-- ============================================================================
CREATE TABLE users (
                       id                  SERIAL          PRIMARY KEY,
                       first_name          VARCHAR(100),
                       last_name           VARCHAR(100),
                       username            VARCHAR(100),
                       email               VARCHAR(255)    NOT NULL UNIQUE,
                       password            VARCHAR(255)    NOT NULL,
                       date_of_birth       TIMESTAMP,
                       creation_date       TIMESTAMP       DEFAULT NOW(),
                       role                VARCHAR(20)     NOT NULL DEFAULT 'USER'
                           CHECK (role IN ('USER', 'EMPLOYEE', 'ADMIN')),
                       verification_status VARCHAR(20)     NOT NULL DEFAULT 'NONE'
                           CHECK (verification_status IN ('NONE', 'PENDING', 'VERIFIED', 'REJECTED')),
                       verification_token  VARCHAR(255)
);

COMMENT ON TABLE  users IS 'Utilisateurs de la plateforme (clients, employés, admin)';
COMMENT ON COLUMN users.role IS 'Enum : USER (client), EMPLOYEE (employé support), ADMIN';
COMMENT ON COLUMN users.verification_status IS 'Statut de vérification : NONE (user), PENDING/VERIFIED/REJECTED (employee)';
COMMENT ON COLUMN users.password IS 'Hash BCrypt — jamais en clair (RM-05)';

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role  ON users (role);

-- ============================================================================
-- 2. CONVERSATIONS — Conversations support client (PoC)
-- ============================================================================
-- Entité JPA : com.arn.ycyw.your_car_your_way.entity.Conversation
-- Réf. BR   : BR-SUP-01, BR-SUP-02
-- ============================================================================
CREATE TABLE conversations (
                               id                  SERIAL          PRIMARY KEY,
                               subject             VARCHAR(255),
                               customer_id         INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               employee_id         INTEGER         REFERENCES users(id) ON DELETE SET NULL,
                               status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                                   CHECK (status IN ('OPEN', 'CLOSED', 'PENDING')),
                               created_at          TIMESTAMP       DEFAULT NOW(),
                               updated_at          TIMESTAMP       DEFAULT NOW()
);

COMMENT ON TABLE  conversations IS 'Conversations de support client (PoC)';
COMMENT ON COLUMN conversations.customer_id IS 'FK → users.id — Client ayant initié la conversation';
COMMENT ON COLUMN conversations.employee_id IS 'FK → users.id — Employé assigné (nullable si pas encore assigné)';

CREATE INDEX idx_conversations_customer  ON conversations (customer_id);
CREATE INDEX idx_conversations_employee  ON conversations (employee_id);
CREATE INDEX idx_conversations_status    ON conversations (status);

-- ============================================================================
-- 3. CONVERSATION_PARTICIPANTS — Table d'association N:N (Solution globale)
-- ============================================================================
-- Permet plusieurs participants par conversation (évolution V2)
-- Réf. ADD  : §4.2 (diagramme de classes), §5.1 (MPD)
-- ============================================================================
CREATE TABLE conversation_participants (
                                           id                  SERIAL          PRIMARY KEY,
                                           user_id             INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           conversation_id     INTEGER         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                           joined_at           TIMESTAMP       DEFAULT NOW(),
                                           UNIQUE (user_id, conversation_id)
);

COMMENT ON TABLE conversation_participants IS 'Association N:N users ↔ conversations (évolution V2)';

CREATE INDEX idx_cp_user         ON conversation_participants (user_id);
CREATE INDEX idx_cp_conversation ON conversation_participants (conversation_id);

-- ============================================================================
-- 4. MESSAGES — Messages de conversation (PoC)
-- ============================================================================
-- Entité JPA : com.arn.ycyw.your_car_your_way.entity.Message
-- Réf. BR   : BR-SUP-01, BR-SUP-02
-- ============================================================================
CREATE TABLE messages (
                          id                  SERIAL          PRIMARY KEY,
                          conversation_id     INTEGER         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                          sender_id           INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          content             TEXT            NOT NULL,
                          sent_at             TIMESTAMP       DEFAULT NOW(),
                          is_read             BOOLEAN         NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE  messages IS 'Messages échangés dans les conversations (PoC)';
COMMENT ON COLUMN messages.is_read IS 'Indicateur de lecture par le destinataire';

CREATE INDEX idx_messages_conversation ON messages (conversation_id);
CREATE INDEX idx_messages_sender       ON messages (sender_id);
CREATE INDEX idx_messages_sent_at      ON messages (sent_at DESC);

-- ============================================================================
-- 5. AGENCIES — Agences de location (Solution globale)
-- ============================================================================
-- Réf. BR   : BR-LOC-01 (recherche par ville/pays)
-- Réf. ADD  : §5.4 (détail tables solution globale)
-- ============================================================================
CREATE TABLE agencies (
                          id                  SERIAL          PRIMARY KEY,
                          name                VARCHAR(255)    NOT NULL,
                          address             VARCHAR(500),
                          city                VARCHAR(100)    NOT NULL,
                          country             VARCHAR(100)    NOT NULL,
                          postal_code         VARCHAR(20),
                          phone               VARCHAR(20),
                          email               VARCHAR(255),
                          latitude            DECIMAL(9,6),
                          longitude           DECIMAL(9,6),
                          is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE  agencies IS 'Agences physiques de location de véhicules';
COMMENT ON COLUMN agencies.latitude IS 'Coordonnée GPS — préparation PostGIS (recherche proximité)';

CREATE INDEX idx_agencies_city    ON agencies (city);
CREATE INDEX idx_agencies_country ON agencies (country);

-- ============================================================================
-- 6. VEHICLES — Véhicules de location (Solution globale)
-- ============================================================================
-- Classification ACRISS décomposée en 4 attributs atomiques (1NF)
-- Réf. BR   : BR-LOC-01 (recherche de véhicules)
-- ============================================================================
CREATE TABLE vehicles (
                          id                      SERIAL          PRIMARY KEY,
                          agency_id               INTEGER         NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
                          brand                   VARCHAR(100)    NOT NULL,
                          model                   VARCHAR(100)    NOT NULL,
                          year                    INTEGER         NOT NULL,
                          license_plate           VARCHAR(20)     NOT NULL UNIQUE,
                          acriss_category         CHAR(1)         NOT NULL CHECK (acriss_category IN ('M','E','C','I','S','F','P','L')),
                          acriss_type             CHAR(1)         NOT NULL CHECK (acriss_type IN ('B','C','D','W','V','L','S','T')),
                          acriss_transmission     CHAR(1)         NOT NULL CHECK (acriss_transmission IN ('M','A','N')),
                          acriss_fuel_aircon      CHAR(1)         NOT NULL CHECK (acriss_fuel_aircon IN ('R','N','D','Q','H','E')),
                          seats                   INTEGER         NOT NULL DEFAULT 5,
                          daily_rate              DECIMAL(10,2)   NOT NULL,
                          status                  VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE'
                              CHECK (status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE'))
);

COMMENT ON TABLE  vehicles IS 'Véhicules de location avec classification ACRISS 4 caractères';
COMMENT ON COLUMN vehicles.acriss_category IS 'M=Mini, E=Economy, C=Compact, I=Intermediate, S=Standard, F=Fullsize, P=Premium, L=Luxury';
COMMENT ON COLUMN vehicles.acriss_type IS 'B=2-3door, C=2/4door, D=4-5door, W=Wagon, V=Van, L=Limousine, S=Sport, T=Convertible';
COMMENT ON COLUMN vehicles.acriss_transmission IS 'M=Manual, A=Automatic, N=Manual4WD';
COMMENT ON COLUMN vehicles.acriss_fuel_aircon IS 'R=Petrol+AC, N=Petrol-noAC, D=Diesel+AC, Q=Diesel-noAC, H=Hybrid, E=Electric';

CREATE INDEX idx_vehicles_agency ON vehicles (agency_id);
CREATE INDEX idx_vehicles_status ON vehicles (status);

-- ============================================================================
-- 7. OFFERS — Offres de location (Solution globale)
-- ============================================================================
-- Offre = résultat de recherche avant réservation (expire en 30 min)
-- Réf. BR   : BR-LOC-02
-- ============================================================================
CREATE TABLE offers (
                        id                      SERIAL          PRIMARY KEY,
                        vehicle_id              INTEGER         NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
                        user_id                 INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        pickup_agency_id        INTEGER         NOT NULL REFERENCES agencies(id),
                        dropoff_agency_id       INTEGER         NOT NULL REFERENCES agencies(id),
                        start_date              DATE            NOT NULL,
                        end_date                DATE            NOT NULL,
                        daily_rate              DECIMAL(10,2)   NOT NULL,
                        total_price             DECIMAL(10,2)   NOT NULL,
                        status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED')),
                        expires_at              TIMESTAMP       NOT NULL,
                        CHECK (end_date > start_date)
);

COMMENT ON TABLE  offers IS 'Offres de location — expiration 30 minutes (BR-LOC-02)';
COMMENT ON COLUMN offers.expires_at IS 'Expiration automatique : created_at + 30 min';

CREATE INDEX idx_offers_user    ON offers (user_id);
CREATE INDEX idx_offers_vehicle ON offers (vehicle_id);
CREATE INDEX idx_offers_status  ON offers (status);

-- ============================================================================
-- 8. RENTALS — Locations confirmées (Solution globale)
-- ============================================================================
-- Location créée après paiement Stripe confirmé (webhook)
-- Réf. BR   : BR-LOC-03
-- ============================================================================
CREATE TABLE rentals (
                         id                      SERIAL          PRIMARY KEY,
                         user_id                 INTEGER         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         offer_id                INTEGER         UNIQUE REFERENCES offers(id),
                         vehicle_id              INTEGER         NOT NULL REFERENCES vehicles(id),
                         pickup_agency_id        INTEGER         NOT NULL REFERENCES agencies(id),
                         dropoff_agency_id       INTEGER         NOT NULL REFERENCES agencies(id),
                         start_date              DATE            NOT NULL,
                         end_date                DATE            NOT NULL,
                         total_price             DECIMAL(10,2)   NOT NULL,
                         status                  VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMED'
                             CHECK (status IN ('CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
                         CHECK (end_date > start_date)
);

COMMENT ON TABLE  rentals IS 'Locations confirmées après paiement (BR-LOC-03)';
COMMENT ON COLUMN rentals.offer_id IS 'UNIQUE — 1 offre = max 1 location';

CREATE INDEX idx_rentals_user    ON rentals (user_id);
CREATE INDEX idx_rentals_vehicle ON rentals (vehicle_id);
CREATE INDEX idx_rentals_status  ON rentals (status);

-- ============================================================================
-- 9. PAYMENTS — Paiements Stripe (Solution globale)
-- ============================================================================
-- Aucune donnée bancaire stockée côté serveur (RM-07)
-- Réf. BR   : BR-LOC-04, RM-07
-- ============================================================================
CREATE TABLE payments (
                          id                          SERIAL          PRIMARY KEY,
                          rental_id                   INTEGER         NOT NULL UNIQUE REFERENCES rentals(id),
                          user_id                     INTEGER         NOT NULL REFERENCES users(id),
                          stripe_payment_intent_id    VARCHAR(255)    UNIQUE,
                          amount                      DECIMAL(10,2)   NOT NULL,
                          currency                    VARCHAR(3)      NOT NULL DEFAULT 'EUR',
                          status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
                          paid_at                     TIMESTAMP
);

COMMENT ON TABLE  payments IS 'Paiements Stripe — aucune donnée bancaire stockée (RM-07)';
COMMENT ON COLUMN payments.stripe_payment_intent_id IS 'Identifiant Stripe — seul référence au paiement';

CREATE INDEX idx_payments_rental ON payments (rental_id);
CREATE INDEX idx_payments_user   ON payments (user_id);
CREATE INDEX idx_payments_status ON payments (status);

-- ============================================================================
-- 10. INVOICES — Factures (Solution globale)
-- ============================================================================
-- Facture PDF générée automatiquement après paiement
-- Réf. BR   : BR-LOC-04
-- ============================================================================
CREATE TABLE invoices (
                          id                  SERIAL          PRIMARY KEY,
                          payment_id          INTEGER         NOT NULL UNIQUE REFERENCES payments(id),
                          rental_id           INTEGER         NOT NULL REFERENCES rentals(id),
                          user_id             INTEGER         NOT NULL REFERENCES users(id),
                          invoice_number      VARCHAR(50)     NOT NULL UNIQUE,
                          total_ht            DECIMAL(10,2)   NOT NULL,
                          tax_rate            DECIMAL(5,2)    NOT NULL,
                          total_ttc           DECIMAL(10,2)   NOT NULL,
                          currency            VARCHAR(3)      NOT NULL DEFAULT 'EUR',
                          pdf_url             VARCHAR(500),
                          issued_at           TIMESTAMP       DEFAULT NOW()
);

COMMENT ON TABLE  invoices IS 'Factures générées après paiement confirmé (BR-LOC-04)';
COMMENT ON COLUMN invoices.invoice_number IS 'Format : YCYW-YYYY-NNNN (ex : YCYW-2026-0001)';
COMMENT ON COLUMN invoices.tax_rate IS 'Taux TVA en % (20.00 France, 20.00 UK, variable USA)';

CREATE INDEX idx_invoices_rental ON invoices (rental_id);
CREATE INDEX idx_invoices_user   ON invoices (user_id);

-- ============================================================================
-- RÉSUMÉ DE LA STRUCTURE
-- ============================================================================
-- PoC (4 tables) :
--    users                      — BR-AUTH-01/02
--    conversations              — BR-SUP-01
--    conversation_participants  — Évolution V2 (N:N)
--    messages                   — BR-SUP-01/02
--
-- Solution globale (6 tables) :
--    agencies                   — BR-LOC-01
--    vehicles                   — BR-LOC-01 (ACRISS)
--    offers                     — BR-LOC-02
--    rentals                    — BR-LOC-03
--    payments                   — BR-LOC-04, RM-07
--    invoices                   — BR-LOC-04
--
-- Total : 10 tables, conforme au MPD de l'ADD V3.1
-- ============================================================================

-- ============================================================================
-- YOUR CAR YOUR WAY — Données d'initialisation (Seed)
-- ============================================================================
-- Ce script insère les données de départ nécessaires au fonctionnement
-- de l'application : admin, employé de test, agences, véhicules.
--
-- Les mots de passe sont hashés avec BCrypt (coût 10).
-- Mot de passe par défaut pour tous les comptes : "Password1!"
-- ============================================================================

-- ============================================================================
-- UTILISATEURS DE BASE
-- ============================================================================

-- Admin principal (mot de passe : Password1!)
-- BCrypt hash de "Password1!" avec coût 10
INSERT INTO users (first_name, last_name, username, email, password, role, verification_status, creation_date)
VALUES (
           'Arnaud', 'Derisbourg', 'admin',
           'arnaud1720@gmail.com',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'ADMIN', 'NONE', NOW()
       );

-- Employé vérifié (mot de passe : Password1!)
INSERT INTO users (first_name, last_name, username, email, password, role, verification_status, creation_date)
VALUES (
           'Marie', 'Dupont', 'marie.dupont',
           'employee@ycyw.test',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'EMPLOYEE', 'VERIFIED', NOW()
       );

-- Client test (mot de passe : Password1!)
INSERT INTO users (first_name, last_name, username, email, password, role, verification_status, creation_date)
VALUES (
           'Jean', 'Martin', 'jean.martin',
           'client@ycyw.test',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'USER', 'NONE', NOW()
       );

-- Employé en attente de validation (mot de passe : Password1!)
INSERT INTO users (first_name, last_name, username, email, password, role, verification_status, creation_date)
VALUES (
           'Sophie', 'Bernard', 'sophie.bernard',
           'pending@ycyw.test',
           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
           'EMPLOYEE', 'PENDING', NOW()
       );

-- ============================================================================
-- AGENCES (Solution globale — 6 agences dans 3 pays)
-- ============================================================================

-- UK (siège historique)
INSERT INTO agencies (name, address, city, country, postal_code, phone, email, latitude, longitude)
VALUES
    ('YCYW London Heathrow', 'Terminal 5, Heathrow Airport', 'London', 'United Kingdom', 'TW6 1EW',
     '+44 20 7946 0958', 'heathrow@ycyw.com', 51.470020, -0.454296),
    ('YCYW Manchester Airport', 'Terminal 1, Manchester Airport', 'Manchester', 'United Kingdom', 'M90 1QX',
     '+44 161 489 3000', 'manchester@ycyw.com', 53.353740, -2.275010);

-- France (expansion Europe)
INSERT INTO agencies (name, address, city, country, postal_code, phone, email, latitude, longitude)
VALUES
    ('YCYW Paris CDG', 'Terminal 2E, Aéroport CDG', 'Paris', 'France', '95700',
     '+33 1 48 62 00 00', 'cdg@ycyw.com', 49.009691, 2.547925),
    ('YCYW Bordeaux Mérignac', 'Aéroport Bordeaux-Mérignac', 'Bordeaux', 'France', '33700',
     '+33 5 56 34 50 00', 'bordeaux@ycyw.com', 44.828337, -0.715584);

-- USA (expansion internationale)
INSERT INTO agencies (name, address, city, country, postal_code, phone, email, latitude, longitude)
VALUES
    ('YCYW New York JFK', 'Terminal 4, JFK Airport', 'New York', 'United States', '11430',
     '+1 718 244 4444', 'jfk@ycyw.com', 40.641766, -73.780968),
    ('YCYW Los Angeles LAX', 'World Way, LAX Airport', 'Los Angeles', 'United States', '90045',
     '+1 310 646 5252', 'lax@ycyw.com', 33.942536, -118.408075);

-- ============================================================================
-- VÉHICULES (Solution globale — 2 par agence)
-- ============================================================================

-- London Heathrow (agency_id = 1)
INSERT INTO vehicles (agency_id, brand, model, year, license_plate, acriss_category, acriss_type, acriss_transmission, acriss_fuel_aircon, seats, daily_rate, status)
VALUES
    (1, 'Volkswagen', 'Golf', 2024, 'AB-123-CD', 'C', 'D', 'M', 'R', 5, 45.00, 'AVAILABLE'),
    (1, 'BMW', '5 Series', 2024, 'EF-456-GH', 'F', 'D', 'A', 'D', 5, 120.00, 'AVAILABLE');

-- Manchester (agency_id = 2)
INSERT INTO vehicles (agency_id, brand, model, year, license_plate, acriss_category, acriss_type, acriss_transmission, acriss_fuel_aircon, seats, daily_rate, status)
VALUES
    (2, 'Ford', 'Fiesta', 2023, 'IJ-789-KL', 'E', 'C', 'M', 'R', 5, 35.00, 'AVAILABLE'),
    (2, 'Mercedes', 'E-Class', 2024, 'MN-012-OP', 'P', 'D', 'A', 'D', 5, 150.00, 'AVAILABLE');

-- Paris CDG (agency_id = 3)
INSERT INTO vehicles (agency_id, brand, model, year, license_plate, acriss_category, acriss_type, acriss_transmission, acriss_fuel_aircon, seats, daily_rate, status)
VALUES
    (3, 'Renault', 'Clio', 2024, 'QR-345-ST', 'E', 'C', 'M', 'R', 5, 38.00, 'AVAILABLE'),
    (3, 'Peugeot', '5008', 2024, 'UV-678-WX', 'S', 'W', 'A', 'D', 7, 85.00, 'AVAILABLE');

-- Bordeaux (agency_id = 4)
INSERT INTO vehicles (agency_id, brand, model, year, license_plate, acriss_category, acriss_type, acriss_transmission, acriss_fuel_aircon, seats, daily_rate, status)
VALUES
    (4, 'Citroën', 'C3', 2023, 'YZ-901-AB', 'E', 'C', 'M', 'R', 5, 32.00, 'AVAILABLE'),
    (4, 'Tesla', 'Model 3', 2024, 'CD-234-EF', 'S', 'D', 'A', 'E', 5, 95.00, 'AVAILABLE');

-- New York JFK (agency_id = 5)
INSERT INTO vehicles (agency_id, brand, model, year, license_plate, acriss_category, acriss_type, acriss_transmission, acriss_fuel_aircon, seats, daily_rate, status)
VALUES
    (5, 'Toyota', 'Camry', 2024, 'GH-567-IJ', 'I', 'D', 'A', 'R', 5, 55.00, 'AVAILABLE'),
    (5, 'Cadillac', 'Escalade', 2024, 'KL-890-MN', 'L', 'V', 'A', 'R', 7, 200.00, 'AVAILABLE');

-- Los Angeles LAX (agency_id = 6)
INSERT INTO vehicles (agency_id, brand, model, year, license_plate, acriss_category, acriss_type, acriss_transmission, acriss_fuel_aircon, seats, daily_rate, status)
VALUES
    (6, 'Chevrolet', 'Malibu', 2024, 'OP-123-QR', 'I', 'D', 'A', 'R', 5, 50.00, 'AVAILABLE'),
    (6, 'Ford', 'Mustang', 2024, 'ST-456-UV', 'S', 'T', 'A', 'R', 4, 110.00, 'AVAILABLE');

-- ============================================================================
-- CONVERSATION DE TEST (PoC)
-- ============================================================================

-- Conversation entre le client (id=3) et l'employée (id=2)
INSERT INTO conversations (subject, customer_id, employee_id, status, created_at, updated_at)
VALUES ('Problème de réservation', 3, 2, 'OPEN', NOW(), NOW());

-- Messages de test dans la conversation
INSERT INTO messages (conversation_id, sender_id, content, sent_at, is_read)
VALUES
    (1, 3, 'Bonjour, j''ai un problème avec ma réservation à Bordeaux.', NOW() - INTERVAL '10 minutes', TRUE),
    (1, 2, 'Bonjour Jean, je regarde votre dossier. Pouvez-vous me donner votre numéro de réservation ?', NOW() - INTERVAL '8 minutes', TRUE),
    (1, 3, 'Merci ! C''est la réservation du 15 février pour une Citroën C3.', NOW() - INTERVAL '5 minutes', FALSE);

-- ============================================================================
-- VÉRIFICATIONS
-- ============================================================================
DO $$
    DECLARE
        t_count INTEGER;
    BEGIN
        SELECT COUNT(*) INTO t_count
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE';

        RAISE NOTICE '=========================================';
        RAISE NOTICE 'Initialisation terminée !';
        RAISE NOTICE 'Tables créées       : % / 10', t_count;
        RAISE NOTICE 'Utilisateurs        : % (admin + employé + client + pending)', (SELECT COUNT(*) FROM users);
        RAISE NOTICE 'Agences             : % (UK, France, USA)', (SELECT COUNT(*) FROM agencies);
        RAISE NOTICE 'Véhicules           : % (2 par agence)', (SELECT COUNT(*) FROM vehicles);
        RAISE NOTICE 'Conversations test  : %', (SELECT COUNT(*) FROM conversations);
        RAISE NOTICE 'Messages test       : %', (SELECT COUNT(*) FROM messages);
        RAISE NOTICE '=========================================';
    END
$$;