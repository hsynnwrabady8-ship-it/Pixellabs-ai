# Security Specification — PixelLab AI Studio

## 1. Data Invariants
1. **Authentication Mandatory**: No unauthenticated user may read, list, create, update, or delete any document under `/users/{userId}/projects/{projectId}`.
2. **Strict Tenant Isolation**: A user (`request.auth.uid`) may only access documents under their own `/users/{userId}/projects/{projectId}` path where `request.auth.uid == userId` and `data.userId == request.auth.uid`.
3. **Immutable Identity & Creation Timestamp**: On updates, `userId` and `createdAt` must remain strictly equal to `resource.data.userId` and `resource.data.createdAt`.
4. **Strict Schema & Bounds Validation**: Every string, integer, boolean, list, and timestamp field is validated via `isValidProject(data)` on both `create` and `update`, with explicit `. hasOnly(...)` key allowlists.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/projects").get()` -> Reject
2. **Cross-User Read (PII/Tenant Leak)**: `bobDb.doc("users/alice/projects/p1").get()` -> Reject
3. **Identity Spoofing on Create**: Alice creates `users/alice/projects/p1` with `userId: "bob"` -> Reject
4. **Shadow Field Injection on Create**: Payload includes `"isAdmin": true` -> Reject via `keys().hasOnly(...)`
5. **Shadow Field Injection on Update**: Update payload includes `"isVerified": true` -> Reject via `affectedKeys().hasOnly(...)`
6. **Value Poisoning on Update**: Update `canvasWidth` to `999999` or `"wide"` -> Reject via `isValidProject(incoming())`
7. **Resource Poisoning (Oversized String)**: `title` with 500 chars (> 120 max) -> Reject
8. **ID Poisoning**: Document ID with invalid characters or > 128 chars -> Reject via `isValidId(projectId)`
9. **Immutable Field Tampering (`createdAt`)**: Update changes `createdAt` to a new timestamp -> Reject
10. **Immutable Field Tampering (`userId`)**: Update changes `userId` to another UID -> Reject
11. **Unbounded Array Attack**: `layerTitles` with 60 elements (> 50 max) or non-string element -> Reject
12. **Future Timestamp Spoofing**: `createdAt` or `updatedAt` in the future (`> request.time`) -> Reject
