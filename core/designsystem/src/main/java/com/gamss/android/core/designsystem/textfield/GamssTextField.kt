package com.gamss.android.core.designsystem.textfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gamss.android.core.designsystem.theme.GamssTheme

/**
 * 라벨 텍스트 + [OutlinedTextField] 조합의 입력 필드.
 * 라벨이 필드 위에 별도로 표시되는 형태
 */
@Composable
fun GamssTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = GamssTheme.typography.body4Medium,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GamssTheme.colors.gray200,
                unfocusedBorderColor = GamssTheme.colors.gray200,
            ),
        )
    }
}

@Preview(name = "GamssTextField", showBackground = true)
@Composable
private fun GamssTextFieldPreview() {
    GamssTheme {
        GamssTextField(
            modifier = Modifier.fillMaxWidth(),
            label = "닉네임 변경",
            value = "송지연",
            onValueChange = {},
        )
    }
}
