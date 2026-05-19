# Memo Management

## Goal

Allow users to add, edit, pin, archive, and delete personal notes attached to saved image memories.

## User Scenario

1. User opens a saved memory.
2. User adds a short note explaining why the screenshot matters.
3. User saves the note.
4. User later searches for text in that memo.
5. User archives or deletes memories that are no longer needed.

## Functional Requirements

- Create or update memo content for a memory item.
- Autosave or explicit save based on UI decision.
- Search memo content.
- Pin important memories.
- Archive memories without deleting underlying data.
- Delete memory item, image file, OCR, classification, memo, and tag refs.
- Confirm destructive delete.

## Non-Functional Requirements

- Memo edits should survive configuration changes.
- Save operation should be fast and non-blocking.
- Delete should be transactional where possible.
- UI must clearly distinguish archive from delete.

## UI Structure

- Memory detail screen.
- Editable memo field.
- Pin action.
- Archive action.
- Delete confirmation dialog.
- Detail overflow menu.

## Processing Flow

```text
User edits memo
  -> ViewModel validates content
  -> SaveMemoUseCase
  -> Memo DAO upsert
  -> Memory detail state refresh
```

## Data Flow

- Input: memory ID, memo body, pin/archive/delete action.
- Output: updated memory detail state.

## Database Interaction

- Upsert `memos`.
- Update `memory_items.isPinned`.
- Update `memory_items.archivedAt`.
- Delete linked rows when memory is permanently deleted.

## API Interaction

No API dependency.

## AI/OCR Logic

No direct AI processing. Memo text participates in search.

## Error Handling

| Error | Handling |
| --- | --- |
| Save failed | Keep local edited text and show retry |
| Delete failed | Keep item visible and show error |
| Missing memory | Navigate back with message |
| Image file delete failed | Mark database delete result and log cleanup issue |

## Edge Cases

- User edits memo while OCR processing updates detail screen.
- User deletes memory during processing.
- Very long memo text.
- Empty memo should remove or clear memo row.

## Dependencies

- Room.
- ViewModel.
- Kotlin Flow.
- Navigation.

## TODO Checklist

- [ ] Create `MemoEntity`.
- [ ] Implement memo DAO upsert/delete.
- [ ] Build memo editor UI.
- [ ] Add save state handling.
- [ ] Add pin/archive fields to memory entity.
- [ ] Implement archive query behavior.
- [ ] Implement delete cascade/transaction.
- [ ] Add delete confirmation dialog.
- [ ] Add memo search support.
- [ ] Add tests for save/archive/delete.

## Current Status

Not Started

## Progress Notes

- Memo management should be implemented after core memory detail loading exists.

## Future Improvements

- Markdown-style memo formatting.
- Reminder dates.
- Link extraction from memo text.

