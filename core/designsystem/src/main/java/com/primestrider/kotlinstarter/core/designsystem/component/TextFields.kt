package com.primestrider.kotlinstarter.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.primestrider.kotlinstarter.core.designsystem.R
import com.primestrider.kotlinstarter.core.designsystem.theme.ControlShapes
import com.primestrider.kotlinstarter.core.designsystem.theme.LocalAppColors
import com.primestrider.kotlinstarter.core.designsystem.theme.PreviewTheme
import com.primestrider.kotlinstarter.core.designsystem.theme.Radii
import com.primestrider.kotlinstarter.core.designsystem.theme.Spacing
import com.primestrider.kotlinstarter.core.designsystem.theme.ThemePreviews
import com.primestrider.kotlinstarter.core.designsystem.theme.controlErrorFill
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFill
import com.primestrider.kotlinstarter.core.designsystem.theme.controlFocusFill

/**
 * Filled, borderless text field with a floating label: neutral fill at rest, a tint of `primary` while
 * focused, a tint of `destructive` in error. Extras: [errorText] (wins over [supportingText] and marks the
 * field as an error), [maxLength] (truncates input and shows an "n/max" counter), and [prefix] (a small
 * chip before the value, e.g. a phone country code).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: String? = null,
) {
    val counter = maxLength?.let { stringResource(R.string.component_counter, value.codePointCount(0, value.length), it) }
    val message = errorText ?: supportingText
    TextField(
        value = value,
        onValueChange = { onValueChange(if (maxLength != null) it.takeCodePoints(maxLength) else it) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().heightIn(min = FIELD_MIN_HEIGHT),
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leading(leadingIcon, prefix),
        trailingIcon = trailingIcon,
        supportingText =
            if (message == null && counter == null) {
                null
            } else {
                { SupportingRow(message, counter) }
            },
        isError = errorText != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = imeActions(imeAction, onImeAction),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        enabled = enabled,
        visualTransformation = visualTransformation,
        shape = ControlShapes.field,
        colors = appFieldColors(),
    )
}

/** Password field that owns its show/hide toggle (kept across rotation). */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        errorText = errorText,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.Lock else Icons.Filled.Lock,
                    contentDescription =
                        stringResource(if (visible) R.string.component_hide_password else R.string.component_show_password),
                )
            }
        },
    )
}

/** Search box with a search icon and a clear button that appears once there is a query. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.component_search),
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().heightIn(min = FIELD_MIN_HEIGHT),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon =
            if (query.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.component_clear_search))
                    }
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = ControlShapes.field,
        colors = appFieldColors(),
    )
}

private val FIELD_MIN_HEIGHT = 58.dp

/** Leading slot: optional icon, then the optional [prefix] chip. Null when there is neither. */
private fun leading(
    icon: ImageVector?,
    prefix: String?,
): (@Composable () -> Unit)? {
    if (icon == null && prefix == null) return null
    return {
        Row(
            Modifier.padding(start = Spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(it, contentDescription = null) }
            prefix?.let { PrefixChip(it) }
        }
    }
}

@Composable
private fun PrefixChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        modifier =
            Modifier
                .background(LocalAppColors.current.background, RoundedCornerShape(Radii.md))
                .padding(horizontal = Spacing.s2, vertical = Spacing.s1),
    )
}

/**
 * Shared colours of every filled field: no indicator line in any state, container by state, primary label
 * and cursor while focused, destructive label and cursor in error.
 */
@Composable
internal fun appFieldColors(): TextFieldColors {
    val c = LocalAppColors.current
    val dim = c.muted.copy(alpha = DISABLED_ALPHA)
    return TextFieldDefaults.colors(
        focusedContainerColor = c.controlFocusFill,
        unfocusedContainerColor = c.controlFill,
        disabledContainerColor = c.controlFill,
        errorContainerColor = c.controlErrorFill,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        focusedTextColor = c.foreground,
        unfocusedTextColor = c.foreground,
        disabledTextColor = c.foreground.copy(alpha = DISABLED_ALPHA),
        errorTextColor = c.foreground,
        cursorColor = c.primary,
        errorCursorColor = c.destructive,
        focusedLabelColor = c.primary,
        unfocusedLabelColor = c.muted,
        disabledLabelColor = dim,
        errorLabelColor = c.destructive,
        focusedPlaceholderColor = c.muted,
        unfocusedPlaceholderColor = c.muted,
        disabledPlaceholderColor = dim,
        focusedLeadingIconColor = c.muted,
        unfocusedLeadingIconColor = c.muted,
        disabledLeadingIconColor = dim,
        errorLeadingIconColor = c.muted,
        focusedTrailingIconColor = c.muted,
        unfocusedTrailingIconColor = c.muted,
        disabledTrailingIconColor = dim,
        errorTrailingIconColor = c.muted,
        focusedSupportingTextColor = c.muted,
        unfocusedSupportingTextColor = c.muted,
        disabledSupportingTextColor = dim,
        errorSupportingTextColor = c.destructive,
    )
}

private const val DISABLED_ALPHA = 0.38f

/** Message on the start, "n/max" counter on the end, as Material 3 lays them out. */
@Composable
private fun SupportingRow(
    message: String?,
    counter: String?,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(message.orEmpty(), modifier = Modifier.weight(1f))
        counter?.let { Text(it) }
    }
}

/** First [max] Unicode code points, so an emoji (a surrogate pair) is never cut in half. */
internal fun String.takeCodePoints(max: Int): String =
    if (codePointCount(0, length) <= max) this else substring(0, offsetByCodePoints(0, max))

/** Runs [onImeAction] when the keyboard action matching [imeAction] is pressed; nothing otherwise. */
private fun imeActions(
    imeAction: ImeAction,
    onImeAction: (() -> Unit)?,
): KeyboardActions {
    if (onImeAction == null) return KeyboardActions.Default
    val run: KeyboardActionScope.() -> Unit = { onImeAction() }
    return when (imeAction) {
        ImeAction.Done -> KeyboardActions(onDone = run)
        ImeAction.Go -> KeyboardActions(onGo = run)
        ImeAction.Next -> KeyboardActions(onNext = run)
        ImeAction.Search -> KeyboardActions(onSearch = run)
        ImeAction.Send -> KeyboardActions(onSend = run)
        else -> KeyboardActions.Default
    }
}

@ThemePreviews
@Composable
private fun TextFieldsPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(value = "", onValueChange = {}, label = "Name", placeholder = "Your name")
            AppTextField(value = "Ada", onValueChange = {}, label = "Filled", supportingText = "Helper text")
            AppTextField(value = "x", onValueChange = {}, label = "Error", errorText = "Too short")
            PasswordTextField(value = "secret", onValueChange = {}, label = "Password")
            SearchField(query = "", onQueryChange = {})
        }
    }
}
