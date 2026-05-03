package mikun.fcztk.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassBottomNavigationBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<GlassTabItem>,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = true
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    color = if (isDarkTheme)
                        Color(0xFF121212).copy(alpha = 0.7f)
                    else
                        Color(0xFFFAFAFA).copy(alpha = 0.8f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.1f)
                    else
                        Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(28.dp)
                )
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                GlassTabItem(
                    isSelected = selectedTabIndex == index,
                    icon = tab.icon,
                    label = tab.label,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
fun RowScope.GlassTabItem(
    isSelected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val selectedColor = if (isDarkTheme) Color(0xFF0091FF) else Color(0xFF0088FF)
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(50))
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) selectedColor else contentColor.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) selectedColor else contentColor.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .fillMaxWidth()
            .height(75.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    clip = true
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    color = if (isDarkTheme)
                        Color(0xFF121212).copy(alpha = 0.9f)
                    else
                        Color(0xFFFAFAFA).copy(alpha = 0.95f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.1f)
                    else
                        Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(22.dp)
                )
        )
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(20.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    fontSize = 20.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
            
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailingIcon()
            }
        }
    }
}

data class GlassTabItem(
    val icon: ImageVector,
    val label: String
)
