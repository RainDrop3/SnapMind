# Search and Filter

## Goal

Allow users to quickly find saved memories by OCR text, memo text, tag, category, and date.

## User Scenario

1. User opens the memory list.
2. User types a word remembered from a screenshot.
3. App returns matching memories.
4. User applies tag/category/date filters.
5. User opens the target memory detail.

### Drawer Quick Filter Scenario

1. User opens the Memory page.
2. User swipes from the left edge to open the drawer.
3. User taps `Code` in the category list.
4. Memory list immediately shows only `Code` screenshots/photos.
5. User clears the active filter chip to return to all memories.

## Functional Requirements

- Search OCR full text.
- Search memo text.
- Search tag names.
- Search classification labels.
- Filter by category.
- Filter by one or more tags.
- Apply category/tag filters from `DrawerLayout` quick filter menu.
- Show active drawer-applied filter as a clearable chip.
- Filter by date range.
- Sort by newest first by default.
- Support empty query with active filters.

## Non-Functional Requirements

- Query updates should debounce text input.
- Results should render incrementally from Room `Flow`.
- Search should work offline.
- Query should remain responsive with at least 5,000 memories.

## UI Structure

- Memory list/search screen.
- Search text field.
- Filter bottom sheet or panel.
- Drawer quick filter menu for category/tag filtering.
- Tag chips.
- Category selector.
- Date range selector.
- Empty state.

## Processing Flow

```text
User query/filter change
  -> ViewModel updates search state
  -> Debounced search use case
  -> Room query
  -> Domain mapping
  -> UI list state
```

```text
Drawer category/tag selection
  -> MemoryFilters updated
  -> Drawer closes
  -> Room query applies selected filter
  -> Memory list state updates
```

## Data Flow

- Input: query string, tag IDs, category labels, date range.
- Input from drawer: selected category label or selected tag ID.
- Output: ordered list of `MemoryListItemUiModel`.

## Database Interaction

- Query `memory_items`.
- Join `ocr_texts`, `memos`, `classifications`, `tags`, `memory_tag_cross_refs`.
- Query tag/category counts for drawer display.
- Use indexes from `database-design.md`.

## API Interaction

No API dependency.

## AI/OCR Logic

Uses OCR/classification/tag results already stored in Room. Does not run AI processing directly.

## Error Handling

| Error | Handling |
| --- | --- |
| Query failure | Show error state and retry |
| Invalid date range | Disable apply action until corrected |
| No results | Show empty search state |
| Processing still pending | Show items if base metadata matches, with processing badge |
| Selected drawer tag deleted | Clear tag filter and show message |

## Edge Cases

- Query matches tag and OCR text.
- User searches while import processing is still running.
- Filters exclude all results.
- Deleted memory remains in old UI state.
- Drawer category/tag filter combines with text query and date filters.
- Category label such as `Code` exists from both classifier and tag; classifier category has priority unless user selected a tag item.

## Dependencies

- Room.
- Kotlin Flow.
- ViewModel.
- Navigation.
- Drawer filtering feature.

## TODO Checklist

- [ ] Define `MemoryFilters` domain model.
- [ ] Implement search DAO query.
- [ ] Add debounce in ViewModel.
- [ ] Build search input UI.
- [ ] Build filter UI.
- [ ] Apply filters from drawer category/tag selection.
- [ ] Show active drawer filter as clearable chip.
- [ ] Add tag/category count query for drawer.
- [ ] Add empty/error states.
- [ ] Add date range filtering.
- [ ] Add tests for combined filters.
- [ ] Consider FTS migration after baseline search works.

## Current Status

Not Started

## Progress Notes

- Start with SQL `LIKE`; move to FTS only when dataset size or ranking requires it.

## Future Improvements

- Search result highlighting.
- Recent searches.
- Saved filters.
- FTS ranking and typo tolerance.
