package com.github.riku32.chippy8.VM;

public interface Keypad {
    /**
     * Check if key is pressed
     *
     * @param key to check
     * @return pressed
     */
    boolean pressed(int key);
}
