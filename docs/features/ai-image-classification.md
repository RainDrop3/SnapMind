# AI Image Classification

## Goal

Classify imported screenshots/photos into useful categories so users can filter memories by content type without manual organization.

## User Scenario

1. User saves an image to SnapMind.
2. App processes the image in the background.
3. App assigns a category such as `receipt`, `chat`, `document`, `product`, `travel`, or `unknown`.
4. User filters the memory list by category.

## Functional Requirements

- Load bundled `.tflite` model from app assets.
- Run inference after image import.
- Store top category, confidence, model version, and candidate labels.
- Mark classification status as `SUCCESS` or `FAILED`.
- Allow retry from memory detail screen when classification fails.
- Use `unknown` when confidence is below threshold.

## Non-Functional Requirements

- Inference must run off the main thread.
- Single image inference target: under 1 second on mid-range devices.
- TFLite interpreter must be reused where safe.
- Memory usage must be bounded during bitmap decode.

## UI Structure

- Memory list category chip.
- Memory detail classification row.
- Search/filter category selector.
- Retry processing action when failed.

## Processing Flow

```text
MemoryItem pending classification
  -> Bitmap decode
  -> Resize/normalize
  -> TFLite inference
  -> Confidence thresholding
  -> ClassificationEntity insert
  -> MemoryItem status update
```

## Data Flow

- Input: local image URI.
- Output: list of classification predictions and selected display category.

## Database Interaction

- Read `memory_items.imageUri`.
- Insert rows into `classifications`.
- Update `memory_items.classificationStatus`.
- Query classifications for category filter.

## API Interaction

No API dependency for initial release. Future remote model config may use `ModelConfigApi`.

## AI/OCR Logic

- Decode bitmap with orientation correction.
- Resize to model input dimensions.
- Normalize pixels using model-specific mean/std.
- Run TFLite interpreter.
- Map output indices to labels.
- Apply confidence threshold from `specs/ml-training-spec.md`.

## Error Handling

| Error | Handling |
| --- | --- |
| Model load failure | Mark status `FAILED`, log model version |
| Bitmap decode failure | Mark status `FAILED`, keep memory usable |
| Inference exception | Mark status `FAILED`, expose retry |
| Low confidence | Store `unknown` with candidate list |

## Edge Cases

- Screenshot contains mostly text.
- Image is too small for meaningful classification.
- Transparent image background.
- Model labels do not match app category enum.

## Dependencies

- TensorFlow Lite.
- Android Bitmap APIs.
- Room.
- Coroutine dispatchers.

## TODO Checklist

- [ ] Add TFLite dependency and asset loading.
- [ ] Define model labels file format.
- [ ] Implement bitmap preprocessing.
- [ ] Implement classifier interface.
- [ ] Persist classification results.
- [ ] Add category filter support.
- [ ] Add retry action.
- [ ] Add unit tests for output thresholding.
- [ ] Add instrumentation smoke test with sample asset.

## Current Status

Not Started

## Progress Notes

- Classification categories should be stable before UI filter chips are finalized.

## Future Improvements

- Add remote model config.
- Add user correction feedback for category quality.
- Add multiple specialized models for screenshots versus camera photos.

