package com.baiyou.pocket.games;

import com.baiyou.pocket.game.MemoryDuelGame;

/** g047 卡片棋局：5×4 大棋盘与更狡猾的 AI（六成记牌率）比拼收牌，
 *  收得多者胜，奖励也更高。 */
public class CardChessMemoryGame extends MemoryDuelGame {

    public CardChessMemoryGame() {
        super(360, 560, 5, 4, 0.6f);
    }

    @Override
    protected float cellX(int i) {
        return 24 + (i % 5) * 63;
    }

    @Override
    protected float cellY(int i) {
        return 70 + (i / 5) * 98;
    }

    @Override
    protected float cellW() {
        return 54;
    }

    @Override
    protected float cellH() {
        return 86;
    }

    @Override
    protected int bgColor() {
        return 0xFF26201A;
    }
}
