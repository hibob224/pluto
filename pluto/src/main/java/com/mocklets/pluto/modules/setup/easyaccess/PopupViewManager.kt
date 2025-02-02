package com.mocklets.pluto.modules.setup.easyaccess

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.mocklets.pluto.R
import com.mocklets.pluto.core.DeviceInfo
import com.mocklets.pluto.core.extensions.color
import com.mocklets.pluto.core.extensions.inflate
import com.mocklets.pluto.core.preferences.Preferences
import com.mocklets.pluto.core.ui.hapticFeedback
import com.mocklets.pluto.core.ui.soundFeedback
import com.mocklets.pluto.databinding.PlutoLayoutPopupBinding
import kotlin.math.abs

internal class PopupViewManager(
    private val context: Context,
    private val listener: OnPopupInteractionListener
) {
    private val deviceInfo = DeviceInfo(context)
    private val dragUpLimit = deviceInfo.height * DRAG_UP_THRESHOLD
    private val dragDownLimit = deviceInfo.height * DRAG_DOWN_THRESHOLD

    val view: View = context.inflate(R.layout.pluto___layout_popup)
    val layoutParams = getInitialLayoutParams()

    init {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction = 0
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // remember the initial position.
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        // get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (lastAction == MotionEvent.ACTION_DOWN) {
                            view.hapticFeedback(true)
                            view.soundFeedback()
                            listener.onClick()
                        }
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val movementX = event.rawX - initialTouchX
                        val movementY = event.rawY - initialTouchY

                        if (abs(movementX) > 1 || abs(movementY) > 1) {
//                            layoutParams.x = initialX + movementX.toInt()
                            val currentY = initialY + (event.rawY - initialTouchY).toInt()
                            if (currentY > dragUpLimit && currentY < dragDownLimit) {
                                layoutParams.y = currentY

                                listener.onLayoutParamsUpdated(layoutParams)
                                lastAction = event.action
                                return true
                            }
                            return false
                        }
                        return false
                    }
                }
                return false
            }
        })

        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) {
                v?.let {
                    val binding = PlutoLayoutPopupBinding.bind(it)
                    binding.card.setCardBackgroundColor(
                        context.color(if (Preferences(context).isDarkAccessPopup) R.color.pluto___dark else R.color.pluto___app_bg)
                    )
                }
                val gravityHorizontal =
                    if (Preferences(context).isRightHandedAccessPopup) Gravity.END else Gravity.START
                layoutParams.gravity = gravityHorizontal or Gravity.TOP
                listener.onLayoutParamsUpdated(layoutParams)
            }

            override fun onViewDetachedFromWindow(v: View?) {
            }
        })
    }

    private fun getInitialLayoutParams(): WindowManager.LayoutParams {
        val params: WindowManager.LayoutParams
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
        } else {
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
        }

        val gravityHorizontal = if (Preferences(context).isRightHandedAccessPopup) Gravity.END else Gravity.START
        params.gravity = gravityHorizontal or Gravity.TOP
        params.x = (context.resources.getDimension(R.dimen.pluto___popup_bubble_width) * INIT_THRESHOLD_X).toInt()
        params.y = (deviceInfo.height * INIT_THRESHOLD_Y).toInt()

        return params
    }

    companion object {
        const val DRAG_UP_THRESHOLD = 0.03
        const val DRAG_DOWN_THRESHOLD = 0.9
        const val INIT_THRESHOLD_X = -0.75
        const val INIT_THRESHOLD_Y = 0.65
    }
}
