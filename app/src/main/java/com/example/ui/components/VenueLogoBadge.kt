package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

data class VenueBrandStyle(
    val brandName: String,
    val shortCode: String,
    val icon: ImageVector,
    val badgeGradient: Brush,
    val borderColor: Color,
    val textColor: Color,
    val iconTint: Color
)

@Composable
fun rememberVenueBrandStyle(venueName: String): VenueBrandStyle {
    return remember(venueName) {
        val upper = venueName.trim().uppercase()
        when {
            upper.contains("WINPOT") -> VenueBrandStyle(
                brandName = "WINPOT",
                shortCode = "WP",
                icon = Icons.Default.Stars,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0284C7))),
                borderColor = Color(0xFF38BDF8),
                textColor = Color(0xFFF8FAFC),
                iconTint = Color(0xFF38BDF8)
            )
            upper.contains("CALIENTE") -> VenueBrandStyle(
                brandName = "CALIENTE",
                shortCode = "CAL",
                icon = Icons.Default.LocalFireDepartment,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF7F1D1D), Color(0xFFDC2626))),
                borderColor = Color(0xFFEF4444),
                textColor = Color(0xFFFFFFFF),
                iconTint = Color(0xFFFCA5A5)
            )
            upper.contains("GOLDEN") || upper.contains("ISLAND") -> VenueBrandStyle(
                brandName = "GOLDEN ISLAND",
                shortCode = "GI",
                icon = Icons.Default.Diamond,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF78350F), Color(0xFFD97706))),
                borderColor = Color(0xFFFBBF24),
                textColor = Color(0xFFFFFBEB),
                iconTint = Color(0xFFFDE68A)
            )
            upper.contains("PLAY CITY") || upper.contains("PLAYCITY") -> VenueBrandStyle(
                brandName = "PLAY CITY",
                shortCode = "PC",
                icon = Icons.Default.Casino,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF4C1D95), Color(0xFF7C3AED))),
                borderColor = Color(0xFFA78BFA),
                textColor = Color(0xFFFAF5FF),
                iconTint = Color(0xFFDDD6FE)
            )
            upper.contains("CIRSA") -> VenueBrandStyle(
                brandName = "CIRSA",
                shortCode = "CR",
                icon = Icons.Default.Shield,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF064E3B), Color(0xFF059669))),
                borderColor = Color(0xFF34D399),
                textColor = Color(0xFFF0FDF4),
                iconTint = Color(0xFFA7F3D0)
            )
            else -> VenueBrandStyle(
                brandName = if (upper.isNotBlank()) upper else "CORPORATIVO",
                shortCode = if (upper.length >= 2) upper.take(2) else "CORP",
                icon = Icons.Default.Storefront,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF334155))),
                borderColor = Color(0xFF64748B),
                textColor = Color(0xFFF8FAFC),
                iconTint = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
fun VenueLogoBadge(
    venueName: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val style = rememberVenueBrandStyle(venueName)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, style.borderColor.copy(alpha = 0.5f)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(style.badgeGradient)
                .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 4.dp else 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Brand Icon
                Icon(
                    imageVector = style.icon,
                    contentDescription = style.brandName,
                    tint = style.iconTint,
                    modifier = Modifier.size(if (compact) 14.dp else 18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Brand Name / Monogram
                Column {
                    Text(
                        text = style.brandName,
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = style.textColor,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!compact && venueName.trim().uppercase() != style.brandName) {
                        val subName = venueName.trim().uppercase().removePrefix(style.brandName).trim()
                        if (subName.isNotBlank()) {
                            Text(
                                text = subName,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = style.textColor.copy(alpha = 0.8f),
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

@Composable
fun TechnicianMonogramAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val initials = remember(name) {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.isEmpty() -> "US"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
        }
    }

    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = Slate800,
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                Brush.radialGradient(
                    listOf(Color(0xFF1E3A8A), Slate900)
                )
            )
        ) {
            Text(
                text = initials,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.38).sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}