package app.rive.runtime.kotlin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleObserver
import app.rive.runtime.kotlin.renderers.Renderer

abstract class RiveTextureView(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs),
    TextureView.SurfaceTextureListener {

    protected val activity by lazy(LazyThreadSafetyMode.NONE) {
        // If this fails we have a problem.
        getContextAsType<Activity>()!!
    }

    protected val lifecycleObserver: LifecycleObserver by lazy { createObserver() }
    protected var renderer: Renderer? = null
    private lateinit var viewSurface: Surface
    protected abstract fun createRenderer(): Renderer
    protected abstract fun createObserver(): LifecycleObserver
    protected inline fun <reified T> getContextAsType(): T? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is T) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {} // called every time when swapBuffers is called
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        onSurfaceTextureAvailable(surface, width, height)
    }

    @CallSuper
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Register this SurfaceView for the SurfaceHolder callbacks below
        surfaceTextureListener = this
        isOpaque = false
        // If no renderer has been made, we can't move forward.
        // Only make the renderer once we are ready to display things.
        renderer = createRenderer().apply { make() }
    }

    @CallSuper
    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        if (this::viewSurface.isInitialized) {
            viewSurface.release()
        }
        viewSurface = Surface(surfaceTexture)
        renderer?.apply {
            stop()
            setSurface(viewSurface)
        }
    }

    @CallSuper
    override fun onDetachedFromWindow() {
        // If we delete, we must have a Renderer.
        renderer!!.delete()
        renderer = null
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        when (visibility) {
            VISIBLE -> renderer?.start()
            else -> renderer?.stop()
        }
    }

    @CallSuper
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        viewSurface.release()
        return false
    }
}
