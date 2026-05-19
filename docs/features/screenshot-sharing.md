# Screenshot Sharing

## Goal

Allow users to send screenshots/photos into SnapMind from Android share sheet and create a persistent memory item ready for OCR, classification, tagging, and memo editing.

## User Scenario

1. User opens another app and taps Share on an image.
2. User selects SnapMind from the Android share sheet.
3. SnapMind opens an import confirmation screen.
4. User confirms save.
5. App stores the image reference, creates a memory record, and starts processing.
6. User lands on the memory detail screen or returns to the memory list.

## Functional Requirements

- Accept `Intent.ACTION_SEND` with MIME type `image/*`.
- Accept `Intent.ACTION_SEND_MULTIPLE` for batch image import.
- Validate URI readability before creating a database record.
- Copy shared content into app-controlled storage unless persistent access is guaranteed.
- Create one `MemoryItemEntity` per imported image.
- Queue OCR, classification, and auto-tagging after successful persistence.
- Prevent duplicate imports by checking normalized `imageUri` or file hash when available.

## Non-Functional Requirements

- Import confirmation should render within 500 ms for a single image.
- Long-running copy/processing work must run off the main thread.
- Batch import should show progress and partial failure results.
- Import should work offline.

## UI Structure

- `ShareImportActivity` or import route entry point.
- Import preview screen:
  - image preview
  - filename/source app label when available
  - save/cancel actions
  - batch count indicator
- Optional processing status row on detail screen.

## Processing Flow

```text
Share Intent
  -> Intent parser
  -> URI validation
  -> Local file copy
  -> MemoryItem insert
  -> Processing queue enqueue
  -> Navigation to detail/list
```

## Data Flow

- Input: shared `Uri`, MIME type, optional `ClipData`.
- Output: `MemoryItem` domain model with local image URI and processing status `PENDING`.

## Database Interaction

- Insert into `memory_items`.
- Initialize processing status columns.
- Optionally insert source metadata if supported later.

## API Interaction

No remote API is required for initial release.

## AI/OCR Logic

This feature does not run OCR directly. It triggers processing through the import use case after local persistence succeeds.

## Error Handling

| Error | Handling |
| --- | --- |
| Unsupported MIME type | Show unsupported image message and do not insert DB row |
| URI permission denied | Show permission error and return to previous app |
| File copy failed | Show retry/cancel options |
| Duplicate image | Open existing memory detail instead of creating duplicate |
| Partial batch failure | Save valid items and list failed items |

## Edge Cases

- Shared image URI becomes invalid after activity restart.
- Sender app provides no MIME type.
- Very large image causes decode failure.
- Batch contains duplicate URIs.
- User cancels import after preview.

## Dependencies

- Android intent filters.
- ContentResolver.
- Room.
- Coroutine dispatchers.
- Navigation.

## TODO Checklist

- [ ] Add image share intent filters to `AndroidManifest.xml`.
- [ ] Implement shared intent parser.
- [ ] Implement app-private image copy helper.
- [ ] Create import confirmation UI.
- [ ] Create `ImportMemoryUseCase`.
- [ ] Insert `MemoryItemEntity` with pending statuses.
- [ ] Trigger processing queue after save.
- [ ] Handle duplicate imports.
- [ ] Add single-image import tests.
- [ ] Add batch import tests.

## Current Status

Not Started

## Progress Notes

- Initial implementation should prioritize `ACTION_SEND` before batch sharing.
- Local copy is preferred over relying on external URI durability.

## Future Improvements

- Add background batch import with notification progress.
- Add image hash-based duplicate detection.
- Support direct gallery picker import from inside the app.

