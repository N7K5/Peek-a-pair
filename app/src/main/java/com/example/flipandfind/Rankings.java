package com.example.flipandfind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Rankings {
    public static final class Group {
        private final int rank;
        private final int score;
        private final List<Integer> playerIndices;

        private Group(int rank, int score, List<Integer> playerIndices) {
            this.rank = rank;
            this.score = score;
            this.playerIndices = Collections.unmodifiableList(playerIndices);
        }

        public int getRank() {
            return rank;
        }

        public int getScore() {
            return score;
        }

        public List<Integer> getPlayerIndices() {
            return playerIndices;
        }
    }

    private Rankings() {
    }

    /** Returns dense shared ranks: 1, 1, 2 when two players tie for first. */
    public static List<Group> fromScores(int[] scores) {
        List<Integer> distinctScores = new ArrayList<>();
        for (int score : scores) {
            if (!distinctScores.contains(score)) {
                distinctScores.add(score);
            }
        }
        Collections.sort(distinctScores, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return Integer.compare(right, left);
            }
        });

        Map<Integer, List<Integer>> playersByScore = new LinkedHashMap<>();
        for (int score : distinctScores) {
            playersByScore.put(score, new ArrayList<>());
        }
        for (int player = 0; player < scores.length; player++) {
            playersByScore.get(scores[player]).add(player);
        }

        List<Group> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Integer, List<Integer>> entry : playersByScore.entrySet()) {
            result.add(new Group(rank++, entry.getKey(), entry.getValue()));
        }
        return result;
    }
}
