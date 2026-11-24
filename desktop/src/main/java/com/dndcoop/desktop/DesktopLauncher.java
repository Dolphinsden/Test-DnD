package com.dndcoop.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.dndcoop.ui.DndCoopGame;

public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("The Ashen Lantern of Hollowbrook — D&D 5E Couch Co-op");
        config.setWindowedMode(1440, 900);
        config.setWindowSizeLimits(1080, 700, -1, -1);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new DndCoopGame(), config);
    }
}
