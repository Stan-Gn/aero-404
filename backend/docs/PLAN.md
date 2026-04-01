# 🚁 AERO — Plan współpracy

## Jak używać tego pliku
- Wklej ten plik na początku każdej sesji z Claude
- Powiedz np. **"Zaczynamy Fazę 2, punkt 2.1"**
- Claude wie dokładnie co generować — bez dodatkowych pytań
- Po ukończeniu punktu zaznacz `[x]`

---

## Stack technologiczny
- Java 21 (rekordy dla DTO, sealed classes dla wyjątków)
- Spring Boot 3.4.x
- Spring Security 6 + JWT (jjwt)
- Spring Data JPA + Hibernate (`ddl-auto=update`)
- PostgreSQL
- Lombok (encje), MapStruct (mapowanie), Bean Validation
- Springdoc OpenAPI 3
- Maven
- Deploy: Render.com + Dockerfile + GitHub

## Struktura pakietów
```
com.aero
├── config/
├── controller/
├── service/
├── repository/
├── domain/        ← encje JPA + enumy
├── dto/           ← request/response (Java records)
├── mapper/        ← MapStruct
├── security/
└── exception/
```

## Zasady generowania kodu (zawsze stosuj)
- DTO = Java record z Bean Validation
- Encje = Lombok @Data @Builder @NoArgsConstructor @AllArgsConstructor
- FetchType.LAZY na wszystkich relacjach
- @Transactional na metodach zapisu w serwisach
- Nigdy nie zwracaj encji JPA z kontrolera
- Enumy dla wszystkich statusów i ról
- Dedykowane wyjątki np. HelicopterNotFoundException extends RuntimeException
- Endpointy pod /api/v1/...

---

## FAZA 1 — Fundament

### 1.1 Konfiguracja projektu
Wygeneruj:
- `pom.xml` z zależnościami: web, jpa, security, postgresql, lombok, mapstruct, springdoc, jjwt, validation
- `application.yml` — datasource, jpa ddl-auto=update, jwt secret + expiration
- `application-prod.yml` — zmienne środowiskowe: `${DATABASE_URL}`, `${JWT_SECRET}`, `${JWT_EXPIRATION}`
- [ ] Ukończono

### 1.2 Infrastruktura wyjątków
Wygeneruj:
- `ErrorResponseDto` (record: timestamp, status, message, List<String> errors)
- `GlobalExceptionHandler` (@RestControllerAdvice):
  - EntityNotFoundException → 404
  - ValidationException → 400
  - AccessDeniedException → 403
  - MethodArgumentNotValidException → 400 z listą błędów pól
- [ ] Ukończono

### 1.3 Spring Security + JWT
Wygeneruj:
- Enum `UserRole`: ADMIN, PLANNER, SUPERVISOR, PILOT
- Encja `AppUser` (id, firstName, lastName, email, password, role)
- predefiniowany user zapisany do bazy danych o danch : login:admin, hasło:admin, emai:admin@aero-404.pl i resztę pól admin. 
- ADMIN przydziela role do dodanych użytkowników. 
- endpoint w którym admin przydziela role do użytkownika.
- `AppUserRepository`
- `JwtTokenProvider` — generateToken(email, role), validateToken, getEmail
- `JwtAuthenticationFilter` — extends OncePerRequestFilter
- `UserDetailsServiceImpl` — loadUserByUsername po email
- `SecurityConfig`:
  - białe listy: /api/v1/auth/**
  - reszta wymaga JWT
  - Bean `PasswordEncoder` → BCryptPasswordEncoder
- `AuthController` z dwoma endpointami:
  - POST `/api/v1/auth/register`:
    - `RegisterRequestDto` (record: firstName, lastName, email, password, role)
    - walidacja: email unikalny, hasło min. 8 znaków
    - zapis użytkownika z hasłem zahashowanym BCrypt (`passwordEncoder.encode(password)`)
    - zwraca `RegisterResponseDto` (record: id, email, role)
  - POST `/api/v1/auth/login`:
    - `LoginRequestDto` (record: email, password)
    - weryfikacja: `passwordEncoder.matches(rawPassword, encodedPassword)`
    - jeśli błędne hasło → 401 z komunikatem "Invalid credentials"
    - jeśli poprawne → generuj JWT przez `JwtTokenProvider`
    - zwraca `LoginResponseDto` (record: token, email, role)
- Każdy kolejny request: `JwtAuthenticationFilter` wyciąga token z nagłówka `Authorization: Bearer <token>`, waliduje i ustawia `SecurityContext`
- w klasie TestController jest endpoint "/test" zrób tak żeby tylko admin po zalogowaniu mógł go uruchomić. 
- [ ] Ukończono

---

## FAZA 2 — Moduły administracyjne (ADMIN)

### 2.1 Helikoptery — PRD sekcja 6.1
Wygeneruj:
- Enum `HelicopterStatus`: ACTIVE, INACTIVE
- Encja `Helicopter`: id, regNumber, type, description, maxCrew, maxPayload, status, reviewDate, rangeKm
- `HelicopterRequestDto` (record + walidacja pól wg PRD 6.1)
- `HelicopterResponseDto` (record)
- `HelicopterMapper` (MapStruct)
- `HelicopterRepository`
- `HelicopterService`: findAll, findById, create, update, delete + walidacja reviewDate wymagane gdy ACTIVE
- `HelicopterController` `/api/v1/helicopters`: GET/, GET/{id}, POST/, PUT/{id}, DELETE/{id}
- Dostęp: ADMIN pełny, pozostałe role GET only
- Sortowanie domyślne: status ASC, regNumber ASC
- [ ] Ukończono

### 2.2 Członkowie załogi — PRD sekcja 6.2
Wygeneruj:
- Enum `CrewRole`: PILOT, OBSERVER
- Encja `CrewMember`: id, firstName, lastName, email, weight, role, licenseNumber, licenseExpiry, trainingExpiry
- `CrewMemberRequestDto` (record + walidacja: email regex, licenseNumber+licenseExpiry wymagane gdy PILOT)
- `CrewMemberResponseDto` (record)
- `CrewMemberMapper` (MapStruct)
- `CrewMemberRepository`
- `CrewMemberService`: findAll, findById, create, update, delete
- `CrewMemberController` `/api/v1/crew-members`: GET/, GET/{id}, POST/, PUT/{id}, DELETE/{id}
- Dostęp: ADMIN pełny, pozostałe role GET only
- Sortowanie domyślne: email ASC
- [ ] Ukończono

### 2.3 Lądowiska — PRD sekcja 6.3
Wygeneruj:
- Encja `Airfield`: id, name, latitude, longitude
- `AirfieldRequestDto` (record + walidacja)
- `AirfieldResponseDto` (record)
- `AirfieldMapper` (MapStruct)
- `AirfieldRepository`
- `AirfieldService`: findAll, findById, create, update, delete
- `AirfieldController` `/api/v1/airfields`: GET/, GET/{id}, POST/, PUT/{id}, DELETE/{id}
- Dostęp: ADMIN pełny, pozostałe role GET only
- Sortowanie domyślne: name ASC
- [ ] Ukończono

### 2.4 Użytkownicy systemu — PRD sekcja 6.4
Wygeneruj:
- `UserRequestDto` (record + walidacja + pole password)
- `UserResponseDto` (record — bez hasła)
- `UserMapper` (MapStruct)
- `UserService`: findAll, findById, create (hashowanie BCrypt), update, delete
- `UserController` `/api/v1/users`: GET/, GET/{id}, POST/, PUT/{id}, DELETE/{id}
- Dostęp: tylko ADMIN
- Sortowanie domyślne: email ASC
- [ ] Ukończono

---

## FAZA 3 — Planowane operacje lotnicze — PRD sekcja 6.5

### 3.1 Model domenowy operacji
Wygeneruj:
- Enum `OperationStatus`: INTRODUCED(1), REJECTED(2), CONFIRMED(3), SCHEDULED(4), PARTIALLY_DONE(5), DONE(6), RESIGNED(7)
- Enum `ActivityType`: VISUAL_INSPECTION, SCAN_3D, FAULT_LOCATION, PHOTOS, PATROL
- Encja `PlannedOperation`: id, autoNumber, orderNumber, shortDescription, kmlFileName(oryginalna nazwa pliku), kmlContent(TEXT — treść pliku KML przechowywana jako BLOB w bazie, ~22KB na 1000 punktów), routeKm, routePoints(TEXT — JSON string z listą punktów lat/lon, np. "[[51.1,17.0],[51.2,17.1],...]", wypełniany automatycznie przy create/update z parsowania KML), proposedDateFrom, proposedDateTo, plannedDateFrom, plannedDateTo, activityTypes(Set<ActivityType>), additionalInfo, status, createdBy(AppUser), contactEmails, remarksAfterExecution
- Encja `OperationComment`: id, operation(ManyToOne), text, createdBy(AppUser), createdAt
- Encja `OperationHistory`: id, operation(ManyToOne), fieldName, oldValue, newValue, changedBy(AppUser), changedAt
- [ ] Ukończono

### 3.2 Parser KML i obliczanie trasy
Wygeneruj:
- `KmlParserService`: parsowanie pliku KML → List<double[]> punktów (lat, lon), walidacja max 5000 punktów, teren Polski (bounding box: lat 49-55, lon 14-24)
- `RouteCalculatorService`: algorytm Haversine — suma odległości między kolejnymi punktami → wynik w km (Integer)
- [ ] Ukończono

### 3.3 Serwis i kontroler operacji
Wygeneruj:
- `PlannedOperationRequestDto` (record + walidacja), `PlannedOperationResponseDto` (record z listą komentarzy i historią)
- `PlannedOperationMapper` (MapStruct)
- `PlannedOperationRepository` (+ metody filtrowania po statusie)
- `PlannedOperationService`:
  - findAll(OperationStatus filter) — domyślny filtr status=CONFIRMED
  - findById, create (oblicz routeKm z KML + zapisz routePoints jako JSON string), update (j.w. gdy zmienia się KML)
  - update: jeśli rola PLANNER → zablokuj edycję pól: plannedDateFrom, plannedDateTo, remarksAfterExecution (zachowaj stare wartości). Pola wyliczane (autoNumber, routeKm, routePoints) i status nie są w RequestDto więc nie da się ich nadpisać z zewnątrz (PRD 6.5.e)
  - reject(id) — status 1→2, tylko SUPERVISOR
  - confirmToPlan(id, plannedDateFrom, plannedDateTo) — status 1→3, tylko SUPERVISOR, daty wymagane
  - resign(id) — status 1/3/4→7, tylko PLANNER
  - addComment(id, text) — dodaje OperationComment
  - zapis historii zmian przy każdej zmianie pola
- `PlannedOperationController` `/api/v1/operations`:
  - GET/, GET/{id}, POST/, PUT/{id}
  - POST /{id}/reject, POST /{id}/confirm, POST /{id}/resign
  - POST /{id}/comments
- Dostęp wg PRD sekcja 7.2
- Sortowanie domyślne: plannedDateFrom ASC
- [ ] Ukończono

---

## FAZA 4 — Zlecenia na lot — PRD sekcja 6.6

### 4.1 Model domenowy zlecenia
Wygeneruj:
- Enum `FlightOrderStatus`: INTRODUCED(1), SUBMITTED(2), REJECTED(3), ACCEPTED(4), PARTIALLY_DONE(5), DONE(6), NOT_DONE(7)
- Encja `FlightOrder`: id, autoNumber, plannedDeparture, plannedLanding, actualDeparture, actualLanding, pilot(CrewMember), crewMembers(Set<CrewMember>), helicopter(Helicopter), departureAirfield(Airfield), arrivalAirfield(Airfield), plannedOperations(Set<PlannedOperation>), crewWeight(obliczane), estimatedRouteKm, status
- [ ] Ukończono

### 4.2 Walidacje biznesowe zlecenia — PRD sekcja 6.6.c
Wygeneruj:
- `FlightOrderValidationService` z dedykowanymi wyjątkami dla każdego warunku:
  - Helikopter: reviewDate >= data lotu → `HelicopterReviewExpiredException`
  - Pilot: licenseExpiry >= data lotu → `PilotLicenseExpiredException`
  - Każdy członek załogi: trainingExpiry >= data lotu → `CrewMemberTrainingExpiredException`
  - Suma wag załogi (pilot + członkowie) <= helicopter.maxPayload → `CrewWeightExceededException`
  - estimatedRouteKm <= helicopter.rangeKm → `RouteExceedsRangeException`
- [ ] Ukończono

### 4.3 Serwis i kontroler zlecenia
Wygeneruj:
- `FlightOrderRequestDto` (record + walidacja), `FlightOrderResponseDto` (record)
- `FlightOrderMapper` (MapStruct)
- `FlightOrderRepository`
- `FlightOrderService`:
  - findAll(FlightOrderStatus filter) — domyślny filtr status=SUBMITTED
  - findById, create, update
  - autouzupełnienie pilota = zalogowany user jeśli rola PILOT
  - obliczenie crewWeight przy zapisie
  - wywołanie FlightOrderValidationService przy zapisie
  - submit(id) — status 1→2, tylko PILOT
  - reject(id) — status 2→3, tylko SUPERVISOR
  - accept(id) — status 2→4, tylko SUPERVISOR
  - markPartiallyDone(id) — status 4→5 + wszystkie operacje 4→5, tylko PILOT
  - markDone(id) — status 4→6 + wszystkie operacje 4→6, tylko PILOT
  - markNotDone(id) — status 4→7 + wszystkie operacje 4→3, tylko PILOT
  - przy dodaniu operacji do zlecenia → operacja status 3→4
- `FlightOrderController` `/api/v1/flight-orders`:
  - GET/, GET/{id}, POST/, PUT/{id}
  - POST /{id}/submit, POST /{id}/reject, POST /{id}/accept
  - POST /{id}/complete (body: `{ "result": "DONE" | "PARTIALLY_DONE" | "NOT_DONE" }`)
- Dostęp wg PRD sekcja 7.2
- Sortowanie domyślne: plannedDeparture ASC
- [ ] Ukończono

---

## FAZA 5 — Zabezpieczenia endpointów

### 5.1 SecurityConfig — grube cięcie per sekcja (zgodnie z PRD 7.2)
Wygeneruj/zaktualizuj `SecurityConfig` z regułami:
```
/api/v1/auth/**                → wszyscy (permitAll)
/api/v1/health                 → wszyscy (permitAll)
/api/v1/helicopters/**         → ADMIN, SUPERVISOR, PILOT (PLANNER brak dostępu)
/api/v1/crew-members/**        → ADMIN, SUPERVISOR, PILOT (PLANNER brak dostępu)
/api/v1/airfields/**           → ADMIN, SUPERVISOR, PILOT (PLANNER brak dostępu)
/api/v1/users/**               → ADMIN, SUPERVISOR, PILOT (PLANNER brak dostępu)
/api/v1/operations/**          → ADMIN, PLANNER, SUPERVISOR, PILOT
/api/v1/flight-orders/**       → ADMIN, PILOT, SUPERVISOR (PLANNER brak dostępu)
```
- [x] Ukończono

### 5.2 @PreAuthorize — precyzyjna logika ról

Dodaj `@PreAuthorize` na metodach serwisów (włącz `@EnableMethodSecurity` w SecurityConfig):

**PlannedOperationService — zmiany statusów:**
- `create` → `hasAnyRole('PLANNER','SUPERVISOR')`
- `update` → SUPERVISOR zawsze (wszystkie statusy), PLANNER tylko własne i tylko w statusach 1-5 (INTRODUCED, REJECTED, CONFIRMED, SCHEDULED, PARTIALLY_DONE): `hasRole('SUPERVISOR') or (hasRole('PLANNER') and @operationSecurity.isOwner(#id, authentication.name) and @operationSecurity.isEditableByPlanner(#id))`
- `reject(id)` → `hasRole('SUPERVISOR')`
- `confirmToPlan(id)` → `hasRole('SUPERVISOR')`
- `resign(id)` → `hasRole('PLANNER')`

**FlightOrderService — zmiany statusów:**
- `create` → `hasRole('PILOT')`
- `update` → `hasAnyRole('PILOT','SUPERVISOR')`
- `submit(id)` → `hasRole('PILOT')`
- `reject(id)` → `hasRole('SUPERVISOR')`
- `accept(id)` → `hasRole('SUPERVISOR')`
- `markPartiallyDone(id)` → `hasRole('PILOT')`
- `markDone(id)` → `hasRole('PILOT')`
- `markNotDone(id)` → `hasRole('PILOT')`

**Dodaj klasę pomocniczą:**
- `OperationSecurityService` (@Component, nazwa bean: `operationSecurity`):
  - `isOwner(Long operationId, String email)` → sprawdza czy `createdBy.email == email`
  - `isEditableByPlanner(Long operationId)` → sprawdza czy status operacji jest w zbiorze {INTRODUCED, REJECTED, CONFIRMED, SCHEDULED, PARTIALLY_DONE} (PRD 6.5.d)
- [ ] Ukończono

---

## FAZA 6 — Demo i dane testowe

### 6.1 DataInitializer
Wygeneruj:
- `DataInitializer` (@Component + CommandLineRunner) — uruchamia się tylko gdy baza jest pusta
- Wstawia: 1 użytkownik ADMIN, 1 PLANNER, 1 SUPERVISOR, 1 PILOT
- Wstawia: 2 helikoptery (1 aktywny, 1 nieaktywny)
- Wstawia: 3 członków załogi (1 Pilot, 2 Observer)
- Wstawia: 3 lądowiska
- Wstawia: 2 planowane operacje w statusie CONFIRMED
- Hasła hashowane BCrypt, domyślne: `password123`
- [ ] Ukończono

### 6.2 Health check i OpenAPI
Wygeneruj:
- `HealthController` GET `/api/v1/health` → `{ "status": "UP", "timestamp": "..." }`
- Konfiguracja Springdoc: `springdoc.api-docs.path=/api-docs`, `springdoc.swagger-ui.path=/swagger-ui.html`
- OpenAPI info: title "AERO API", version "1.0.0"
- [ ] Ukończono
