# Swipe Navigation

## Goal

Allow users to move between core SnapMind features by swiping horizontally with `ViewPager2`, while also providing direct access through a bottom navigation bar.

This feature defines the main app shell used after launch. It should make frequently used features reachable without forcing users through nested menus.

## User Scenario

1. User opens SnapMind.
2. App shows the default Memory tab.
3. User swipes left or right to move between main feature pages.
4. Bottom navigation selection updates to match the current page.
5. User taps a bottom navigation item to jump directly to a feature page.
6. User opens a memory detail from one page and returns to the same tab/page state.

## Functional Requirements

- Use `ViewPager2` as the main feature pager.
- Use `BottomNavigationView` as the direct feature access component.
- Keep `ViewPager2.currentItem` and selected bottom navigation item synchronized.
- Support both swipe navigation and bottom navigation taps.
- Preserve current page after configuration changes.
- Use stable page order and stable navigation item IDs.
- Disable or guard swipe only for pages where a child component requires horizontal gestures.
- Reserve left-edge swipe on the Memory page for `DrawerLayout` quick filtering.
- Detail/import screens should open above the main pager instead of becoming pager pages.

## Main Pages

| Page Index | Bottom Nav Item | Feature Area | Route/Screen |
| ---: | --- | --- | --- |
| 0 | Memory | Saved screenshots/photos | `memoryList` |
| 1 | Search | Search and filters | `search` |
| 2 | Tags | Tag/category browsing | `tagBrowser` |
| 3 | Memo | Pinned/recent memos | `memoHub` |
| 4 | Settings | App/model/debug settings | `settings` |

## Non-Functional Requirements

- Page switch should complete without visible layout jank.
- Pager pages should use lazy data loading where possible.
- Offscreen page limit should be conservative to avoid memory pressure from image-heavy lists.
- Bottom navigation must remain accessible on Android gesture navigation and three-button navigation.
- Main tab state should survive rotation and process recreation where feasible.

## UI Structure

- `MainActivity`
  - root container
  - `ViewPager2`
  - `BottomNavigationView`
- Pager adapter
  - memory list page
  - search page
  - tag browser page
  - memo hub page
  - settings page
- Overlay/navigation destination
  - import preview
  - memory detail
  - filter bottom sheet
  - delete confirmation dialog
- Memory page drawer
  - `DrawerLayout` for category/tag quick filters

## Processing Flow

```text
App launch
  -> MainActivity initializes pager adapter
  -> BottomNavigationView selects default Memory item
  -> User swipes pager
  -> OnPageChangeCallback updates selected bottom nav item
  -> User taps bottom nav item
  -> ViewPager2 sets matching currentItem
```

## Data Flow

- Input: swipe gestures, bottom navigation item selection, saved selected page state.
- Output: selected main page, active bottom navigation item, restored pager state.

## Database Interaction

No direct database interaction. Child pages query Room through their own ViewModels and repositories.

## API Interaction

No direct API interaction. Child pages own their remote/local data requirements.

## AI/OCR Logic

No direct AI/OCR logic. Processing status is displayed by child pages such as Memory and Search.

## Error Handling

| Error | Handling |
| --- | --- |
| Invalid nav item ID | Ignore selection and keep current page |
| Pager adapter page missing | Fall back to Memory page and log error |
| State restore page index out of range | Reset to page index `0` |
| Child page load failure | Show child page error state without breaking pager |
| Drawer gesture conflicts with pager | Left edge swipe opens drawer; regular content swipe changes pager page |

## Edge Cases

- User swipes while a bottom sheet is open.
- User swipes from the left edge while on Memory page.
- User taps bottom navigation repeatedly on the current page.
- User opens detail from Search and presses back.
- User rotates device while on a non-default page.
- Large memory list page consumes too much memory when cached offscreen.

## Dependencies

- AndroidX ViewPager2.
- Material `BottomNavigationView`.
- AndroidX `DrawerLayout`.
- Fragment or RecyclerView adapter depending on UI stack decision.
- ViewModel saved state support.

## TODO Checklist

- [ ] Add `androidx.viewpager2:viewpager2` dependency.
- [ ] Define bottom navigation menu item IDs.
- [ ] Create main pager layout with `ViewPager2` and `BottomNavigationView`.
- [ ] Implement pager adapter.
- [ ] Map bottom nav item IDs to pager indexes.
- [ ] Register `ViewPager2.OnPageChangeCallback`.
- [ ] Handle bottom nav item selection.
- [ ] Preserve selected page with saved instance state or ViewModel state.
- [ ] Define gesture priority between Memory drawer and pager swipe.
- [ ] Ensure detail/import screens are outside pager pages.
- [ ] Add UI test for swipe updating bottom nav selection.
- [ ] Add UI test for bottom nav tap updating pager page.

## Current Status

Not Started

## Progress Notes

- Main pager should be introduced before feature-specific screens become complex.
- Page order must be finalized before writing UI tests to avoid brittle test updates.

## Future Improvements

- Add dynamic tab visibility for experimental features.
- Add badge counts for failed processing or pending imports.
- Add deep link support to open a specific main page.
