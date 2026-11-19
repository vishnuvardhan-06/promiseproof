# PromiseProof

PromiseProof is a public accountability platform for tracking political promises and the
evidence that supports or contradicts them.

It records what was promised, by whom, in which election and state, and what progress has
been reported. Every promise keeps the original source (manifesto or document name, page,
exact quotation and URL) alongside the promise wording, and appears publicly only after an
independent reviewer has checked that source and wording. Evidence documents are uploaded
with structured provenance and published only after a reviewer has approved them.

**Neutrality statement:** PromiseProof does not endorse any party, candidate or policy.
An uploaded document is presented as *submitted evidence*, never as proof that a promise
was kept or broken. Reviewers approve publication of a document; they do not certify that
its contents are true. Approved evidence hides submitter identity from the public
projection.

---

## Tech stack

| Layer    | Technology                                            |
| -------- | ----------------------------------------------------- |
| Backend  | Java 21, Spring Boot 4.1.2, Spring Security, Spring Data JPA |
| Database | MySQL 8                                               |
| Frontend | React 19, Vite 8, React Router                        |
| Auth     | Server-side HTTP session + HTTP-only cookie (no JWT)  |
| CSRF     | Cookie token (`XSRF-TOKEN`) + `X-XSRF-TOKEN` header   |

---

## Prerequisites

- Java 21 (JDK)
- Node.js 20+
- MySQL 8 running on `localhost:3306`

---

## Setup

1. Create the database:

   ```sql
   CREATE DATABASE promiseproof;
   ```

2. Configuration lives in
   `promiseproof-backend/src/main/resources/application.properties`. Every value below can
   be overridden with an environment variable, so no secrets need to be committed:

   | Setting                | Environment variable        | Default                                      |
   | ---------------------- | --------------------------- | -------------------------------------------- |
   | Datasource URL         | `DB_URL`                    | *(required)*                                 |
   | Datasource user        | `DB_USER`                   | *(required)*                                 |
   | Datasource password    | `DB_PASSWORD`               | *(required)*                                 |
   | Upload directory       | `PROMISEPROOF_UPLOAD_DIR`   | `V:/PromiseProof/evidence-storage`           |
   | Allowed browser origins| `PROMISEPROOF_CORS_ORIGINS` | `http://localhost:5173`                      |
   | Secure session cookie  | `PROMISEPROOF_COOKIE_SECURE`| `false` (set `true` behind HTTPS)            |
   | Seed admin name        | `PROMISEPROOF_ADMIN_NAME`   | `Platform Administrator`                     |
   | Seed admin email       | `PROMISEPROOF_ADMIN_EMAIL`  | `admin@promiseproof.local`                   |
   | Seed admin password    | `PROMISEPROOF_ADMIN_PASSWORD`| *(required)*                                 |

   `PROMISEPROOF_CORS_ORIGINS` accepts a comma-separated list.

   The initial administrator password has **no default**. When no administrator
   account exists yet, the backend refuses to finish startup unless
   `PROMISEPROOF_ADMIN_PASSWORD` is set to a strong password (minimum 10 characters,
   with upper case, lower case and a digit). The configured value is hashed with BCrypt
   before the seed account is created, and the environment variable should be supplied
   through a secrets manager in real deployments.

3. Start the backend (from `promiseproof-backend`):

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Tables are created/updated automatically on startup, and the initial administrator
   account is seeded once if it does not already exist.

4. Start the frontend (from `promiseproof-frontend`):

   ```powershell
   npm install
   npm run dev
   ```

5. Open <http://localhost:5173>.

---

## Demo credentials

| Role  | Email                      | Password      |
| ----- | -------------------------- | ------------- |
| Admin | `admin@promiseproof.local` | *(set via `PROMISEPROOF_ADMIN_PASSWORD`)* |

The admin account is created from properties on first startup only, and only when a
strong initial password is supplied through `PROMISEPROOF_ADMIN_PASSWORD`. Change the
seed password before any real deployment; there is no published fallback credential.

Citizens self-register through the UI as `CITIZEN`. An administrator can promote an account
to `RESEARCHER`, `REVIEWER` or `ADMIN` from the Admin page. Role changes and account
disabling are enforced on the affected user's next request, including an existing session.

---

## Roles and permissions

| Capability                                      | Public | Citizen | Researcher | Reviewer | Admin |
| ----------------------------------------------- | :----: | :-----: | :--------: | :------: | :---: |
| Browse published promises, parties, elections, statistics | ✓ | ✓ |    ✓      |    ✓     |   ✓   |
| View/download approved evidence                  |   ✓    |    ✓    |     ✓      |    ✓     |   ✓   |
| View pending/rejected evidence queue             |        |         |            |    ✓     |   ✓   |
| Submit evidence                                  |        |    ✓    |     ✓      |    ✓     |   ✓   |
| View own submissions and notifications           |        |    ✓    |     ✓      |    ✓     |   ✓   |
| Record the three evidence review decisions       |        |         |            |    ✓     |   ✓   |
| Resubmit own rejected evidence                   |        |    ✓    |     ✓      |    ✓     |   ✓   |
| Read an evidence audit history                   |        |         |            |    ✓     |   ✓   |
| Create promises (own, starts as draft)           |        |         |     ✓      |          |   ✓   |
| Edit / delete own draft or submitted promises    |        |         |     ✓      |          |   ✓   |
| Add / edit original sources on own promises      |        |         |     ✓      |          |   ✓   |
| Submit own promise for publication review        |        |         |     ✓      |          |   ✓   |
| Review (mark source + wording checked)           |        |         |            |    ✓     |   ✓   |
| Publish / return a reviewed promise to draft     |        |         |            |    ✓     |   ✓   |
| Propose a promise status change (own promises)   |        |         |     ✓      |          |   ✓   |
| Decide status verification proposals             |        |         |            |    ✓     |   ✓   |
| Import candidates from manifesto text            |        |         |     ✓      |          |   ✓   |
| Manage parties and elections                     |        |         |            |          |   ✓   |
| Read the platform-wide audit trail               |        |         |            |          |   ✓   |
| Manage user accounts and roles                   |        |         |            |          |   ✓   |

Anonymous users cannot modify anything and only see **published** promises. Draft, submitted
and reviewed promises are visible to their owner and to reviewers/admins through the
workflow endpoints. Pending and rejected evidence is not publicly listed or downloadable;
the submitter can still download their own document. The public approved-evidence
projection omits submitter email and name.

Ownership and separation-of-duties rules are enforced in the backend, not by hiding buttons:

- A researcher can edit or delete only promises they created, and can delete only while the
  promise is a draft. Admins can act on any promise for exceptional cases, and every action
  is audited.
- A creator can never review or publish their own promise: the review and publish step must
  be performed by a different person.
- A status proposal is decided by a reviewer/admin who is neither the proposer nor the
  uploader of the evidence it relies on. Administrators are not exempt.
- A reviewer cannot review evidence they uploaded themselves.

Administrator safeguards: an admin cannot change their own role or disable their own
account, and the last active administrator cannot be demoted or disabled.

---

## Implemented modules

### 1. Core records and evidence polish
- Party, election, promise and evidence CRUD with friendly validation errors.
- Seven promise statuses: `NOT_ASSESSED` (neutral default: not enough information to
  establish progress), `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`,
  `PARTIALLY_COMPLETED`, `DELAYED`, `CANCELLED`.
- Promise create/update endpoints do not accept implementation statuses. New promises always
  start as `NOT_ASSESSED` with verification state `UNREVIEWED`; the status becomes public
  only through the verification workflow below.
- Delete guards return `409 Conflict` with an explanation instead of a database error.
- Single error format (`ApiError`) with no stack traces exposed; unknown routes return 404.
- Public evidence endpoint exposes approved documents only.

### 2. Authentication and roles
- Session-based login (`POST /api/auth/login`), logout, registration and `GET /api/auth/me`.
- BCrypt password hashing and server-side role authorization.
- Session id rotation on login (session-fixation protection).
- CSRF protection for all state-changing requests using a cookie token echoed in the
  `X-XSRF-TOKEN` header; the SPA retries once with a fresh token if one goes stale.

### 3. Evidence audit history
- Append-only `evidence_audits` table records uploads, approvals and rejections.
- Each entry stores the actor's email and role, the status transition and the review comment.
- Reviewers and admins can expand a document's timeline in the UI.

### 4. Public dashboard, search and analytics
- Aggregated statistics: promise status counts, evidence counts, coverage by party, election
  and state. The public response intentionally contains no audit or reviewer details; those
  live behind the administrator-only audit endpoints.
- Full-text-ish search across title, description and party, plus filters for status, party,
  election, state and "has approved evidence", with pagination.
- Progress bars per party, election and state on the public page.

### 5. Promise status verification
- Two separate concepts are stored per promise: the **implementation status** and a
  **verification state**.
  - Implementation status: `NOT_ASSESSED`, `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`,
    `PARTIALLY_COMPLETED`, `DELAYED`, `CANCELLED`.
  - Verification state: `UNREVIEWED`, `UNDER_REVIEW`, `EVIDENCE_REVIEWED`,
    `STATUS_VERIFIED`, `DISPUTED`.
  - `NOT_ASSESSED` means the record exists but implementation has not been assessed yet. It
    is deliberately distinct from `NOT_STARTED`, which should only be used when evidence
    supports that implementation has not begun.
- `POST`/`PUT /api/promises` reject any implementation status other than the neutral initial
  `NOT_ASSESSED` (or echoing the current status unchanged), so a researcher cannot create or
  edit a promise to `COMPLETED` and bypass review.
- Researchers propose a new promise status through
  `POST /api/status-proposals` (`{ promiseId, proposedStatus, reason, evidenceId }`).
- The proposal must reference an evidence document that a reviewer already approved for that
  promise, so the status change is evidence-backed by construction. `NOT_ASSESSED` is not a
  proposable status.
- Submitting a proposal moves the verification state to `UNDER_REVIEW` (or `DISPUTED` if the
  status was previously verified). Approving evidence documents moves it to
  `EVIDENCE_REVIEWED`; document approval alone does not verify the promise status.
- Reviewers and administrators decide through
  `PUT /api/status-proposals/{id}/decision` (`{ decision, comment }`).
- Only an approval applies the proposed status to the promise, sets the verification state to
  `STATUS_VERIFIED`, and writes a transition into the append-only
  `promise_status_changes` history (old status and new status are both kept). A rejection keeps
  the public status and falls back to `EVIDENCE_REVIEWED` (approved evidence exists) or
  `UNREVIEWED`.
- A promise can have at most one pending status proposal at a time.
- The supporting evidence is **revalidated when the decision is made**, not only when the
  proposal is submitted: it must still exist, still belong to the promise, still be
  `APPROVED`, and its claim review must not be `NOT_SUPPORTED`. This closes the window in
  which evidence could be rejected or replaced after a proposal was raised.
- The decision endpoint refuses a decision by the proposer, and by the person who uploaded
  the evidence the proposal relies on. Those checks apply to administrators too.
- The review response includes the promise wording, party/election, current and proposed
  status, the researcher's reason, the evidence title/type/source/file/status/uploader, and
  the previous status-transition history, so a decision is made with the full context.
- Administrators have a separate, audited exception path for correcting a demonstrably wrong
  status: `PUT /api/promises/{id}/status` (`{ status, reason }`). It requires a reason, writes
  both a status-history row and a `PROMISE_STATUS_ADMIN_CORRECTION` audit entry, and resets the
  verification state to `UNREVIEWED`.

### 6. Independent publication workflow and original sources
- Every promise carries a publication state, independent of its implementation status:

  ```
  DRAFT ──submit──▶ SUBMITTED ──review──▶ REVIEWED ──publish──▶ PUBLISHED
     ▲                   │                    │
     └────── reject ─────┴────────────────────┘
  ```

  - `DRAFT`: visible only to its owner (and admins). New promises start here.
  - `SUBMITTED`: waiting for an independent reviewer.
  - `REVIEWED`: the reviewer has checked the original source and the exact wording.
  - `PUBLISHED`: visible on the public site and included in public statistics/search.
  - Legacy records with no stored publication state are treated as `PUBLISHED`, so existing
    public data does not disappear after the upgrade.
- Only `PUBLISHED` promises appear in `GET /promises`, `GET /promises/search`,
  `GET /promises/{id}` and the public statistics. Draft/submitted/reviewed promises remain
  readable through `GET /promises/mine` and the publication queue.
- `POST /promises/{id}/submit-for-publication` requires at least one original source and
  moves a draft to `SUBMITTED`.
- `PUT /promises/{id}/publication` accepts `{ decision, comment }`:
  - `REVIEWED` moves `SUBMITTED → REVIEWED`.
  - `PUBLISHED` moves `REVIEWED → PUBLISHED` (it cannot skip review).
  - `REJECTED` returns the promise to `DRAFT` and requires a comment explaining why.
- The creator of a promise cannot record the review or publish it, even if they hold the
  reviewer or admin role. The reviewer is looking for the original source and the exact
  promise wording, not for programme delivery.
- Editing the wording, metadata or sources of a non-draft promise returns it to `DRAFT` and
  writes a `PROMISE_REOPENED_FOR_REVIEW` audit entry, so a published claim can never be
  silently rewritten.
- Original sources are first-class records (`promise_sources`) rather than a free-text field:
  `manifestoName`, `sourceUrl`, `pageNumber`, `exactQuotation`, `sourceType` and
  `publishedDate`. Managed with
  `GET|POST /promises/{id}/sources` and `PUT|DELETE /promises/{id}/sources/{sourceId}` by the
  promise owner or an admin.
- Source URLs must be valid `http`/`https` URLs. A promise cannot be submitted for
  publication while it has no source, and the publication queue shows the quotations and
  links so the reviewer can compare them with the promise text.

### 7. Evidence provenance and three-dimensional review
- Uploads (`POST /evidence`, multipart) now record structured provenance: title, evidence
  type, description, official source URL, issuing department, document reference number,
  document date, and the claim scope ("what part of the promise does this support?").
- A SHA-256 hash of the stored file is calculated on upload and kept with the record, so a
  document can be checked for later substitution.
- Uploaded files are still validated by magic bytes (PDF/JPG/PNG) and by size before storage.
- Review is split into three separate decisions instead of one approve/reject switch:
  - **Document review** — `ACCEPTED` / `REJECTED`: may this document be published?
  - **Source review** — `VERIFIED` / `DISPUTED`: is it traceable to the claimed issuer?
  - **Claim review** — `SUPPORTED` / `PARTIAL` / `NOT_SUPPORTED`: does it actually support the
    specific claim?
  - Plus a review comment and verification notes recording what the document proves.
- The overall evidence status is derived from those dimensions: `REJECTED` if the document
  is rejected or the source is disputed; `APPROVED` only when the document is accepted and
  the source verified; otherwise `PENDING`. Publishing a document is never presented as
  proof that the contents are true.
- Self-review is blocked: the uploader cannot review their own document. A review cannot be
  repeated once recorded — the document must be resubmitted first
  (`POST /evidence/{id}/resubmit`, submitter or admin), which returns it to the queue.
- A status proposal may rely only on evidence whose overall status is `APPROVED` and whose
  claim review is not `NOT_SUPPORTED`.
- The public approved-evidence view exposes the document review, source review and claim
  review outcomes but never the submitter or reviewer identity.

### 8. Role-specific dashboards
- `/dashboard` adapts to the signed-in role:
  - **Citizen:** evidence contribution counts, a link back to promises, and the status of
    every document they submitted.
  - **Researcher:** promise form, manifesto import, owned promises and personal evidence.
  - **Reviewer:** review-queue summary and quick access to pending decisions.
  - **Admin:** platform counts, account/party/election links and the same tools as a
    researcher.
- New records are attributed to the account that created them (`createdByEmail/Name` on
  promises, `uploadedByEmail/Name` on evidence).

### 9. Notifications
- In-app notification table with unread counts and a bell in the navigation bar.
- Reviewers and admins are notified when evidence is submitted; submitters are notified
  when their document is approved or rejected.
- Users are notified when an administrator changes their role.
- Mark one or all notifications as read.

### 10. User and role administration
- Admin page lists accounts and lets an administrator change roles or disable accounts.
- Guards keep at least one active administrator and prevent self-lockout.
- Role changes and disabling are applied to existing sessions on the next request, so
  disabling an account revokes access immediately.
- A role change issues an in-app notification to the affected user.
- Audit entries for user management name the **acting administrator** as the actor and keep
  the affected account as the target, with the previous and new role and enabled state
  recorded in the detail. Disabling and re-enabling raise distinct `USER_DISABLED` /
  `USER_ENABLED` events instead of a generic update.

### 11. Manifesto import
- Paste manifesto text; `POST /api/promises/extract` proposes candidate promise sentences
  using a deterministic text heuristic (bullet/number stripping, signal-word detection,
  de-duplication). It is **not** an AI model and not an authoritative reading.
- Nothing is saved until a researcher or admin reviews, selects and confirms the proposals.
  Each saved promise then needs its original source, exact quotation and independent
  publication review.

### 12. Routing and navigation
- React Router routes: `/`, `/parties`, `/elections`, `/login`, `/register`, `/dashboard`,
  `/review`, `/admin` and a not-found page.
- `RequireAuth` and `RequireRole` guards redirect anonymous users to sign-in and show an
  access notice for insufficient roles.

---

## API overview

All endpoints are under `/api`. CSRF is required for `POST`, `PUT` and `DELETE`; first call
`GET /api/auth/csrf` (the SPA does this automatically) and send the returned token in the
`X-XSRF-TOKEN` header.

```
GET    /auth/csrf                          public
POST   /auth/register                      public
POST   /auth/login                         public
POST   /auth/logout                        public
GET    /auth/me                            authenticated

GET    /parties                            public
GET    /parties/{id}                       public
POST   /parties                            admin
PUT    /parties/{id}                       admin
DELETE /parties/{id}                       admin

GET    /elections                          public
GET    /elections/{id}                     public
POST   /elections                          admin
PUT    /elections/{id}                     admin
DELETE /elections/{id}                     admin

GET    /promises                           public (published only)
GET    /promises/search                    public (published only)
GET    /promises/{id}                      public (published only)
GET    /promises/mine                      authenticated
POST   /promises/extract                   researcher, admin
POST   /promises                           researcher, admin
PUT    /promises/{id}                      researcher (owner), admin
DELETE /promises/{id}                      researcher (owner, draft only), admin
GET    /promises/publication-queue         reviewer, admin
POST   /promises/{id}/submit-for-publication  researcher (owner), admin
PUT    /promises/{id}/publication          reviewer, admin
GET    /promises/{id}/sources              public for published promises, otherwise owner/admin
POST   /promises/{id}/sources              researcher (owner), admin
PUT    /promises/{id}/sources/{sourceId}   researcher (owner), admin
DELETE /promises/{id}/sources/{sourceId}   researcher (owner), admin
PUT    /promises/{id}/status               admin (audited status correction)

POST   /status-proposals                    researcher, admin
GET    /status-proposals/pending           reviewer, admin
GET    /status-proposals/promise/{id}     researcher, admin
PUT    /status-proposals/{id}/decision    reviewer, admin

POST   /evidence                           any signed-in user
GET    /evidence/mine                      authenticated
GET    /evidence/pending                   reviewer, admin
GET    /evidence/promise/{id}              reviewer, admin
GET    /evidence/promise/{id}/approved     public
GET    /evidence/{id}/download             public for approved evidence on published promises; owner or reviewer/admin otherwise
PUT    /evidence/{id}/review               reviewer, admin
POST   /evidence/{id}/resubmit             submitter, admin
GET    /evidence/{id}/audit                reviewer, admin

GET    /notifications                      authenticated
POST   /notifications/{id}/read            authenticated
POST   /notifications/read-all             authenticated

GET    /users                              admin
PUT    /users/{id}                         admin

GET    /stats/overview                     public
GET    /audit/evidence/recent              admin
GET    /audit/recent                       admin
```

`GET /promises/search` accepts `q`, `status`, `partyId`, `electionId`, `state`,
`withApprovedEvidence`, `page` and `size`, and only returns published promises.

`POST /promises/extract` accepts `{ "text": "..." }` and returns proposed
`{ title, description }` pairs without saving anything.

`POST /promises/{id}/sources` and `PUT /promises/{id}/sources/{sourceId}` accept
`{ manifestoName, sourceUrl, pageNumber, exactQuotation, sourceType, publishedDate }`.
`sourceType` is `MANIFESTO`, `PARTY_DOCUMENT`, `GOVERNMENT_ANNOUNCEMENT`,
`IMPLEMENTATION_REPORT` or `OTHER`; `sourceUrl` must be `http`/`https`.

`PUT /promises/{id}/publication` accepts `{ decision, comment }` where `decision` is
`REVIEWED`, `PUBLISHED` or `REJECTED`. A rejection requires a comment.

`PUT /evidence/{id}/review` accepts `{ documentReview, sourceReview, claimReview,
reviewComment, verificationNotes }`. A legacy `{ status, reviewComment }` body is still
accepted and maps `APPROVED` to all three positive decisions.

`PUT /users/{id}` accepts optional `role` and `enabled` fields.

Public promise reads (`GET /promises`, `GET /promises/search`, `GET /promises/{id}`)
return a public projection that never contains creator identity; researcher/admin
surfaces such as `GET /promises/mine` keep creator attribution for internal use and
include the publication state and original sources.

---

## Testing

```powershell
cd promiseproof-backend
.\mvnw.cmd test
```

61 backend tests cover promise status handling, promise ownership, the publication
workflow, evidence provenance and audit actor capture, administrator safeguards, manifesto
extraction, and live-session role/status enforcement.

Coverage includes the public promise projection, publication workflow, status verification
workflow, evidence review dimensions, file signature validation, admin bootstrap protection
and the platform audit trail:

- Public promise reads never expose creator email or name; researcher/admin reads retain them.
- Public listings, searches and statistics exclude drafts, submitted and reviewed promises.
- Promise create/update cannot set an implementation status; new promises start `NOT_ASSESSED`
  / `UNREVIEWED` / `DRAFT`, and direct status edits are rejected with `400`.
- Researchers cannot update or delete another researcher's promise, cannot delete a published
  promise, and a promise cannot be submitted for publication without an original source.
- Publication requires prior review (`SUBMITTED → REVIEWED → PUBLISHED`), and the creator
  cannot review or publish their own promise.
- Editing a published promise returns it to `DRAFT` for re-review.
- Status proposals drive the verification state (`UNDER_REVIEW`, `DISPUTED`,
  `STATUS_VERIFIED`, `EVIDENCE_REVIEWED`, `UNREVIEWED`), and approval writes the old/new status
  into the promise status history.
- A decision revalidates the evidence: rejected or deleted evidence blocks approval, evidence
  from another promise is rejected, a `NOT_SUPPORTED` claim review blocks approval, and the
  proposer or evidence uploader cannot decide the proposal.
- Administrative corrections require a reason, write status history plus a
  `PROMISE_STATUS_ADMIN_CORRECTION` audit entry, and reset verification.
- Uploaded evidence must match the allowed PDF/JPG/PNG magic bytes.
- User-role audit entries name the acting administrator and record the previous/new role and
  enabled state, with a distinct `USER_DISABLED` event.
- The backend refuses to seed an administrator without a strong `PROMISEPROOF_ADMIN_PASSWORD`.
- Audit queries use database-level pagination.

```powershell
cd promiseproof-frontend
npm run build
```

---

## Production checklist

This build is a functional demonstration, not a fully hardened deployment:

- CSRF is enabled with a readable `XSRF-TOKEN` cookie. Keep the frontend and backend on the
  same site (or update CORS and cookie settings together).
- Set `PROMISEPROOF_COOKIE_SECURE=true` and serve both tiers over HTTPS in production.
- Uploaded files are stored on the local filesystem (`PROMISEPROOF_UPLOAD_DIR`) and are
  validated against PDF/JPG/PNG magic bytes, with a 10 MB limit. Move to object storage with
  malware scanning when deploying at scale.
- Supply database credentials and the initial administrator password through environment
  variables or a secrets manager; the backend refuses to seed an administrator without a
  strong password.
- Replace `spring.jpa.hibernate.ddl-auto=update` with a migration tool (Flyway/Liquibase).
- Configure a strict CORS origin list via `PROMISEPROOF_CORS_ORIGINS`.
- Serve the frontend over HTTPS and configure the static host to fall back to `index.html`
  for client-side routes such as `/dashboard` and `/admin`.
- Add rate limiting, email verification and password reset.
- Only reviewers and admins can read audit history; consider full tamper-evidence
  (hash-chained audit rows) if the trail must be independently verifiable.
- Administrators can override ownership on promises for exceptional cases. In a real
  deployment, review the admin audit trail periodically and consider requiring a second
  administrator for destructive actions.
- Define a retention and takedown policy for uploaded documents and cited sources, including
  how a disputed source is annotated rather than silently deleted.
- Set a political-eligibility and conflict-of-interest policy for researchers and reviewers,
  and force a password change on first sign-in for seeded or handed-over accounts.

---

## Not yet implemented

- Email or push delivery of notifications (in-app notifications only).
- Password reset, email verification and profile self-service.
- Model-based (AI) manifesto extraction, document OCR and bulk/csv import.
- Rate limiting, malware scanning and object storage for uploads.
- Database migrations (the app currently relies on Hibernate `ddl-auto=update`).
