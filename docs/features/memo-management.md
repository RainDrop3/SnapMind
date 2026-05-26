# Memo Management

## Goal

Allow users to add, edit, favorite, and delete personal notes attached to saved image memories. Gemini API provides an auto-recommended memo sentence that users can accept or edit.

## User Scenario

1. User imports a screenshot.
2. After processing, Gemini API suggests a memo: "카페 메뉴 가격을 나중에 참고하기 위해 저장했습니다."
3. User taps the recommendation chip to accept it.
4. User later edits the memo to be more specific.
5. User searches for text in that memo.
6. User deletes memories that are no longer needed.

## Functional Requirements

- Display Gemini API memo recommendation as a suggestion chip in detail screen.
- Allow user to accept, edit, or dismiss the Gemini recommendation.
- Create or update memo content for a memory item.
- Autosave or explicit save based on UI decision.
- Search memo content via Room FTS.
- Mark memories as favorite (즐겨찾기).
- Soft-delete memory to trash (휴지통); restore from trash.
- Permanently delete memory item, image file, OCR, Vision labels, classification, memo, and tag refs.
- Confirm destructive delete.

## Non-Functional Requirements

- Memo edits must survive configuration changes.
- Save operation is non-blocking (IO coroutine).
- Delete must be transactional.
- UI must clearly distinguish soft-delete (trash) from permanent delete.

## UI Structure

- Memory detail screen (`DetailActivity`).
- Editable memo text field.
- Gemini recommendation chip below memo field (shown when `geminiMemoStatus == SUGGESTED`).
- Favorite (star) action in toolbar.
- Delete → sends to trash; permanent delete from trash.
- Delete confirmation dialog.

## Gemini Recommendation Flow

```text
Gemini API response received
  → geminiMemoStatus = SUGGESTED
  → Show recommendation chip in detail screen
  → User taps chip      → memo field populated, geminiMemoStatus = ACCEPTED
  → User dismisses chip → geminiMemoStatus = DISMISSED
  → User saves any memo → MemoEntity upsert
```

## Processing Flow

```text
User edits/accepts memo
  → ViewModel validates content
  → SaveMemoUseCase
  → MemoDao upsert (body + geminiSuggestion + updatedAt)
  → Room FTS table updated
  → Memory detail state refresh
```

## Data Flow

- Input: memory ID, memo body, Gemini suggestion (optional), favorite/delete action.
- Output: updated memory detail state.

## Database Interaction

- Upsert `memos` (body, geminiSuggestion, updatedAt).
- Update `memory_items.isFavorite`.
- Update `memory_items.deletedAt` for soft-delete (trash).
- Delete linked rows (OCR, Vision labels, classifications, tags, memo, image file) on permanent delete.

## API Interaction

- Consumes Gemini API result from AI pipeline (stored as `geminiSuggestion` in `MemoEntity`).
- No direct API call from memo management; reads pre-fetched suggestion.

## Error Handling

| Error | Handling |
| --- | --- |
| Save failed | Keep local edited text and show retry |
| Gemini suggestion unavailable | Hide chip; user writes memo manually |
| Delete failed | Keep item visible and show error |
| Missing memory | Navigate back with message |
| Image file delete failed | Mark database delete result and log cleanup issue |

## Edge Cases

- User edits memo while Gemini suggestion is being fetched.
- User deletes memory during processing pipeline.
- Very long memo text (enforce max length).
- Empty memo should clear or remove memo row.
- Restore from trash after permanent delete is not possible (confirm dialog required).

## Dependencies

- Room (MemoEntity, MemoryItemEntity).
- ViewModel + StateFlow.
- Kotlin Flow + Coroutine.
- Gemini API result (produced by AI pipeline).
- Navigation (DetailActivity ↔ MainActivity).

## TODO Checklist

- [ ] Create `MemoEntity` with `geminiSuggestion` field.
- [ ] Implement memo DAO upsert/delete.
- [ ] Build memo editor UI with Gemini suggestion chip.
- [ ] Implement chip accept/dismiss logic.
- [ ] Add save state handling (survive configuration change).
- [ ] Add `isFavorite` field to `MemoryItemEntity`.
- [ ] Add `deletedAt` field (soft delete / trash).
- [ ] Implement trash screen (restore, permanent delete).
- [ ] Implement delete cascade/transaction.
- [ ] Add delete confirmation dialog.
- [ ] Add memo search support via Room FTS.
- [ ] Add tests for save/favorite/delete.

## Current Status

Not Started

## Future Improvements

- Markdown-style memo formatting.
- Reminder dates attached to memos.
- Link extraction from memo text.
