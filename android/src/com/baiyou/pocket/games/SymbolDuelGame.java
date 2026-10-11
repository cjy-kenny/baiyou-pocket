package com.baiyou.pocket.games;

import com.baiyou.pocket.game.MemoryDuelGame;

/** g046 符号对对碰：与 AI 轮流翻牌配对，收得多者胜（AI 有三成记牌率）。 */
public class SymbolDuelGame extends MemoryDuelGame {

    public SymbolDuelGame() {
        super(360, 560, 4, 4, 0.3f);
    }

    @Override
    protected float cellX(int i) {
        return 40 + (i % 4) * 72;
    }

    @Override
    protected float cellY(int i) {
        return 70 + (i / 4) * 100;
    }

    @Override
    protected float cellW() {
        return 62;
    }

    @Override
    protected float cellH() {
        return 88;
    }

    @Override
    protected int bgColor() {
        return 0xFF20262E;
    }
}
