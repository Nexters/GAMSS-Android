package com.gamss.android.domain.analytics

/** iOS 와 같은 이름을 써야 한 퍼널로 묶인다. 이름을 바꾸면 iOS 에도 알려야 한다. */
object AnalyticsEvent {
    const val SCREEN_VIEW = "screen_view"

    const val LOGIN_CLICK = "login_click"
    const val LOGIN_SUCCESS = "login_success"
    const val LOGIN_FAIL = "login_fail"
    const val LOGIN_CANCEL = "login_cancel"

    const val ONBOARDING_NEXT = "onboarding_next"
    const val ONBOARDING_SKIP = "onboarding_skip"
    const val ONBOARDING_COMPLETE = "onboarding_complete"

    const val HOME_SUBMIT = "home_submit"
    const val HOME_SUBMIT_BLOCKED = "home_submit_blocked"
    const val HOME_SUBMIT_FAIL = "home_submit_fail"

    const val CHAT_SEND = "chat_send"
    const val CHAT_SEND_BLOCKED = "chat_send_blocked"
    const val CHAT_SEND_FAIL = "chat_send_fail"
    const val CHAT_END_REQUEST = "chat_end_request"
    const val CHAT_END_CANCEL = "chat_end_cancel"
    const val CHAT_END_CONFIRM = "chat_end_confirm"
    const val CARD_CREATE_SUCCESS = "card_create_success"
    const val CARD_CREATE_FAIL = "card_create_fail"
    const val CARD_FOLD_STEP = "card_fold_step"
    const val CARD_SET_ASIDE = "card_set_aside"
    const val CARD_REOPEN = "card_reopen"

    const val CHAT_LIST_CARD_CLICK = "chat_list_card_click"
    const val CHAT_SEARCH = "chat_search"
    const val CHAT_DELETE_CONFIRM = "chat_delete_confirm"

    const val ARCHIVE_MONTH_CHANGE = "archive_month_change"
    const val ARCHIVE_CARD_OPEN = "archive_card_open"
    const val ARCHIVE_CONVERSATION_VIEW = "archive_conversation_view"
    const val ARCHIVE_SHARE_CLICK = "archive_share_click"
    const val ARCHIVE_CARD_DISCARD = "archive_card_discard"
    const val ARCHIVE_CLEAR = "archive_clear"

    const val CARD_SHRED_TAP = "card_shred_tap"
    const val CARD_SHRED_SUCCESS = "card_shred_success"
    const val CARD_SHRED_FAIL = "card_shred_fail"
}

object AnalyticsProperty {
    const val SCREEN = "screen"
    const val PAGE = "page"
    const val EMOTION = "emotion"
    const val SCOPE = "scope"
    const val STEP = "step"
    const val STAGE = "stage"
    const val YEAR_MONTH = "year_month"
    const val TARGET = "target"
    const val TAP_COUNT = "tap_count"
}

object AnalyticsScope {
    private const val SINGLE = "single"
    private const val EMOTION_ALL = "emotion_all"

    fun of(cardId: Long?): String = if (cardId == null) EMOTION_ALL else SINGLE
}

val Enum<*>.analyticsValue: String get() = name.lowercase()
