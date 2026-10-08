package elemsocial.com.core.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.ui.pack.components.base.ElementBlock
import elemsocial.com.ui.pack.components.buttons.ElementButton
import elemsocial.com.ui.pack.components.buttons.ElementButtonVariant
import elemsocial.com.ui.pack.theme.ElementUiPalette

data class ElementPluginUiNode(
    val type: String,
    val text: String = "",
    val secondary: String = "",
    val callbackId: String? = null,
    val value: String = "",
    val checked: Boolean = false,
    val enabled: Boolean = true,
    val children: List<ElementPluginUiNode> = emptyList()
)

data class ElementPluginUiScreen(
    val pluginId: String,
    val screenId: String,
    val title: String,
    val nodes: List<ElementPluginUiNode>
)

@Composable
fun ElementPluginScreen(
    screen: ElementPluginUiScreen,
    onBack: () -> Unit,
    onAction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElementButton(
                title = "‹",
                onClick = onBack,
                modifier = Modifier.weight(0.22f),
                variant = ElementButtonVariant.Soft
            )
            Text(screen.title, color = ElementUiPalette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(screen.nodes) { node -> ElementPluginUiNodeView(node, onAction) }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun ElementPluginUiNodeView(node: ElementPluginUiNode, onAction: (String, String) -> Unit) {
    when (node.type) {
        "text" -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(node.text, color = ElementUiPalette.TextPrimary, fontSize = 16.sp)
            if (node.secondary.isNotBlank()) Text(node.secondary, color = ElementUiPalette.TextSecondary, fontSize = 13.sp)
        }
        "title" -> Text(node.text, color = ElementUiPalette.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        "button", "soft_button" -> ElementButton(
            title = node.text,
            enabled = node.enabled,
            variant = if (node.type == "soft_button") ElementButtonVariant.Soft else ElementButtonVariant.Primary,
            onClick = { node.callbackId?.let { onAction(it, "{}") } }
        )
        "switch" -> {
            var checked by remember(node.callbackId, node.checked) { mutableStateOf(node.checked) }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(node.text, color = ElementUiPalette.TextPrimary, fontSize = 16.sp)
                    if (node.secondary.isNotBlank()) Text(node.secondary, color = ElementUiPalette.TextSecondary, fontSize = 13.sp)
                }
                Switch(checked = checked, enabled = node.enabled, onCheckedChange = {
                    checked = it
                    node.callbackId?.let { id -> onAction(id, "{\"value\":$it}") }
                })
            }
        }
        "checkbox" -> {
            var checked by remember(node.callbackId, node.checked) { mutableStateOf(node.checked) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, enabled = node.enabled, onCheckedChange = {
                    checked = it
                    node.callbackId?.let { id -> onAction(id, "{\"value\":$it}") }
                })
                Text(node.text, color = ElementUiPalette.TextPrimary, fontSize = 16.sp)
            }
        }
        "input" -> {
            var value by remember(node.callbackId, node.value) { mutableStateOf(node.value) }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    node.callbackId?.let { id -> onAction(id, "{\"value\":${org.json.JSONObject.quote(it)}}") }
                },
                label = { Text(node.text) },
                enabled = node.enabled,
                placeholder = { if (node.secondary.isNotBlank()) Text(node.secondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
        }
        "divider" -> Divider(color = ElementUiPalette.scaledBorder(0.7f))
        "spacer" -> Spacer(Modifier.height(node.value.toFloatOrNull()?.coerceIn(2f, 120f)?.dp ?: 12.dp))
        "card" -> ElementBlock {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (node.text.isNotBlank()) Text(node.text, color = ElementUiPalette.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                if (node.secondary.isNotBlank()) Text(node.secondary, color = ElementUiPalette.TextSecondary, fontSize = 14.sp)
                node.children.forEach { ElementPluginUiNodeView(it, onAction) }
            }
        }
        "row" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            node.children.forEach { child -> androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { ElementPluginUiNodeView(child, onAction) } }
        }
        "column" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.children.forEach { ElementPluginUiNodeView(it, onAction) }
        }
        else -> Unit
    }
}
