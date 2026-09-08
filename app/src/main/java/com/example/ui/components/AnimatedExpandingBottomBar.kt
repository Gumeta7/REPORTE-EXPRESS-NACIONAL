package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

data class NavigationTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AnimatedExpandingBottomBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    items: List<NavigationTabItem>,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val barBgColor = if (isDarkTheme) Color(0xFF13131A) else MaterialTheme.colorScheme.surface
    val barBorder = if (isDarkTheme) BorderStroke(1.dp, Color(0xFF262636)) else BorderStroke(1.dp, Color(0xFFE2E8F0))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        color = barBgColor,
        border = barBorder,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index

                val weight by animateFloatAsState(
                    targetValue = if (isSelected) 2.4f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "tab_weight_${item.title}"
                )

                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "tab_icon_scale_${item.title}"
                )

                val activePillBrush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF5E5BF7), // Electric Indigo
                        Color(0xFF4743EB)  // Electric Indigo Dark
                    )
                )

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(activePillBrush)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onItemSelected(index)
                        }
                        .padding(horizontal = if (isSelected) 8.dp else 4.dp)
                        .testTag(item.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) Color.White else (if (isDarkTheme) Slate400 else Color(0xFF64748B)),
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    expandHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
                            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessHigh)) +
                                    shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessHigh))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}