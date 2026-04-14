package com.yoshi0311.orbito.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yoshi0311.orbito.model.CellState
import com.yoshi0311.orbito.model.GamePhase
import com.yoshi0311.orbito.model.GameState
import com.yoshi0311.orbito.model.Player
import com.yoshi0311.orbito.ui.theme.BlackBall
import com.yoshi0311.orbito.ui.theme.CellAdjacent
import com.yoshi0311.orbito.ui.theme.CellNormal
import com.yoshi0311.orbito.ui.theme.CellSelected
import com.yoshi0311.orbito.ui.theme.WhiteBall

@Composable
fun BoardGrid(
    state: GameState,
    cellSize: Dp,
    ballSize: Dp,
    onCellTap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0..3) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0..3) {
                    GameCell(
                        cellState = state.board[r][c],
                        isSelected = state.selectedCell == Pair(r, c),
                        isAdjacentTarget = isAdjacentTarget(state, r, c),
                        cellSize = cellSize,
                        ballSize = ballSize,
                        enabled = state.phase != GamePhase.DONE,
                        onClick = { onCellTap(r, c) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCell(
    cellState: CellState,
    isSelected: Boolean,
    isAdjacentTarget: Boolean,
    cellSize: Dp,
    ballSize: Dp,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected       -> CellSelected
        isAdjacentTarget -> CellAdjacent
        else             -> CellNormal
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when (cellState) {
            CellState.WHITE -> Ball(color = WhiteBall, size = ballSize)
            CellState.BLACK -> Ball(color = BlackBall, size = ballSize)
            CellState.EMPTY -> {}
        }
    }
}

@Composable
fun Ball(color: Color, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
    )
}

private fun isAdjacentTarget(state: GameState, r: Int, c: Int): Boolean {
    if (state.phase != GamePhase.OPTIONAL_MOVE) return false
    val sel = state.selectedCell ?: return false
    if (state.board[r][c] != CellState.EMPTY) return false
    return kotlin.math.abs(sel.first - r) + kotlin.math.abs(sel.second - c) == 1
}
