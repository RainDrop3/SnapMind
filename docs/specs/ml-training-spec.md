# ML Training Specification

## Goal

Document the expected classification model contract used by the Android TFLite runtime.

## Initial Model Scope

The model is trained on **8 output classes** (the `unknown` class was removed; the
model no longer learns or predicts an "unknown" category):

- `chat`
- `receipt`
- `code`
- `shopping`
- `travel`
- `food`
- `document`
- `youtube`

### App-level `unknown` category

`unknown` remains a valid **app-level** memory category (`MemoryCategory.UNKNOWN`),
but it is no longer produced by the model. The Android runtime derives it via
**Confidence Thresholding**: when the top-1 probability is below the threshold,
the app overrides the predicted label with `unknown` (see Android Runtime Contract).

## Android Runtime Contract

| Field | Requirement |
| --- | --- |
| Format | TensorFlow Lite `.tflite` |
| Input | RGB bitmap tensor |
| Input size | Defined by model metadata, target `224x224` unless changed |
| Output | Float probability array |
| Labels | One label per output index |
| Threshold | Default `0.65` for selected category |
| Unknown behavior | Use `unknown` below threshold |

## Preprocessing

- Apply EXIF orientation before resize.
- Resize to model input dimensions.
- Normalize according to model metadata.
- Do not crop important screenshot edges unless model requires center crop.

## Evaluation Targets

| Metric | Target |
| --- | --- |
| Top-1 accuracy | 80% or higher on validation set |
| Receipt precision | 90% or higher |
| Document precision | 85% or higher |
| Average inference time | Under 1 second on mid-range Android device |

## Dataset Requirements

- Include both screenshots and camera photos.
- Include dark mode and light mode screenshots.
- Include multilingual OCR-heavy screenshots where possible.
- Avoid storing sensitive personal screenshots in the repo.
- Keep dataset outside app source unless using sanitized samples.

## Versioning

Model asset naming:

```text
image_classifier_v1_0_0.tflite
image_classifier_labels_v1_0_0.txt
```

Record model version in every `ClassificationEntity`.
