# API Design

## Goal

Define Retrofit conventions for optional remote features such as model config, metadata sync, or remote recommendations.

Initial release is local-first. APIs are optional and must not block core memory management.

## Retrofit Services

```kotlin
interface ModelConfigApi {
    @GET("v1/model-config")
    suspend fun getModelConfig(): ApiResponse<ModelConfigDto>
}

interface MetadataApi {
    @POST("v1/memories/metadata")
    suspend fun uploadMetadata(
        @Body request: MemoryMetadataRequestDto
    ): ApiResponse<MemoryMetadataResponseDto>
}
```

## DTO Rules

- DTOs live in `data/remote/dto`.
- DTOs must not be exposed to ViewModels.
- Mapping functions convert DTOs to domain models.
- Nullable API fields must be handled explicitly during mapping.

## Result Wrapper

Repository methods should convert network responses to `AppResult`.

```kotlin
sealed interface NetworkError {
    data object Offline : NetworkError
    data object Timeout : NetworkError
    data class Http(val code: Int, val body: String?) : NetworkError
    data class Serialization(val message: String) : NetworkError
}
```

## Error Handling

| Case | Handling |
| --- | --- |
| No network | Continue local processing and show non-blocking message |
| HTTP 401/403 | Disable remote-only feature and log auth/config issue |
| HTTP 5xx | Retry with exponential backoff where safe |
| Serialization error | Treat as remote config failure and keep local defaults |
| Timeout | Cancel request and keep local flow responsive |

## API Constraints

- Do not upload image binaries in initial release.
- Do not require login for core app usage.
- All remote calls must be cancellable.
- Retrofit clients must use configured timeouts.

