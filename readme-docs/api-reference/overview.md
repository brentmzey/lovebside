---
title: "API Overview"
excerpt: "B-Side REST API and WebSocket reference"
category: "api-reference"
slug: "api-overview"
order: 1
---

# API Reference

B-Side provides a comprehensive REST API and real-time WebSocket/SSE connections for building messaging applications.

## Base URLs

| Environment | Base URL | Protocol |
|-------------|----------|----------|
| **Local Development** | `http://localhost:8092` | HTTP |
| **Production** | `https://api.bside.app` | HTTPS |

## Architecture

B-Side uses a dual-layer API architecture:

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Ktor Server │ (Port 8081)
│ API Gateway │ - Rate limiting
│             │ - Auth validation
│             │ - Request logging
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ PocketBase  │ (Port 8092)
│  Database   │ - Data storage
│             │ - Real-time sync
│             │ - File uploads
└─────────────┘
```

## Authentication

B-Side uses **JWT token-based authentication** with refresh tokens.

### Authentication Flow

1. **Login/Register** → Get access token + refresh token
2. **API Requests** → Include access token in header
3. **Token Expired** → Use refresh token to get new access token

### Headers

All authenticated requests must include:

```http
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json
```

### Example: Login

[block:code]
{
  "codes": [
    {
      "code": "curl -X POST http://localhost:8092/api/collections/users/auth-with-password \\\n  -H \"Content-Type: application/json\" \\\n  -d '{\n    \"identity\": \"user@example.com\",\n    \"password\": \"securepassword\"\n  }'",
      "language": "curl"
    },
    {
      "code": "const response = await fetch('http://localhost:8092/api/collections/users/auth-with-password', {\n  method: 'POST',\n  headers: { 'Content-Type': 'application/json' },\n  body: JSON.stringify({\n    identity: 'user@example.com',\n    password: 'securepassword'\n  })\n});\nconst data = await response.json();",
      "language": "javascript"
    },
    {
      "code": "val client = HttpClient()\nval response = client.post(\"http://localhost:8092/api/collections/users/auth-with-password\") {\n    contentType(ContentType.Application.Json)\n    setBody(LoginRequest(\n        identity = \"user@example.com\",\n        password = \"securepassword\"\n    ))\n}",
      "language": "kotlin"
    }
  ]
}
[/block]

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "record": {
    "id": "user123",
    "email": "user@example.com",
    "username": "johndoe",
    "name": "John Doe",
    "avatar": "https://..."
  }
}
```

## Rate Limiting

| Tier | Requests/Minute | Burst |
|------|-----------------|-------|
| **Anonymous** | 20 | 5 |
| **Authenticated** | 100 | 20 |
| **Premium** | 500 | 50 |

Rate limit headers included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1640995200
```

## Error Handling

All errors follow this format:

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": {
    "email": {
      "code": "validation_required",
      "message": "Email is required"
    }
  }
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| `200` | OK | Request succeeded |
| `201` | Created | Resource created |
| `400` | Bad Request | Invalid request data |
| `401` | Unauthorized | Missing or invalid token |
| `403` | Forbidden | Insufficient permissions |
| `404` | Not Found | Resource doesn't exist |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Server error |

## Core Endpoints

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/collections/users/auth-with-password` | Login |
| `POST` | `/api/collections/users/records` | Register |
| `GET` | `/api/collections/users/records/{id}` | Get user |
| `PATCH` | `/api/collections/users/records/{id}` | Update user |

### Messages

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/collections/messages/records` | List messages |
| `POST` | `/api/collections/messages/records` | Send message |
| `GET` | `/api/collections/messages/records/{id}` | Get message |
| `PATCH` | `/api/collections/messages/records/{id}` | Update message |
| `DELETE` | `/api/collections/messages/records/{id}` | Delete message |

### Conversations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/collections/conversations/records` | List conversations |
| `POST` | `/api/collections/conversations/records` | Create conversation |
| `GET` | `/api/collections/conversations/records/{id}` | Get conversation |
| `PATCH` | `/api/collections/conversations/records/{id}` | Update conversation |

## Real-Time Subscriptions

B-Side uses **Server-Sent Events (SSE)** for real-time updates.

### Subscribe to Messages

```javascript
const eventSource = new EventSource(
  'http://localhost:8092/api/realtime',
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);

// Subscribe to a conversation
eventSource.addEventListener('PB_CONNECT', (e) => {
  const clientId = JSON.parse(e.data).clientId;
  
  // Send subscription request
  fetch('http://localhost:8092/api/realtime', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      clientId,
      subscriptions: ['messages', 'conversations']
    })
  });
});

// Listen for new messages
eventSource.addEventListener('messages', (e) => {
  const message = JSON.parse(e.data);
  console.log('New message:', message);
});
```

### Event Types

| Event | Trigger | Payload |
|-------|---------|---------|
| `PB_CONNECT` | Client connects | `{ clientId }` |
| `messages` | New/updated message | `Message` object |
| `conversations` | Conversation update | `Conversation` object |
| `typing` | User typing | `{ userId, conversationId }` |
| `presence` | User online/offline | `{ userId, status }` |

## Pagination

All list endpoints support pagination:

```http
GET /api/collections/messages/records?page=1&perPage=20
```

**Query Parameters:**
- `page`: Page number (default: 1)
- `perPage`: Items per page (default: 30, max: 100)
- `sort`: Sort field (prefix with `-` for descending)
- `filter`: Filter expression

**Response:**
```json
{
  "page": 1,
  "perPage": 20,
  "totalItems": 156,
  "totalPages": 8,
  "items": [...]
}
```

## Filtering

Use PocketBase filter syntax:

```http
# Get unread messages
GET /api/collections/messages/records?filter=(read=false)

# Get messages from specific user
GET /api/collections/messages/records?filter=(author='user123')

# Complex filter
GET /api/collections/messages/records?filter=(conversation='conv123'%26%26created>'2024-01-01')
```

## File Uploads

Upload files (images, documents) to messages:

```javascript
const formData = new FormData();
formData.append('text', 'Check out this image!');
formData.append('conversation', conversationId);
formData.append('attachments', fileBlob, 'image.jpg');

await fetch('http://localhost:8092/api/collections/messages/records', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});
```

## SDK Support

Official SDKs:

- **Kotlin Multiplatform**: Built-in (`pocketbase-kt-sdk`)
- **JavaScript/TypeScript**: `pocketbase`
- **Dart**: `pocketbase`

```kotlin
// Kotlin example
val pb = PocketBase("http://localhost:8092")
val authData = pb.collection("users").authWithPassword(
    identity = "user@example.com",
    password = "password"
)
```

## API Playground

Test the API interactively:
- **Local**: http://localhost:8092/_/
- **Production**: https://api.bside.app/_/

---

> 📘 Need More Details?
> 
> Check out the detailed endpoint documentation:
> - [Authentication](authentication)
> - [Messages](endpoints/messages)
> - [Users](endpoints/users)
> - [WebSockets](websockets)
