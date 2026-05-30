# 팀원 A → 팀원 B 변경 사항 안내

> 팀원 A 작업 중 **팀원 B의 코드·로직·동작에 영향을 주는 변경**만 추려서 정리.
> 전반적 작업 내역은 `docs/progress.md` 참고.

---

## Phase 1 — DB 설계

**B 코드 직접 변경: 없음.**

알아둘 점:
- Room DB(`SnapMindDatabase`) + 9개 DAO + Hilt `DatabaseModule`이 추가됨. 단, `RepositoryModule`은 여전히 `InMemoryMemoryRepository`를 바인딩 → **이 시점까지 B의 UI 동작은 동일** (시드 데이터 6개 그대로 노출).
- B가 새 DAO를 직접 주입받아 쓰고 싶다면 `DatabaseModule`의 `@Provides`로 모두 노출되어 있음. 단, 동일 데이터를 A의 `MemoryRepository`에서 제공하므로 중복 주입은 피할 것.
- `MemoryRepository` 인터페이스 시그니처 변경 0개 → B 파일 컴파일 영향 0.

---

## Phase 2 — 이미지 수집 (저장 로직)

**B 코드 직접 변경 (3개 파일, 사용자 승인 하 예외):**

1. `data/model/MemoryModels.kt` — `MemoryCategory` enum 재정의 (8 → 9개).
   - 제거: `MAP`
   - 추가: `TRAVEL("Travel","TRIP")`, `FOOD("Food","FOOD")`
   - 사유: `docs/specs/ml-training-spec.md`의 9개 라벨과 정렬 → 매핑 테이블 없이 `valueOf` 직결.
2. `feature/home/MemoryGridAdapter.kt`, `feature/memorydetail/DetailActivity.kt` — `thumbnailBackground()` when 분기에서 `MAP` 삭제, `TRAVEL`/`FOOD`는 임시로 `bg_thumbnail_receipt` 재사용. **Phase 4 UI 폴리싱 시 전용 drawable 2종(`bg_thumbnail_travel`, `bg_thumbnail_food`) 추가 권장**.
3. `res/drawable/bg_thumbnail_map.xml` — 미사용 상태로 보존. B가 정리 가능.

**동작·데이터 흐름 변경 (B 코드는 그대로지만 UX 영향):**

- `RepositoryModule` 바인딩이 `InMemoryMemoryRepository` → `RoomMemoryRepository`로 전환. **`MemoryRepository` 인터페이스 시그니처는 동일**하므로 B의 ViewModel·Activity·Fragment 코드 수정 불필요.
- 시드 데이터 6개 제거 → **첫 실행 시 홈/즐겨찾기/태그 Drawer 모두 빈 화면**. 이미지를 import해야 채워짐. ShareActivity/FAB import 흐름은 그대로 동작.
- `importImage()`는 이제 영속화됨 (앱 종료 후에도 유지). 중복 import는 SHA-256 해시로 감지하여 같은 메모리 객체 반환.
- 자동 태그는 Phase 3 AI 파이프라인 완료 전까지 `#Imported` 한 개만 붙음.
