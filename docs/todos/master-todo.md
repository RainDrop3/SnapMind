# Master TODO Dashboard

## Legend

- `[ ]` Not Started
- `[/]` In Progress
- `[x]` Completed
- `[!]` Blocked

## Feature Status

| Feature | Status | Progress | Priority | Depends On |
| --- | --- | ---: | --- | --- |
| Swipe Navigation | Not Started | 0% | P0 | Project Foundation |
| Drawer Filtering | Not Started | 0% | P0 | Swipe Navigation, Tags schema, Search and Filter |
| Screenshot Sharing | Not Started | 0% | P0 | Project Foundation |
| OCR Processing | Not Started | 0% | P0 | Screenshot Sharing, Room |
| AI Image Classification | Not Started | 0% | P1 | Screenshot Sharing, TFLite setup |
| Auto-Tagging | Not Started | 0% | P1 | OCR, Classification, Tags schema |
| Search and Filter | Not Started | 0% | P0 | OCR, Tags, Memo schema |
| Memo Management | Not Started | 0% | P1 | Memory detail, Room |

## Foundation Tasks

- [ ] Create package hierarchy.
- [ ] Add Hilt dependency and application class.
- [ ] Add Room dependency and database skeleton.
- [ ] Add Retrofit/OkHttp dependencies.
- [ ] Add ML Kit OCR dependency.
- [ ] Add TensorFlow Lite dependency.
- [ ] Define `AppResult` and `AppError`.
- [ ] Define dispatcher provider.
- [ ] Add base navigation routes.
- [ ] Add ViewPager2 and BottomNavigationView app shell.
- [ ] Add DrawerLayout quick filtering shell.
- [ ] Add CI test command documentation.

## Priority Queue

### P0

- [ ] Room schema baseline.
- [ ] Main swipe/bottom navigation shell.
- [ ] Drawer category/tag quick filtering.
- [ ] Screenshot share import.
- [ ] OCR extraction.
- [ ] Search by OCR text.

### P1

- [ ] TFLite classification.
- [ ] Auto-tagging.
- [ ] Memo editing.
- [ ] Category/tag filters.
- [ ] Tag management.

### P2

- [ ] Remote model config API.
- [ ] FTS search migration.
- [ ] Batch import background notification.
- [ ] Model update strategy.

## Cross-Team Dependencies

| Dependency | Owner | Blocking |
| --- | --- | --- |
| Room schema v1 | Backend/Data | OCR, search, memo |
| Image storage strategy | Android/Data | import, OCR, classification |
| TFLite label list | AI | classification, category filters |
| Tag taxonomy | AI/Product | auto-tagging, search filters |
| Screen navigation | Frontend | all feature screens |

## Testing Checklist

- [ ] Repository unit tests.
- [ ] DAO tests.
- [ ] OCR processor tests with fake recognizer.
- [ ] Classifier output mapping tests.
- [ ] Auto-tag rule tests.
- [ ] ViewModel state tests.
- [ ] Import flow instrumentation test.
- [ ] Search/filter instrumentation test.
