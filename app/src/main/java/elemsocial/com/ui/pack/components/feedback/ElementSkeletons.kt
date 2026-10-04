package elemsocial.com.ui.pack.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import elemsocial.com.ui.pack.components.base.ElementBlock
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementRowSkeleton(
    modifier: Modifier = Modifier
) {
    ElementBlock(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElementUiPalette.BlockSoft)
            )
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(widthFraction = 0.42f)
                Box(modifier = Modifier.height(5.dp))
                SkeletonLine(widthFraction = 0.94f)
            }
        }
    }
}

@Composable
fun ElementNotificationSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Block)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElementUiPalette.BlockSoft)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(ElementUiPalette.Block)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(13.dp)
                                .clip(CircleShape)
                                .background(ElementUiPalette.BlockSoft)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(widthFraction = 0.42f, height = 9.dp)
                    Box(modifier = Modifier.height(3.dp))
                    SkeletonLine(widthFraction = 0.88f, height = 8.dp)
                }
            }

            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElementUiPalette.BlockSoft)
            )
        }
    }
}

@Composable
fun ElementProfileRelationSkeleton(
    modifier: Modifier = Modifier
) {
    ElementBlock(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElementUiPalette.BlockSoft)
            )
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(widthFraction = 0.40f)
                Box(modifier = Modifier.height(5.dp))
                SkeletonLine(widthFraction = 0.95f)
            }
        }
    }
}

@Composable
fun ElementHallRowSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ElementUiPalette.BlockSoft)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkeletonLine(widthFraction = 0.45f, height = 10.dp)
            SkeletonLine(widthFraction = 0.35f, height = 9.dp)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElementUiPalette.BlockSoft)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ElementUiPalette.BlockSoft)
            )
        }
    }
}

@Composable
fun ElementWalletTransactionSkeleton(
    modifier: Modifier = Modifier
) {
    ElementBlock(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElementUiPalette.BlockSoft)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SkeletonLine(widthFraction = 0.46f, height = 10.dp)
                SkeletonLine(widthFraction = 0.34f, height = 9.dp)
                SkeletonLine(widthFraction = 0.24f, height = 8.dp)
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth(0.82f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.BlockSoft)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    SkeletonLine(widthFraction = 0.85f, height = 9.dp)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ElementUiPalette.BlockSoft)
                )
            }
        }
    }
}

@Composable
fun ElementPostSkeleton(
    modifier: Modifier = Modifier,
    mediaHeight: Dp = 190.dp,
    showMedia: Boolean = true
) {
    ElementBlock(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 6.5.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElementUiPalette.BlockSoft)
                )
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(widthFraction = 0.40f, height = 10.dp)
                    Box(modifier = Modifier.height(5.dp))
                    SkeletonLine(widthFraction = 0.56f, height = 9.dp)
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonLine(widthFraction = 0.96f, height = 10.dp)
                SkeletonLine(widthFraction = 0.68f, height = 10.dp)
            }

            if (showMedia) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonLine(widthFraction = 0.92f, height = 10.dp)
                    SkeletonLine(widthFraction = 0.58f, height = 10.dp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(82.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
                Box(
                    modifier = Modifier
                        .width(82.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
                Box(
                    modifier = Modifier
                        .width(118.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
            }
        }
    }
}

@Composable
fun ElementGiftSkeleton(
    modifier: Modifier = Modifier
) {
    ElementBlock(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElementUiPalette.BlockSoft)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                SkeletonLine(widthFraction = 0.62f, height = 10.dp)
                SkeletonLine(widthFraction = 0.82f, height = 9.dp)
            }

            SkeletonLine(widthFraction = 0.54f, height = 8.dp)
        }
    }
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: Dp = 9.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction.coerceIn(0.15f, 1f))
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(ElementUiPalette.BlockSoft)
    )
}
