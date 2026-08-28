package com.ororura.analyzer.resume.domain;

final class ScoreMath {

    private ScoreMath() {
    }

    static int score(double value) {
        return (int) Math.clamp(Math.round(value), 0, 100);
    }
}
