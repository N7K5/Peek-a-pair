package com.example.flipandfind;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

/** Versioned, pure-Java history codec suitable for a SharedPreferences string. */
public final class GameHistoryCodec {
    private static final int MAGIC = 0x50415048; // PAPH (Peek-a-Pair history)
    private static final int VERSION = 1;
    private static final int MAX_DECODED_ENTRIES = 1_000;
    private static final int MAX_ENCODED_CHARACTERS = 2_000_000;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private GameHistoryCodec() {
    }

    public static String encode(GameHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("History is required");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeLong(history.getTotalGamesPlayed());
            output.writeLong(history.getTotalActiveDurationMillis());
            output.writeLong(history.getTotalPairsPlayed());
            output.writeInt(history.getRetainedGameCount());
            for (GameHistoryEntry entry : history.getEntries()) {
                writeEntry(output, entry);
            }
            output.flush();
            return toHex(bytes.toByteArray());
        } catch (IOException exception) {
            // Byte-array streams should not fail, but keep the API deterministic if they do.
            throw new IllegalStateException("Could not encode game history", exception);
        }
    }

    /** Decodes retained games using the supplied current cap. Empty data creates empty history. */
    public static GameHistory decode(String encoded, int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("History must retain at least one game");
        }
        if (encoded == null || encoded.isEmpty()) {
            return GameHistory.empty(maxEntries);
        }
        if (encoded.length() > MAX_ENCODED_CHARACTERS) {
            throw new IllegalArgumentException("Encoded history is too large");
        }

        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(fromHex(encoded)));
            if (input.readInt() != MAGIC) {
                throw new IllegalArgumentException("History header is invalid");
            }
            int version = input.readInt();
            if (version != VERSION) {
                throw new IllegalArgumentException("History version is not supported");
            }
            long totalGames = input.readLong();
            long totalDuration = input.readLong();
            long totalPairs = input.readLong();
            int entryCount = input.readInt();
            if (entryCount < 0 || entryCount > MAX_DECODED_ENTRIES) {
                throw new IllegalArgumentException("History entry count is invalid");
            }
            ArrayList<GameHistoryEntry> entries = new ArrayList<>(entryCount);
            for (int index = 0; index < entryCount; index++) {
                entries.add(readEntry(input));
            }
            if (input.read() != -1) {
                throw new IllegalArgumentException("History contains unexpected trailing data");
            }
            return GameHistory.restore(
                maxEntries,
                totalGames,
                totalDuration,
                totalPairs,
                entries
            );
        } catch (EOFException exception) {
            throw new IllegalArgumentException("History data is incomplete", exception);
        } catch (IOException exception) {
            throw new IllegalArgumentException("History data could not be decoded", exception);
        }
    }

    private static void writeEntry(DataOutputStream output, GameHistoryEntry entry)
        throws IOException {
        output.writeLong(entry.getCompletedAtEpochMillis());
        output.writeLong(entry.getActiveDurationMillis());
        output.writeInt(entry.getPlayerCount());
        output.writeInt(entry.getPairCount());
        output.writeInt(entry.getParticipantCount());
        for (int participant = 0; participant < entry.getParticipantCount(); participant++) {
            output.writeUTF(entry.getParticipantName(participant));
            output.writeInt(entry.getScore(participant));
        }
        writeOptionalArray(output, entry.copyAccuracyPercents());
        writeOptionalArray(output, entry.copyLongestStreaks());
    }

    private static GameHistoryEntry readEntry(DataInputStream input) throws IOException {
        long completedAt = input.readLong();
        long duration = input.readLong();
        int playerCount = input.readInt();
        int pairCount = input.readInt();
        int participantCount = input.readInt();
        if (participantCount < 1 || participantCount > GameHistoryEntry.MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("History participant count is invalid");
        }
        String[] names = new String[participantCount];
        int[] scores = new int[participantCount];
        for (int participant = 0; participant < participantCount; participant++) {
            names[participant] = input.readUTF();
            scores[participant] = input.readInt();
        }
        int[] accuracies = readOptionalArray(input, participantCount);
        int[] streaks = readOptionalArray(input, participantCount);
        return new GameHistoryEntry(
            completedAt,
            duration,
            playerCount,
            pairCount,
            names,
            scores,
            accuracies,
            streaks
        );
    }

    private static void writeOptionalArray(DataOutputStream output, int[] values)
        throws IOException {
        output.writeBoolean(values != null);
        if (values != null) {
            for (int value : values) {
                output.writeInt(value);
            }
        }
    }

    private static int[] readOptionalArray(DataInputStream input, int expectedLength)
        throws IOException {
        if (!input.readBoolean()) {
            return null;
        }
        int[] values = new int[expectedLength];
        for (int index = 0; index < expectedLength; index++) {
            values[index] = input.readInt();
        }
        return values;
    }

    private static String toHex(byte[] bytes) {
        char[] encoded = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & 0xff;
            encoded[index * 2] = HEX[value >>> 4];
            encoded[index * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(encoded);
    }

    private static byte[] fromHex(String encoded) {
        if ((encoded.length() & 1) != 0) {
            throw new IllegalArgumentException("Encoded history has an invalid length");
        }
        byte[] bytes = new byte[encoded.length() / 2];
        for (int index = 0; index < bytes.length; index++) {
            int high = Character.digit(encoded.charAt(index * 2), 16);
            int low = Character.digit(encoded.charAt(index * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Encoded history contains invalid characters");
            }
            bytes[index] = (byte) ((high << 4) | low);
        }
        return bytes;
    }
}
