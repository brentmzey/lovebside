package love.bside.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import love.bside.app.ui.design.tokens.BsideColors
import love.bside.app.ui.design.tokens.BsideTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BsideScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = BsideTypography.HeadlineMedium) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BsideColors.Surface,
                    titleContentColor = BsideColors.TextPrimary,
                    navigationIconContentColor = BsideColors.TextPrimary,
                    actionIconContentColor = BsideColors.TextPrimary
                )
            )
        },
        containerColor = BsideColors.Surface,
        floatingActionButton = floatingActionButton,
        content = content
    )
}
