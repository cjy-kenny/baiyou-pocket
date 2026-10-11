package com.baiyou.pocket.games;

import com.baiyou.pocket.game.MemoryDuelGame;

/** g060 记忆牌战：6×4 大棋盘、记性不错的对手（四成五记牌率），
 *  收对多者胜，胜局奖励更丰厚。 */
public class MemoryBattleGame extends MemoryDuelGame {

    public MemoryBattleGame() {
        super(360, 560, 6, 4, 0.45f);
    }

    @Override
    protected float cellX(int i) {
        return 32 + (i % 6) * 50;
    }

    @Override
    protected float cellY(int i) {
        return 76 + (i / 6) * 96;
    }

    @Override
    protected float cellW() {
        return 42;
    }

    @Override
    protected float cellH() {
        return 84;
    }

    @Override
    protected int bgColor() {
        return 0xFF1E2622;
    }
}
