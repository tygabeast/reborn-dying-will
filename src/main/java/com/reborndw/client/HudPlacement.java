package com.reborndw.client;

import com.reborndw.config.ClientConfig;

/** Turns an anchor plus offsets into the top-left corner of a box of the given size. */
final class HudPlacement {
    private HudPlacement() {}

    static int[] place(ClientConfig.Anchor anchor, int dx, int dy, int w, int h, int screenW, int screenH) {
        int cx = screenW / 2, m = 4;
        int x, y;
        switch (anchor) {
            case HOTBAR_LEFT -> { x = cx - 91 - 8 - w; y = screenH - 7 - h; }
            case HOTBAR_RIGHT -> { x = cx + 91 + 8; y = screenH - 7 - h; }
            case TOP_LEFT -> { x = m; y = m; }
            case TOP_RIGHT -> { x = screenW - m - w; y = m; }
            case MIDDLE_LEFT -> { x = m; y = (screenH - h) / 2; }
            case MIDDLE_RIGHT -> { x = screenW - m - w; y = (screenH - h) / 2; }
            case BOTTOM_LEFT -> { x = m; y = screenH - m - h; }
            default -> { x = screenW - m - w; y = screenH - m - h; }
        }
        return new int[]{x + dx, y + dy};
    }

    static boolean rightSide(ClientConfig.Anchor a) {
        return a == ClientConfig.Anchor.TOP_RIGHT || a == ClientConfig.Anchor.MIDDLE_RIGHT || a == ClientConfig.Anchor.BOTTOM_RIGHT;
    }
}
