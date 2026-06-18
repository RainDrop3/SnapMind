# AI Pipeline

## Goal

Define the full AI processing pipeline used to enrich imported image memories: on-device OCR and TFLite classification, followed by optional remote Vision API labeling and optional Gemini API memo recommendation.

## Pipeline Overview

```text
Image URI
  → Image validation
  → Bitmap decode with size bounds
  → ML Kit OCR preprocessing
  → ML Kit text recognition          [on-device]
  → TFLite preprocessing
  → TFLite CNN inference             [on-device]
  → Vision API label request         [remote · Retrofit · if enabled]
  → (Gemini memo recommendation is NOT auto-run; on-demand via detail screen button)
  → YouTube title search             [remote · Retrofit · if youtube category and enabled]
  → Tag rule engine (OCR + Vision + TFLite)
  → Room persistence (FTS indexed)
```

All steps run on Coroutine background dispatchers and do not block the UI thread.

## OCR Flow (ML Kit)

1. Load image from app-private URI.
2. Create `InputImage` for ML Kit.
3. Run `TextRecognizer` on IO/Default dispatcher.
4. Normalize recognized text:
   - trim whitespace
   - remove duplicated blank lines
   - preserve line order
   - store full text and block-level text
5. Persist OCR result and status metadata.
6. Feed OCR text into tag rule engine and YouTube title matcher.

## TFLite Classification Flow

1. Decode bitmap using target model input size.
2. Correct orientation using EXIF where available.
3. Resize and normalize pixels per model spec.
4. Run interpreter with configured thread count.
5. Convert logits to category predictions.
6. Apply confidence thresholding: if the top-1 probability is below the threshold
   (`0.65`), override the predicted label with `unknown`.
7. Store top category, confidence score, and top-N candidates.

Model output classes (8): `chat` · `receipt` · `code` · `shopping` · `travel` · `food` · `document` · `youtube`

App-level categories add `unknown`, which is derived at runtime via thresholding rather than emitted by the model.

## Vision API Flow (Google Cloud Vision)

1. Confirm remote enrichment is enabled and an API key is configured.
2. Decode a bounded bitmap and re-encode a downsampled JPEG/WebP payload for upload.
3. POST to Vision API `annotate` endpoint with `LABEL_DETECTION` only.
4. Parse label annotations sorted by score.
5. Persist `VisionLabelEntity` rows for audit/debug.
6. Map high-confidence labels to normalized tag strings.
7. Merge with OCR-derived and TFLite-derived tags; de-duplicate.
8. Persist tags in `TagEntity` + `MemoryTagCrossRef`.

## Gemini API Flow (Memo Recommendation)

Gemini recommendation is **on-demand only** — it is no longer part of the automatic import pipeline.
The remote enrichment worker marks `geminiMemoStatus = SKIPPED` at import; the suggestion is produced
only when the user taps the "Gemini 메모 추천받기" button on the detail screen
(`GeminiMemoSuggester`, called from `DetailViewModel.requestGeminiSuggestion()`).

1. User taps the recommendation button on the detail screen.
2. Confirm Gemini recommendation is enabled (settings switch) and an embedded API key is present.
3. Send a downsampled image payload to Gemini API with a short prompt.
4. Receive candidate memo sentence.
5. Persist the suggestion in `MemoEntity.geminiSuggestion` and set `geminiMemoStatus = SUGGESTED`.
6. Display recommendation chip in detail screen; user accepts, edits, or dismisses.
7. Persist final memo in `MemoEntity.body`.

## YouTube Data API Flow

Triggered only when TFLite category is `youtube` and YouTube search is enabled.

1. ML Kit OCR extracts candidate video title from screenshot text.
2. Send only the selected candidate title as query to YouTube Data API v3 `search.list`.
3. Parse first result's `videoId`.
4. Persist `YoutubeLinkEntity` with `videoId`, title, and URL.
5. Display "▶ 영상 바로 이동" button in detail screen.
6. Tapping opens YouTube app or browser via `Intent.ACTION_VIEW`.

## Preprocessing Rules

| Step | Requirement |
| --- | --- |
| Decode | Avoid loading full-size bitmap when not needed |
| Resize | Match TFLite model input dimensions exactly |
| Normalize | Use model-specific mean/std from `ml-training-spec.md` |
| Orientation | Apply EXIF rotation before OCR/classification |
| Large Images | Downsample for AI/API processing; keep original stored reference |
| Remote Payloads | Upload only bounded, re-encoded image bytes for Vision/Gemini; do not include OCR text or memo body in those requests |

## Auto-Tag Sources

| Source | Method |
| --- | --- |
| OCR text | Keyword extraction from recognized text |
| TFLite label | Category-to-tag mapping |
| Vision API labels | High-confidence visual labels from Google Cloud Vision |
| Share metadata | Filename or source app package |
| Date/time | Time-based context tags |
| User-defined | Manual tag assignment |

## Processing Status Fields

Persist status on `MemoryItem`:

| Field | States |
| --- | --- |
| `ocrStatus` | `PENDING` · `RUNNING` · `SUCCESS` · `FAILED` |
| `classificationStatus` | `PENDING` · `RUNNING` · `SUCCESS` · `FAILED` |
| `visionLabelStatus` | `PENDING` · `RUNNING` · `SUCCESS` · `FAILED` · `SKIPPED` |
| `geminiMemoStatus` | `PENDING` · `RUNNING` · `SUGGESTED` · `ACCEPTED` · `DISMISSED` · `FAILED` · `SKIPPED` |
| `youtubeLinkStatus` | `PENDING` · `RUNNING` · `SUCCESS` · `FAILED` · `SKIPPED` |
| `taggingStatus` | `PENDING` · `RUNNING` · `SUCCESS` · `FAILED` |

## Error and Degradation Strategy

| Step | Failure Handling |
| --- | --- |
| ML Kit OCR fails | Continue with empty OCR; classification-only tags |
| TFLite fails | Mark `FAILED`; user can retry from detail screen |
| Vision API offline/error | Skip label generation; OCR + TFLite tags only |
| Gemini API offline/error | Skip recommendation; user writes memo manually |
| YouTube API offline/error | Hide deep-link button; show retry option |
| Remote feature disabled | Mark the relevant status `SKIPPED`; local OCR/classification/tagging continues |

## Model Update Strategy

Initial release uses bundled TFLite assets in `app/src/main/assets`.

Future remote update rules:

- Download only on Wi-Fi unless user allows mobile data.
- Verify checksum before replacing local model.
- Keep previous model version until new model loads successfully.
- Store active model version in preferences or Room metadata.
