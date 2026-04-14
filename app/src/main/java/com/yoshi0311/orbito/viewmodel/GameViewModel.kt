package com.yoshi0311.orbito.viewmodel

import androidx.lifecycle.ViewModel
import com.yoshi0311.orbito.model.CellState
import com.yoshi0311.orbito.model.GamePhase
import com.yoshi0311.orbito.model.GameState
import com.yoshi0311.orbito.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun onCellTap(row: Int, col: Int) {
        val s = _state.value
        when (s.phase) {
            GamePhase.OPTIONAL_MOVE -> handleOptionalMove(s, row, col)
            GamePhase.PLACE -> handlePlace(s, row, col)
            GamePhase.DONE -> {}
        }
    }

    fun skipOptionalMove() {
        val s = _state.value
        if (s.phase == GamePhase.OPTIONAL_MOVE) {
            _state.value = s.copy(phase = GamePhase.PLACE, selectedCell = null)
        }
    }

    fun restart() {
        _state.value = GameState()
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
            // 선택된 공이 있고 인접 빈 칸 탭 → 이동 후 PLACE 단계로
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
            // 상대 공 탭 → 선택 (또는 다른 상대 공으로 재선택)
            s.board[row][col] == opponentColor -> {
                _state.value = s.copy(selectedCell = Pair(row, col))
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

        val rotated = rotate(newBoard.toImmutable())
        val winner = checkWinner(rotated)

        _state.value = s.copy(
            board = rotated,
            whiteSideCount = if (s.currentPlayer == Player.WHITE) s.whiteSideCount - 1 else s.whiteSideCount,
            blackSideCount = if (s.currentPlayer == Player.BLACK) s.blackSideCount - 1 else s.blackSideCount,
            currentPlayer = if (s.currentPlayer == Player.WHITE) Player.BLACK else Player.WHITE,
            phase = if (winner != null) GamePhase.DONE else GamePhase.OPTIONAL_MOVE,
            winner = winner
        )
    }

    // ── 회전 ─────────────────────────────────────────────────────────────────
    //
    // 반시계 방향 1칸 회전. src → dst 매핑.
    //
    // 바깥 궤도:   안쪽 궤도:
    // 1 2 3 4      . . . .
    // 5 . . 8      . 6 7 .
    // 9 . . C      . A B .
    // D E F G      . . . .

    private fun rotate(board: List<List<CellState>>): List<List<CellState>> {
        val new = mutableBoard(board)
        // src → dst: new[dst] = old[src]
        val mapping = listOf(
            // 바깥 궤도 (12칸)
            Pair(0,0) to Pair(1,0), Pair(0,1) to Pair(0,0), Pair(0,2) to Pair(0,1),
            Pair(0,3) to Pair(0,2), Pair(1,3) to Pair(0,3), Pair(2,3) to Pair(1,3),
            Pair(3,3) to Pair(2,3), Pair(3,2) to Pair(3,3), Pair(3,1) to Pair(3,2),
            Pair(3,0) to Pair(3,1), Pair(2,0) to Pair(3,0), Pair(1,0) to Pair(2,0),
            // 안쪽 궤도 (4칸)
            Pair(1,1) to Pair(2,1), Pair(1,2) to Pair(1,1),
            Pair(2,2) to Pair(1,2), Pair(2,1) to Pair(2,2)
        )
        for ((src, dst) in mapping) {
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

