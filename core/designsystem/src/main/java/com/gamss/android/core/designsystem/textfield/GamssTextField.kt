package com.gamss.android.core.designsystem.textfield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 라벨 텍스트 + 입력 필드 조합의 입력 필드.
 * 라벨이 필드 위에 별도로 표시되는 형태
 *
 * 시안의 필드는 높이가 52dp라, 최소 높이 56dp에 자체 여백을 가진
 * [androidx.compose.material3.OutlinedTextField] 대신 [BasicTextField]로 그린다.
 *
 * 테두리는 손그림 시안이라 선 두께로 그릴 수 없어 `bg_textfield_default` 벡터를 깔고
 * gray950 으로 틴트한다. 덕분에 다크 모드에서도 색만 반전된다.
 */
@Composable
fun GamssTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = GamssTheme.typography.body4Medium,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = GamssTheme.typography.body3Medium.copy(color = GamssTheme.colors.gray950),
            cursorBrush = SolidColor(GamssTheme.colors.gray950),
        ) { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = FieldMinHeight)
                    .background(GamssTheme.colors.gray025),
                contentAlignment = Alignment.CenterStart,
            ) {
                // matchParentSize 는 부모 크기 계산에 참여하지 않아, 필드 높이는 그대로 내용이
                // 정하고 테두리만 정해진 크기에 맞춰 늘어난다. Modifier.paint 는 반대로
                // 남은 공간을 꽉 채우도록 제약을 고정해 버려 쓸 수 없다.
                Image(
                    modifier = Modifier.matchParentSize(),
                    painter = painterResource(R.drawable.bg_textfield_default),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(GamssTheme.colors.gray950),
                )
                Box(modifier = Modifier.padding(horizontal = GamssTheme.spacing.spacing300)) {
                    if (placeholder != null && value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = GamssTheme.typography.body3Medium,
                            color = GamssTheme.colors.gray400,
                        )
                    }
                    innerTextField()
                }
            }
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing100))
            Text(
                text = errorMessage,
                style = GamssTheme.typography.body5Medium,
                color = GamssTheme.colors.red,
            )
        }
    }
}

private val FieldMinHeight = 52.dp

@Preview(name = "GamssTextField", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssTextFieldPreview() {
    GamssTheme {
        GamssTextField(
            modifier = Modifier.fillMaxWidth(),
            label = "변경할 닉네임을 입력해주세요.",
            placeholder = "닉네임은 2~10자 사이로 입력해주세요.",
            value = "송지연",
            onValueChange = {},
        )
    }
}

@Preview(name = "GamssTextField - Placeholder", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssTextFieldPlaceholderPreview() {
    GamssTheme {
        GamssTextField(
            modifier = Modifier.fillMaxWidth(),
            label = "변경할 닉네임을 입력해주세요.",
            placeholder = "닉네임은 2~10자 사이로 입력해주세요.",
            value = "",
            onValueChange = {},
        )
    }
}

@Preview(name = "GamssTextField - Error", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun GamssTextFieldErrorPreview() {
    GamssTheme {
        GamssTextField(
            modifier = Modifier.fillMaxWidth(),
            label = "변경할 닉네임을 입력해주세요.",
            placeholder = "닉네임은 2~10자 사이로 입력해주세요.",
            errorMessage = "닉네임은 2자 이상으로 입력해주세요.",
            value = "아",
            onValueChange = {},
        )
    }
}
