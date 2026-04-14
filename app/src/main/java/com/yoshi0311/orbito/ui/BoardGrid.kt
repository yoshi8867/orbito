package com.yoshi0311.orbito.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.yoshi0311.orbito.model.CellState
import com.yoshi0311.orbito.model.GamePhase
import com.yoshi0311.orbito.model.GameState
import com.yoshi0311.orbito.model.Player
import com.yoshi0311.orbito.model.ROTATION_MAPPING
import com.yoshi0311.orbito.ui.theme.BlackBall
import com.yoshi0311.orbito.ui.theme.CellAdjacent
import com.yoshi0311.orbito.ui.theme.CellNormal
import com.yoshi0311.orbito.ui.theme.CellSelected
import com.yoshi0311.orbito.ui.theme.WhiteBall

private val GAP = 4.dp

@Composable
fun BoardGrid(
    state: GameState,
    cellSize: Dp,
    ballSize: Dp,
    onCellTap: (Int, Int) -> Unit,
    onRotationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boardSize = cellSize * 4 + GAP * 3
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    val gapPx     = with(density) { GAP.toPx() }
    val ballSizePx = with(density) { ballSize.toPx() }
    val step = cellSizePx + gapPx

    fun cellCenter(r: Int, c: Int) = Offset(
        x = c * step + cellSizePx / 2f,
        y = r * step + cellSizePx / 2f
    )

    // 회전 애니메이션 progress (0f → 1f)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(state.isRotating) {
        if (state.isRotating) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            onRotationComplete()
        }
    }

    Box(modifier = modifier.size(boardSize)) {

        // ── 셀 격자 ──────────────────────────────────────────────────────────
        // 회전 중에는 공을 숨기고 Canvas 오버레이에서 그림
        Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
            for (r in 0..3) {
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    for (c in 0..3) {
                        val displayState = if (state.isRotating) CellState.EMPTY else state.board[r][c]
                        GameCell(
                            cellState        = displayState,
                            isSelected       = !state.isRotating && state.selectedCell == Pair(r, c),
                            isAdjacentTarget = !state.isRotating && isAdjacentTarget(state, r, c),
                            cellSize         = cellSize,
                            ballSize         = ballSize,
                            enabled          = !state.isRotating && state.phase != GamePhase.DONE,
                            onClick          = { onCellTap(r, c) }
                        )
                    }
                }
            }
        }

        // ── 회전 애니메이션 오버레이 ──────────────────────────────────────────
        if (state.isRotating) {
            val preBoard = state.boardBeforeRotation ?: state.board
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (r in 0..3) {
                    for (c in 0..3) {
                        val preState = preBoard[r][c]
                        if (preState == CellState.EMPTY) continue
                        val dst = ROTATION_MAPPING[Pair(r, c)] ?: continue

                        val ballColor = if (preState == CellState.WHITE) Color.White else Color(0xFF212121)
                        val start = cellCenter(r, c)
                        val end   = cellCenter(dst.first, dst.second)
                        val cx = lerp(start.x, end.x, progress.value)
                        val cy = lerp(start.y, end.y, progress.value)

                        // 드롭 섀도
                        drawCircle(
                            color  = Color.Black.copy(alpha = 0.22f),
                            radius = ballSizePx / 2f + 2f,
                            center = Offset(cx, cy + 3f)
                        )
                        // 공 본체
                        drawCircle(
                            color  = ballColor,
                            radius = ballSizePx / 2f,
                            center = Offset(cx, cy)
                        )
                        // 하이라이트 (흰 공은 약하게, 검정 공은 약간 강하게)
                        val hlAlpha = if (preState == CellState.WHITE) 0.12f else 0.18f
                        drawCircle(
                            color  = Color.White.copy(alpha = hlAlpha),
                            radius = ballSizePx / 4f,
                            center = Offset(cx - ballSizePx * 0.12f, cy - ballSizePx * 0.12f)
                        )
                    }
                }
            }
        }
    }
}

// ── 개별 셀 ──────────────────────────────────────────────────────────────────

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

// ── 유틸 ──────────────────────────────────────────────────────────────────────

private fun isAdjacentTarget(state: GameState, r: Int, c: Int): Boolean {
    if (state.phase != GamePhase.OPTIONAL_MOVE) return false
    val sel = state.selectedCell ?: return false
    if (state.board[r][c] != CellState.EMPTY) return false
    return kotlin.math.abs(sel.first - r) + kotlin.math.abs(sel.second - c) == 1
}
