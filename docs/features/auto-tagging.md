# Auto-Tagging

## Goal

Automatically generate useful tags from three sources — ML Kit OCR text, TFLite classification labels, and Google Cloud Vision API visual labels — so users can organize memories with minimal manual work.

## User Scenario

1. User imports a receipt screenshot.
2. ML Kit OCR extracts merchant name and amount text.
3. TFLite CNN predicts category `receipt`.
4. Vision API returns labels: `["Receipt", "Paper", "Font"]`.
5. App assigns tags: `receipt`, `finance`, and merchant-derived keyword tags.
6. User can filter by these tags or remove incorrect ones.

## Tag Sources

| Source | Method | Example |
| --- | --- | --- |
| ML Kit OCR | Keyword extraction from recognized text | merchant name, URL |
| TFLite classification | Category-to-tag mapping | `receipt` → `receipt`, `finance` |
| Google Cloud Vision API | High-confidence visual labels (score ≥ 0.80) | `Food`, `Screenshot` |
| Share metadata | Filename or source app package | — |
| User-defined | Manual tag assignment from detail screen | — |

## Functional Requirements

- Generate tags after OCR, TFLite classification, and Vision API labeling complete.
- De-duplicate tags case-insensitively across all sources.
- Track tag source (`OCR` / `TFLITE` / `VISION` / `USER`).
- Persist generated tags and user-added tags in `TagEntity` + `MemoryTagCrossRef`.
- Allow user to remove generated tags.
- Allow user to create, rename, and delete user-managed tags.
- Allow user to assign tags to memories from detail screen.
- Expose top-3 popular tags for drawer quick navigation.
- Re-run auto-tagging when processing is retried.

## Non-Functional Requirements

- Tag generation (rule engine step) must complete under 300 ms for normal OCR text.
- Rules are deterministic and unit-testable.
- Vision API labels filtered by score threshold (default 0.80) to avoid noisy tags.
- Maximum tags per memory item: 20 (combined from all sources).

## Processing Flow

```text
OCR result + TFLite result + Vision API labels
  → Keyword extraction from OCR text
  → Category-to-tag mapping from TFLite label
  → Vision label filtering (score >= 0.80) and normalization
  → Tag de-duplication (case-insensitive)
  → Cap at max tag limit (20)
  → TagEntity upsert
  → MemoryTagCrossRef insert
  → MemoryItem.taggingStatus = SUCCESS
```

## Database Interaction

- Read `ocr_texts`, `classifications`, `vision_labels`.
- Upsert `tags` (with source field).
- Insert/delete `memory_tag_cross_refs`.
- Update user-managed tag metadata on create/rename/delete.
- Update `memory_items.taggingStatus`.

## API Interaction

- Consumes Vision API label results (produced by Vision API pipeline step).
- Tagging step runs after Vision API call completes.
- If Vision API fails (`visionLabelStatus = FAILED`), tagging continues with OCR + TFLite sources only.

## Error Handling

| Error | Handling |
| --- | --- |
| Missing OCR result | Generate TFLite + Vision-only tags |
| Missing TFLite result | Generate OCR + Vision-only tags |
| Vision API failed | Generate OCR + TFLite-only tags |
| Tag DAO failure | Mark `taggingStatus = FAILED`, allow retry |
| Excessive tag count | Apply max limit (20), drop lowest-priority tags |
| User renames tag to duplicate name | Block rename and show validation message |
| User deletes tag used by active filter | Remove filter and refresh list |

## Edge Cases

- Same tag appears from multiple sources (OCR + Vision) → de-duplicate, track all sources.
- User removed a generated tag and auto-tagging runs again on retry → do not re-add removed tag.
- Vision API returns language-specific labels → normalize to consistent casing.
- OCR text contains private numbers or IDs → tag rule engine should avoid noisy numeric tags.

## Dependencies

- Room.
- ML Kit OCR feature.
- TFLite classification feature.
- Google Cloud Vision API (via Retrofit).
- Coroutine dispatchers.

## TODO Checklist

- [ ] Define tag normalization rules.
- [ ] Define OCR keyword extraction rules.
- [ ] Define TFLite category-to-tag mapping.
- [ ] Implement Vision API label filtering (score threshold).
- [ ] Implement tag de-duplication with source tracking.
- [ ] Add tag DAO upsert with source field.
- [ ] Add generated/user tag source tracking.
- [ ] Add user tag create/rename/delete support.
- [ ] Add manual tag assignment/removal support.
- [ ] Add top-3 popular tag query for drawer shortcut.
- [ ] Add tag chips UI in list and detail.
- [ ] Add tag filter query.
- [ ] Add unit tests for rule engine.

## Current Status

Not Started

## Future Improvements

- User-configurable tag rules.
- Learned tag suggestions based on user edits.
- Entity extraction for dates, prices, URLs, and locations.
