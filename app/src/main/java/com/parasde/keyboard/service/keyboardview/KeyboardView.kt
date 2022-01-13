package com.parasde.keyboard.service.keyboardview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.get
import com.parasde.keyboard.R
import com.parasde.keyboard.service.KeyboardListener
import com.parasde.keyboard.service.KeyboardType
import java.util.*
import kotlin.collections.ArrayList

@SuppressLint("ClickableViewAccessibility")
class KeyboardView(private val context: Context, private val layoutInflater: LayoutInflater, private val keyboardListener: KeyboardListener) {
    private val keyboardLayout = layoutInflater.inflate(R.layout.keyboard_layout, null) as LinearLayout
    var inputConnection: InputConnection? = null

    // caps lock 상태
    private var isCaps = false
    private lateinit var capsState: View

    // touch listener
    var touchView: View? = null
    var interval = 100L
    val intervalLimit = 20L
    val handler = Handler(Looper.getMainLooper())
    private val handlerRunnable = object: Runnable {
        override fun run() {
            if (interval > intervalLimit) interval -= 2
            handler.postDelayed(this, interval)
            if (touchView != null) clickListener.onClick(touchView)
        }
    }

    fun initialize() {
        val numberPadText = context.resources.getStringArray(R.array.number_pad)
        val line1Text: Array<String>
        val line2Text: Array<String>
        val line3Text: Array<String>
        val line4Text = context.resources.getStringArray(R.array.line4_en)
        if (!isCaps) {
            line1Text = context.resources.getStringArray(R.array.line1_en)
            line2Text = context.resources.getStringArray(R.array.line2_en)
            line3Text = context.resources.getStringArray(R.array.line3_en)
        } else {
            line1Text = context.resources.getStringArray(R.array.caps_line1_en)
            line2Text = context.resources.getStringArray(R.array.caps_line2_en)
            line3Text = context.resources.getStringArray(R.array.caps_line3_en)
        }

        val keyArray = ArrayList<String>()
        keyArray.addAll(numberPadText)
        keyArray.addAll(line1Text)
        keyArray.addAll(line2Text)
        keyArray.addAll(line3Text)
        keyArray.addAll(line4Text)
        setComponent(keyArray)
    }

    fun getLayout(): LinearLayout {
        isCaps = false
        initialize()
        return this.keyboardLayout
    }

    // 레이아웃에 버튼 배치
    private fun setComponent(key: ArrayList<String>) {
        var keyCount = 0
        for (i in 0 until keyboardLayout.childCount) {
            if (keyboardLayout[i] is LinearLayout) {
                val childLinear = keyboardLayout[i] as LinearLayout
                for (n in 0 until childLinear.childCount) {
                    val const = childLinear[n]
                    val actionButton = const.findViewById<Button>(R.id.keyButton)
                    val actionImageButton = const.findViewById<ImageButton>(R.id.keyButtonImage)
                    actionButton.text = key[keyCount]

                    if (isImageButton(key[keyCount])) {
                        actionButton.visibility = View.GONE
                        actionImageButton.contentDescription = key[keyCount]
                        actionImageButton.setOnClickListener(clickListener)
                        // 상태 점멸등은 invisible
                        const.findViewById<View>(R.id.keyState).visibility = View.GONE
                    } else {
                        actionButton.setOnClickListener(clickListener)
                    }

                    when (key[keyCount]) {
                        "CAPS" -> {
                            actionImageButton.setImageResource(R.drawable.ic_keyboard_capslock_24)
                            capsState = const.findViewById<View>(R.id.keyState)
                            if (isCaps) capsState.setBackgroundResource(R.drawable.circle_on)
                            else capsState.setBackgroundResource(R.drawable.circle_off)
                            capsState.visibility = View.VISIBLE
                        }
                        "DEL" -> {
                            actionImageButton.setImageResource(R.drawable.ic_backspace_24)
                            actionImageButton.setOnTouchListener(touchListener)
                        }
                        "!/?" -> actionImageButton.setImageResource(R.drawable.ic_star_24)
                        "⎵" -> actionImageButton.setImageResource(R.drawable.ic_space_bar_24)
                        "⏎" -> actionImageButton.setImageResource(R.drawable.ic_subdirectory_arrow_left_24)
                    }
                    keyCount++
                }
            }
        }
    }

    private fun isImageButton (keyName: String): Boolean {
        return keyName == "DEL" || keyName == "⎵" || keyName == "⏎" ||  keyName == "!/?" || keyName == "CAPS"
    }

    // 클릭
    private val clickListener = View.OnClickListener { view ->
        inputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)
        val cursor: CharSequence? = inputConnection?.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES)
        if (cursor != null && cursor.length >= 2) {
            val eventTime = SystemClock.uptimeMillis()
            inputConnection?.finishComposingText()
            inputConnection?.sendKeyEvent(
                KeyEvent(
                    eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD
                )
            )
            inputConnection?.sendKeyEvent(
                KeyEvent(
                    SystemClock.uptimeMillis(), eventTime,
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD
                )
            )
            inputConnection?.sendKeyEvent(
                KeyEvent(
                    eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD
                )
            )
            inputConnection?.sendKeyEvent(
                KeyEvent(
                    SystemClock.uptimeMillis(), eventTime,
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD
                )
            )
        } else {
            if (view is Button) {
                inputConnection?.commitText(view.text, 1)
            } else if (view is ImageButton) {
                when(view.contentDescription.toString()) {
                    "CAPS" -> {
                        isCaps = !isCaps
                        initialize()
                    }
                    "DEL" -> {
                        inputConnection?.sendKeyEvent(KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL))
                    }
                    "!/?" -> keyboardListener.mode(KeyboardType.SC)
                    "⎵" -> inputConnection?.commitText(" ", 1)
                    "⏎" -> {
                        val eventTime = SystemClock.uptimeMillis()
                        inputConnection?.sendKeyEvent(
                            KeyEvent(
                                eventTime, eventTime,
                                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0, 0, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD
                            )
                        )
                        inputConnection?.sendKeyEvent(
                            KeyEvent(
                                SystemClock.uptimeMillis(), eventTime,
                                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, 0, 0, 0,
                                KeyEvent.FLAG_SOFT_KEYBOARD
                            )
                        )
                    }
                }
            }
        }
    }

    // 터치
    private val touchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(handlerRunnable)
                handler.postDelayed(handlerRunnable, interval)
                touchView = view
                return@OnTouchListener false
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(handlerRunnable)
                touchView = null
                // touch 종료 시 interval 값 초기화
                interval = 100L
                return@OnTouchListener false
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(handlerRunnable)
                touchView = null
                return@OnTouchListener true
            }
            else -> {
                return@OnTouchListener false
            }
        }
    }
}