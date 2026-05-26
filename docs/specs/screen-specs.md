# Screen Specifications

## Activity Structure

| Activity | Role |
| --- | --- |
| `MainActivity` | App shell: ViewPager2, BottomNavigationView, DrawerLayout |
| `ShareActivity` | Receives external `ACTION_SEND` share intent |
| `DetailActivity` | Image detail: OCR text, tags, memo editor, YouTube deep-link |

Intent flow:

- `ShareActivity` → `MainActivity` (one-way; triggers import pipeline)
- `MainActivity` ↔ `DetailActivity` (two-way; memo edit result returned)

---

## Main App Shell (MainActivity)

### Purpose

Provide tab-based access to core SnapMind features with `ViewPager2` and `BottomNavigationView`, plus a right-side `DrawerLayout` for utility actions.

### UI Elements

- `ViewPager2` occupying the main content area.
- `BottomNavigationView` fixed to the bottom of the screen (4 tabs).
- `DrawerLayout` on the right side, opened from toolbar menu icon.
- **Search button** at the top-right of the home (홈) tab toolbar.
- **FAB (+)** at the bottom-right for uploading a new image.

### Bottom Navigation Tabs (4)

| Index | Tab | Purpose |
| ---: | --- | --- |
| 0 | 홈 | Saved screenshots/photos grid |
| 1 | 즐겨찾기 | Favorited memories |
| 2 | 태그별 사진 | Browse photos by tag |
| 3 | 설정 | App settings |

### Behavior

- Swiping `ViewPager2` changes the selected bottom navigation item.
- Tapping a bottom navigation item changes `ViewPager2.currentItem`.
- Selected page is restored after configuration changes.
- Detail screen (`DetailActivity`) is launched above the main shell.

### Acceptance Criteria

- [ ] User can swipe between 4 main tabs.
- [ ] User can tap bottom navigation to jump to each tab.
- [ ] Bottom navigation selection always matches the visible pager page.
- [ ] Selected tab survives rotation.
- [ ] Back from `DetailActivity` returns to the originating tab.
- [ ] Search button on home tab opens search screen.
- [ ] FAB (+) opens image import flow.

---

## Right-Side Drawer Menu (DrawerLayout)

### Purpose

Provide utility actions and quick navigation from a right-side sliding panel.

### UI Elements

- Drawer opened from toolbar menu icon (right side).
- Menu items:
  - 🗑 **휴지통** — view and restore deleted memories
  - 📄 **PDF로 추출하기** — export selected memories as PDF
  - 👨‍💻 **개발자 소개** — developer info page
  - 🏷 **인기 태그 TOP 3 바로가기** — shortcut to the 3 most-used tags

### Behavior

- Opens by swiping from the right edge or tapping toolbar icon.
- Tapping "인기 태그 TOP 3 바로가기" navigates to 태그별 사진 tab filtered by each top tag.
- Drawer closes after selection.

### Acceptance Criteria

- [ ] User can open drawer from toolbar icon.
- [ ] User can navigate to trash, PDF export, developer info, and top-3 tags.
- [ ] Top-3 tag shortcuts reflect current tag usage counts.

---

## Home Screen (홈 · Tab 0)

### Purpose

Show all saved memories as a thumbnail grid with processing status indicators.

### UI Elements

- Search button at top-right toolbar.
- `RecyclerView` thumbnail grid (Glide + status overlay).
- Processing badges per item: processing / done / error.
- Empty state message.
- FAB (+) at bottom-right for image import.

### State

- Loading.
- Empty.
- Content.
- Error.

---

## Favorites Screen (즐겨찾기 · Tab 1)

### Purpose

Show memories marked as favorite.

### UI Elements

- `RecyclerView` thumbnail grid with same layout as home screen.
- Empty state for no favorites.

---

## Tag Browse Screen (태그별 사진 · Tab 2)

### Purpose

Browse memories grouped or filtered by tag.

### UI Elements

- Tag chip row at top.
- `RecyclerView` grid filtered by selected tag.
- Empty state per tag.

---

## Settings Screen (설정 · Tab 3)

### Purpose

Configure app behavior.

### UI Elements

- API key management (Vision, Gemini, YouTube).
- Gemini memo recommendation toggle.
- YouTube deep-link toggle.
- Processing retry settings.
- Storage usage info.

---

## Import Confirmation Screen (ShareActivity)

### Purpose

Confirm shared image import before persistence.

### UI Elements

- Image preview.
- Source metadata (app name, MIME type).
- Save button.
- Cancel button.
- Batch count if multiple images.

### State

- Preview loading.
- Ready.
- Saving.
- Partial failure.
- Error.

---

## Memory Detail Screen (DetailActivity)

### Purpose

Show image, OCR text, classification, Vision labels, tags, editable memo, and YouTube deep-link button.

### UI Elements

- Full image preview.
- Memo editor with Gemini recommendation chip (if available).
- Tag chips (OCR + Vision + user tags).
- Category row (TFLite result + confidence).
- OCR text section (collapsible).
- **▶ 영상 바로 이동** button (visible when category == youtube and YouTube API returned result).
- Retry processing actions.
- Favorite / delete actions.

### State

- Loading.
- Content.
- Processing (OCR / Vision / Gemini pending).
- Processing failed.
- Save failed.
- Missing memory.

### Memo Recommendation Flow

1. If `geminiMemoStatus == SUGGESTED`, show recommendation chip below memo editor.
2. User taps chip → memo field is populated; `geminiMemoStatus = ACCEPTED`.
3. User dismisses chip → `geminiMemoStatus = DISMISSED`.
4. User saves any memo → persisted in `MemoEntity`.

---

## Search Screen

### Purpose

Full-text search via Room FTS across OCR text, memo text, tag names, and category labels.

### UI Elements

- Query field.
- Tag multi-select filter.
- Category selector.
- Date range selector.
- Clear filters action.
- `RecyclerView` result list.

### State

- No query (show recent or tips).
- Results.
- No results.
- Error.
