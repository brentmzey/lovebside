package love.bside.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import love.bside.app.ui.design.tokens.*

/**
 * BSide text input field with beautiful styling.
 * KMP-compatible, works across all platforms.
 * 
 * **Responsive Design:**
 * Use `modifier = Modifier.responsiveFormWidth()` for form inputs (login, signup)
 * to ensure proper sizing on mobile (full width) and desktop (max 480dp).
 * 
 * Example:
 * ```
 * BsideTextField(
 *     value = email,
 *     onValueChange = { email = it },
 *     placeholder = "Email address",
 *     modifier = Modifier.responsiveFormWidth()  // Adapts to platform
 * )
 * ```
 */
@Composable
fun BsideTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onDone: () -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(BsideShapes.TextField)
            .background(BsideColors.Surface)
            .padding(horizontal = BsideSpacing.Medium, vertical = BsideSpacing.Small),
        enabled = enabled,
        textStyle = BsideTypography.BodyLarge.copy(
            color = BsideColors.TextPrimary
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        cursorBrush = SolidColor(BsideColors.Primary),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = BsideTypography.InputHint,
                        color = BsideColors.TextSecondary
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * Circular avatar component with initials or image.
 * KMP-compatible.
 */
@Composable
fun BsideAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = BsideIconSizes.Avatar.Medium,
    backgroundColor: Color = BsideColors.Primary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(2).uppercase(),
            style = BsideTypography.TitleMedium,
            color = BsideColors.OnPrimary
        )
    }
}
