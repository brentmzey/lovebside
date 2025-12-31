package love.bside.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.play
import platform.Foundation.NSURL
import platform.UIKit.UIView
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val player = remember(url) {
        val nsUrl = NSURL.URLWithString(url) ?: return@remember null
        AVPlayer.playerWithURL(nsUrl)
    }

    DisposableEffect(url) {
        player?.play()
        onDispose {
            player?.pause()
        }
    }
    
    if (player == null) return

    UIKitView(
        factory = {
            val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player)
            UIView().apply {
                backgroundColor = UIColor.blackColor
                layer.addSublayer(playerLayer)
            }
        },
        modifier = modifier,
        update = { view ->
            val playerLayer = view.layer.sublayers?.firstOrNull() as? AVPlayerLayer
            playerLayer?.frame = view.bounds
        }
    )
}
