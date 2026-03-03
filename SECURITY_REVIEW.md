# Security Review

## Recon & Threat Model (lightweight)
- **Application type:** Spring Boot (Java 21) backend with React + Vite frontend.
- **Entrypoints:** REST endpoints under `/api/v1/**` and SPA routes rendered by frontend.
- **Auth model:** Feature-dependent; mobile scanner endpoints are intentionally unauthenticated for peer device handoff.
- **Data handling:** Uploaded files are staged to local temp storage and polled/downloaded by session ID.

### Highest-risk surfaces reviewed
1. Mobile scanner session creation, validation, upload and download flows.
2. File upload controls (type/size/count) and path safety.
3. Response-header hardening for browser attack mitigations (CSP, frame protections, MIME sniffing).
4. Frontend mobile workspace UX and accessibility touchpoints.

## Findings Summary
- **High:** 1
- **Medium:** 2
- **Low:** 1

## Findings and fixes

### 1) High — Session bypass / unauthorized upload to arbitrary session IDs (CWE-639, OWASP A01)
- **Risk:** `uploadFiles` implicitly created sessions when a session ID was unknown, allowing an attacker to push files into guessed IDs without desktop-side session creation.
- **Location:** `app/common/src/main/java/stirling/software/common/service/MobileScannerService.java` (`uploadFiles`).
- **Exploit scenario:** Attacker sends `POST /api/v1/mobile-scanner/upload/{guessed-id}` and causes unauthorized content injection or resource abuse.
- **Fix applied:** Uploads now require an existing, non-expired session via `getValidActiveSession`; no implicit session auto-creation.

### 2) Medium — Unrestricted upload payload characteristics (CWE-434/CWE-400)
- **Risk:** No file type/size/count controls, increasing abuse and DoS risk.
- **Location:** `app/common/src/main/java/stirling/software/common/service/MobileScannerService.java` (`uploadFiles`).
- **Exploit scenario:** Oversized or high-volume uploads exhaust disk/memory or store unexpected content.
- **Fix applied:** Added strict checks: max 20 files/request, max 25MB/file, and extension allowlist (`jpg/jpeg/png/webp/pdf`).

### 3) Medium — Missing baseline security response headers (OWASP A05)
- **Risk:** Absent browser hardening headers increase clickjacking and content-type sniffing risks.
- **Location:** Global HTTP responses.
- **Exploit scenario:** UI embedded in hostile frames or browser MIME confusion.
- **Fix applied:** Added a global filter with CSP, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, and HSTS when request is secure.

### 4) Low — Content-Disposition construction from raw filename (CWE-113 hardening)
- **Risk:** Manual header string concatenation is easier to misuse and less robust.
- **Location:** `app/core/src/main/java/stirling/software/SPDF/controller/api/misc/MobileScannerController.java` (`downloadFile`).
- **Exploit scenario:** Malformed filename edge cases could produce unsafe header values.
- **Fix applied:** Replaced manual header building with Spring `ContentDisposition` builder and added handling for invalid filename/session inputs.

## Tests added
- Added unit tests to verify:
  1. uploads are rejected when the session is not pre-created,
  2. unsupported extensions are rejected.

## Notes
- No repository hardcoded production secrets were identified in audited backend/frontend paths.
