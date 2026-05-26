# AI Pipeline

## Goal

Define the full AI processing pipeline used to enrich imported image memories: on-device OCR and TFLite classification, followed by remote Vision API labeling and Gemini API memo recommendation.

## Pipeline Overview

```text
Image URI
  → Image validation
  → Bitmap decode with size bounds
  → ML Kit OCR preprocessing
  → ML Kit text recognition          [on-device]
  → TFLite preprocessing
  → TFLite CNN inference             [on-device]
  → Vision API label request         [remote · Retrofit]
  → Gemini API memo recommendation   [remote · Retrofit]
  → YouTube title search             [remote · Retrofit · if youtube category]
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
6. Store top category, confidence score, and top-N candidates.

Categories: `chat` · `receipt` · `code` · `shopping` · `travel` · `food` · `document`

## Vision API Flow (Google Cloud Vision)

1. Encode resized image as base64 or send URI (per API requirement).
2. POST to Vision API `annotate` endpoint with `LABEL_DETECTION` and `TEXT_DETECTION` features.
3. Parse label annotations sorted by score.
4. Map high-confidence labels to normalized tag strings.
5. Merge with OCR-derived and TFLite-derived tags; de-duplicate.
6. Persist tags in `TagEntity` + `MemoryTagCrossRef`.

## Gemini API Flow (Memo Recommendation)

1. Send image (or description derived from OCR + classification) to Gemini API.
2. Prompt: "이 이미지를 저장한 이유를 한 문장으로 추천해주세요."
3. Receive candidate memo sentence.
4. Display recommendation in detail screen; user accepts, edits, or dismisses.
5. Persist final memo in `MemoEntity`.

## YouTube Data API Flow

Triggered only when TFLite category is `youtube` (or similar classifier output).

1. ML Kit OCR extracts candidate video title from screenshot text.
2. Send title as query to YouTube Data API v3 `search.list`.
3. Parse first result's `videoId`.
4. Generate deep-link URL: `https://www.youtube.com/watch?v={videoId}`.
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
| `geminiMemoStatus` | `PENDING` · `RUNNING` · `SUGGESTED` · `ACCEPTED` · `DISMISSED` · `FAILED` |
| `taggingStatus` | `PENDING` · `RUNNING` · `SUCCESS` · `FAILED` |

## Error and Degradation Strategy

| Step | Failure Handling |
| --- | --- |
| ML Kit OCR fails | Continue with empty OCR; classification-only tags |
| TFLite fails | Mark `FAILED`; user can retry from detail screen |
| Vision API offline/error | Skip label generation; OCR + TFLite tags only |
| Gemini API offline/error | Skip recommendation; user writes memo manually |
| YouTube API offline/error | Hide deep-link button; show retry option |

## Model Update Strategy

Initial release uses bundled TFLite assets in `app/src/main/assets`.

Future remote update rules:

- Download only on Wi-Fi unless user allows mobile data.
- Verify checksum before replacing local model.
- Keep previous model version until new model loads successfully.
- Store active model version in preferences or Room metadata.
