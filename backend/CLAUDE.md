# AERO — Backend Java/Spring

## Stack technologiczny
- Java 21 (rekordy, sealed classes, pattern matching)
- Spring Boot 3.4.x
- Spring Security 6 (JWT)
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Flyway (migracje DB)
- MapStruct (mapowanie DTO)
- Lombok
- OpenAPI 3 / Springdoc
- Maven

## Architektura
- Pakiety: controller / service / repository / domain / dto / mapper / config
- REST API: /api/v1/...
- DTO pattern — nigdy nie zwracamy encji JPA z kontrolera
- Walidacja: Bean Validation (@Valid) na DTO
- Obsługa błędów: GlobalExceptionHandler (@RestControllerAdvice)

## Dobre praktyki — ZAWSZE stosuj
- Encje JPA: @Table, @Column(nullable=false) wszędzie
- Relacje: FetchType.LAZY domyślnie
- Serwisy: @Transactional na metodach zapisu
- Enumy dla statusów (nie Stringi!)
- Testy: minimum jednostkowe dla serwisów (Mockito)
- Logi: SLF4J, nie System.out.println
- Brak logiki biznesowej w kontrolerach

## Kontekst projektu
@docs/PRD.md

## Plan realizacji
@docs/PLAN.md