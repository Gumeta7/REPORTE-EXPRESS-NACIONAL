package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Diamond
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    val iconTint: Color,
    val logoDrawableDark: Int? = null,
    val logoDrawableLight: Int? = null
)

@Composable
fun rememberVenueBrandStyle(venueName: String): VenueBrandStyle {
    return remember(venueName) {
        val upper = venueName.trim().uppercase()
        when {
            upper.contains("WINPOT") || upper.contains("CORPORATIVO") || upper.contains("CORP") || upper.contains("GDL") -> VenueBrandStyle(
                brandName = "WINPOT",
                shortCode = "WP",
                icon = Icons.Default.Stars,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
                borderColor = Color(0xFFE11D48),
                textColor = Color(0xFFF8FAFC),
                iconTint = Color(0xFFE11D48),
                logoDrawableDark = R.drawable.logo_winpot_dark,
                logoDrawableLight = R.drawable.logo_winpot_light
            )
            upper.contains("CAPRI") -> VenueBrandStyle(
                brandName = "CAPRI",
                shortCode = "CAP",
                icon = Icons.Default.Casino,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF4A0E17), Color(0xFF881337))),
                borderColor = Color(0xFFBE123C),
                textColor = Color(0xFFFFF1F2),
                iconTint = Color(0xFFFDA4AF),
                logoDrawableDark = R.drawable.logo_capri_dark,
                logoDrawableLight = R.drawable.logo_capri_light
            )
            upper.contains("DIAMOND") || upper.contains("DIAMONDS") -> VenueBrandStyle(
                brandName = "DIAMONDS",
                shortCode = "DM",
                icon = Icons.Default.Diamond,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF450A0A), Color(0xFF991B1B))),
                borderColor = Color(0xFFDC2626),
                textColor = Color(0xFFFEF2F2),
                iconTint = Color(0xFFF87171),
                logoDrawableDark = R.drawable.logo_diamonds,
                logoDrawableLight = R.drawable.logo_diamonds
            )
            upper.contains("VENETO") -> VenueBrandStyle(
                brandName = "VENETO",
                shortCode = "VN",
                icon = Icons.Default.Casino,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF1C1917), Color(0xFF44403C))),
                borderColor = Color(0xFFB45309),
                textColor = Color(0xFFFFFBEB),
                iconTint = Color(0xFFF59E0B),
                logoDrawableDark = R.drawable.logo_veneto,
                logoDrawableLight = R.drawable.logo_veneto
            )
            else -> VenueBrandStyle(
                brandName = if (upper.isNotBlank()) upper else "WINPOT",
                shortCode = if (upper.length >= 2) upper.take(2) else "WP",
                icon = Icons.Default.Stars,
                badgeGradient = Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
                borderColor = Color(0xFFE11D48),
                textColor = Color(0xFFF8FAFC),
                iconTint = Color(0xFFE11D48),
                logoDrawableDark = R.drawable.logo_winpot_dark,
                logoDrawableLight = R.drawable.logo_winpot_light
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
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val logoDrawable = if (isDarkTheme) style.logoDrawableDark else style.logoDrawableLight

    if (logoDrawable != null) {
        // Official Institutional Brand Logo Image - Clean, Direct & Unboxed
        Box(
            modifier = modifier.padding(horizontal = if (compact) 4.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = logoDrawable),
                contentDescription = style.brandName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(if (compact) 32.dp else 42.dp)
                    .widthIn(min = 60.dp, max = if (compact) 105.dp else 145.dp)
            )
        }
    } else {
        // Fallback Stylized Heraldic Badge
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
                    Icon(
                        imageVector = style.icon,
                        contentDescription = style.brandName,
                        tint = style.iconTint,
                        modifier = Modifier.size(if (compact) 14.dp else 18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

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