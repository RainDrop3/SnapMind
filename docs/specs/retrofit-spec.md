# Retrofit Specification

## Base Rules

- Core app behavior must not require network access.
- Retrofit is used only for optional remote configuration or future sync.
- DTOs are mapped to domain models before reaching ViewModels.
- Errors are converted to `AppResult.Error`.

## Client Configuration

| Setting | Value |
| --- | --- |
| Connect timeout | 10 seconds |
| Read timeout | 15 seconds |
| Write timeout | 15 seconds |
| Serialization | Kotlinx Serialization or Moshi |
| Logging | Debug builds only |

## Model Config Endpoint

```http
GET /v1/model-config
```

### Response

```json
{
  "activeModelVersion": "1.0.0",
  "minSupportedAppVersion": 1,
  "classificationThreshold": 0.65,
  "labelVersion": "2026-05-19"
}
```

## Metadata Endpoint

```http
POST /v1/memories/metadata
```

### Request

```json
{
  "localMemoryId": "123",
  "ocrTextLength": 248,
  "topCategory": "receipt",
  "tags": ["receipt", "finance"]
}
```

### Response

```json
{
  "accepted": true,
  "serverMetadataId": "meta_123"
}
```

## Error Response

```json
{
  "code": "INVALID_REQUEST",
  "message": "Request body is invalid"
}
```

