package com.gamss.android.feature.chat.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gamss.android.core.designsystem.component.chat.GamssLoadingMessageBubble
import com.gamss.android.core.ui.chat.CharacterAvatar
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.chat.R

/**
 * @param character 다음에 도착할 답장의 발신자. pendingComments의 첫 메시지 등으로 미리 알 때
 *  넘기면 빈 프로필 대신 그 캐릭터 아바타와 이름을 보여준다.
 */
@Composable
fun LoadingMessageBubble(character: EmotionCharacter? = null) {
    val senderStatus = if (character != null) {
        character.displayName
    } else {
        stringResource(R.string.chat_room_message_writing_label)
    }
    GamssLoadingMessageBubble(
        senderStatus = senderStatus,
        avatar = character?.let { { CharacterAvatar(it) } },
    )
}
