package com.cavetale.tetris;

import com.cavetale.core.command.AbstractCommand;

public final class TetrisCommand extends AbstractCommand<TetrisPlugin> {
    protected TetrisCommand(final TetrisPlugin plugin) {
        super(plugin, "tetris");
    }

    @Override
    protected void onEnable() {
    }
}
