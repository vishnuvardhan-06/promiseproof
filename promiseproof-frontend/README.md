# PromiseProof frontend

React 19 + Vite client for the PromiseProof platform.

```powershell
npm install
npm run dev      # http://localhost:5173
npm run build    # production bundle in dist/
npm run lint
```

The dev server expects the Spring Boot API on `http://localhost:8080` (see
`src/services/http.js`). Session cookies are sent with every request, so run the backend
as well; the app itself has no offline or mock mode.

For a production build, set `VITE_API_BASE_URL` to the API path or origin used by your
deployment. If it is unset, the app uses `http://localhost:8080/api`.

Routes: `/` public record, `/parties`, `/elections`, `/login`, `/register`, `/dashboard`
(role-aware), `/review` (reviewer/admin, evidence queue, promise publication queue and
promise status proposals), `/admin` (admin). The HTTP helper fetches the CSRF token from
`/api/auth/csrf` and sends it as `X-XSRF-TOKEN` on writes.

Researchers create promises as drafts, add the original source and exact quotation, then
submit the promise for independent publication review. Reviewers compare the source with
the wording on `/review`, mark it reviewed and publish it; the creator can never review or
publish their own promise, and edited credentials send it back to draft.

Evidence uploads collect the issuing department, document reference and date, official URL
and claim scope, and the server stores a SHA-256 hash of the file. Reviewers record three
separate decisions (document, source, claim) rather than a single approve/reject switch;
citizens and researchers can resubmit their own rejected document.

Researchers also propose promise status changes from a promise on their dashboard when
approved evidence exists; reviewers decide those proposals on the review page. The public
status only changes after an approval, supported evidence is revalidated at decision time,
and the proposer cannot decide their own proposal.

See the [project README](../README.md) for setup, roles, API reference and the
production checklist.
