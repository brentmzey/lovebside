package love.bside.app.security

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {
    private val current = AtomicReference<WeakReference<FragmentActivity>?>(null)

    fun currentActivity(): FragmentActivity? = current.get()?.get()

    override fun onActivityResumed(activity: Activity) {
        if (activity is FragmentActivity) {
            current.set(WeakReference(activity))
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity is FragmentActivity && current.get()?.get() == activity) {
            current.set(null)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activity is FragmentActivity && current.get()?.get() == activity) {
            current.set(null)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
