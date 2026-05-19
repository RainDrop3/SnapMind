# Drawer Filtering

## Goal

Use `DrawerLayout` as a fast category/tag filtering menu for the Memory home screen. Users can open the left drawer with an edge swipe and immediately filter the main memory list by saved categories such as `Shopping`, `Code`, `Map`, or by popular tags.

This behaves like folder navigation in email apps: selecting a drawer item changes the visible list scope immediately.

## User Scenario

1. User opens SnapMind on the Memory page.
2. User swipes from the left edge of the screen.
3. App opens a navigation drawer.
4. Drawer shows all saved categories and popular tags.
5. User taps `Code`.
6. Drawer closes.
7. Main memory list immediately shows screenshots/photos tagged or classified as `Code`.
8. User can manage the selected tag/category from the drawer or detail screen.

## Functional Requirements

- Use `DrawerLayout` around the main Memory page content.
- Open drawer by swiping from the left screen edge.
- Provide a visible drawer/menu button for accessibility and discoverability.
- Show saved categories in the drawer.
- Show popular tags in the drawer.
- Show item counts for categories/tags where available.
- Selecting a category applies a category filter to the Memory list.
- Selecting a tag applies a tag filter to the Memory list.
- Drawer selection state must reflect the active filter.
- The active filter must be clearable.
- Filtered list must update immediately from Room `Flow`.
- Provide tag/category management entry points from the drawer.
- Allow user-created tags to be assigned to memories from memory detail.
- Allow tag rename/delete where safe.

## Drawer Sections

| Section | Items | Source |
| --- | --- | --- |
| Quick Filters | All, Unprocessed, Favorites/Pinned, Archived | `memory_items` status fields |
| Categories | Shopping, Code, Map, Receipt, Document, Chat, Unknown | `classifications.label` and category mapping |
| Popular Tags | Most-used generated/user tags | `tags` and `memory_tag_cross_refs` |
| Tag Management | Create tag, Manage tags | tag management screen/dialog |

## Non-Functional Requirements

- Drawer should open smoothly without forcing Memory list reload.
- Category/tag counts should be loaded asynchronously.
- Drawer content should remain usable with at least 200 tags.
- Popular tags should be capped to avoid an overloaded drawer.
- Tag management actions must not block list filtering.

## UI Structure

- `DrawerLayout`
  - main content:
    - Memory list toolbar
    - active filter chip row
    - memory thumbnail/list content
  - drawer content:
    - Quick Filters section
    - Categories section
    - Popular Tags section
    - Tag Management actions

## Processing Flow

```text
User swipes from left edge
  -> DrawerLayout opens
  -> User taps category/tag
  -> ViewModel updates MemoryFilters
  -> DrawerLayout closes
  -> Room Flow emits filtered memory list
  -> Memory list renders filtered results
```

## Data Flow

- Input: drawer item selection, active filter state, saved category/tag data.
- Output: updated `MemoryFilters`, selected drawer item state, filtered memory list.

## Database Interaction

- Read categories from `classifications.label`.
- Read tags from `tags`.
- Read tag usage counts from `memory_tag_cross_refs`.
- Query `memory_items` with selected category/tag filters.
- Update tag rows for create/rename/delete operations.
- Update `memory_tag_cross_refs` when user assigns or removes tags.

## API Interaction

No API dependency.

## AI/OCR Logic

- Categories come from TFLite classification output.
- Tags may come from auto-tagging rules, OCR keyword extraction, or user-created tags.
- Drawer filtering does not run AI processing directly.

## Error Handling

| Error | Handling |
| --- | --- |
| Category count query fails | Show category names without counts and keep drawer usable |
| Tag list query fails | Show drawer error row with retry |
| Selected tag was deleted | Clear active tag filter and show message |
| Rename creates duplicate tag | Block save and show duplicate tag message |
| Drawer gesture conflicts with pager | Left edge swipe opens drawer; non-edge horizontal swipe remains pager navigation |

## Edge Cases

- User selects a tag with zero visible memories after other filters are applied.
- Auto-tagging creates a new tag while drawer is open.
- User deletes the currently selected tag.
- User opens drawer from Search tab instead of Memory tab.
- Left edge swipe conflicts with Android system back gesture.
- Very long tag names need truncation.

## Dependencies

- AndroidX DrawerLayout.
- Material components for navigation drawer/list items.
- Room.
- Kotlin Flow.
- ViewModel saved state.
- Search/filter feature.
- Auto-tagging feature.

## TODO Checklist

- [ ] Add `androidx.drawerlayout:drawerlayout` dependency if not already available transitively.
- [ ] Define drawer layout for Memory page.
- [ ] Add drawer open button in Memory toolbar.
- [ ] Add left edge swipe behavior.
- [ ] Define `DrawerFilterItem` UI model.
- [ ] Query category list and counts.
- [ ] Query popular tags and counts.
- [ ] Apply category filter from drawer selection.
- [ ] Apply tag filter from drawer selection.
- [ ] Show active filter chip and clear action.
- [ ] Add tag creation action.
- [ ] Add tag rename/delete management action.
- [ ] Add tag assignment/removal support in memory detail.
- [ ] Handle deleted selected tag.
- [ ] Add UI test for selecting `Code` category from drawer.
- [ ] Add UI test for selecting popular tag from drawer.

## Current Status

Not Started

## Progress Notes

- Drawer filtering should be implemented after base Memory list state exists.
- Gesture priority must be tested with `ViewPager2` because both components use horizontal gestures.

## Future Improvements

- Add custom drawer sorting.
- Add pinned favorite tags.
- Add saved filter presets.
- Add drag-and-drop tag organization.

