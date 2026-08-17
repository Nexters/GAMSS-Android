package com.gamss.android.core.designsystem.component

import androidx.annotation.DrawableRes
import com.gamss.android.core.designsystem.R

object GamssIcons {
    @DrawableRes
    val Logo: Int = R.drawable.ic_gamss_logo

    @DrawableRes
    val Setting: Int = R.drawable.ic_setting

    /** 배경까지 담긴 32x32 버튼 에셋. 2색이라 tint 하지 않는다. */
    @DrawableRes
    val SendButtonOn: Int = R.drawable.ic_send_button_on

    @DrawableRes
    val SendButtonOff: Int = R.drawable.ic_send_button_off

    @DrawableRes
    val TabArchive: Int = R.drawable.ic_tab_archive

    @DrawableRes
    val TabHome: Int = R.drawable.ic_tab_home

    @DrawableRes
    val TabChat: Int = R.drawable.ic_tab_chat

    @DrawableRes
    val RightChevron: Int = R.drawable.ic_right_chevron

    /** 선으로만 그린 X. 원형으로 채운 지우기 버튼(ic_close)과 다른 에셋이다. */
    @DrawableRes
    val Close: Int = R.drawable.ic_close_line

    /** 두 가지 색이 들어 있어 tint 하지 않는다. 채움은 gray700, 체크는 gray025. */
    @DrawableRes
    val CheckCircleOn: Int = R.drawable.ic_check_circle_on

    /** 테두리와 체크 모두 gray500. */
    @DrawableRes
    val CheckCircleOff: Int = R.drawable.ic_check_circle_off

    @DrawableRes
    val ClearButton: Int = R.drawable.ic_close
}
