/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lvh.remoteime;

import android.app.Dialog;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.os.Handler;


/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {

    static final String TAG= "RemoteIME-SoftKeyboard";
    static final int PORT = 22222;
    private InputMethodManager mInputMethodManager;

    private LatinKeyboardView mInputView;
    
    private StringBuilder mComposing = new StringBuilder();


    private LatinKeyboard mCurKeyboard;

    /** Remote 
    */
    private Handler mHandler;
    private Thread  mThread;
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mHandler = new Handler();
        startRemoteServer(PORT);
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onCreate Thread=" + Thread.currentThread());
        }
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
		mCurKeyboard = new LatinKeyboard(this, R.xml.remote_ime);
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onInitializeInterface");
        }
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onCreateInputView");
        }
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        setLatinKeyboard(mCurKeyboard);
        return mInputView;
    }

    private void setLatinKeyboard(LatinKeyboard nextKeyboard) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "setLatinKeyboard =" + nextKeyboard);
        }
        mInputView.setKeyboard(nextKeyboard);
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onCreateCandidatesView not used!");
        }
        return null;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onStartInput");
        }
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);

                
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                  break;
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);

    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onFinishInput");
        }
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onStartInputView");
        }
        // Apply the selected keyboard to the input view.
        setLatinKeyboard(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onCurrentInputMethodSubtypeChanged");
        }
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onUpdateSelection");
        }
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);

            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onDisplayCompletions not used!");
        }
    }
  
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "Thread=" + Thread.currentThread() + " /onKeyDown = " + event);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "onKeyUp = " + keyCode);
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "commitTyped mComposing.length()= " + mComposing.length());
        }
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
        }
    }


    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "keyDownUp keyEventCode= " + keyEventCode);
        }
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "sendKey keyCode= " + keyCode);
        }
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }
//KeyboardView.OnKeyboardActionListener support function
    private void handleLanguageSwitch() {
        mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
    }

    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "handleCharacter primaryCode= " + primaryCode);
        }
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        getCurrentInputConnection().commitText(String.valueOf((char) primaryCode), 1);
    }

    private void handleClose() {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "handleClose ");
        }
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "handleBackspace  length = " + length);
        }
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
    }


//{{KeyboardView.OnKeyboardActionListener
    public void onKey(int primaryCode, int[] keyCodes) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "KeyboardView.OnKeyboardActionListener onKey,Thread = " + Thread.currentThread());
        }
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            return;
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "KeyboardView.OnKeyboardActionListener onText,Thread = " + Thread.currentThread());
        }
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();

    }

    public void swipeRight() {
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }
//KeyboardView.OnKeyboardActionListener}}

    public void injectString(String s) {
        if(com.lvh.remoteime.Debug.TraceFlow) {
            Log.d(TAG, "injectString Thread = " + Thread.currentThread());
        }
        mHandler.post(new InjectRunnable(s));
    }
    private class InjectRunnable implements Runnable {
        String mText;
        public InjectRunnable(String s) {
            mText = s;
            if(com.lvh.remoteime.Debug.TraceFlow) {
                Log.d(TAG, "InjectRunnable Thread = " + Thread.currentThread());
            }
        }
        private boolean isActived() {
            return SoftKeyboard.this.getCurrentInputStarted();
        }
       @Override
        public void run() {
            if(com.lvh.remoteime.Debug.TraceFlow) {
                Log.d(TAG, "InjectRunnable Runnable Thread = " + Thread.currentThread() + " /Active=" + isActived());
            }
            if(isActived()) {
                InputConnection ic = getCurrentInputConnection();
                if(ic != null) {
                    if( false == ic.commitText(mText, mText.length())) {
                        if(com.lvh.remoteime.Debug.DebugFlags) {
                            Log.d(TAG, "commitText fail");
                        }
                    }
                }
            }
        }

    }
    private void startRemoteServer(int port) {
        mThread = new RemoteServer(this, port);
        mThread.start();
    }
}
