package com.nota.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nota.notes.R

// مفاتيح وجهات الدرج — تُطابق شاشات App (String لأن Screen enum خاص بـ App)
object DrawerKeys {
    const val LIST = "list"
    const val ARCHIVE = "archive"
    const val TRASH = "trash"
    const val FOLDERS = "folders"
    const val CATEGORIES = "categories"
    const val TAGS = "tags"
    const val FAVORITES = "favorites"
    const val REMINDERS = "reminders"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val ACCOUNT = "account"
    const val SELF = "self"
}

// القائمة الجانبية الموحدة: كل التنقل مسمى بأيقونة مفهومة بدل أزرار الشريط المبهمة
// المدفوعة بشارة قفل لغير المشترك — والضغطة تعبر البوابة نفسها (paywall عند اللزوم)
@Composable
fun NotaDrawerContent(
    selectedKey: String,
    accountId: String?,
    accountLabel: String?,
    isPro: Boolean,
    canFolders: Boolean,
    onNavigate: (String) -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        // ترويسة العلامة: الشعار + اسم التطبيق + الإصدار — أعلى القائمة (قرار 23 سبتمبر 2026)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val ver = remember {
            runCatching {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
            }.getOrDefault("")
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandHeader(size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (ver.isNotBlank()) {
                    Text(
                        "v$ver",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // الترويسة: أفاتار الحساب (صورته أو حرفه) + البريد + شارة الباقة — الضغطة لشاشة الحساب
        Row(
            Modifier.fillMaxWidth().clickable { onNavigate(DrawerKeys.ACCOUNT) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAvatar(accountId = accountId, label = accountLabel, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (accountLabel.isNullOrBlank()) stringResource(R.string.acc_signed_out) else accountLabel,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    if (isPro) stringResource(R.string.acc_plan_pro) else stringResource(R.string.acc_plan_free),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        // فاصل بعد الترويسة — بنية أوضح للقائمة الطويلة (27 سبتمبر 2026)
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        DrawerEntry(
            key = DrawerKeys.LIST, selectedKey = selectedKey,
            label = stringResource(R.string.app_name),
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.ARCHIVE, selectedKey = selectedKey,
            label = stringResource(R.string.archive_title),
            icon = { Icon(painterResource(R.drawable.ic_archive), contentDescription = null) },
            onNavigate = onNavigate
        )
        // السلة بعد الأرشيف مباشرة (27 سبتمبر 2026)
        DrawerEntry(
            key = DrawerKeys.TRASH, selectedKey = selectedKey,
            label = stringResource(R.string.trash_title),
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.FOLDERS, selectedKey = selectedKey,
            label = stringResource(R.string.folders_title),
            icon = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
            locked = !canFolders,
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.CATEGORIES, selectedKey = selectedKey,
            label = stringResource(R.string.cat_title),
            icon = { Icon(Icons.Filled.Label, contentDescription = null) },
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.TAGS, selectedKey = selectedKey,
            label = stringResource(R.string.mgr_tags),
            icon = { Icon(Icons.Filled.Sell, contentDescription = null) },
            locked = !isPro,
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.FAVORITES, selectedKey = selectedKey,
            label = stringResource(R.string.favorites_title),
            icon = { Icon(Icons.Filled.FavoriteBorder, contentDescription = null) },
            locked = !isPro,
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.REMINDERS, selectedKey = selectedKey,
            label = stringResource(R.string.mgr_reminders),
            icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
            locked = !isPro,
            onNavigate = onNavigate
        )
        // رسائل النفس (27 سبتمبر 2026): محادثة المستخدم مع نفسه — مجانية للجميع
        DrawerEntry(
            key = DrawerKeys.SELF, selectedKey = selectedKey,
            label = stringResource(R.string.self_chat_title),
            icon = { Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null) },
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.BACKUP, selectedKey = selectedKey,
            label = stringResource(R.string.backup_screen_title),
            icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
            locked = !isPro,
            onNavigate = onNavigate
        )
        DrawerEntry(
            key = DrawerKeys.SETTINGS, selectedKey = selectedKey,
            label = stringResource(R.string.settings_cd),
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            onNavigate = onNavigate
        )
        // عن التطبيق: بند مستقل في الدرج — فُك ارتباطه بشاشة الإعدادات (قرار 23 سبتمبر 2026)
        DrawerEntry(
            key = DrawerKeys.ABOUT, selectedKey = selectedKey,
            label = stringResource(R.string.about_title),
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            onNavigate = onNavigate
        )
        // بند الحساب المستقل محذوف: الترويسة (الصورة + البريد + الباقة) تفتح شاشة الحساب — بلا تكرار
        // حشوة سفلية سخية: آخر بند (عن التطبيق) يظهر كاملًا فوق حافة اللوحة (27 سبتمبر 2026)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DrawerEntry(
    key: String,
    selectedKey: String,
    label: String,
    icon: @Composable () -> Unit,
    locked: Boolean = false,
    onNavigate: (String) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = (selectedKey == key),
        onClick = { onNavigate(key) },
        icon = icon,
        badge = if (locked) {
            {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        } else null,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
