# Backend/Data TODO

Backend in this project means local data, repositories, Room, file storage, and optional Retrofit APIs.

## Room

- [ ] Define `MemoryItemEntity`.
- [ ] Define `OcrTextEntity`.
- [ ] Define `ClassificationEntity`.
- [ ] Define `TagEntity`.
- [ ] Define `MemoryTagCrossRef`.
- [ ] Define `MemoEntity`.
- [ ] Add type converters for status enums and timestamps.
- [ ] Create `SnapMindDatabase`.
- [ ] Add DAO interfaces.
- [ ] Add database migration policy.
- [ ] Add DAO tests.

## Repository

- [ ] Create domain repository interfaces.
- [ ] Implement memory repository.
- [ ] Implement OCR result persistence.
- [ ] Implement classification result persistence.
- [ ] Implement tag upsert and linking.
- [ ] Implement memo upsert/delete.
- [ ] Map Room entities to domain models.

## File Storage

- [ ] Define app-private image directory.
- [ ] Implement content URI copy.
- [ ] Implement safe filename generation.
- [ ] Handle image delete cleanup.
- [ ] Add duplicate detection placeholder.

## Retrofit

- [ ] Create Retrofit client module.
- [ ] Create `ModelConfigApi`.
- [ ] Create DTO mapping conventions.
- [ ] Add network error mapping.
- [ ] Keep API features optional for initial release.

## Status

Current Status: Not Started

Progress: 0%

