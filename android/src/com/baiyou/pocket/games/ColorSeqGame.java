package com.baiyou.pocket.games;

import com.baiyou.pocket.game.SeqMemoryGame;

/** g048 色彩记忆：五色横条渐次点亮，按同样顺序点回去；一轮加一色。 */
public class ColorSeqGame extends SeqMemoryGame {

    public ColorSeqGame() {
        super(360, 560);
    }

    @Override
    protected int padCount() {
        return 5;
    }

    @Override
    protected float padX(int i) {
        return 52 + i * 64;
    }

    @Override
    protected float padY(int i) {
        return 290;
    }

    @Override
    protected float padR(int i) {
        return 27;
    }

    @Override
    protected boolean roundPads() {
        return true;
    }

    @Override
    protected int padColor(int i) {
        switch (i) {
            case 0: return 0xFF523430;
            case 1: return 0xFF504A2C;
            case 2: return 0xFF2C5036;
            case 3: return 0xFF2C3E50;
            case 4: return 0xFF462C50;
            default: return 0xFF333333;
        }
    }
}
