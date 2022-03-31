package com.cavetale.tetris;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class TetrisPlugin extends JavaPlugin {
    @Getter protected static TetrisPlugin instance;
    private TetrisCommand tetrisCommand = new TetrisCommand(this);
    private EventListener eventListener = new EventListener(this);
    protected Sessions sessions = new Sessions(this);

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        tetrisCommand.enable();
        eventListener.enable();
        sessions.enable();
    }

    @Override
    public void onDisable() {
        sessions.disable();
    }
}
