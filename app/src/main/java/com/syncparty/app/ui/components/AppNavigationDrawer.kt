package com.syncparty.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syncparty.app.data.local.UserProfile
import com.syncparty.app.theme.*

enum class AppDestination(val title: String, val icon: ImageVector, val color: Color) {
    HOME("Home Dashboard", Icons.Default.Home, PrimaryPurple),
    CRUNCHYROLL("Crunchyroll Hub", Icons.Default.LockOpen, AccentOrange),
    INSTAGRAM("Instagram & Socials", Icons.Default.CameraAlt, AccentPink),
    YOUTUBE("YouTube Party Studio", Icons.Default.PlayCircle, DangerRed),
    SCREEN_SHARE("Screen Share Studio", Icons.AutoMirrored.Filled.ScreenShare, AccentCyan),
    HISTORY("Watch History", Icons.Default.History, AccentGreen),
    SETTINGS("Settings & Updates", Icons.Default.Settings, TextSecondaryDark)
}

@Composable
fun AppDrawerContent(
    userProfile: UserProfile,
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    onSignOut: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.fillMaxHeight().width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // User Profile Header in Drawer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp, top = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PrimaryPurple, AccentPink))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = AccentGreen, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = userProfile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Items List
                AppDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                tint = if (isSelected) destination.color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            onNavigate(destination)
                            onCloseDrawer()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = destination.color.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Bottom Logout Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerRed.copy(alpha = 0.12f))
                    .clickable {
                        onSignOut()
                        onCloseDrawer()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerRed)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
            }
        }
    }
}
