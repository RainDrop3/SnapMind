# ML Training Specification

## Goal

Document the expected classification model contract used by the Android TFLite runtime.

## Initial Model Scope

The first release should classify broad memory categories:

- `chat`
- `receipt`
- `document`
- `product`
- `travel`
- `food`
- `event`
- `code`
- `unknown`

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

