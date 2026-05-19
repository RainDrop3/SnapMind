# Screen Specifications

## Main App Shell

### Purpose

Provide swipe-based access to core SnapMind features with `ViewPager2` and direct tab access with `BottomNavigationView`.

### UI Elements

- `ViewPager2` occupying the main content area.
- `BottomNavigationView` fixed to the bottom of the screen.
- `DrawerLayout` available on the Memory page for category/tag quick filters.
- Bottom navigation items:
  - Memory
  - Search
  - Tags
  - Memo
  - Settings
- Child feature pages rendered inside pager pages.

### Behavior

- Swiping `ViewPager2` changes the selected bottom navigation item.
- Tapping a bottom navigation item changes `ViewPager2.currentItem`.
- Selected page is restored after configuration changes.
- Memory detail and import preview are opened above the main shell, not as bottom nav tabs.
- Horizontal child gestures must not make pager navigation unusable.
- Left-edge swipe on the Memory page opens the filter drawer.
- Non-edge horizontal swipe changes the `ViewPager2` page.

### State

- Default selected page: Memory.
- Selected page restored.
- Child page loading/error/content states remain owned by each page.

### Acceptance Criteria

- [ ] User can swipe between main features.
- [ ] User can tap bottom navigation to jump to each main feature.
- [ ] Bottom navigation selection always matches the visible pager page.
- [ ] Selected page survives rotation.
- [ ] Back from detail returns to the originating main page.
- [ ] Left-edge swipe opens drawer without breaking pager swipes.

## Drawer Filter Menu

### Purpose

Provide fast category/tag filtering from the Memory page, similar to folder navigation in email apps.

### UI Elements

- Drawer header with current memory count or app label.
- Quick filter items:
  - All
  - Unprocessed
  - Pinned
  - Archived
- Category list:
  - Shopping
  - Code
  - Map
  - Receipt
  - Document
  - Chat
  - Unknown
- Popular tag list.
- Tag management actions:
  - Create tag
  - Manage tags
- Optional count badge per category/tag.

### Behavior

- Opens from left edge swipe on Memory page.
- Opens from toolbar menu button.
- Tapping a category applies a category filter to the main Memory list.
- Tapping a tag applies a tag filter to the main Memory list.
- Drawer closes after selection.
- Active filter appears as a chip above the Memory list.
- Clear action removes the active drawer filter.
- Tag management action opens tag create/edit/delete UI.

### State

- Loading drawer data.
- Drawer content loaded.
- Drawer data error with retry.
- Active category selected.
- Active tag selected.

### Acceptance Criteria

- [ ] User can open drawer by swiping from the left edge.
- [ ] User can open drawer from toolbar button.
- [ ] Selecting `Code` filters the main list to `Code` screenshots/photos.
- [ ] Selecting a popular tag filters the main list by that tag.
- [ ] Active filter is visible and clearable.
- [ ] User can enter tag management from the drawer.

## Memory List Screen

### Purpose

Show saved memories with image thumbnail, processing state, tags, category, and memo preview.

### UI Elements

- Search input.
- Drawer open button.
- Active category/tag filter chip row.
- Filter action.
- Memory thumbnail grid/list.
- Processing badges.
- Empty state.
- Import/gallery action if supported.

### State

- Loading.
- Empty.
- Content.
- Error.
- Searching/filtering.

## Import Confirmation Screen

### Purpose

Confirm shared image import before persistence.

### UI Elements

- Image preview.
- Source metadata.
- Save button.
- Cancel button.
- Batch count if multiple images.

### State

- Preview loading.
- Ready.
- Saving.
- Partial failure.
- Error.

## Memory Detail Screen

### Purpose

Show image, OCR text, classification, tags, and editable memo.

### UI Elements

- Full image preview.
- Memo editor.
- Tag chips.
- Category row.
- OCR text section.
- Retry processing actions.
- Pin/archive/delete actions.

### State

- Loading.
- Content.
- Processing.
- Processing failed.
- Save failed.
- Missing memory.

## Search/Filter Screen

### Purpose

Allow query and structured filtering.

### UI Elements

- Query field.
- Tag multi-select.
- Category selector.
- Date range selector.
- Clear filters action.
- Result list.

### State

- No query.
- Results.
- No results.
- Error.
