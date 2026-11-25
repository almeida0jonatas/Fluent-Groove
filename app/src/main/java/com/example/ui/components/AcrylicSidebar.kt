package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AcrylicDensity
import com.example.model.MyMusicTab
import com.example.model.Playlist
import com.example.model.ScreenRoute
import com.example.ui.theme.FgBorder
import com.example.ui.theme.FgBorderStrong
import com.example.ui.theme.FgIsDark
import com.example.ui.theme.FgPrimaryDeep
import com.example.ui.theme.FgText

@Composable
fun AcrylicSidebar(
    expanded: Boolean,
    isDrawerMode: Boolean,
    acrylicDensity: AcrylicDensity,
    route: ScreenRoute,
    playlists: List<Playlist>,
    onToggleExpanded: () -> Unit,
    onCloseDrawer: () -> Unit,
    onNavigate: (ScreenRoute) -> Unit,
    onRequestCreatePlaylist: () -> Unit,
    onImportFiles: () -> Unit,
    onImportFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effectiveExpanded = if (isDrawerMode) true else expanded
    val widthModifier = if (isDrawerMode) {
        Modifier.width(276.dp)
    } else {
        Modifier.width(if (expanded) 248.dp else 56.dp)
    }

    Row(
        modifier = modifier
            .then(widthModifier)
            .fillMaxHeight()
            .fluentAcrylic(acrylicDensity, verticalGradient = true)
            .then(if (isDrawerMode) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            // Top menu toggle / header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { if (isDrawerMode) onCloseDrawer() else onToggleExpanded() },
                    modifier = Modifier.testTag("sidebar_toggle_button"),
                ) {
                    Icon(
                        imageVector = if (isDrawerMode) Icons.Filled.Close else Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.cd_open_menu),
                        tint = FgText,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (isDrawerMode) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FgText,
                        modifier = Modifier.padding(end = 14.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary navigation items
            SidebarNavItem(
                icon = Icons.Filled.LibraryMusic,
                label = stringResource(R.string.nav_my_music),
                selected = route is ScreenRoute.MyMusic || route is ScreenRoute.AlbumDetail || route is ScreenRoute.ArtistDetail,
                expanded = effectiveExpanded,
                testTag = "nav_my_music",
                onClick = {
                    onNavigate(ScreenRoute.MyMusic(MyMusicTab.HOME))
                    if (isDrawerMode) onCloseDrawer()
                },
            )
            SidebarNavItem(
                icon = Icons.Filled.History,
                label = stringResource(R.string.nav_recent_plays),
                selected = route is ScreenRoute.RecentPlays,
                expanded = effectiveExpanded,
                testTag = "nav_recent_plays",
                onClick = {
                    onNavigate(ScreenRoute.RecentPlays)
                    if (isDrawerMode) onCloseDrawer()
                },
            )
            SidebarNavItem(
                icon = Icons.Filled.GraphicEq,
                label = stringResource(R.string.nav_now_playing),
                selected = route is ScreenRoute.NowPlaying,
                expanded = effectiveExpanded,
                testTag = "nav_now_playing",
                onClick = {
                    onNavigate(ScreenRoute.NowPlaying)
                    if (isDrawerMode) onCloseDrawer()
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(1.dp)
                    .background(FgBorderStrong),
            )

            // Playlists section
            if (effectiveExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.nav_playlists),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = FgText,
                    )
                    IconButton(
                        onClick = onRequestCreatePlaylist,
                        modifier = Modifier.testTag("sidebar_create_playlist_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_new_playlist),
                            tint = FgText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(playlists, key = { it.id }) { pl ->
                    val isSelected = route is ScreenRoute.PlaylistDetail && route.playlistId == pl.id
                    SidebarNavItem(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        label = pl.name,
                        selected = isSelected,
                        expanded = effectiveExpanded,
                        testTag = "nav_playlist_${pl.id}",
                        onClick = {
                            onNavigate(ScreenRoute.PlaylistDetail(pl.id))
                            if (isDrawerMode) onCloseDrawer()
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .height(1.dp)
                    .background(FgBorderStrong),
            )

            // Import & Settings footer actions
            SidebarNavItem(
                icon = Icons.Filled.FileUpload,
                label = stringResource(R.string.action_import_files),
                selected = false,
                expanded = effectiveExpanded,
                testTag = "sidebar_import_files",
                onClick = {
                    onImportFiles()
                    if (isDrawerMode) onCloseDrawer()
                },
            )
            SidebarNavItem(
                icon = Icons.Filled.CreateNewFolder,
                label = stringResource(R.string.action_import_folder),
                selected = false,
                expanded = effectiveExpanded,
                testTag = "sidebar_import_folder",
                onClick = {
                    onImportFolder()
                    if (isDrawerMode) onCloseDrawer()
                },
            )
            SidebarNavItem(
                icon = Icons.Filled.Settings,
                label = stringResource(R.string.nav_settings),
                selected = route is ScreenRoute.Settings,
                expanded = effectiveExpanded,
                testTag = "nav_settings",
                onClick = {
                    onNavigate(ScreenRoute.Settings)
                    if (isDrawerMode) onCloseDrawer()
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Subtle vertical acrylic separator
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(FgBorder),
        )
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                if (selected) {
                    if (FgIsDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.34f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Signature Windows 10 Fluent active accent indicator bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(22.dp)
                .background(if (selected) FgPrimaryDeep else Color.Transparent),
        )
        Spacer(modifier = Modifier.width(13.dp))
        Icon(
            imageVector = icon,
            contentDescription = if (!expanded) label else null,
            tint = if (selected) FgPrimaryDeep else FgText,
            modifier = Modifier.size(20.dp),
        )
        if (expanded) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (selected) FgPrimaryDeep else FgText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}
