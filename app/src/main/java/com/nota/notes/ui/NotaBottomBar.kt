package com.nota.notes.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import com.nota.notes.data.SettingsStore

// الشريط السفلي (قرار 26 سبتمبر 2026): 5 خانات — الثابتة للمجاني والمخصصة للمدفوع + زر الإضافة بارزًا
data class SlotMeta(val icon: ImageVector? = null, val drawable: Int? = null, @StringRes val label: Int)

fun slotMeta(key: String): SlotMeta = when (key) {
    DrawerKeys.LIST -> SlotMeta(Icons.Filled.Home, label = R.string.app_name)
    DrawerKeys.FAVORITES -> SlotMeta(Icons.Filled.FavoriteBorder, label = R.string.favorites_title)
    SettingsStore.NAV_ADD -> SlotMeta(Icons.Filled.Add, label = R.string.new_note_cd)
    DrawerKeys.REMINDERS -> SlotMeta(Icons.Filled.Notifications, label = R.string.mgr_reminders)
    DrawerKeys.SELF -> SlotMeta(Icons.Filled.ChatBubbleOutline, label = R.string.self_chat_title)
    DrawerKeys.SETTINGS -> SlotMeta(Icons.Filled.Settings, label = R.string.settings_cd)
    DrawerKeys.ARCHIVE -> SlotMeta(drawable = R.drawable.ic_archive, label = R.string.archive_title)
    DrawerKeys.FOLDERS -> SlotMeta(drawable = R.drawable.ic_folder, label = R.string.folders_title)
    DrawerKeys.CATEGORIES -> SlotMeta(Icons.Filled.Label, label = R.string.cat_title)
    DrawerKeys.TAGS -> SlotMeta(Icons.Filled.Sell, label = R.string.mgr_tags)
    DrawerKeys.BACKUP -> SlotMeta(Icons.Filled.Backup, label = R.string.backup_screen_title)
    DrawerKeys.ABOUT -> SlotMeta(Icons.Filled.Info, label = R.string.about_title)
    else -> SlotMeta(Icons.Filled.Home, label = R.string.app_name)
}

// الوجهات المدفوعة — تُوسم بقفل للمجاني (قاعدة القفل المرئي 26 سبتمبر 2026)
private val PRO_ONLY_SLOTS = setOf(
    DrawerKeys.FAVORITES, DrawerKeys.REMINDERS, DrawerKeys.TAGS, DrawerKeys.FOLDERS, DrawerKeys.BACKUP
)

@Composable
fun NotaBottomBar(
    slots: List<String>,
    selectedKey: String,
    isPro: Boolean,
    onNavigate: (String) -> Unit,
    onAdd: () -> Unit
) {
    val hctx = androidx.compose.ui.platform.LocalContext.current
    NavigationBar {
        slots.take(5).forEach { key ->
            val meta = slotMeta(key)
            val isAdd = key == SettingsStore.NAV_ADD
            val locked = !isPro && key in PRO_ONLY_SLOTS
            // أيقونات فقط بلا نصوص (قرار 26 سبتمبر 2026) — contentDescription يحمل الاسم لقارئ الشاشة
            NavigationBarItem(
                selected = !isAdd && key == selectedKey,
                onClick = { Haptics.tick(hctx); if (isAdd) onAdd() else onNavigate(key) },
                icon = {
                    val desc = stringResource(meta.label)
                    val icon: @Composable () -> Unit = {
                        if (meta.icon != null) Icon(meta.icon, contentDescription = desc, modifier = Modifier.size(26.dp))
                        else if (meta.drawable != null) Icon(painterResource(meta.drawable), contentDescription = desc, modifier = Modifier.size(26.dp))
                    }
                    if (locked) BadgedBox(badge = { Badge { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(10.dp)) } }) { icon() }
                    else icon()
                },
                alwaysShowLabel = false
            )
        }
    }
}
