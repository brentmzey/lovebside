# Architecture: Federated Backbone

## Overview

The B-Side platform employs a "Federated" architecture to balance rapid development with high-performance computational capabilities. This approach splits responsibilities between a "Boring" backend (PocketBase) and an "Exciting" backend (Ktor).

## Components

### 1. The Gateway (Nginx)

The entry point for all client traffic. It intelligently routes requests based on the URL path.

- **Role**: Load Balancer, SSL Termination, Routing.
- **Routing Rules**:
  - `/api/pb/*` -> **PocketBase** (Direct DB Access, Auth, Realtime)
  - `/api/v1/*` -> **Ktor** (Matching, heavy logic)
  - `/static/*` -> **CDN/S3** (Media)

### 2. The "Boring" Backend (PocketBase)

Handles the standard CRUD operations, Authentication, and Real-time subscriptions.

- **Why?**: It's fast, built on SQLite (WAL mode), and provides "backend-in-a-box" features for 90% of app needs.
- **Responsibilities**:
  - User Authentication & Management.
  - Messaging (Chat) CRUD & Subs.
  - Simple Profile updates.

### 3. The "Exciting" Backend (Ktor)

Handles the complex business logic that requires heavy computation or custom algorithms.

- **Why?**: Kotlin-based, type-safe, and highly performant for algorithmic tasks. Uses `coroutines` for concurrency.
- **Responsibilities**:
  - **Affinity Matching**: Calculating compatibility scores based on Proust Questionnaire.
  - **Geometric Profiles**: Generating multi-dimensional profile shapes.
  - **Job Queue**: Processing background tasks safely without blocking the main API.

### 4. Storage (S3 / CDN)

Offloads heavy media storage from the application servers.

- **Local Dev**: Mocked via local filesystem or MinIO.
- **Prod**: AWS S3 + CloudFront (or similar).
- **Optimization**: Images are resized/compressed before upload or on-the-fly via Lambda@Edge.

## Diagram

```mermaid
graph TD
    Client[Client App (KMP)] --> Nginx
    Nginx -- "/api/pb/*" --> PB[PocketBase]
    Nginx -- "/api/v1/*" --> Ktor[Ktor Server]
    Nginx -- "/static/*" --> CDN[S3 / CDN]
    
    Ktor --> PB_DB[(SQLite / PBase Data)]
    PB --> PB_DB
```

## Future Considerations

- **Load Balancing**: Multiple Ktor instances can be spun up behind Nginx. PocketBase (SQLite) is vertical-scale primarily, but Litestream can confirm replication.
