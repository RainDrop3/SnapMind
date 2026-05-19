# AI Pipeline

## Goal

Define the on-device OCR, classification, preprocessing, and tagging pipeline used to enrich imported image memories.

## Pipeline Overview

```text
Image URI
  -> Image validation
  -> Bitmap decode with size bounds
  -> OCR preprocessing
  -> ML Kit text recognition
  -> TFLite preprocessing
  -> TFLite inference
  -> Tag rule engine
  -> Room persistence
```

## OCR Flow

1. Load image from local URI or copied file.
2. Create `InputImage` for ML Kit.
3. Run text recognition on a background dispatcher.
4. Normalize recognized text:
   - trim whitespace
   - remove duplicated blank lines
   - preserve line order
   - store full text and block-level text
5. Persist OCR result and confidence/status metadata.

## TensorFlow Lite Inference Flow

1. Decode bitmap using target model input size.
2. Correct orientation using EXIF where available.
3. Resize and normalize pixels.
4. Run interpreter with configured thread count.
5. Convert logits/probabilities to category predictions.
6. Store top category and top-N classification candidates.

## Preprocessing Rules

| Step | Requirement |
| --- | --- |
| Decode | Avoid loading full-size bitmap into memory when not needed |
| Resize | Match model input dimensions exactly |
| Normalize | Use model-specific mean/std from `ml-training-spec.md` |
| Orientation | Apply EXIF rotation before OCR/classification |
| Large Images | Downsample for AI processing, keep original stored reference |

## Auto-Tag Sources

- OCR keyword extraction.
- TFLite classification label.
- Filename or share metadata.
- User-defined tags.
- Date/time context.

## Processing Status

Persist status fields on `MemoryItem`:

- `ocrStatus`: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`
- `classificationStatus`: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`
- `taggingStatus`: `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`

## Model Update Strategy

Initial release uses bundled TFLite assets in `app/src/main/assets`.

Future remote update rules:

- Download only on Wi-Fi unless user allows mobile data.
- Verify checksum before replacing local model.
- Keep previous model version until new model loads successfully.
- Store active model version in preferences or Room metadata.

