package com.example.maszyna_stanow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.maszyna_stanow.model.StateNode
import kotlin.math.roundToInt

@Composable
fun StateNodeView(
    stateNode: StateNode,
    isSelected: Boolean,
    isMoveMode: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isActive -> Color(0xFFFFD700) // Złoty kolor dla aktywnego stanu
        isMoveMode && isSelected -> Color.Green
        isSelected -> Color.Blue
        else -> Color.Black
    }
    
    val backgroundColor = when {
        stateNode.isInitial -> Color(0xFFBBDEFB)
        stateNode.isFinal -> Color(0xFFFFCCBC)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(stateNode.position.x.roundToInt(), stateNode.position.y.roundToInt()) }
            .size(80.dp)
            .background(backgroundColor, CircleShape)
            // Pogrubiona obwódka dla aktywnego stanu
            .border(if (isActive) 6.dp else if (stateNode.isInitial) 4.dp else 2.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = stateNode.name)
    }
}
