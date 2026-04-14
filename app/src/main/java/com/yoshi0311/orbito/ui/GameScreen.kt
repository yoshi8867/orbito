package com.yoshi0311.orbito.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yoshi0311.orbito.model.GamePhase
import com.yoshi0311.orbito.model.GameState
import com.yoshi0311.orbito.model.Player
import com.yoshi0311.orbito.ui.theme.AppBackground
import com.yoshi0311.orbito.ui.theme.BlackBall
import com.yoshi0311.orbito.ui.theme.WhiteBall
import com.yoshi0311.orbito.viewmodel.GameViewModel

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.background(AppBackground)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet  = maxWidth >= 600.dp
            val cellSize  = if (isTablet) minOf(maxWidth * 0.45f, maxHeight * 0.58f) / 4
                            else (maxWidth - 120.dp) / 4
            val ballSize      = cellSize * 0.68f
            val sideBallSize  = if (isTablet) 24.dp else 18.dp
            val boardWidth    = cellSize * 4 + 12.dp   // 4셀 + 3간격(4dp each)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TurnIndicator(state)

                Spacer(Modifier.height(20.dp))

                TimerBar(timeLeft = state.timeLeft, width = boardWidth)

                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SideBallsPanel(
                        count     = state.whiteSideCount,
                        ballColor = WhiteBall,
                        ballSize  = sideBallSize,
                        isTablet  = isTablet
                    )
                    Spacer(Modifier.width(10.dp))
                    BoardGrid(
                        state              = state,
                        cellSize           = cellSize,
                        ballSize           = ballSize,
                        onCellTap          = viewModel::onCellTap,
                        onRotationComplete = viewModel::onRotationComplete
                    )
                    Spacer(Modifier.width(10.dp))
                    SideBallsPanel(
                        count     = state.blackSideCount,
                        ballColor = BlackBall,
                        ballSize  = sideBallSize,
                        isTablet  = isTablet
                    )
                }

                Spacer(Modifier.height(24.dp))

                TextButton(onClick = viewModel::restart) {
                    Text(
                        text = "RESTART",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        if (state.winner != null) {
            WinnerOverlay(
                winner    = state.winner!!,
                isTimeout = state.timeLeft == 0,
                onRestart = viewModel::restart
            )
        }
    }
}

// ── 차례 + 타이머 숫자 표시 ───────────────────────────────────────────────────

@Composable
private fun TurnIndicator(state: GameState) {
    if (state.phase == GamePhase.DONE) return

    val playerColor = if (state.currentPlayer == Player.WHITE) WhiteBall else BlackBall
    val label = when {
        state.phase == GamePhase.OPTIONAL_MOVE && state.selectedCell != null ->
            "TAP ADJACENT CELL"
        state.phase == GamePhase.OPTIONAL_MOVE ->
            if (state.currentPlayer == Player.WHITE) "WHITE  ·  MOVE OR PLACE"
            else "BLACK  ·  MOVE OR PLACE"
        else ->
            if (state.currentPlayer == Player.WHITE) "WHITE  ·  PLACE YOUR BALL"
            else "BLACK  ·  PLACE YOUR BALL"
    }
    val timerColor = if (state.timeLeft <= 5) Color(0xFFFF6B6B) else Color.White

    Row(
        modifier = Modifier
            .background(Color(0x26000000), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Ball(color = playerColor, size = 10.dp)
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = state.timeLeft.toString(),
            color = timerColor,
            fontSize = if (state.timeLeft <= 5) 14.sp else 12.sp,
            fontWeight = if (state.timeLeft <= 5) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── 타이머 바 ────────────────────────────────────────────────────────────────

@Composable
private fun TimerBar(timeLeft: Int, width: Dp) {
    val fraction = (timeLeft / 20f).coerceIn(0f, 1f)
    val barColor = if (timeLeft <= 5) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .width(width)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxSize(fraction)   // width fraction
                .background(barColor)
        )
    }
}

// ── 승리 오버레이 ─────────────────────────────────────────────────────────────

@Composable
private fun WinnerOverlay(winner: Player, isTimeout: Boolean, onRestart: () -> Unit) {
    val winnerColor = if (winner == Player.WHITE) WhiteBall else BlackBall
    val winnerName  = if (winner == Player.WHITE) "WHITE" else "BLACK"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color(0x26FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 48.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isTimeout) "TIME OUT" else "WINNER",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                letterSpacing = 3.sp
            )
            Ball(color = winnerColor, size = 36.dp)
            Text(
                text = winnerName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp
            )
            TextButton(onClick = onRestart) {
                Text(
                    text = "PLAY AGAIN",
                    color = Color.White,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
