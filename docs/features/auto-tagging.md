# Auto-Tagging

## Goal

Automatically generate useful tags from OCR text, classification output, and rule-based heuristics so users can organize memories with minimal manual work.

## User Scenario

1. User imports a receipt screenshot.
2. OCR extracts merchant and amount text.
3. Classifier predicts `receipt`.
4. App assigns tags such as `receipt`, `finance`, and merchant-derived tags.
5. User can search/filter using generated tags and remove incorrect tags.

## Functional Requirements

- Generate tags after OCR and/or classification completes.
- De-duplicate tags case-insensitively.
- Persist generated tags and user-added tags.
- Distinguish generated tags from user tags when needed.
- Allow user to remove generated tags.
- Allow user to create, rename, and delete user-managed tags.
- Allow user to assign tags to memories from detail screen.
- Expose popular/user tags to drawer quick filtering.
- Re-run auto-tagging when OCR/classification is retried.

## Non-Functional Requirements

- Tag generation should complete under 300 ms for normal OCR text.
- Rules must be deterministic and unit-testable.
- Tag display should avoid noisy low-value tags.

## UI Structure

- Tag chips on memory list and detail.
- Add/remove tag controls on detail screen.
- Filter by tag in search screen.
- Drawer popular tag section.
- Tag management entry from drawer.

## Processing Flow

```text
OCR result + classification result
  -> Keyword extraction
  -> Category mapping
  -> Rule evaluation
  -> Tag normalization
  -> Tag upsert
  -> MemoryTagCrossRef insert
```

## Data Flow

- Input: OCR text, classification label, existing user tags.
- Output: normalized tag list linked to memory item.

## Database Interaction

- Read `ocr_texts` and `classifications`.
- Upsert `tags`.
- Insert/delete `memory_tag_cross_refs`.
- Update user-managed tag metadata on create/rename/delete.
- Update `memory_items.taggingStatus`.

## API Interaction

No API dependency for initial release.

## AI/OCR Logic

- OCR text feeds keyword rules.
- Classification label maps to category tags.
- No generative AI is used in initial release.

## Error Handling

| Error | Handling |
| --- | --- |
| Missing OCR result | Generate classification-only tags |
| Missing classification | Generate OCR-only tags |
| Tag DAO failure | Mark tagging failed and allow retry |
| Excessive tag count | Apply max tag limit and drop lowest priority tags |
| User renames tag to duplicate name | Block rename and show validation message |
| User deletes tag currently used by filters | Remove filter and refresh list |

## Edge Cases

- OCR text contains private numbers or IDs.
- Same tag appears from multiple sources.
- User removed a generated tag and auto-tagging runs again.
- User-created tag has the same normalized name as generated tag.
- Popular tag list changes while drawer is open.
- OCR result is empty.

## Dependencies

- Room.
- OCR feature.
- Classification feature.
- Coroutine dispatchers.

## TODO Checklist

- [ ] Define tag normalization rules.
- [ ] Define default tag categories.
- [ ] Implement keyword extraction.
- [ ] Implement category-to-tag mapping.
- [ ] Add tag DAO upsert methods.
- [ ] Add generated/user tag source tracking.
- [ ] Add user tag create/rename/delete support.
- [ ] Add manual tag assignment/removal support.
- [ ] Add popular tag query for drawer quick filtering.
- [ ] Add tag chips UI.
- [ ] Add tag filter query.
- [ ] Add unit tests for rule engine.

## Current Status

Not Started

## Progress Notes

- Rule-based tagging should ship first because it is predictable and testable.

## Future Improvements

- Add user-configurable tag rules.
- Add learned tag suggestions based on user edits.
- Add entity extraction for dates, prices, URLs, and locations.
