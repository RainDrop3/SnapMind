# Navigation Flow

## Routes

| Route | Parameters | Purpose |
| --- | --- | --- |
| `memoryList` | none | Default home screen |
| `memoryDetail/{memoryId}` | `memoryId: Long` | Memory detail and memo edit |
| `importPreview` | shared/import session ID | Confirm imported images |
| `search` | optional query/filter args | Search and filters |
| `tagBrowser` | none | Browse generated/user tags and categories |
| `tagManage` | optional `tagId: Long` | Create, rename, or delete user-managed tags |
| `memoHub` | none | Access pinned/recent memo-focused memories |
| `settings` | none | App/model/debug settings |

## Main Pager Navigation

The root app experience uses `ViewPager2` plus `BottomNavigationView`.

| Page Index | Bottom Nav ID | Route | Swipe Enabled | Notes |
| ---: | --- | --- | --- | --- |
| 0 | `nav_memory` | `memoryList` | Yes | Default launch page |
| 1 | `nav_search` | `search` | Yes | Query and filter page |
| 2 | `nav_tags` | `tagBrowser` | Yes | Tag/category browsing |
| 3 | `nav_memo` | `memoHub` | Yes | Memo-focused entry point |
| 4 | `nav_settings` | `settings` | Yes | Settings and model/debug controls |

### Synchronization Rules

- When `ViewPager2` page changes, update `BottomNavigationView.selectedItemId`.
- When bottom navigation item is selected, update `ViewPager2.currentItem`.
- Re-selecting the current bottom navigation item should keep the current page and may scroll that page to top if implemented.
- Invalid page index or item ID falls back to `memoryList`.
- Detail and import destinations are not pager pages.
- Memory page owns a `DrawerLayout` for category/tag filtering.
- Left-edge swipe on Memory opens the drawer before pager page swipe is considered.

## Drawer Filtering Navigation

| Drawer Item Type | Action | Destination/Result |
| --- | --- | --- |
| Quick filter | Apply fixed filter | Memory list refreshes |
| Category | Apply category filter | Memory list refreshes |
| Popular tag | Apply tag filter | Memory list refreshes |
| Create tag | Open tag management | `tagManage` |
| Manage tags | Open tag management | `tagManage` |

## Primary Flows

### Share Import

```text
External App
  -> Android Share Sheet
  -> SnapMind importPreview
  -> Save
  -> memoryDetail/{memoryId}
```

### Search

```text
memoryList
  -> search
  -> memoryDetail/{memoryId}
  -> back to search results
```

### Main Feature Swipe

```text
memoryList
  -> swipe left
  -> search
  -> swipe left
  -> tagBrowser
  -> tap Memo bottom nav item
  -> memoHub
```

### Drawer Category Filter

```text
memoryList
  -> left edge swipe
  -> drawer opens
  -> tap Code
  -> drawer closes
  -> memoryList filtered by Code
```

### Drawer Tag Management

```text
memoryList
  -> left edge swipe
  -> drawer opens
  -> tap Manage tags
  -> tagManage
  -> create/rename/delete tag
  -> back to memoryList
```

### Memo Edit

```text
memoryDetail/{memoryId}
  -> edit memo inline
  -> save
  -> updated detail state
```

## Back Behavior

- Import cancel returns to previous app or memory list.
- Detail back returns to list/search source.
- Delete from detail navigates back to previous list.
- Search clear keeps user on search screen.
- Back from a detail screen returns to the originating pager page.
- Back on the main pager should exit the app unless a child page has its own back stack or active search/filter state.
- Back while drawer is open closes the drawer.
- Back from tag management returns to Memory page with the previous filter state restored when possible.
