package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

private val SlipGroupSize = DpSize(76.03.dp, 52.02.dp)
private const val SLIP_ROTATION = 19.88f

/** 기울기는 벡터 자체에 들어 있어 따로 회전시키지 않는다. */
@Composable
fun GamssTape(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.img_tape_pink),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun GamssPaperSlip(
    modifier: Modifier = Modifier,
    outlineColor: Color = GamssTheme.sticker.slipOutline,
    borderColor: Color = GamssTheme.sticker.slipBorder,
) {
    Box(modifier = modifier.size(SlipGroupSize)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 70.dp, height = 30.dp)
                .rotate(SLIP_ROTATION)
                .background(GamssTheme.colors.white, RoundedCornerShape(2.dp))
                .border(0.5.dp, outlineColor, RoundedCornerShape(2.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 5.12.dp, y = 5.12.dp)
                .size(width = 65.79.dp, height = 41.77.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 22.dp)
                    .rotate(SLIP_ROTATION)
                    .border(1.dp, borderColor, RoundedCornerShape(1.dp)),
            )
        }
    }
}
