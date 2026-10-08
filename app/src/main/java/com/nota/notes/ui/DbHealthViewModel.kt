package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import com.nota.notes.data.DbHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// صحة القاعدة: يبلّغ عن الحجر الصحي ويُسقطه — الواجهة تعرض حوار الاستعادة
@HiltViewModel
class DbHealthViewModel @Inject constructor(
    private val holder: DbHolder
) : ViewModel() {
    val corruptionFound = holder.corruptionFound
    fun dismiss() = holder.dismissCorruption()
}
