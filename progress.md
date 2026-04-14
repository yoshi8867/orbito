# Orbito 개발 진행도

██████████████████████ 100% (4/4)

---

### 1단계: 프로젝트 세팅 ✅
- [x] GitHub 저장소 생성 (yoshi8867/orbito)
- [x] architecture-templates clone + customizer.sh 실행
- [x] Hilt/Room/Navigation 제거, 순수 Compose + ViewModel 구조로 변경
- [x] build.gradle.kts 정리

### 2단계: 데이터 모델 ✅
- [x] `model/GameState.kt` — Player, CellState, GamePhase, GameState
- [x] OPTIONAL_MOVE → PLACE → DONE 상태 머신

### 3단계: 게임 로직 ✅
- [x] `viewmodel/GameViewModel.kt`
- [x] 선택/이동 (OPTIONAL_MOVE): 상대 공 탭 → 선택, 인접 빈 칸 탭 → 이동
- [x] SKIP 기능
- [x] 배치 (PLACE): 빈 칸 탭 → 공 배치
- [x] 회전 로직 (바깥 궤도 12칸 + 안쪽 궤도 4칸 반시계)
- [x] 승리 판정 (행/열/대각선 4연속)

### 4단계: UI ✅
- [x] `ui/theme/Color.kt` — 회색 배경, 흰/검정 공 색상
- [x] `ui/SideBalls.kt` — 모바일(1열) / 태블릿(2열) 반응형
- [x] `ui/BoardGrid.kt` — 4×4 격자, 선택/인접 하이라이트
- [x] `ui/GameScreen.kt` — 차례 표시, SKIP/RESTART 버튼, 승리 오버레이
- [x] 반응형: 600dp 기준 모바일/태블릿 분기

---

### 마지막 업데이트
2026-04-14 | 전체 구현 완료

---

### 테스트 항목 (사용자 확인 필요)
- [ ] 기본 게임 플레이 (공 배치 → 회전 → 승리 판정)
- [ ] OPTIONAL_MOVE: 상대 공 선택 → 이동 / SKIP
- [ ] 승리 오버레이 표시 + PLAY AGAIN
- [ ] 모바일과 태블릿에서 레이아웃 확인
