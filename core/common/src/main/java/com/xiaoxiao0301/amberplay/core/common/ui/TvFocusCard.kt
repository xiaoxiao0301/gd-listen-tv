package com.xiaoxiao0301.amberplay.core.common.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xiaoxiao0301.amberplay.core.common.theme.Purple

/**
 * TV 通用焦点卡片：聚焦时显示紫色边框 + 轻微放大
 */
@Composable
fun TvFocusCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.08f else 1.0f)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Purple else Color.Transparent,
                shape = shape,
            )
            .clip(shape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .animateContentSize(),
        content = content,
    )
}
