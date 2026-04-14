package com.yoshi0311.orbito.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.orbito.model.CellState
import com.yoshi0311.orbito.model.GamePhase
import com.yoshi0311.orbito.model.GameState
import com.yoshi0311.orbito.model.Player
import com.yoshi0311.orbito.model.ROTATION_MAPPING
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init { startTimer() }

    fun onCellTap(row: Int, col: Int) {
        val s = _state.value
        if (s.isRotating) return
        when (s.phase) {
            GamePhase.OPTIONAL_MOVE -> handleOptionalMove(s, row, col)
            GamePhase.PLACE -> handlePlace(s, row, col)
            GamePhase.DONE -> {}
        }
    }

    fun restart() {
        _state.value = GameState()
        startTimer()
    }

    // UI에서 회전 애니메이션이 끝났을 때 호출
    fun onRotationComplete() {
        val s = _state.value
        if (!s.isRotating) return
        if (s.phase == GamePhase.DONE) {
            // 회전 중 타임아웃 발생 시
            _state.value = s.copy(isRotating = false, boardBeforeRotation = null)
            return
        }
        val winner = checkWinner(s.board)
        _state.value = s.copy(
            isRotating = false,
            boardBeforeRotation = null,
            currentPlayer = if (s.currentPlayer == Player.WHITE) Player.BLACK else Player.WHITE,
            phase = if (winner != null) GamePhase.DONE else GamePhase.OPTIONAL_MOVE,
            winner = winner
        )
        if (winner == null) startTimer()
    }

    // ── 타이머 ────────────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        _state.value = _state.value.copy(timeLeft = 20)
        timerJob = viewModelScope.launch {
            for (remaining in 19 downTo 0) {
                delay(1000L)
                val s = _state.value
                if (s.phase == GamePhase.DONE) return@launch
                _state.value = s.copy(timeLeft = remaining)
                if (remaining == 0) {
                    val winner = if (s.currentPlayer == Player.WHITE) Player.BLACK else Player.WHITE
                    _state.value = _state.value.copy(phase = GamePhase.DONE, winner = winner)
                    return@launch
                }
            }
        }
    }

    // ── OPTIONAL_MOVE ────────────────────────────────────────────────────────

    private fun handleOptionalMove(s: GameState, row: Int, col: Int) {
        val opponentColor = if (s.currentPlayer == Player.WHITE) CellState.BLACK else CellState.WHITE
        val sel = s.selectedCell

        when {
            // 같은 셀 탭 → 선택 해제
            sel != null && sel.first == row && sel.second == col -> {
                _state.value = s.copy(selectedCell = null)
            }
            // 선택된 공 + 인접 빈 칸 탭 → 상대 공 이동 후 PLACE 단계로
            sel != null && s.board[row][col] == CellState.EMPTY && isAdjacent(sel, row, col) -> {
                val newBoard = mutableBoard(s.board)
                newBoard[row][col] = newBoard[sel.first][sel.second]
                newBoard[sel.first][sel.second] = CellState.EMPTY
                _state.value = s.copy(
                    board = newBoard.toImmutable(),
                    selectedCell = null,
                    phase = GamePhase.PLACE
                )
            }
            // 상대 공 탭 → 선택 (또는 재선택)
            s.board[row][col] == opponentColor -> {
                _state.value = s.copy(selectedCell = Pair(row, col))
            }
            // 빈 셀 탭 → 스킵 + 즉시 배치 (한 번에)
            s.board[row][col] == CellState.EMPTY -> {
                handlePlace(s.copy(selectedCell = null), row, col)
            }
        }
    }

    // ── PLACE ────────────────────────────────────────────────────────────────

    private fun handlePlace(s: GameState, row: Int, col: Int) {
        if (s.board[row][col] != CellState.EMPTY) return
        val sideCount = if (s.currentPlayer == Player.WHITE) s.whiteSideCount else s.blackSideCount
        if (sideCount <= 0) return

        val ownColor = if (s.currentPlayer == Player.WHITE) CellState.WHITE else CellState.BLACK
        val newBoard = mutableBoard(s.board)
        newBoard[row][col] = ownColor
        val boardAfterPlace = newBoard.toImmutable()
        val rotated = rotate(boardAfterPlace)

        timerJob?.cancel()   // 배치 완료 → 타이머 정지 (다음 플레이어 턴에 재시작)
        _state.value = s.copy(
            board = rotated,
            boardBeforeRotation = boardAfterPlace,
            whiteSideCount = if (s.currentPlayer == Player.WHITE) s.whiteSideCount - 1 else s.whiteSideCount,
            blackSideCount = if (s.currentPlayer == Player.BLACK) s.blackSideCount - 1 else s.blackSideCount,
            isRotating = true
        )
    }

    // ── 회전 ─────────────────────────────────────────────────────────────────

    private fun rotate(board: List<List<CellState>>): List<List<CellState>> {
        val new = mutableBoard(board)
        for ((src, dst) in ROTATION_MAPPING) {
            new[dst.first][dst.second] = board[src.first][src.second]
        }
        return new.toImmutable()
    }

    // ── 승리 판정 ─────────────────────────────────────────────────────────────

    private fun checkWinner(board: List<List<CellState>>): Player? {
        fun CellState.toPlayer() = if (this == CellState.WHITE) Player.WHITE else Player.BLACK

        for (r in 0..3) {
            val c = board[r][0]
            if (c != CellState.EMPTY && board[r].all { it == c }) return c.toPlayer()
        }
        for (col in 0..3) {
            val c = board[0][col]
            if (c != CellState.EMPTY && (0..3).all { board[it][col] == c }) return c.toPlayer()
        }
        val d1 = board[0][0]
        if (d1 != CellState.EMPTY && (0..3).all { board[it][it] == d1 }) return d1.toPlayer()
        val d2 = board[0][3]
        if (d2 != CellState.EMPTY && (0..3).all { board[it][3 - it] == d2 }) return d2.toPlayer()
        return null
    }

    // ── 유틸 ──────────────────────────────────────────────────────────────────

    private fun isAdjacent(from: Pair<Int, Int>, toRow: Int, toCol: Int) =
        kotlin.math.abs(from.first - toRow) + kotlin.math.abs(from.second - toCol) == 1

    private fun mutableBoard(board: List<List<CellState>>) =
        board.map { it.toMutableList() }.toMutableList()

    private fun List<MutableList<CellState>>.toImmutable() = map { it.toList() }
}
