package com.example.ui.theme

import androidx.annotation.DrawableRes
import com.example.R

/**
 * Centralne miejsce na własne ikonki używane w aplikacji.
 * Aby podmienić dowolną ikonę na własną grafikę, zastąp odpowiedni plik
 * w katalogu app/src/main/res/drawable/ zachowując tę samą nazwę zasobu.
 */
object AppIcons {
    @DrawableRes val calendar: Int = R.drawable.ic_app_calendar
    @DrawableRes val notes: Int = R.drawable.ic_app_notes
    @DrawableRes val add: Int = R.drawable.ic_app_add
    @DrawableRes val event: Int = R.drawable.ic_app_event
    @DrawableRes val noteItem: Int = R.drawable.ic_app_note_item
}
