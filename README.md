# Orbito

Android 보드게임 앱 — Kotlin + Jetpack Compose

## 게임 규칙

4×4 격자판에서 2인이 번갈아가며 진행.

1. (선택) 상대 구슬을 인접 칸으로 이동
2. 자신의 구슬을 빈 칸에 배치
3. 자동 회전 (안쪽/바깥쪽 궤도 반시계 방향)
4. 같은 색 4개 연속 시 승리

## 기술 스택

- Kotlin + Jetpack Compose
- MVVM (ViewModel + StateFlow)
- 완전 오프라인
