package com.parasde.keyboard.service

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.parasde.keyboard.R
import com.parasde.keyboard.service.keyboardview.KeyboardView
import com.parasde.keyboard.service.keyboardview.KeyboardSpecialCharView
import java.lang.NullPointerException

class KeyboardService: InputMethodService() {
    private val tag = "keyboard service"

    private lateinit var keyboardLayout:LinearLayout
    private lateinit var keyboardFrame: FrameLayout
    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboardSpecialCharView: KeyboardSpecialCharView
    private var curMode = 0

    private val keyboardListener = object: KeyboardListener {
        override fun mode(mode: Int) {
            if (curMode != mode) curMode = mode
            // 1 => special char, 0 => normal
            keyboardFrame.removeAllViews()
            if (mode == KeyboardType.SC)    {
                keyboardFrame.addView(keyboardSpecialCharView.getLayout())
                keyboardSpecialCharView.inputConnection = currentInputConnection
            } else {
                keyboardFrame.addView(keyboardView.getLayout())
                keyboardView.inputConnection = currentInputConnection
            }
        }
    }

    override fun onCreate () {
        super.onCreate()
        Log.i(tag, "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag,"onDestroy")
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Log.i(tag,"onWindowShown")
    }

    override fun hideWindow() {
        super.hideWindow()
        Log.i(tag,"hideWindow")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.i(tag,"onFinishInput")
    }

    override fun onCreateInputView(): View {
        keyboardLayout = layoutInflater.inflate(R.layout.keyboard_view, null) as LinearLayout
        keyboardFrame = keyboardLayout.findViewById(R.id.keyboardFrame)

        keyboardView = KeyboardView(applicationContext, layoutInflater, keyboardListener)
        keyboardView.initialize()

        keyboardSpecialCharView = KeyboardSpecialCharView(applicationContext, layoutInflater, keyboardListener)
        keyboardSpecialCharView.initialize()

        return keyboardLayout
    }

    override fun updateInputViewShown() {
        super.updateInputViewShown()
        Log.i(tag,"updateInputViewShown")
        try {
            currentInputConnection.finishComposingText()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        keyboardListener.mode(curMode)
    }
}