package com.examportal.proctor;

/**
 * ViolationType — all browser events detected by the frontend proctor hook.
 *
 * REVIEWED PROCTORING TECHNIQUES:
 *   TAB_SWITCH        — document.visibilitychange fires when tab is hidden
 *   WINDOW_BLUR       — window blur (only when IN fullscreen to avoid false positives)
 *   FULLSCREEN_EXIT   — fullscreenchange when fullscreenElement becomes null
 *   COPY_PASTE        — copy/cut/paste events, preventDefault stops data transfer
 *   CONTEXT_MENU      — right-click, preventDefault blocks inspect element
 *   KEYBOARD_SHORTCUT — Ctrl+C/V/U/A/S, F12, Alt+Tab blocked via keydown
 *   MOUSE_LEAVE       — CHANGE: mouse leaving the document window
 *                       (indicates user moving to another window/monitor)
 *   DEVTOOLS_OPEN     — CHANGE: detected via window.outerWidth - window.innerWidth > threshold
 *                       or console.log timing trick (frontend implements this)
 */
public enum ViolationType {
    TAB_SWITCH,
    WINDOW_BLUR,
    FULLSCREEN_EXIT,
    COPY_PASTE,
    CONTEXT_MENU,
    KEYBOARD_SHORTCUT,
    /** CHANGE: Mouse left the document area — user moved to another window */
    MOUSE_LEAVE,
    /** CHANGE: DevTools opened detection */
    DEVTOOLS_OPEN
}
