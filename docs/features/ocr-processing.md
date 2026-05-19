# OCR Processing

## Goal

Extract searchable text from imported screenshots/photos using ML Kit OCR and persist it for search, filtering, and auto-tagging.

## User Scenario

1. User imports a screenshot containing text.
2. App shows processing status.
3. OCR completes in the background.
4. User searches for words from the screenshot.
5. Matching memory appears in search results.

## Functional Requirements

- Run ML Kit text recognition for each imported image.
- Store full recognized text.
- Store OCR status on the memory item.
- Preserve line breaks where useful for detail screen display.
- Support retry when OCR fails.
- Use OCR output as input for auto-tagging.

## Non-Functional Requirements

- OCR must be cancellable.
- OCR should not block import persistence.
- Large images must be downsampled for processing.
- OCR result writes must be transactional with status update.

## UI Structure

- Processing status indicator.
- Detail screen OCR text section.
- Retry OCR button when failed.
- Search result highlight support in future iteration.

## Processing Flow

```text
MemoryItem pending OCR
  -> Load image
  -> Create ML Kit InputImage
  -> Run recognizer
  -> Normalize text
  -> Save OcrTextEntity
  -> Update ocrStatus
  -> Trigger auto-tagging
```

## Data Flow

- Input: local image URI.
- Output: normalized OCR text, optional block metadata, OCR status.

## Database Interaction

- Read `memory_items.imageUri`.
- Insert or replace `ocr_texts`.
- Update `memory_items.ocrStatus`.
- Search queries join `memory_items` and `ocr_texts`.

## API Interaction

No API dependency.

## AI/OCR Logic

- Use ML Kit Text Recognition.
- Normalize whitespace.
- Keep full text for search.
- Optional future block storage can support text bounding boxes and highlighting.

## Error Handling

| Error | Handling |
| --- | --- |
| Image not readable | Mark OCR failed and show file error |
| ML Kit failure | Mark OCR failed and allow retry |
| Empty OCR result | Mark OCR success with empty text |
| Database write failure | Keep status failed and log error |

## Edge Cases

- Handwritten text may not be recognized.
- Mixed languages may reduce accuracy.
- Vertical or rotated text.
- Screenshots with tiny UI text.
- Image contains sensitive text; keep processing local.

## Dependencies

- ML Kit Text Recognition.
- Room.
- Coroutine dispatchers.
- ContentResolver/file storage.

## TODO Checklist

- [ ] Add ML Kit OCR dependency.
- [ ] Implement OCR processor abstraction.
- [ ] Implement image loading for OCR.
- [ ] Normalize OCR result text.
- [ ] Create `OcrTextEntity` and DAO methods.
- [ ] Wire OCR processing after import.
- [ ] Add retry action.
- [ ] Add OCR search query.
- [ ] Add OCR unit tests with mocked recognizer.
- [ ] Add instrumentation test using sample screenshot.

## Current Status

Not Started

## Progress Notes

- OCR output must be persisted even when classification fails.

## Future Improvements

- Add OCR language hints.
- Add bounding box display for matched text.
- Add FTS table for large datasets.

