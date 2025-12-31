package love.bside.app.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse(url))
                val mediaController = MediaController(context)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setOnPreparedListener { mp ->
                    mp.start()
                    mp.isLooping = true
                }
            }
        },
        update = { view ->
             // Optional: Update URL if it changes, though usually VideoView is recreated
             // if the key changes. handling dynamic updates is complex for VideoView.
        }
    )
}
