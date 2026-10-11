package com.baiyou.pocket.games;

import com.baiyou.pocket.game.SeqMemoryGame;

/** g042 序列复现：四块感应板按随机顺序闪光，照原样点回来；
 *  序列一轮加一，记错即结束。 */
public class SeqSimonGame extends SeqMemoryGame {

    public SeqSimonGame() {
        super(360, 560);
    }

    @Override
    protected int padCount() {
        return 4;
    }

    @Override
    protected float padX(int i) {
        return 105 + (i % 2) * 150;
    }

    @Override
    protected float padY(int i) {
        return 230 + (i / 2) * 150;
    }

    @Override
    protected float padR(int i) {
        return 62;
    }

    @Override
    protected int padColor(int i) {
        switch (i) {
            case 0: return 0xFF2E5244;
            case 1: return 0xFF2E3E5C;
            case 2: return 0xFF5C4A2E;
            default: return 0xFF4A2E52;
        }
    }
}
