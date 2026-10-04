package elemsocial.com.ui.pack.components.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.domain.model.PostReactions
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementReactionBar(
    reactions: PostReactions,
    onToggle: (reaction: String, isCurrentlySet: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(ElementUiPalette.Interaction)
                .clickable { pickerOpen = true }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_reaction_emoji),
                contentDescription = "Реакция",
                tint = ElementUiPalette.InteractionText,
                modifier = Modifier.size(20.dp)
            )
        }

        reactions.results.forEach { (emoji, count) ->
            val isSet = reactions.userReactions.contains(emoji)
            val background = if (isSet) {
                ElementUiPalette.Accent.copy(alpha = 0.20f)
            } else {
                ElementUiPalette.Interaction
            }
            val textColor = if (isSet) ElementUiPalette.Accent else ElementUiPalette.InteractionText

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(background)
                    .clickable { onToggle(emoji, isSet) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = emoji, fontSize = 15.sp)
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (pickerOpen) {
        ElementReactionPickerModal(
            currentReactions = reactions,
            onSelect = { unified ->
                val isSet = reactions.userReactions.contains(unified)
                onToggle(unified, isSet)
            },
            onDismiss = { pickerOpen = false }
        )
    }
}
