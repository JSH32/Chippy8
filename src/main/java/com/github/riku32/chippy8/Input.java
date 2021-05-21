package com.github.riku32.chippy8;

import com.github.riku32.chippy8.VM.Keypad;
import net.java.games.input.Keyboard;

import java.awt.*;
import java.awt.event.KeyEvent;

public class Input implements Keypad {
    private final boolean[] keys = new boolean[16];

    public Input() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                synchronized (Keyboard.class) {
                    switch (e.getID()) {
                        case KeyEvent.KEY_PRESSED:
                            set(true, e.getKeyCode());
                            break;

                        case KeyEvent.KEY_RELEASED:
                            set(false, e.getKeyCode());
                            break;
                    }
                    return false;
                }
            }
        });
    }

    public boolean pressed(int value) {
        return keys[value];
    }

    private void set(boolean value, int keycode){
        switch(keycode) {
            case KeyEvent.VK_1:
                keys[0x1] = value;
                break;
            case KeyEvent.VK_2:
                keys[0x2] = value;
                break;
            case KeyEvent.VK_3:
                keys[0x3] = value;
                break;
            case KeyEvent.VK_4:
                keys[0xC] = value;
                break;
            case KeyEvent.VK_Q:
                keys[0x4] = value;
                break;
            case KeyEvent.VK_W:
                keys[0x5] = value;
                break;
            case KeyEvent.VK_E:
                keys[0x6] = value;
                break;
            case KeyEvent.VK_R:
                keys[0xD] = value;
                break;
            case KeyEvent.VK_A:
                keys[0x7] = value;
                break;
            case KeyEvent.VK_S:
                keys[0x8] = value;
                break;
            case KeyEvent.VK_D:
                keys[0x9] = value;
                break;
            case KeyEvent.VK_F:
                keys[0xE] = value;
                break;
            case KeyEvent.VK_Z:
                keys[0xA] = value;
                break;
            case KeyEvent.VK_X:
                keys[0x0] = value;
                break;
            case KeyEvent.VK_C:
                keys[0xB] = value;
                break;
            case KeyEvent.VK_V:
                keys[0xF] = value;
                break;
        }
    }
}
