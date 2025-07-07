package com.cavetale.tetris;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;

@Getter
@RequiredArgsConstructor
public enum TetrisSkin {
    DEFAULT(Material.WHITE_CONCRETE),
    GRID(Material.BLACK_CONCRETE),
    ;

    private final Material frameMaterial;
}
