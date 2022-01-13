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
class KeyboardSpecialCharView(private val context: Context, private val layoutInflater: LayoutInflater, private val keyboardListener: KeyboardListener) {
    private val specialCharKeyboardLayout = layoutInflater.inflate(R.layout.keyboard_special_char_layout, null) as LinearLayout
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

    // 키보드 레이아웃 문자 구성
    // caps 상태에 따라 배치되는 문자가 달라진다.
    fun initialize() {
        val numberPadText = context.resources.getStringArray(R.array.number_pad)
        val line1Text: Array<String>
        val line2Text: Array<String>
        val line3Text: Array<String>
        val line4Text: Array<String>
        if (!isCaps) {
            line1Text = context.resources.getStringArray(R.array.sc_line_1)
            line2Text = context.resources.getStringArray(R.array.sc_line_2)
            line3Text = context.resources.getStringArray(R.array.sc_line_3)
            line4Text = context.resources.getStringArray(R.array.sc_line_4)
        } else {
            line1Text = context.resources.getStringArray(R.array.sc_caps_line_1)
            line2Text = context.resources.getStringArray(R.array.sc_caps_line_2)
            line3Text = context.resources.getStringArray(R.array.sc_caps_line_3)
            line4Text = context.resources.getStringArray(R.array.sc_caps_line_4)
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
        return this.specialCharKeyboardLayout
    }

    // 레이아웃에 버튼 배치
    private fun setComponent(key: ArrayList<String>) {
        var keyCount = 0
        for (i in 0 until specialCharKeyboardLayout.childCount) {
            if (specialCharKeyboardLayout[i] is LinearLayout) {
                val childLinear = specialCharKeyboardLayout[i] as LinearLayout
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
                        "ABC" -> actionImageButton.setImageResource(R.drawable.ic_text_rotation_none_24)
                        "⎵" -> actionImageButton.setImageResource(R.drawable.ic_space_bar_24)
                        "⏎" -> actionImageButton.setImageResource(R.drawable.ic_subdirectory_arrow_left_24)
                    }
                    keyCount++
                }
            }
        }
    }

    private fun isImageButton (keyName: String): Boolean {
        return keyName == "ABC" || keyName == "DEL" || keyName == "⎵" || keyName == "⏎" || keyName == "CAPS"
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
                    "ABC" -> keyboardListener.mode(KeyboardType.NORMAL)
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