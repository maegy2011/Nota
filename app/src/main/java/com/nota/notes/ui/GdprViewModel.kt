package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import com.nota.notes.data.GdprExportRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// P3-B: جسر Hilt لعدّاء التصدير — يُمرَّر من SettingsRoute إلى PrivacySection.
// لا منطق هنا — التجميع في GdprExportRunner، والنقي في GdprExport.
@HiltViewModel
class GdprViewModel @Inject constructor(
    val runner: GdprExportRunner
) : ViewModel()
