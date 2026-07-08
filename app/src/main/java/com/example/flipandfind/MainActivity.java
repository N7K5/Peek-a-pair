package com.example.flipandfind;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
@SuppressWarnings("deprecation")
public final class MainActivity extends Activity {
    private static final String SETTINGS = "flip_and_find_settings";
    private static final String DARK_THEME = "dark_theme";
    private static final String THEME_PRESET = "theme_preset";
    private static final String SWAP_AFTER_MISS = "swap_after_miss";
    private static final String MISS_REVEAL_TENTHS = "miss_reveal_tenths";
    private static final String USE_CARD_COLORS = "use_card_colors";
    private static final String TABLETOP_MODE = "tabletop_mode";
    private static final String CARD_BACK_STYLE = "card_back_style";
    private static final String TURN_HANDOFF = "turn_handoff";
    private static final String SOUND_ENABLED = "sound_enabled";
    private static final String HAPTICS_ENABLED = "haptics_enabled";
    private static final String REDUCED_MOTION = "reduced_motion";
    private static final String HIGH_CONTRAST = "high_contrast";
    private static final String COLOR_BLIND_PALETTE = "color_blind_palette";
    private static final String LARGER_CARDS = "larger_cards";
    private static final int REQUEST_FACE_PHOTO = 7001;

    private int BACKGROUND;
    private int SURFACE;
    private int SURFACE_TINT;
    private int INK;
    private int MUTED;
    private int PRIMARY;
    private int PRIMARY_DARK;
    private int ACCENT;
    private int SUCCESS;
    private int DIVIDER;

    private static final int[] PLAYER_COLORS = {
        Color.rgb(91, 75, 219),
        Color.rgb(232, 74, 95),
        Color.rgb(0, 137, 123),
        Color.rgb(245, 158, 11),
        Color.rgb(59, 130, 246),
        Color.rgb(139, 92, 246),
        Color.rgb(217, 119, 6),
        Color.rgb(15, 118, 110),
        Color.rgb(190, 24, 93),
        Color.rgb(71, 85, 105),
        Color.rgb(8, 145, 178),
        Color.rgb(101, 163, 13),
        Color.rgb(194, 65, 12),
        Color.rgb(126, 34, 206),
        Color.rgb(5, 150, 105)
    };

    private static final IconCatalog.Category[] CATEGORY_ORDER = {
        IconCatalog.Category.RANDOM,
        IconCatalog.Category.FACES,
        IconCatalog.Category.FLAGS,
        IconCatalog.Category.ANIMALS,
        IconCatalog.Category.FOOD,
        IconCatalog.Category.NATURE,
        IconCatalog.Category.ACTIVITIES,
        IconCatalog.Category.TRAVEL,
        IconCatalog.Category.OBJECTS,
        IconCatalog.Category.SYMBOLS,
        IconCatalog.Category.RUBICS,
        IconCatalog.Category.NUMBERS,
        IconCatalog.Category.WORDS,
        IconCatalog.Category.BLANK
    };

    private static final long REVEAL_DELAY_MS = 900L;
    private static final long COMPUTER_THINK_MS = 650L;
    private static final long AFTER_MATCH_MS = 450L;
    private static final long MATCH_FLIGHT_MS = 300L;

    private enum Screen {
        SETUP,
        PROFILES,
        ADVANCED,
        HISTORY,
        GAME,
        RESULTS
    }

    private static final class CardBackOptionBinding {
        final LinearLayout option;
        final TextView name;

        CardBackOptionBinding(LinearLayout option, TextView name) {
            this.option = option;
            this.name = name;
        }
    }

    @FunctionalInterface
    private interface AdvancedToggleAction {
        void run(TextView toggle);
    }

    private enum ThemeVisualStyle {
        CLASSIC,
        FLAT,
        GLASS,
        ARCADE,
        PAPER,
        BUBBLE
    }

    private enum ThemePreset {
        SYSTEM("Default (follows system)", "DEFAULT", -1),
        MINIMAL_LIGHT("Minimal light", "MIN•L", 0),
        MINIMAL_DARK("Minimal dark", "MIN•D", 1),
        SIMPLE("Simple / Flat (follows system)", "SIMPLE", -1),
        MIXED_COLOR("Mixed Bubble (follows system)", "MIXED", -1),
        DARK_TEAL("Dark teal", "D•TEAL", 1),
        DARK_VIOLET("Dark violet", "D•VIOLET", 1),
        DARK_ORANGE("Dark orange", "D•ORANGE", 1),
        DARK_BLUE("Dark blue", "D•BLUE", 1),
        DARK_ROSE("Dark rose", "D•ROSE", 1),
        DARK_VIBRANT_ORANGE("Dark vibrant orange", "V•ORANGE", 1),
        DARK_VIBRANT_WHITE("Dark vibrant white", "V•WHITE", 1),
        DARK_VIBRANT_CYAN("Dark vibrant cyan", "V•CYAN", 1),
        DARK_VIBRANT_LIME("Dark vibrant lime", "V•LIME", 1),
        DARK_VIBRANT_PINK("Dark vibrant pink", "V•PINK", 1),
        LIGHT_TEAL("Light teal", "L•TEAL", 0),
        LIGHT_VIOLET("Light violet", "L•VIOLET", 0),
        LIGHT_ORANGE("Light orange", "L•ORANGE", 0),
        LIGHT_BLUE("Light blue", "L•BLUE", 0),
        LIGHT_ROSE("Light rose", "L•ROSE", 0),
        GLASS("Aurora Glass (follows system)", "GLASS", -1),
        ARCADE("Neon Arcade", "ARCADE", 1),
        PAPER("Paper Table", "PAPER", 0);

        private final String displayName;
        private final String buttonLabel;
        private final int mode;

        ThemePreset(String displayName, String buttonLabel, int mode) {
            this.displayName = displayName;
            this.buttonLabel = buttonLabel;
            this.mode = mode;
        }

        private static ThemePreset load(SharedPreferences settings) {
            String saved = settings.getString(THEME_PRESET, "");
            if (saved != null && !saved.isEmpty()) {
                try {
                    return canonical(valueOf(saved));
                } catch (IllegalArgumentException ignored) {
                    // Migrate from the original dark/light boolean below.
                }
            }
            if (settings.contains(DARK_THEME)) {
                return settings.getBoolean(DARK_THEME, true) ? DARK_TEAL : LIGHT_TEAL;
            }
            return SYSTEM;
        }

        private static ThemePreset canonical(ThemePreset preset) {
            switch (preset) {
                case DARK_VIBRANT_WHITE:
                    return MINIMAL_DARK;
                case DARK_VIBRANT_ORANGE:
                    return DARK_ORANGE;
                default:
                    return preset;
            }
        }

        private static ThemePreset[] selectable() {
            return new ThemePreset[] {
                SYSTEM,
                MINIMAL_LIGHT,
                MINIMAL_DARK,
                SIMPLE,
                MIXED_COLOR,
                GLASS,
                ARCADE,
                PAPER,
                DARK_TEAL,
                DARK_VIOLET,
                DARK_ORANGE,
                DARK_BLUE,
                DARK_ROSE,
                DARK_VIBRANT_CYAN,
                DARK_VIBRANT_LIME,
                DARK_VIBRANT_PINK,
                LIGHT_TEAL,
                LIGHT_VIOLET,
                LIGHT_ORANGE,
                LIGHT_BLUE,
                LIGHT_ROSE
            };
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Screen screen = Screen.SETUP;
    private int selectedHumanPlayers = 2;
    private int selectedPairs = GameState.recommendedPairsForPlayers(2);
    private IconCatalog.Category selectedIconCategory = IconCatalog.Category.RANDOM;
    private ComputerDifficulty selectedDifficulty = ComputerDifficulty.EASY;
    private boolean trickyMode;
    private boolean swapAfterMiss;
    private int missRevealTenths = MissRevealDuration.DEFAULT_TENTHS;
    private boolean useCardColors;
    private TabletopMode tabletopMode = TabletopMode.STATIC_THEME;
    private CardBackStyle cardBackStyle = CardBackStyle.CLASSIC;
    private ThemePreset themePreset = ThemePreset.SYSTEM;
    private boolean darkTheme = true;
    private boolean turnHandoffEnabled;
    private boolean soundEnabled;
    private boolean hapticsEnabled = true;
    private boolean reducedMotion;
    private boolean highContrast;
    private boolean colorBlindPalette;
    private boolean largerCards;
    private boolean pairCountCustomized;
    private int actionGeneration;
    private boolean paused;
    private boolean inputLocked;
    private boolean leaveDialogVisible;
    private boolean animateBoardEntrance;
    private boolean turnHandoffRequired;
    private boolean immersiveWindowConfigured;

    private GameState game;
    private GameStats gameStats;
    private ComputerMemory computerMemory;
    private GameFeedback gameFeedback;
    private GameHistoryStore historyStore;
    private GameSeries gameSeries;
    private GameTimer gameTimer;
    private long lastGameDurationMillis;
    private boolean completedGameRecorded;
    private long pairDecisionElapsedMillis;
    private long pairDecisionSegmentStartedAtElapsed = -1L;
    private long pendingPairDecisionDurationMillis = GameStats.NO_DECISION_TIME;
    private CardTileView[] cardViews;
    private BoardLayout boardLayout;
    private int[] restoredBoardSlots;
    private PlayerProfileStore profileStore;
    private final List<EditText> profileNameInputs = new ArrayList<>();
    private final List<Long> profileEditorIds = new ArrayList<>();
    private long pendingPhotoProfileId = -1L;
    private int pendingProfileScrollY = -1;
    private int pendingAdvancedScrollY = -1;

    private TextView setupPlayerValue;
    private TextView setupPairValue;
    private TextView iconCategoryHint;
    private TextView difficultyHint;
    private TextView trickyToggle;
    private TextView trickyHint;
    private TextView swapToggle;
    private TextView swapHint;
    private TextView colorToggle;
    private TextView colorHint;
    private FlowLayout iconCategoryFlow;
    private LinearLayout difficultySection;
    private LinearLayout difficultyRow;
    private TextView playerMinus;
    private TextView playerPlus;
    private TextView pairMinus;
    private TextView pairPlus;
    private TextView recommendedPairsButton;
    private TextView pairTouchButton;
    private TextView repeatingPairButton;
    private Runnable pairRepeatRunnable;

    private PlayerAvatarView turnAvatar;
    private TextView turnName;
    private TextView turnMessage;
    private TextView progressText;
    private HorizontalScrollView scoreScroll;
    private LinearLayout scoreRow;
    private TextView[] scoreChipViews;
    private LinearLayout gameRoot;
    private FrameLayout gameHost;
    private LinearLayout gameHeader;
    private LinearLayout gameBoardSection;
    private TextView leaveGameButton;
    private View turnHandoffOverlay;
    private int renderedHeaderPlayer = -1;
    private int renderedScorePlayer = -1;
    private boolean renderedScoreFinished;
    private int[] renderedScores;
    private boolean playerChromeApplied;
    private int appliedPlayerChromeColor;

    private AnimatorSet activeMatchFlight;
    private ViewGroup matchFlightHost;
    private final List<ImageView> matchFlightGhosts = new ArrayList<>();
    private final List<Bitmap> matchFlightBitmaps = new ArrayList<>();
    private long matchFlightGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(SETTINGS, MODE_PRIVATE);
        profileStore = new PlayerProfileStore(this);
        historyStore = new GameHistoryStore(this);
        boolean freshRootLaunch = savedInstanceState == null && isTaskRoot();
        if (freshRootLaunch && profileStore.size() == GameState.MIN_HUMAN_PLAYERS) {
            profileStore.ensureCount(PlayerRoster.DEFAULT_PLAYERS);
        }
        selectedHumanPlayers = profileStore.size();
        selectedPairs = GameState.recommendedPairsForPlayers(selectedHumanPlayers);
        themePreset = ThemePreset.load(settings);
        darkTheme = resolveDarkTheme(themePreset);
        swapAfterMiss = settings.getBoolean(SWAP_AFTER_MISS, false);
        missRevealTenths = MissRevealDuration.clampTenths(
            settings.getInt(MISS_REVEAL_TENTHS, MissRevealDuration.DEFAULT_TENTHS)
        );
        useCardColors = settings.getBoolean(USE_CARD_COLORS, false);
        tabletopMode = TabletopMode.fromPreference(settings.getString(TABLETOP_MODE, null));
        cardBackStyle = CardBackStyle.fromPreference(
            settings.getString(CARD_BACK_STYLE, null)
        );
        turnHandoffEnabled = settings.getBoolean(TURN_HANDOFF, false);
        soundEnabled = settings.getBoolean(SOUND_ENABLED, false);
        hapticsEnabled = settings.getBoolean(HAPTICS_ENABLED, true);
        reducedMotion = settings.getBoolean(REDUCED_MOTION, false);
        highContrast = settings.getBoolean(HIGH_CONTRAST, false);
        colorBlindPalette = settings.getBoolean(COLOR_BLIND_PALETTE, false);
        largerCards = settings.getBoolean(LARGER_CARDS, false);
        if (colorBlindPalette && !useCardColors) {
            useCardColors = true;
            settings.edit().putBoolean(USE_CARD_COLORS, true).apply();
        }
        gameFeedback = new GameFeedback();
        gameFeedback.setSoundEnabled(soundEnabled);
        gameFeedback.setHapticsEnabled(hapticsEnabled);
        applyThemePalette();
        enterImmersiveMode();

        if (savedInstanceState != null) {
            int savedPlayers = clamp(
                savedInstanceState.getInt("selectedPlayers", profileStore.size()),
                GameState.MIN_HUMAN_PLAYERS,
                GameState.MAX_HUMAN_PLAYERS
            );
            if (savedPlayers > profileStore.size()) {
                profileStore.ensureCount(savedPlayers);
            } else if (savedPlayers < profileStore.size()) {
                profileStore.resizeUsingDefaultProfiles(savedPlayers);
            }
            selectedHumanPlayers = profileStore.size();
            selectedPairs = savedInstanceState.getInt(
                "selectedPairs",
                GameState.recommendedPairsForPlayers(selectedHumanPlayers)
            );
            pairCountCustomized = savedInstanceState.getBoolean("pairsCustomized", false);
            try {
                selectedIconCategory = IconCatalog.Category.valueOf(
                    savedInstanceState.getString(
                        "iconCategory",
                        IconCatalog.Category.RANDOM.name()
                    )
                );
            } catch (IllegalArgumentException ignored) {
                selectedIconCategory = IconCatalog.Category.RANDOM;
            }
            try {
                selectedDifficulty = ComputerDifficulty.valueOf(
                    savedInstanceState.getString(
                        "computerDifficulty",
                        ComputerDifficulty.EASY.name()
                    )
                );
            } catch (IllegalArgumentException ignored) {
                selectedDifficulty = ComputerDifficulty.EASY;
            }
            trickyMode = savedInstanceState.getBoolean("trickyMode", false);
            swapAfterMiss = savedInstanceState.getBoolean("swapAfterMiss", swapAfterMiss);
            useCardColors = savedInstanceState.getBoolean("useCardColors", useCardColors);
            if (colorBlindPalette && !useCardColors) {
                useCardColors = true;
                settings.edit().putBoolean(USE_CARD_COLORS, true).apply();
            }
            if (selectedIconCategory == IconCatalog.Category.BLANK && !useCardColors) {
                useCardColors = true;
                settings.edit().putBoolean(USE_CARD_COLORS, true).apply();
            }
            pendingPhotoProfileId = savedInstanceState.getLong(
                "pendingPhotoProfileId",
                -1L
            );
            turnHandoffRequired = savedInstanceState.getBoolean(
                "turnHandoffRequired",
                false
            );
            restoreGame(savedInstanceState);
            String savedScreen = savedInstanceState.getString("screen", Screen.SETUP.name());
            try {
                screen = Screen.valueOf(savedScreen);
            } catch (IllegalArgumentException ignored) {
                screen = Screen.SETUP;
            }
        }

        if (game != null && game.getPhase() == GameState.Phase.FINISHED) {
            showResultsScreen();
        } else if (screen == Screen.GAME && game != null) {
            showGameScreen();
        } else if (screen == Screen.RESULTS && game != null) {
            showResultsScreen();
        } else if (screen == Screen.PROFILES) {
            showPlayerSettingsScreen();
        } else if (screen == Screen.ADVANCED) {
            showAdvancedSettingsScreen();
        } else if (screen == Screen.HISTORY) {
            showHistoryScreen();
        } else {
            showSetupScreen();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (screen == Screen.PROFILES) {
            saveProfileNamesFromInputs();
        }
        super.onSaveInstanceState(outState);
        outState.putString("screen", screen.name());
        outState.putInt("selectedPlayers", selectedHumanPlayers);
        outState.putInt("selectedPairs", selectedPairs);
        outState.putBoolean("pairsCustomized", pairCountCustomized);
        outState.putString("iconCategory", selectedIconCategory.name());
        outState.putString("computerDifficulty", selectedDifficulty.name());
        outState.putBoolean("trickyMode", trickyMode);
        outState.putBoolean("swapAfterMiss", swapAfterMiss);
        outState.putBoolean("useCardColors", useCardColors);
        outState.putLong("pendingPhotoProfileId", pendingPhotoProfileId);
        outState.putBoolean("turnHandoffRequired", turnHandoffRequired);

        if (game != null) {
            outState.putBoolean("hasGame", true);
            outState.putInt("gamePlayers", game.getHumanPlayerCount());
            outState.putInt("gamePairs", game.getPairCount());
            outState.putIntArray("deck", game.copyPairIds());
            outState.putIntArray("cardStates", game.copyCardStateOrdinals());
            outState.putIntArray("scores", game.copyScores());
            outState.putInt("currentPlayer", game.getCurrentPlayer());
            outState.putInt("firstCard", game.getFirstCard());
            outState.putInt("secondCard", game.getSecondCard());
            outState.putInt("remainingPairs", game.getRemainingPairs());
            outState.putString("phase", game.getPhase().name());
            if (computerMemory != null) {
                outState.putIntArray("memoryIndices", computerMemory.copyRememberedIndices());
                outState.putIntArray("memoryPairIds", computerMemory.copyRememberedPairIds());
            }
            if (boardLayout != null) {
                outState.putIntArray("boardSlots", boardLayout.copySlotPermutation());
            }
            if (gameStats != null) {
                outState.putIntArray("statsAttempts", gameStats.copyAttempts());
                outState.putIntArray("statsMatches", gameStats.copyMatches());
                outState.putIntArray("statsCurrentStreaks", gameStats.copyCurrentStreaks());
                outState.putIntArray("statsLongestStreaks", gameStats.copyLongestStreaks());
                outState.putIntArray("statsMaxDeficits", gameStats.copyMaxDeficits());
                outState.putLongArray(
                    "statsDecisionTimeTotals",
                    gameStats.copyDecisionTimeTotalsMillis()
                );
                outState.putIntArray("statsTimedAttempts", gameStats.copyTimedAttempts());
            }
            outState.putLong(
                "pairDecisionElapsed",
                currentPairDecisionElapsedMillis(SystemClock.elapsedRealtime())
            );
            outState.putLong(
                "pendingPairDecisionDuration",
                pendingPairDecisionDurationMillis
            );
            if (gameTimer != null) {
                outState.putLong(
                    "gameActiveDuration",
                    gameTimer.getElapsedMillis(SystemClock.elapsedRealtime())
                );
                outState.putBoolean(
                    "gameTimerRunning",
                    screen == Screen.GAME && game.getPhase() != GameState.Phase.FINISHED
                );
            }
            outState.putLong("lastGameDuration", lastGameDurationMillis);
            outState.putBoolean("completedGameRecorded", completedGameRecorded);
            if (gameSeries != null) {
                outState.putStringArray("seriesNames", gameSeries.copyParticipantNames());
                outState.putInt("seriesGames", gameSeries.getGameCount());
                outState.putInt("seriesTies", gameSeries.getTiedGameCount());
                outState.putIntArray("seriesWins", gameSeries.copyWins());
                outState.putIntArray(
                    "seriesCurrentStreaks",
                    gameSeries.copyCurrentWinStreaks()
                );
                outState.putIntArray(
                    "seriesLongestStreaks",
                    gameSeries.copyLongestWinStreaks()
                );
            }
        }
    }

    private void restoreGame(Bundle state) {
        if (!state.getBoolean("hasGame", false)) {
            return;
        }
        try {
            game = GameState.restore(
                state.getInt("gamePlayers"),
                state.getInt("gamePairs"),
                state.getIntArray("deck"),
                state.getIntArray("cardStates"),
                state.getIntArray("scores"),
                state.getInt("currentPlayer"),
                state.getInt("firstCard", -1),
                state.getInt("secondCard", -1),
                state.getInt("remainingPairs"),
                state.getString("phase", GameState.Phase.WAITING_FIRST.name())
            );
            computerMemory = ComputerMemory.restore(
                selectedDifficulty,
                state.getIntArray("memoryIndices"),
                state.getIntArray("memoryPairIds")
            );
            long[] decisionTimeTotals = state.getLongArray("statsDecisionTimeTotals");
            int[] timedAttempts = state.getIntArray("statsTimedAttempts");
            gameStats = decisionTimeTotals == null || timedAttempts == null
                ? GameStats.restore(
                    game.getTotalPlayerCount(),
                    state.getIntArray("statsAttempts"),
                    state.getIntArray("statsMatches"),
                    state.getIntArray("statsCurrentStreaks"),
                    state.getIntArray("statsLongestStreaks"),
                    state.getIntArray("statsMaxDeficits")
                )
                : GameStats.restore(
                    game.getTotalPlayerCount(),
                    state.getIntArray("statsAttempts"),
                    state.getIntArray("statsMatches"),
                    state.getIntArray("statsCurrentStreaks"),
                    state.getIntArray("statsLongestStreaks"),
                    state.getIntArray("statsMaxDeficits"),
                    decisionTimeTotals,
                    timedAttempts
                );
            if (!java.util.Arrays.equals(
                gameStats.copyMatches(),
                game.copyScores()
            )) {
                throw new IllegalArgumentException("Saved statistics do not match the scores");
            }
            restoredBoardSlots = state.getIntArray("boardSlots");
            long nowElapsed = SystemClock.elapsedRealtime();
            gameTimer = GameTimer.restore(
                state.getLong("gameActiveDuration", 0L),
                state.getBoolean(
                    "gameTimerRunning",
                    game.getPhase() != GameState.Phase.FINISHED
                ),
                nowElapsed
            );
            lastGameDurationMillis = state.getLong("lastGameDuration", 0L);
            completedGameRecorded = state.getBoolean("completedGameRecorded", false);
            pairDecisionElapsedMillis = game.getPhase() == GameState.Phase.WAITING_SECOND
                ? Math.max(0L, state.getLong("pairDecisionElapsed", 0L))
                : 0L;
            pairDecisionSegmentStartedAtElapsed = -1L;
            pendingPairDecisionDurationMillis = game.getPhase() == GameState.Phase.RESOLVING
                ? Math.max(
                    GameStats.NO_DECISION_TIME,
                    state.getLong(
                        "pendingPairDecisionDuration",
                        GameStats.NO_DECISION_TIME
                    )
                )
                : GameStats.NO_DECISION_TIME;
            String[] seriesNames = state.getStringArray("seriesNames");
            if (seriesNames == null) {
                gameSeries = new GameSeries(currentParticipantNames());
            } else {
                gameSeries = GameSeries.restore(
                    seriesNames,
                    state.getInt("seriesGames", 0),
                    state.getInt("seriesTies", 0),
                    state.getIntArray("seriesWins"),
                    state.getIntArray("seriesCurrentStreaks"),
                    state.getIntArray("seriesLongestStreaks")
                );
            }
        } catch (RuntimeException ignored) {
            game = null;
            gameStats = null;
            computerMemory = null;
            gameSeries = null;
            gameTimer = null;
            resetPairDecisionClock();
            screen = Screen.SETUP;
        }
    }

    private void showSetupScreen() {
        cancelPendingActions();
        screen = Screen.SETUP;
        setWindowBackground(BACKGROUND);
        game = null;
        gameStats = null;
        computerMemory = null;
        gameSeries = null;
        gameTimer = null;
        lastGameDurationMillis = 0L;
        completedGameRecorded = false;
        resetPairDecisionClock();
        cardViews = null;
        boardLayout = null;
        restoredBoardSlots = null;
        profileNameInputs.clear();
        profileEditorIds.clear();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout setupHost = new FrameLayout(this);
        applyScreenBackground(setupHost);
        applySafeCutoutInsets(setupHost);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        applyScreenBackground(scroll);
        setupHost.addView(
            scroll,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );

        LinearLayout content = verticalLayout();
        content.setPadding(dp(24), dp(24), dp(24), dp(32));
        scroll.addView(content, matchWrap());

        LinearLayout brandRow = horizontalLayout();
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(brandRow, matchWrap());

        TextView logo = label("✦", 29, contrastTextColor(PRIMARY), true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(rounded(PRIMARY, 18));
        logo.setElevation(dp(themedElevation(4)));
        brandRow.addView(logo, fixed(dp(64), dp(64)));

        LinearLayout titleBlock = verticalLayout();
        brandRow.addView(titleBlock, margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 16, 0, 0, 0));
        titleBlock.addView(label("Peek-a-Pair", 32, INK, true), matchWrap());
        titleBlock.addView(label("Remember. Match. Win together.", 15, MUTED, false), margins(matchWrap(), 0, 3, 0, 0));

        TextView exitApp = label("×", 27, INK, false);
        exitApp.setGravity(Gravity.CENTER);
        exitApp.setFocusable(true);
        exitApp.setContentDescription("Exit Peek-a-Pair");
        exitApp.setBackground(ripple(SURFACE_TINT, 14));
        exitApp.setOnClickListener(view -> {
            cancelPendingActions();
            finishAndRemoveTask();
        });
        brandRow.addView(exitApp, fixed(dp(48), dp(48)));

        TextView intro = label(
            "A quick pass-and-play challenge for friends and family.",
            16,
            MUTED,
            false
        );
        intro.setLineSpacing(0f, 1.16f);
        content.addView(intro, margins(matchWrap(), 0, 20, 0, 0));

        LinearLayout setupCard = verticalLayout();
        setupCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        setupCard.setBackground(rounded(SURFACE, 22));
        setupCard.setElevation(dp(themedElevation(3)));
        content.addView(setupCard, margins(matchWrap(), 0, 8, 0, 0));

        LinearLayout playerHeading = horizontalLayout();
        playerHeading.setGravity(Gravity.CENTER_VERTICAL);
        setupCard.addView(playerHeading, matchWrap());
        playerHeading.addView(
            sectionTitle("PLAYERS"),
            weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );
        TextView editPlayers = label("EDIT PLAYERS", 11, PRIMARY_DARK, true);
        editPlayers.setGravity(Gravity.CENTER);
        editPlayers.setMinHeight(dp(48));
        editPlayers.setPadding(dp(12), dp(8), dp(12), dp(8));
        editPlayers.setFocusable(true);
        editPlayers.setBackground(ripple(SURFACE_TINT, 14));
        editPlayers.setContentDescription("Edit player names and photos");
        editPlayers.setOnClickListener(view -> showPlayerSettingsScreen());
        playerHeading.addView(editPlayers, wrapWrap());
        setupCard.addView(label("How many people are playing?", 19, INK, true), margins(matchWrap(), 0, 5, 0, 0));

        LinearLayout playerStepper = stepperRow();
        setupCard.addView(playerStepper, margins(matchWrap(), 0, 15, 0, 0));
        playerMinus = stepButton("−");
        playerPlus = stepButton("+");
        setupPlayerValue = stepValue();
        playerMinus.setOnClickListener(view -> changePlayers(-1));
        playerPlus.setOnClickListener(view -> changePlayers(1));
        playerStepper.addView(playerMinus, fixed(dp(72), dp(52)));
        playerStepper.addView(setupPlayerValue, margins(weighted(0, dp(52), 1f), 8, 0, 8, 0));
        playerStepper.addView(playerPlus, fixed(dp(72), dp(52)));

        difficultySection = verticalLayout();
        View difficultyDivider = new View(this);
        difficultyDivider.setBackgroundColor(DIVIDER);
        difficultySection.addView(
            difficultyDivider,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)), 0, 21, 0, 21)
        );
        difficultySection.addView(sectionTitle("BOT LEVEL"), matchWrap());
        difficultySection.addView(
            label("How strong should the bot be?", 19, INK, true),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        difficultyHint = label("", 14, MUTED, false);
        difficultySection.addView(difficultyHint, margins(matchWrap(), 0, 4, 0, 0));
        difficultyRow = verticalLayout();
        difficultySection.addView(difficultyRow, margins(matchWrap(), 0, 12, 0, 0));
        setupCard.addView(difficultySection, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(DIVIDER);
        setupCard.addView(divider, margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)), 0, 21, 0, 21));

        setupCard.addView(sectionTitle("BOARD SIZE"), matchWrap());
        LinearLayout pairHeading = horizontalLayout();
        pairHeading.setGravity(Gravity.CENTER_VERTICAL);
        setupCard.addView(pairHeading, margins(matchWrap(), 0, 5, 0, 0));
        pairHeading.addView(
            label("How many matching pairs?", 19, INK, true),
            weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );
        recommendedPairsButton = label("DEFAULT", 10, PRIMARY_DARK, true);
        recommendedPairsButton.setGravity(Gravity.CENTER);
        recommendedPairsButton.setMinHeight(dp(48));
        recommendedPairsButton.setPadding(dp(10), dp(7), dp(10), dp(7));
        recommendedPairsButton.setFocusable(true);
        recommendedPairsButton.setBackground(ripple(SURFACE_TINT, 12));
        recommendedPairsButton.setOnClickListener(view -> useRecommendedPairCount());
        pairHeading.addView(
            recommendedPairsButton,
            margins(wrapWrap(), 10, 0, 0, 0)
        );
        LinearLayout pairStepper = stepperRow();
        setupCard.addView(pairStepper, margins(matchWrap(), 0, 15, 0, 0));
        pairMinus = stepButton("−");
        pairPlus = stepButton("+");
        setupPairValue = stepValue();
        pairMinus.setOnClickListener(view -> changePairs(-1));
        pairPlus.setOnClickListener(view -> changePairs(1));
        configurePairRepeat(pairMinus, -1);
        configurePairRepeat(pairPlus, 1);
        pairStepper.addView(pairMinus, fixed(dp(72), dp(52)));
        pairStepper.addView(setupPairValue, margins(weighted(0, dp(52), 1f), 8, 0, 8, 0));
        pairStepper.addView(pairPlus, fixed(dp(72), dp(52)));

        View iconDivider = new View(this);
        iconDivider.setBackgroundColor(DIVIDER);
        setupCard.addView(
            iconDivider,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)), 0, 21, 0, 21)
        );

        setupCard.addView(sectionTitle("CARD CONTENT"), matchWrap());
        setupCard.addView(
            label("Choose what appears on the cards", 19, INK, true),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        iconCategoryHint = label("", 14, MUTED, false);
        setupCard.addView(iconCategoryHint, margins(matchWrap(), 0, 4, 0, 0));

        iconCategoryFlow = new FlowLayout(this);
        iconCategoryFlow.setSpacing(dp(7), dp(7));
        setupCard.addView(iconCategoryFlow, margins(matchWrap(), 0, 13, 0, 0));

        LinearLayout trickyRow = horizontalLayout();
        trickyRow.setGravity(Gravity.CENTER_VERTICAL);
        setupCard.addView(trickyRow, margins(matchWrap(), 0, 15, 0, 0));
        LinearLayout trickyText = verticalLayout();
        trickyRow.addView(
            trickyText,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 0, 0, 12, 0)
        );
        trickyText.addView(label("Tricky mode", 17, INK, true), matchWrap());
        trickyHint = label("", 13, MUTED, false);
        trickyText.addView(trickyHint, margins(matchWrap(), 0, 3, 0, 0));
        trickyToggle = label("", 12, INK, true);
        trickyToggle.setGravity(Gravity.CENTER);
        trickyToggle.setMinWidth(dp(60));
        trickyToggle.setMinHeight(dp(48));
        trickyToggle.setFocusable(true);
        trickyToggle.setOnClickListener(view -> {
            trickyMode = !trickyMode;
            renderSetupControls();
        });
        trickyRow.addView(trickyToggle, fixed(dp(64), dp(48)));

        LinearLayout swapRow = horizontalLayout();
        swapRow.setGravity(Gravity.CENTER_VERTICAL);
        setupCard.addView(swapRow, margins(matchWrap(), 0, 15, 0, 0));
        LinearLayout swapText = verticalLayout();
        swapRow.addView(
            swapText,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 0, 0, 12, 0)
        );
        swapText.addView(label("Moving cards", 17, INK, true), matchWrap());
        swapHint = label("", 13, MUTED, false);
        swapText.addView(swapHint, margins(matchWrap(), 0, 3, 0, 0));
        swapToggle = label("", 12, INK, true);
        configureToggleButton(swapToggle);
        swapToggle.setOnClickListener(view -> {
            swapAfterMiss = !swapAfterMiss;
            getSharedPreferences(SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean(SWAP_AFTER_MISS, swapAfterMiss)
                .apply();
            renderSetupControls();
        });
        swapRow.addView(swapToggle, fixed(dp(64), dp(48)));

        LinearLayout colorRow = horizontalLayout();
        colorRow.setGravity(Gravity.CENTER_VERTICAL);
        setupCard.addView(colorRow, margins(matchWrap(), 0, 15, 0, 0));
        LinearLayout colorText = verticalLayout();
        colorRow.addView(
            colorText,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 0, 0, 12, 0)
        );
        colorText.addView(label("Pair colors", 17, INK, true), matchWrap());
        colorHint = label("", 13, MUTED, false);
        colorText.addView(colorHint, margins(matchWrap(), 0, 3, 0, 0));
        colorToggle = label("", 12, INK, true);
        configureToggleButton(colorToggle);
        colorToggle.setOnClickListener(view -> {
            useCardColors = !useCardColors;
            if (!useCardColors && selectedIconCategory == IconCatalog.Category.BLANK) {
                selectedIconCategory = IconCatalog.Category.RANDOM;
            }
            if (!useCardColors && colorBlindPalette) {
                colorBlindPalette = false;
            }
            getSharedPreferences(SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean(USE_CARD_COLORS, useCardColors)
                .putBoolean(COLOR_BLIND_PALETTE, colorBlindPalette)
                .apply();
            renderSetupControls();
        });
        colorRow.addView(colorToggle, fixed(dp(64), dp(48)));

        View startAnchor = new View(this);
        startAnchor.setFocusable(false);
        startAnchor.setClickable(false);
        startAnchor.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setupCard.addView(
            startAnchor,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)), 0, 22, 0, 0)
        );

        TextView start = actionButton("START GAME", PRIMARY, contrastTextColor(PRIMARY));
        start.setTextSize(18);
        start.setTypeface(prominentButtonTypeface());
        start.setLetterSpacing(0.065f);
        start.setContentDescription("Start game");
        start.setOnClickListener(view -> {
            if (!start.isEnabled()) {
                return;
            }
            start.setEnabled(false);
            startNewGame();
        });

        TextView advanced = actionButton(
            "ADVANCED SETTINGS",
            SURFACE_TINT,
            darkTheme ? INK : PRIMARY_DARK
        );
        advanced.setOnClickListener(view -> showAdvancedSettingsScreen());
        setupCard.addView(
            advanced,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)), 0, 10, 0, 0)
        );

        long historyGameCount = historyStore.getHistory().getTotalGamesPlayed();
        TextView history = actionButton(
            historyGameCount == 0L
                ? "GAME HISTORY"
                : "GAME HISTORY  •  " + historyGameCount + " GAME"
                    + (historyGameCount == 1L ? "" : "S"),
            SURFACE_TINT,
            darkTheme ? INK : PRIMARY_DARK
        );
        history.setOnClickListener(view -> showHistoryScreen());
        setupCard.addView(
            history,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)), 0, 9, 0, 0)
        );

        TextView footnote = label("MADE FOR SQUAD O' MOUNTAINS", 12, MUTED, true);
        footnote.setGravity(Gravity.CENTER);
        footnote.setLetterSpacing(0.08f);
        content.addView(footnote, margins(matchWrap(), 0, 22, 0, 0));

        colorToggle.setId(View.generateViewId());
        start.setId(View.generateViewId());
        advanced.setId(View.generateViewId());
        history.setId(View.generateViewId());
        start.setAccessibilityTraversalAfter(colorToggle.getId());
        start.setAccessibilityTraversalBefore(advanced.getId());
        advanced.setAccessibilityTraversalAfter(start.getId());
        advanced.setAccessibilityTraversalBefore(history.getId());
        history.setAccessibilityTraversalAfter(advanced.getId());
        start.setNextFocusDownId(advanced.getId());
        advanced.setNextFocusUpId(start.getId());
        advanced.setNextFocusDownId(history.getId());
        history.setNextFocusUpId(advanced.getId());

        FrameLayout.LayoutParams stickyStartParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(58),
            Gravity.BOTTOM
        );
        stickyStartParams.setMargins(dp(44), 0, dp(44), dp(14));
        setupHost.addView(start, stickyStartParams);
        Runnable updateStickyStart = () -> updateStickyStartPosition(
            setupHost,
            startAnchor,
            start
        );
        scroll.setOnScrollChangeListener(
            (view, scrollX, scrollY, oldScrollX, oldScrollY) -> updateStickyStart.run()
        );
        setupHost.addOnLayoutChangeListener(
            (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateStickyStart.run()
        );
        startAnchor.addOnLayoutChangeListener(
            (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateStickyStart.run()
        );
        setupHost.getViewTreeObserver().addOnGlobalLayoutListener(updateStickyStart::run);

        renderSetupControls();
        setContentView(setupHost);
        enterImmersiveMode();
        setupHost.post(updateStickyStart);
    }

    private void showAdvancedSettingsScreen() {
        cancelPendingActions();
        screen = Screen.ADVANCED;
        setWindowBackground(BACKGROUND);
        game = null;
        gameStats = null;
        computerMemory = null;
        cardViews = null;
        boardLayout = null;
        restoredBoardSlots = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        applyScreenBackground(scroll);
        scroll.setOnScrollChangeListener(
            (view, scrollX, scrollY, oldScrollX, oldScrollY) ->
                CardBackAnimationTicker.wake()
        );
        applySafeCutoutInsets(scroll);

        LinearLayout content = verticalLayout();
        content.setPadding(dp(22), dp(22), dp(22), dp(32));
        scroll.addView(content, matchWrap());

        LinearLayout heading = horizontalLayout();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(heading, matchWrap());

        TextView back = label("‹", 32, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back to game setup");
        back.setBackground(ripple(SURFACE_TINT, 14));
        back.setOnClickListener(view -> showSetupScreen());
        heading.addView(back, fixed(dp(48), dp(48)));

        LinearLayout titleBlock = verticalLayout();
        heading.addView(
            titleBlock,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 12, 0, 0, 0)
        );
        titleBlock.addView(label("Advanced settings", 27, INK, true), matchWrap());
        titleBlock.addView(
            label("Gameplay and accessibility choices stay on this device", 13, MUTED, false),
            margins(matchWrap(), 0, 3, 0, 0)
        );

        TextView explanation = label(
            "Tune turn privacy, feedback, motion, readability, and the tabletop appearance.",
            14,
            MUTED,
            false
        );
        explanation.setLineSpacing(0f, 1.12f);
        content.addView(explanation, margins(matchWrap(), 0, 18, 0, 0));

        LinearLayout themeCard = advancedSettingsCard();
        content.addView(themeCard, margins(matchWrap(), 0, 0, 0, 12));
        themeCard.addView(sectionTitle("THEME"), matchWrap());
        themeCard.addView(
            label(themePreset.displayName, 19, INK, true),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        themeCard.addView(
            label(
                "Choose colors, button shapes, typography, transparency, and surface style.",
                13,
                MUTED,
                false
            ),
            margins(matchWrap(), 0, 4, 0, 0)
        );
        TextView chooseTheme = actionButton(
            "CHOOSE THEME",
            SURFACE_TINT,
            darkTheme ? INK : PRIMARY_DARK
        );
        chooseTheme.setOnClickListener(view -> {
            pendingAdvancedScrollY = scroll.getScrollY();
            showThemePicker();
        });
        themeCard.addView(
            chooseTheme,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)), 0, 13, 0, 0)
        );

        LinearLayout gameplayCard = advancedSettingsCard();
        content.addView(gameplayCard, margins(matchWrap(), 0, 0, 0, 12));
        gameplayCard.addView(sectionTitle("GAMEPLAY FEEDBACK"), matchWrap());
        gameplayCard.addView(
            advancedToggle(
                "Private turn handoff",
                "Cover the board between players with a large avatar and Tap to start.",
                turnHandoffEnabled,
                toggle -> {
                    turnHandoffEnabled = !turnHandoffEnabled;
                    saveAdvancedToggle(
                        TURN_HANDOFF,
                        turnHandoffEnabled,
                        toggle,
                        "Private turn handoff"
                    );
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );

        LinearLayout missRevealControl = verticalLayout();
        missRevealControl.setPadding(dp(15), dp(13), dp(15), dp(10));
        missRevealControl.setBackground(outlined(SURFACE_TINT, DIVIDER, 1, 15));
        LinearLayout missRevealHeading = horizontalLayout();
        missRevealHeading.setGravity(Gravity.CENTER_VERTICAL);
        missRevealControl.addView(missRevealHeading, matchWrap());
        TextView missRevealTitle = label("Wrong-pair reveal time", 16, INK, true);
        missRevealHeading.addView(
            missRevealTitle,
            weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );
        TextView missRevealValue = label(
            MissRevealDuration.displayText(missRevealTenths),
            14,
            PRIMARY,
            true
        );
        missRevealValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        missRevealValue.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        missRevealHeading.addView(
            missRevealValue,
            margins(wrapWrap(), 10, 0, 0, 0)
        );
        missRevealControl.addView(
            label(
                "How long an incorrect pair stays face up before the next turn.",
                13,
                MUTED,
                false
            ),
            margins(matchWrap(), 0, 4, 0, 0)
        );

        SeekBar missRevealSlider = new SeekBar(this);
        missRevealSlider.setMax(MissRevealDuration.maxProgress());
        missRevealSlider.setProgress(
            MissRevealDuration.progressForTenths(missRevealTenths)
        );
        missRevealSlider.setKeyProgressIncrement(1);
        missRevealSlider.setProgressTintList(ColorStateList.valueOf(PRIMARY));
        missRevealSlider.setProgressBackgroundTintList(ColorStateList.valueOf(DIVIDER));
        missRevealSlider.setThumbTintList(ColorStateList.valueOf(PRIMARY));
        missRevealSlider.setContentDescription(
            "Wrong-pair reveal time, "
                + MissRevealDuration.displayText(missRevealTenths)
        );
        missRevealSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                missRevealTenths = MissRevealDuration.tenthsForProgress(progress);
                String duration = MissRevealDuration.displayText(missRevealTenths);
                missRevealValue.setText(duration);
                seekBar.setContentDescription("Wrong-pair reveal time, " + duration);
                if (fromUser) {
                    getSharedPreferences(SETTINGS, MODE_PRIVATE)
                        .edit()
                        .putInt(MISS_REVEAL_TENTHS, missRevealTenths)
                        .apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // The live value label announces the final selection.
            }
        });
        missRevealControl.addView(
            missRevealSlider,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)), 0, 4, 0, 0)
        );
        LinearLayout missRevealRange = horizontalLayout();
        TextView shortestReveal = label("0.5s", 12, MUTED, false);
        TextView longestReveal = label("3.0s", 12, MUTED, false);
        longestReveal.setGravity(Gravity.END);
        missRevealRange.addView(
            shortestReveal,
            weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );
        missRevealRange.addView(
            longestReveal,
            weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        );
        missRevealControl.addView(missRevealRange, matchWrap());
        gameplayCard.addView(
            missRevealControl,
            margins(matchWrap(), 0, 8, 0, 0)
        );

        gameplayCard.addView(
            advancedToggle(
                "Match sound",
                "Play one richer chime only when two cards match.",
                soundEnabled,
                toggle -> {
                    soundEnabled = !soundEnabled;
                    gameFeedback.setSoundEnabled(soundEnabled);
                    saveAdvancedToggle(SOUND_ENABLED, soundEnabled, toggle, "Match sound");
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );
        gameplayCard.addView(
            advancedToggle(
                "Haptics",
                "Use light touch feedback for cards and turn results.",
                hapticsEnabled,
                toggle -> {
                    hapticsEnabled = !hapticsEnabled;
                    gameFeedback.setHapticsEnabled(hapticsEnabled);
                    saveAdvancedToggle(HAPTICS_ENABLED, hapticsEnabled, toggle, "Haptics");
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );
        gameplayCard.addView(
            advancedToggle(
                "Reduced motion",
                "Make card flips, spreading, and movement instant.",
                reducedMotion,
                toggle -> {
                    reducedMotion = !reducedMotion;
                    saveAdvancedToggle(
                        REDUCED_MOTION,
                        reducedMotion,
                        toggle,
                        "Reduced motion"
                    );
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );

        LinearLayout accessibilityCard = advancedSettingsCard();
        content.addView(accessibilityCard, margins(matchWrap(), 0, 0, 0, 12));
        accessibilityCard.addView(sectionTitle("ACCESSIBILITY"), matchWrap());
        accessibilityCard.addView(
            advancedToggle(
                "High-contrast cards",
                "Strengthen card borders, faces, and symbol contrast.",
                highContrast,
                toggle -> {
                    highContrast = !highContrast;
                    saveAdvancedToggle(
                        HIGH_CONTRAST,
                        highContrast,
                        toggle,
                        "High-contrast cards"
                    );
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );
        accessibilityCard.addView(
            advancedToggle(
                "Color-blind palette + cues",
                "Use safer colors plus pair-specific patterns. This also turns Pair Colors on.",
                colorBlindPalette,
                toggle -> {
                    colorBlindPalette = !colorBlindPalette;
                    if (colorBlindPalette) {
                        useCardColors = true;
                    }
                    getSharedPreferences(SETTINGS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(COLOR_BLIND_PALETTE, colorBlindPalette)
                        .putBoolean(USE_CARD_COLORS, useCardColors)
                        .apply();
                    renderToggleState(
                        toggle,
                        colorBlindPalette,
                        "Color-blind palette + cues"
                    );
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );
        accessibilityCard.addView(
            advancedToggle(
                "Larger cards",
                "Pack the board more tightly so cards can use more of the available area.",
                largerCards,
                toggle -> {
                    largerCards = !largerCards;
                    saveAdvancedToggle(LARGER_CARDS, largerCards, toggle, "Larger cards");
                }
            ),
            margins(matchWrap(), 0, 8, 0, 0)
        );

        LinearLayout tabletopCard = advancedSettingsCard();
        content.addView(tabletopCard, margins(matchWrap(), 0, 0, 0, 12));
        tabletopCard.addView(sectionTitle("TABLETOP"), matchWrap());
        tabletopCard.addView(
            label("Board background", 19, INK, true),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        tabletopCard.addView(
            label(
                "The game surround still follows the current player. Choose how the inner tabletop is colored.",
                13,
                MUTED,
                false
            ),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        EnumMap<TabletopMode, LinearLayout> tabletopBindings =
            new EnumMap<>(TabletopMode.class);
        for (TabletopMode mode : TabletopMode.values()) {
            boolean selected = mode == tabletopMode;
            String description = mode == TabletopMode.STATIC_THEME
                ? "Pure black in dark themes and pure white in light themes."
                : "A subtle shade of the current player's color.";
            LinearLayout choice = advancedChoice(
                mode.getDisplayName(),
                description,
                selected
            );
            choice.setContentDescription(
                mode.getDisplayName() + (selected ? ", selected" : ", not selected")
            );
            tabletopBindings.put(mode, choice);
            choice.setOnClickListener(view -> {
                if (tabletopMode == mode) {
                    return;
                }
                tabletopMode = mode;
                getSharedPreferences(SETTINGS, MODE_PRIVATE)
                    .edit()
                    .putString(TABLETOP_MODE, mode.getPreferenceId())
                    .apply();
                renderTabletopSelection(tabletopBindings);
                view.announceForAccessibility(
                    mode.getDisplayName() + " tabletop selected"
                );
            });
            tabletopCard.addView(
                choice,
                margins(matchWrap(), 0, 9, 0, 0)
            );
        }

        LinearLayout cardBackCard = advancedSettingsCard();
        content.addView(cardBackCard, margins(matchWrap(), 0, 0, 0, 12));
        cardBackCard.addView(sectionTitle("CARD BACK"), matchWrap());
        cardBackCard.addView(
            label("Choose a design", 19, INK, true),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        cardBackCard.addView(
            label(
                "Choose a design for face-down cards.",
                13,
                MUTED,
                false
            ),
            margins(matchWrap(), 0, 5, 0, 0)
        );

        FlowLayout cardBackFlow = new FlowLayout(this);
        cardBackFlow.setSpacing(dp(8), dp(8));
        cardBackCard.addView(cardBackFlow, margins(matchWrap(), 0, 10, 0, 0));
        EnumMap<CardBackStyle, CardBackOptionBinding> cardBackBindings =
            new EnumMap<>(CardBackStyle.class);
        for (CardBackStyle style : CardBackStyle.values()) {
            LinearLayout option = verticalLayout();
            option.setGravity(Gravity.CENTER_HORIZONTAL);
            option.setPadding(dp(7), dp(8), dp(7), dp(7));
            option.setFocusable(true);
            option.setClickable(true);

            CardTileView preview = new CardTileView(this);
            preview.setCornerRadiusFraction(cardCornerRadiusFraction());
            preview.setCardNumber(style.ordinal() + 1);
            preview.setReducedMotion(reducedMotion);
            preview.setCardBackStyle(style);
            preview.setClickable(false);
            preview.setFocusable(false);
            preview.setEnabled(false);
            preview.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            );
            option.addView(preview, centered(dp(54), dp(66)));

            TextView optionName = label(
                style.getDisplayName(),
                10,
                INK,
                true
            );
            optionName.setGravity(Gravity.CENTER);
            optionName.setSingleLine(true);
            option.addView(optionName, margins(matchWrap(), 0, 7, 0, 0));
            cardBackBindings.put(
                style,
                new CardBackOptionBinding(option, optionName)
            );
            option.setOnClickListener(view -> {
                if (screen != Screen.ADVANCED || cardBackStyle == style) {
                    return;
                }
                cardBackStyle = style;
                getSharedPreferences(SETTINGS, MODE_PRIVATE)
                    .edit()
                    .putString(CARD_BACK_STYLE, style.getPreferenceId())
                    .apply();
                renderCardBackSelection(cardBackBindings);
                view.announceForAccessibility(
                    style.getDisplayName() + " card back selected"
                );
            });

            ViewGroup.MarginLayoutParams optionParams = new ViewGroup.MarginLayoutParams(
                dp(98),
                dp(116)
            );
            cardBackFlow.addView(option, optionParams);
        }
        renderCardBackSelection(cardBackBindings);

        TextView done = actionButton("DONE", PRIMARY, contrastTextColor(PRIMARY));
        done.setOnClickListener(view -> showSetupScreen());
        content.addView(
            done,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)), 0, 6, 0, 0)
        );

        setContentView(scroll);
        enterImmersiveMode();
        int scrollTarget = pendingAdvancedScrollY;
        pendingAdvancedScrollY = -1;
        if (scrollTarget >= 0) {
            restoreScrollBeforeFirstDraw(scroll, scrollTarget);
        }
        scroll.post(CardBackAnimationTicker::wake);
    }

    private void renderTabletopSelection(
        EnumMap<TabletopMode, LinearLayout> bindings
    ) {
        for (TabletopMode mode : TabletopMode.values()) {
            LinearLayout choice = bindings.get(mode);
            if (choice == null) {
                continue;
            }
            boolean selected = mode == tabletopMode;
            choice.setSelected(selected);
            choice.setBackground(outlined(
                selected ? SURFACE_TINT : SURFACE,
                selected ? PRIMARY : DIVIDER,
                selected ? 2 : 1,
                15
            ));
            choice.setContentDescription(
                mode.getDisplayName() + (selected ? ", selected" : ", not selected")
            );
            if (choice.getChildCount() > 0 && choice.getChildAt(0) instanceof TextView) {
                TextView title = (TextView) choice.getChildAt(0);
                title.setText((selected ? "✓ " : "") + mode.getDisplayName());
                title.setTextColor(selected ? PRIMARY_DARK : INK);
            }
        }
    }

    private void showHistoryScreen() {
        cancelPendingActions();
        screen = Screen.HISTORY;
        setWindowBackground(BACKGROUND);
        game = null;
        gameStats = null;
        computerMemory = null;
        gameSeries = null;
        gameTimer = null;
        cardViews = null;
        boardLayout = null;
        restoredBoardSlots = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        applyScreenBackground(scroll);
        applySafeCutoutInsets(scroll);

        LinearLayout content = verticalLayout();
        content.setPadding(dp(22), dp(22), dp(22), dp(32));
        scroll.addView(content, matchWrap());

        LinearLayout heading = horizontalLayout();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(heading, matchWrap());

        TextView back = label("‹", 32, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back to game setup");
        back.setBackground(ripple(SURFACE_TINT, 14));
        back.setOnClickListener(view -> showSetupScreen());
        heading.addView(back, fixed(dp(48), dp(48)));

        LinearLayout titleBlock = verticalLayout();
        heading.addView(
            titleBlock,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 12, 0, 0, 0)
        );
        titleBlock.addView(label("Game history", 27, INK, true), matchWrap());
        titleBlock.addView(
            label("Completed games stored only on this device", 13, MUTED, false),
            margins(matchWrap(), 0, 3, 0, 0)
        );

        GameHistory history = historyStore.getHistory();
        LinearLayout summary = verticalLayout();
        summary.setPadding(dp(18), dp(17), dp(18), dp(17));
        summary.setBackground(rounded(SURFACE, 20));
        summary.setElevation(dp(themedElevation(2)));
        content.addView(summary, margins(matchWrap(), 0, 20, 0, 0));

        if (history.getTotalGamesPlayed() == 0L) {
            summary.addView(label("No completed games yet", 19, INK, true), matchWrap());
            summary.addView(
                label(
                    "Finish a game and it will appear here with its winner, score, and play time.",
                    13,
                    MUTED,
                    false
                ),
                margins(matchWrap(), 0, 6, 0, 0)
            );
        } else {
            summary.addView(
                label(
                    history.getTotalGamesPlayed() + " game"
                        + (history.getTotalGamesPlayed() == 1L ? "" : "s")
                        + " played",
                    20,
                    INK,
                    true
                ),
                matchWrap()
            );
            summary.addView(
                label(
                    "Total play time  "
                        + formatDuration(history.getTotalActiveDurationMillis()),
                    15,
                    PRIMARY_DARK,
                    true
                ),
                margins(matchWrap(), 0, 8, 0, 0)
            );
            summary.addView(
                label(
                    history.getTotalPairsPlayed() + " pairs matched  •  average "
                        + formatDuration(history.getAverageActiveDurationMillis()),
                    13,
                    MUTED,
                    false
                ),
                margins(matchWrap(), 0, 4, 0, 0)
            );
            String retainedLeader = retainedHistoryLeader(history.getRetainedWinnerCounts());
            if (!retainedLeader.isEmpty()) {
                summary.addView(
                    label(retainedLeader, 13, SUCCESS, true),
                    margins(matchWrap(), 0, 7, 0, 0)
                );
            }
        }

        if (history.getTotalGamesPlayed() > 0L) {
            TextView reset = actionButton(
                "RESET HISTORY",
                SURFACE_TINT,
                Color.rgb(232, 74, 95)
            );
            reset.setOnClickListener(view -> confirmResetHistory());
            content.addView(
                reset,
                margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)), 0, 12, 0, 0)
            );

            content.addView(
                sectionTitle("RECENT GAMES"),
                margins(matchWrap(), 0, 22, 0, 0)
            );
            List<GameHistoryEntry> entries = history.getEntries();
            int shown = entries.size();
            DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
            );
            for (int index = 0; index < shown; index++) {
                GameHistoryEntry entry = entries.get(index);
                content.addView(
                    historyEntryCard(entry, dateFormat),
                    margins(matchWrap(), 0, index == 0 ? 8 : 10, 0, 0)
                );
            }
        }

        TextView done = actionButton("DONE", PRIMARY, contrastTextColor(PRIMARY));
        done.setOnClickListener(view -> showSetupScreen());
        content.addView(
            done,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)), 0, 18, 0, 0)
        );

        setContentView(scroll);
        enterImmersiveMode();
    }

    private View historyEntryCard(GameHistoryEntry entry, DateFormat dateFormat) {
        LinearLayout card = verticalLayout();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(SURFACE, 18));
        card.setElevation(dp(themedElevation(1)));

        TextView date = label(
            dateFormat.format(new Date(entry.getCompletedAtEpochMillis())),
            12,
            MUTED,
            true
        );
        card.addView(date, matchWrap());

        String winnerNames = TextUtils.join(" & ", entry.getWinnerNames());
        card.addView(
            label(
                entry.isTie() ? "Tie: " + winnerNames : winnerNames + " won",
                18,
                entry.isTie() ? ACCENT : SUCCESS,
                true
            ),
            margins(matchWrap(), 0, 6, 0, 0)
        );

        String players = entry.getPlayerCount() == 1
            && entry.getParticipantCount() == 2
            ? "Solo vs Bot"
            : entry.getPlayerCount() + " players";
        card.addView(
            label(
                players + "  •  " + entry.getPairCount() + " pairs  •  "
                    + formatDuration(entry.getActiveDurationMillis()),
                13,
                MUTED,
                false
            ),
            margins(matchWrap(), 0, 5, 0, 0)
        );
        TextView scores = label(entry.getCompactScoreSummary(), 13, INK, true);
        scores.setLineSpacing(0f, 1.08f);
        card.addView(scores, margins(matchWrap(), 0, 6, 0, 0));
        return card;
    }

    private String retainedHistoryLeader(Map<String, Integer> winnerCounts) {
        int mostWins = 0;
        ArrayList<String> leaders = new ArrayList<>();
        for (Map.Entry<String, Integer> winner : winnerCounts.entrySet()) {
            int wins = winner.getValue();
            if (wins > mostWins) {
                mostWins = wins;
                leaders.clear();
                leaders.add(winner.getKey());
            } else if (wins == mostWins && wins > 0) {
                leaders.add(winner.getKey());
            }
        }
        if (leaders.isEmpty()) {
            return "";
        }
        return "Recent leader" + (leaders.size() == 1 ? "" : "s") + "  "
            + TextUtils.join(" & ", leaders) + "  •  " + mostWins + " top finish"
            + (mostWins == 1 ? "" : "es");
    }

    private void confirmResetHistory() {
        int dialogTheme = darkTheme
            ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert;
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme)
            .setTitle("Reset game history?")
            .setMessage("Every saved game and all history totals will be removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (ignored, which) -> {
                historyStore.reset();
                showHistoryScreen();
            })
            .create();
        dialog.show();
        keepDialogImmersive(dialog);
    }

    private void renderCardBackSelection(
        EnumMap<CardBackStyle, CardBackOptionBinding> bindings
    ) {
        for (CardBackStyle style : CardBackStyle.values()) {
            CardBackOptionBinding binding = bindings.get(style);
            if (binding == null) {
                continue;
            }
            boolean selected = style == cardBackStyle;
            binding.option.setSelected(selected);
            binding.option.setBackground(outlined(
                selected ? SURFACE_TINT : SURFACE,
                selected ? PRIMARY : DIVIDER,
                selected ? 2 : 1,
                16
            ));
            binding.option.setContentDescription(
                style.getDisplayName()
                    + " card back"
                    + ", "
                    + (selected ? "selected" : "not selected")
            );
            binding.name.setText(
                (selected ? "✓ " : "")
                    + style.getDisplayName()
            );
            binding.name.setTextColor(selected ? PRIMARY_DARK : INK);
        }
    }

    private LinearLayout advancedSettingsCard() {
        LinearLayout card = verticalLayout();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(SURFACE, 20));
        card.setElevation(dp(themedElevation(2)));
        return card;
    }

    private LinearLayout advancedChoice(
        String title,
        String description,
        boolean selected
    ) {
        LinearLayout choice = verticalLayout();
        choice.setMinimumHeight(dp(64));
        choice.setPadding(dp(13), dp(11), dp(13), dp(11));
        choice.setFocusable(true);
        choice.setClickable(true);
        choice.setBackground(outlined(
            selected ? SURFACE_TINT : SURFACE,
            selected ? PRIMARY : DIVIDER,
            selected ? 2 : 1,
            15
        ));
        choice.addView(
            label((selected ? "✓ " : "") + title, 15, selected ? PRIMARY_DARK : INK, true),
            matchWrap()
        );
        choice.addView(
            label(description, 12, MUTED, false),
            margins(matchWrap(), 0, 4, 0, 0)
        );
        return choice;
    }

    private LinearLayout advancedToggle(
        String title,
        String description,
        boolean enabled,
        AdvancedToggleAction toggleAction
    ) {
        LinearLayout row = horizontalLayout();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(66));
        LinearLayout text = verticalLayout();
        row.addView(
            text,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 0, 0, 12, 0)
        );
        text.addView(label(title, 15, INK, true), matchWrap());
        TextView detail = label(description, 12, MUTED, false);
        detail.setLineSpacing(0f, 1.08f);
        text.addView(detail, margins(matchWrap(), 0, 3, 0, 0));

        TextView toggle = label("", 12, INK, true);
        configureToggleButton(toggle);
        renderToggleState(toggle, enabled, title);
        toggle.setOnClickListener(view -> toggleAction.run(toggle));
        row.addView(toggle, fixed(dp(64), dp(48)));
        return row;
    }

    private void saveAdvancedToggle(
        String key,
        boolean value,
        TextView toggle,
        String label
    ) {
        getSharedPreferences(SETTINGS, MODE_PRIVATE)
            .edit()
            .putBoolean(key, value)
            .apply();
        renderToggleState(toggle, value, label);
    }

    private void showPlayerSettingsScreen() {
        cancelPendingActions();
        screen = Screen.PROFILES;
        setWindowBackground(BACKGROUND);
        game = null;
        gameStats = null;
        computerMemory = null;
        cardViews = null;
        boardLayout = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        applyScreenBackground(scroll);
        applySafeCutoutInsets(scroll);

        LinearLayout content = verticalLayout();
        content.setPadding(dp(22), dp(22), dp(22), dp(32));
        scroll.addView(content, matchWrap());

        LinearLayout heading = horizontalLayout();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(heading, matchWrap());

        TextView back = label("‹", 32, INK, false);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back to game setup");
        back.setBackground(ripple(SURFACE_TINT, 14));
        back.setOnClickListener(view -> {
            saveProfileNamesFromInputs();
            showSetupScreen();
        });
        heading.addView(back, fixed(dp(48), dp(48)));

        LinearLayout titleBlock = verticalLayout();
        heading.addView(
            titleBlock,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 12, 0, 10, 0)
        );
        titleBlock.addView(label("Player settings", 27, INK, true), matchWrap());
        titleBlock.addView(
            label("Names and photos stay on this device", 13, MUTED, false),
            margins(matchWrap(), 0, 3, 0, 0)
        );

        TextView resetAll = label("RESET ALL", 11, PRIMARY_DARK, true);
        resetAll.setGravity(Gravity.CENTER);
        resetAll.setMinHeight(dp(48));
        resetAll.setPadding(dp(11), dp(8), dp(11), dp(8));
        resetAll.setBackground(ripple(SURFACE_TINT, 14));
        resetAll.setOnClickListener(view -> confirmResetAllProfiles());
        heading.addView(resetAll, wrapWrap());

        TextView explanation = label(
            "Use a short name and take an optional face photo. Empty names use Player A, Player B, and so on.",
            14,
            MUTED,
            false
        );
        explanation.setLineSpacing(0f, 1.12f);
        content.addView(explanation, margins(matchWrap(), 0, 18, 0, 0));

        profileNameInputs.clear();
        profileEditorIds.clear();
        List<PlayerProfile> profiles = profileStore.getProfiles();
        for (int index = 0; index < profiles.size(); index++) {
            PlayerProfile profile = profiles.get(index);
            int position = index + 1;

            LinearLayout card = verticalLayout();
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            card.setBackground(outlined(SURFACE, DIVIDER, 1, 20));
            content.addView(card, margins(matchWrap(), 0, index == 0 ? 8 : 10, 0, 0));

            LinearLayout identityRow = horizontalLayout();
            identityRow.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(identityRow, matchWrap());

            PlayerAvatarView avatar = new PlayerAvatarView(this);
            avatar.setProfile(profile, position, playerColor(index));
            avatar.setOnClickListener(view -> {
                saveProfileNamesFromInputs();
                pendingPhotoProfileId = profile.getId();
                launchFaceCamera();
            });
            identityRow.addView(avatar, fixed(dp(72), dp(72)));

            LinearLayout editor = verticalLayout();
            identityRow.addView(
                editor,
                margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 14, 0, 0, 0)
            );
            TextView playerLabel = sectionTitle("PLAYER " + defaultPlayerLetter(position));
            editor.addView(playerLabel, matchWrap());

            EditText nameInput = new EditText(this);
            nameInput.setSingleLine(true);
            nameInput.setText(profile.getCustomName());
            nameInput.setHint(profile.getDisplayName(position));
            nameInput.setTextSize(16);
            nameInput.setTextColor(INK);
            nameInput.setHintTextColor(MUTED);
            nameInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS
            );
            nameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(24)});
            nameInput.setSelectAllOnFocus(false);
            nameInput.setPadding(dp(12), dp(10), dp(12), dp(10));
            nameInput.setBackground(outlined(SURFACE_TINT, DIVIDER, 1, 13));
            editor.addView(nameInput, margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)), 0, 6, 0, 0));
            profileNameInputs.add(nameInput);
            profileEditorIds.add(profile.getId());

            LinearLayout actions = horizontalLayout();
            card.addView(actions, margins(matchWrap(), 0, 10, 0, 0));
            TextView camera = actionButton(
                profile.hasPhoto() ? "RETAKE PHOTO" : "TAKE PHOTO",
                PRIMARY,
                contrastTextColor(PRIMARY)
            );
            camera.setTextSize(12);
            camera.setOnClickListener(view -> {
                saveProfileNamesFromInputs();
                pendingPhotoProfileId = profile.getId();
                launchFaceCamera();
            });
            actions.addView(camera, weighted(0, dp(48), 1f));

            TextView remove = actionButton("REMOVE", SURFACE_TINT, Color.rgb(232, 74, 95));
            remove.setTextSize(10);
            remove.setContentDescription("Remove " + profile.getDisplayName(position));
            remove.setOnClickListener(view -> {
                saveProfileNamesFromInputs();
                requestRemovePlayer(profile.getId(), scroll.getScrollY());
            });
            setControlEnabled(
                remove,
                profileStore.size() > GameState.MIN_HUMAN_PLAYERS
            );
            actions.addView(remove, margins(fixed(dp(72), dp(48)), 8, 0, 0, 0));

            TextView reset = actionButton("RESET", SURFACE_TINT, INK);
            reset.setTextSize(10);
            reset.setOnClickListener(view -> {
                saveProfileNamesFromInputs();
                PlayerProfile current = profileStore.find(profile.getId());
                if (!deleteProfilePhoto(current)) {
                    Toast.makeText(
                        this,
                        "Could not remove the saved photo. Please try again.",
                        Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                profileStore.resetProfile(profile.getId());
                showPlayerSettingsScreen();
            });
            actions.addView(reset, margins(fixed(dp(66), dp(48)), 8, 0, 0, 0));
        }

        TextView addPlayer = actionButton(
            "ADD PLAYER",
            SURFACE_TINT,
            darkTheme ? INK : PRIMARY_DARK
        );
        addPlayer.setOnClickListener(view -> {
            saveProfileNamesFromInputs();
            int oldPlayers = profileStore.size();
            if (oldPlayers < GameState.MAX_HUMAN_PLAYERS) {
                profileStore.addProfile();
                pendingProfileScrollY = Integer.MAX_VALUE;
                applyRosterCountChange(oldPlayers);
            }
        });
        setControlEnabled(
            addPlayer,
            profileStore.size() < GameState.MAX_HUMAN_PLAYERS
        );
        content.addView(
            addPlayer,
            margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)), 0, 18, 0, 0)
        );

        TextView done = actionButton("DONE", PRIMARY, contrastTextColor(PRIMARY));
        done.setOnClickListener(view -> {
            saveProfileNamesFromInputs();
            showSetupScreen();
        });
        content.addView(done, margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)), 0, 10, 0, 0));

        setContentView(scroll);
        enterImmersiveMode();
        int scrollTarget = pendingProfileScrollY;
        pendingProfileScrollY = -1;
        if (scrollTarget >= 0) {
            scroll.post(() -> {
                if (scrollTarget == Integer.MAX_VALUE) {
                    scroll.fullScroll(View.FOCUS_DOWN);
                } else {
                    scroll.scrollTo(0, scrollTarget);
                }
            });
        }
    }

    private void saveProfileNamesFromInputs() {
        int count = Math.min(profileNameInputs.size(), profileEditorIds.size());
        for (int index = 0; index < count; index++) {
            profileStore.setCustomName(
                profileEditorIds.get(index),
                profileNameInputs.get(index).getText().toString().trim()
            );
        }
    }

    private void requestRemovePlayer(long profileId, int scrollY) {
        if (profileStore.size() <= GameState.MIN_HUMAN_PLAYERS) {
            return;
        }
        PlayerProfile current = profileStore.find(profileId);
        if (current == null) {
            return;
        }
        if (!current.isCustomized()) {
            int oldPlayers = removeSinglePlayerData(profileId);
            if (oldPlayers >= 0) {
                pendingProfileScrollY = scrollY;
                applyRosterCountChange(oldPlayers);
            }
            return;
        }

        int position = profilePosition(profileId);
        String displayName = current.getDisplayName(position);
        int dialogTheme = darkTheme
            ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert;
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme)
            .setTitle("Remove " + displayName + "?")
            .setMessage("Any saved name or face photo for this player will also be deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove", null)
            .create();
        dialog.setOnShowListener(ignored -> {
            TextView removeButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            removeButton.setOnClickListener(view -> {
                removeButton.setEnabled(false);
                int oldPlayers = removeSinglePlayerData(profileId);
                if (oldPlayers < 0) {
                    removeButton.setEnabled(true);
                    return;
                }
                dialog.dismiss();
                pendingProfileScrollY = scrollY;
                applyRosterCountChange(oldPlayers);
            });
        });
        dialog.show();
        keepDialogImmersive(dialog);
    }

    /** Returns the previous roster size on success, or -1 without changing the roster. */
    private int removeSinglePlayerData(long profileId) {
        int oldPlayers = profileStore.size();
        if (oldPlayers <= GameState.MIN_HUMAN_PLAYERS) {
            return -1;
        }
        PlayerProfile current = profileStore.find(profileId);
        if (current == null) {
            return -1;
        }
        boolean hadPhoto = current.hasPhoto();
        if (!deleteProfilePhoto(current)) {
            Toast.makeText(
                this,
                "Could not remove that player's photo. Please try again.",
                Toast.LENGTH_LONG
            ).show();
            return -1;
        }
        if (!profileStore.removeProfile(profileId)) {
            if (hadPhoto) {
                // The photo is already gone; avoid retaining a stale internal-file reference.
                profileStore.setPhotoPath(profileId, "");
            }
            Toast.makeText(this, "Could not remove that player", Toast.LENGTH_LONG).show();
            return -1;
        }
        if (pendingPhotoProfileId == profileId) {
            pendingPhotoProfileId = -1L;
        }
        return oldPlayers;
    }

    private int profilePosition(long profileId) {
        List<PlayerProfile> profiles = profileStore.getProfiles();
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).getId() == profileId) {
                return index + 1;
            }
        }
        return 1;
    }

    private void launchFaceCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(cameraIntent, REQUEST_FACE_PHOTO);
        } catch (ActivityNotFoundException | SecurityException exception) {
            pendingPhotoProfileId = -1L;
            Toast.makeText(this, "No camera app is available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FACE_PHOTO) {
            return;
        }
        long profileId = pendingPhotoProfileId;
        pendingPhotoProfileId = -1L;
        if (resultCode != RESULT_OK || profileId < 0L || data == null || data.getExtras() == null) {
            if (screen == Screen.PROFILES) {
                showPlayerSettingsScreen();
            }
            return;
        }
        Object captured = data.getExtras().get("data");
        if (!(captured instanceof Bitmap)) {
            Toast.makeText(this, "The camera did not return a photo", Toast.LENGTH_LONG).show();
            showPlayerSettingsScreen();
            return;
        }
        String path = saveProfilePhoto(profileId, (Bitmap) captured);
        if (path != null) {
            profileStore.setPhotoPath(profileId, path);
        } else {
            Toast.makeText(this, "Could not save that photo", Toast.LENGTH_LONG).show();
        }
        showPlayerSettingsScreen();
    }

    private String saveProfilePhoto(long profileId, Bitmap bitmap) {
        File directory = new File(getFilesDir(), "player_avatars");
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        File output = new File(directory, "player_" + profileId + ".jpg");
        File temporary = new File(directory, "player_" + profileId + ".tmp");
        File backup = new File(directory, "player_" + profileId + ".bak");
        if (temporary.exists() && !temporary.delete()) {
            return null;
        }
        try (FileOutputStream stream = new FileOutputStream(temporary)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
                temporary.delete();
                return null;
            }
            stream.flush();
            stream.getFD().sync();
        } catch (IOException | RuntimeException ignored) {
            temporary.delete();
            return null;
        }

        boolean hadOldPhoto = output.isFile();
        if (hadOldPhoto) {
            if ((backup.exists() && !backup.delete()) || !output.renameTo(backup)) {
                temporary.delete();
                return null;
            }
        }
        if (!temporary.renameTo(output)) {
            if (hadOldPhoto) {
                backup.renameTo(output);
            }
            temporary.delete();
            return null;
        }
        if (hadOldPhoto) {
            backup.delete();
        }
        return output.getAbsolutePath();
    }

    private boolean deleteProfilePhoto(PlayerProfile profile) {
        if (profile == null || !profile.hasPhoto()) {
            return true;
        }
        File directory = new File(getFilesDir(), "player_avatars");
        File photo = new File(profile.getPhotoPath());
        if (!directory.equals(photo.getParentFile())) {
            return false;
        }
        return !photo.exists() || photo.delete();
    }

    private void confirmResetAllProfiles() {
        int dialogTheme = darkTheme
            ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert;
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme)
            .setTitle("Reset every player?")
            .setMessage("All custom names and saved face photos will be removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset all", (ignored, which) -> {
                for (PlayerProfile profile : profileStore.getProfiles()) {
                    if (!deleteProfilePhoto(profile)) {
                        Toast.makeText(
                            this,
                            "A saved photo could not be removed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show();
                        return;
                    }
                }
                profileStore.resetAllProfiles();
                showPlayerSettingsScreen();
            })
            .create();
        dialog.show();
        keepDialogImmersive(dialog);
    }

    private void changePlayers(int delta) {
        if (delta == 0) {
            return;
        }
        int oldPlayers = profileStore.size();
        if (delta > 0) {
            if (oldPlayers >= GameState.MAX_HUMAN_PLAYERS) {
                return;
            }
            profileStore.addProfile();
            applyRosterCountChange(oldPlayers);
            return;
        }
        if (oldPlayers <= GameState.MIN_HUMAN_PLAYERS) {
            return;
        }
        List<PlayerProfile> profiles = profileStore.getProfiles();
        for (int index = profiles.size() - 1; index >= 0; index--) {
            PlayerProfile profile = profiles.get(index);
            if (!profile.isCustomized()) {
                if (profileStore.removeProfile(profile.getId())) {
                    applyRosterCountChange(oldPlayers);
                }
                return;
            }
        }
        showPlayerRemovalDialog();
    }

    private void applyRosterCountChange(int oldPlayers) {
        int updated = profileStore.size();
        boolean followingRecommendation = !pairCountCustomized;
        selectedHumanPlayers = updated;
        if (followingRecommendation) {
            selectedPairs = GameState.recommendedPairsForPlayers(selectedHumanPlayers);
            pairCountCustomized = false;
        }
        if (screen == Screen.PROFILES) {
            showPlayerSettingsScreen();
        } else {
            renderSetupControls();
        }
    }

    private void showPlayerRemovalDialog() {
        List<PlayerProfile> profiles = profileStore.getProfiles();
        String[] names = new String[profiles.size()];
        for (int index = 0; index < profiles.size(); index++) {
            PlayerProfile profile = profiles.get(index);
            names[index] = profile.getDisplayName(index + 1)
                + (profile.isCustomized() ? "  •  customized" : "");
        }
        boolean[] selected = new boolean[profiles.size()];
        int[] selectedCount = {0};
        int dialogTheme = darkTheme
            ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert;
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme)
            .setTitle("Select players to remove")
            .setMultiChoiceItems(names, selected, (ignored, which, isChecked) -> {
                AlertDialog shown = (AlertDialog) ignored;
                if (isChecked && selectedCount[0] >= profiles.size() - 1) {
                    selected[which] = false;
                    shown.getListView().setItemChecked(which, false);
                    Toast.makeText(this, "Keep at least one player", Toast.LENGTH_SHORT).show();
                    return;
                }
                selected[which] = isChecked;
                selectedCount[0] += isChecked ? 1 : -1;
                shown.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selectedCount[0] > 0);
            })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove selected", null)
            .create();
        dialog.setOnShowListener(ignored -> {
            TextView removeButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            removeButton.setEnabled(false);
            removeButton.setOnClickListener(view -> {
                if (selectedCount[0] <= 0) {
                    return;
                }
                int oldPlayers = profileStore.size();
                int removedCount = 0;
                for (int index = 0; index < profiles.size(); index++) {
                    if (!selected[index]) {
                        continue;
                    }
                    PlayerProfile removal = profiles.get(index);
                    if (!deleteProfilePhoto(removal)) {
                        Toast.makeText(
                            this,
                            removedCount == 0
                                ? "A selected player's photo could not be removed. Please try again."
                                : removedCount + " player(s) removed; one photo could not be removed.",
                            Toast.LENGTH_LONG
                        ).show();
                        dialog.dismiss();
                        if (removedCount > 0) {
                            applyRosterCountChange(oldPlayers);
                        }
                        return;
                    }
                    if (profileStore.removeProfile(removal.getId())) {
                        removedCount++;
                    }
                }
                dialog.dismiss();
                if (removedCount > 0) {
                    applyRosterCountChange(oldPlayers);
                }
            });
        });
        dialog.show();
        keepDialogImmersive(dialog);
    }

    private void changePairs(int delta) {
        int updated = clamp(selectedPairs + delta, GameState.MIN_PAIRS, GameState.MAX_PAIRS);
        if (updated == selectedPairs) {
            return;
        }
        selectedPairs = updated;
        pairCountCustomized = selectedPairs != GameState.recommendedPairsForPlayers(selectedHumanPlayers);
        renderSetupControls();
    }

    private void useRecommendedPairCount() {
        stopPairRepeat();
        selectedPairs = GameState.recommendedPairsForPlayers(profileStore.size());
        pairCountCustomized = false;
        renderSetupControls();
    }

    private void configurePairRepeat(TextView button, int delta) {
        button.setOnLongClickListener(view -> {
            boolean heldByTouch = pairTouchButton == button;
            stopPairRepeat();
            if (!heldByTouch) {
                // Keyboard and accessibility long-clicks have no matching touch-up event.
                changePairs(delta);
                return true;
            }
            pairTouchButton = button;
            repeatingPairButton = button;
            pairRepeatRunnable = new Runnable() {
                @Override
                public void run() {
                    if (repeatingPairButton != button
                        || screen != Screen.SETUP
                        || !button.isEnabled()) {
                        stopPairRepeat();
                        return;
                    }
                    changePairs(delta);
                    if (button.isEnabled()) {
                        handler.postDelayed(this, 110L);
                    } else {
                        stopPairRepeat();
                    }
                }
            };
            handler.post(pairRepeatRunnable);
            return true;
        });
        button.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                stopPairRepeat();
                pairTouchButton = button;
            } else if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL) {
                stopPairRepeat();
            }
            return false;
        });
    }

    private void stopPairRepeat() {
        if (pairRepeatRunnable != null) {
            handler.removeCallbacks(pairRepeatRunnable);
        }
        pairRepeatRunnable = null;
        repeatingPairButton = null;
        pairTouchButton = null;
    }

    private void renderSetupControls() {
        if (setupPlayerValue == null) {
            return;
        }
        selectedHumanPlayers = profileStore.size();
        setupPlayerValue.setText(String.valueOf(selectedHumanPlayers));
        setupPairValue.setText(String.valueOf(selectedPairs));
        setupPairValue.setContentDescription(selectedPairs + " matching pairs");
        int recommended = GameState.recommendedPairsForPlayers(selectedHumanPlayers);
        boolean recommendationActive = !pairCountCustomized && selectedPairs == recommended;
        if (recommendedPairsButton != null) {
            recommendedPairsButton.setText("DEFAULT");
            recommendedPairsButton.setContentDescription(
                recommendationActive
                    ? "Default pair count selected: " + recommended
                    : "Use default pair count: " + recommended
            );
            setControlEnabled(recommendedPairsButton, !recommendationActive);
        }
        setControlEnabled(playerMinus, selectedHumanPlayers > GameState.MIN_HUMAN_PLAYERS);
        setControlEnabled(playerPlus, selectedHumanPlayers < GameState.MAX_HUMAN_PLAYERS);
        setControlEnabled(pairMinus, selectedPairs > GameState.MIN_PAIRS);
        setControlEnabled(pairPlus, selectedPairs < GameState.MAX_PAIRS);
        if (difficultySection != null) {
            difficultySection.setVisibility(
                selectedHumanPlayers == 1 ? View.VISIBLE : View.GONE
            );
        }
        renderDifficultySelector();
        renderIconCategorySelector();
        renderTrickyControl();
        renderMovementControl();
        renderColorControl();
    }

    private void renderDifficultySelector() {
        if (difficultyRow == null || difficultyHint == null) {
            return;
        }
        difficultyRow.removeAllViews();
        ComputerDifficulty[] difficulties = ComputerDifficulty.values();
        LinearLayout row = null;
        for (int index = 0; index < difficulties.length; index++) {
            ComputerDifficulty difficulty = difficulties[index];
            if (index % 2 == 0) {
                row = horizontalLayout();
                difficultyRow.addView(
                    row,
                    index == 0 ? matchWrap() : margins(matchWrap(), 0, 7, 0, 0)
                );
            }
            boolean selected = difficulty == selectedDifficulty;
            TextView chip = selectorChip(difficulty.getDisplayName(), selected);
            chip.setContentDescription(
                difficulty.getDisplayName() + " bot level" + (selected ? ", selected" : "")
            );
            chip.setOnClickListener(view -> {
                selectedDifficulty = difficulty;
                renderDifficultySelector();
            });
            row.addView(
                chip,
                margins(weighted(0, dp(48), 1f), index % 2 == 0 ? 0 : 4, 0, index % 2 == 0 ? 4 : 0, 0)
            );
        }
        switch (selectedDifficulty) {
            case BASIC:
                difficultyHint.setText("Keeps one earlier card in mind and often slips up");
                break;
            case EASY:
                difficultyHint.setText("Remembers 4 recent cards and sometimes slips up");
                break;
            case MEDIUM:
                difficultyHint.setText("Remembers 12 recent cards and rarely slips up");
                break;
            default:
                difficultyHint.setText("Remembers every seen card but can make a rare mistake");
                break;
        }
    }

    private void renderIconCategorySelector() {
        if (iconCategoryFlow == null || iconCategoryHint == null) {
            return;
        }
        iconCategoryFlow.removeAllViews();
        for (IconCatalog.Category category : CATEGORY_ORDER) {
            boolean selected = category == selectedIconCategory;
            boolean sampleSupported = CardTileView.allGlyphsSupported(
                new String[] {category.getSample()}
            );
            TextView chip = selectorChip(
                (sampleSupported ? category.getSample() + "  " : "")
                    + category.getDisplayName(),
                selected
            );
            chip.setMaxLines(1);
            chip.setContentDescription(
                category.getDisplayName() + " card collection" + (selected ? ", selected" : "")
            );
            chip.setOnClickListener(view -> {
                selectedIconCategory = category;
                if (category == IconCatalog.Category.BLANK) {
                    useCardColors = true;
                    getSharedPreferences(SETTINGS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(USE_CARD_COLORS, true)
                        .apply();
                }
                renderSetupControls();
            });
            iconCategoryFlow.addView(chip);
        }
        if (selectedIconCategory == IconCatalog.Category.RANDOM) {
            iconCategoryHint.setText(
                "A fresh mix from emoji collections — custom modes excluded"
            );
        } else if (selectedIconCategory == IconCatalog.Category.BLANK) {
            iconCategoryHint.setText(
                "Hard: match by color alone — turning Pair colors off returns to Random"
            );
        } else if (selectedIconCategory == IconCatalog.Category.WORDS) {
            iconCategoryHint.setText("Short words with no more than 5 letters");
        } else if (selectedIconCategory == IconCatalog.Category.RUBICS) {
            iconCategoryHint.setText(
                "Hard: match distinct 3 × 3 scrambled cube faces"
            );
        } else if (selectedIconCategory == IconCatalog.Category.NUMBERS) {
            iconCategoryHint.setText("Numbers with no more than 3 digits");
        } else {
            boolean sampleSupported = CardTileView.allGlyphsSupported(
                new String[] {selectedIconCategory.getSample()}
            );
            iconCategoryHint.setText(
                (sampleSupported ? selectedIconCategory.getSample() + "  " : "")
                    + selectedIconCategory.getDisplayName() + " collection"
            );
        }
    }

    private void renderTrickyControl() {
        if (trickyToggle == null || trickyHint == null) {
            return;
        }
        trickyToggle.setText(trickyMode ? "ON" : "OFF");
        trickyToggle.setSelected(trickyMode);
        trickyToggle.setTextColor(trickyMode ? contrastTextColor(PRIMARY) : INK);
        trickyToggle.setBackground(ripple(trickyMode ? PRIMARY : SURFACE_TINT, 16));
        trickyToggle.setContentDescription(
            "Tricky mode " + (trickyMode ? "on" : "off")
        );
        if (selectedIconCategory == IconCatalog.Category.BLANK) {
            trickyHint.setText(
                trickyMode
                    ? "Uses deliberately similar—but still different—pair colors"
                    : "Turn on for closer, more confusing pair colors"
            );
        } else if (selectedIconCategory == IconCatalog.Category.RUBICS) {
            trickyHint.setText(
                trickyMode
                    ? "Extra hard: cube faces differ by only one sticker"
                    : "Turn on for near-identical cube-face scrambles"
            );
        } else if (selectedIconCategory == IconCatalog.Category.NUMBERS) {
            trickyHint.setText(
                trickyMode
                    ? "Uses easily confused numbers such as 487 and 478"
                    : "Turn on for numbers with nearly identical digits"
            );
        } else {
            trickyHint.setText(
                trickyMode
                    ? "Uses deliberately similar symbols or easily confused words"
                    : "Turn on for near-lookalike cards"
            );
        }
    }

    private void renderMovementControl() {
        if (swapToggle == null || swapHint == null) {
            return;
        }
        renderToggleState(swapToggle, swapAfterMiss, "Moving cards");
        swapHint.setText(
            swapAfterMiss
                ? "After a miss, revealed cards move to new random places"
                : "Keep every card in its original position"
        );
    }

    private void renderColorControl() {
        if (colorToggle == null || colorHint == null) {
            return;
        }
        renderToggleState(colorToggle, useCardColors, "Pair colors");
        colorHint.setText(
            colorBlindPalette
                ? "Accessible colors and pair-specific patterns are active"
                : selectedIconCategory == IconCatalog.Category.BLANK
                ? "Required in Blank mode — match cards by color alone"
                : useCardColors
                ? (trickyMode
                    ? "Similar—but still different—colors are used for tricky pairs"
                    : "Each pair uses a strongly distinct color")
                : "Cards use symbols, numbers, cube faces, or words without a pair-color hint"
        );
    }

    private void configureToggleButton(TextView toggle) {
        toggle.setGravity(Gravity.CENTER);
        toggle.setMinWidth(dp(60));
        toggle.setMinHeight(dp(48));
        toggle.setFocusable(true);
    }

    private void renderToggleState(TextView toggle, boolean enabled, String label) {
        toggle.setText(enabled ? "ON" : "OFF");
        toggle.setSelected(enabled);
        toggle.setTextColor(enabled ? contrastTextColor(PRIMARY) : INK);
        toggle.setBackground(ripple(enabled ? PRIMARY : SURFACE_TINT, 16));
        toggle.setContentDescription(label + " " + (enabled ? "on" : "off"));
    }

    private TextView selectorChip(String text, boolean selected) {
        TextView chip = label(
            text,
            14,
            selected ? contrastTextColor(PRIMARY) : INK,
            true
        );
        chip.setGravity(Gravity.CENTER);
        chip.setMinHeight(dp(48));
        chip.setPadding(dp(14), dp(9), dp(14), dp(9));
        chip.setFocusable(true);
        chip.setSelected(selected);
        chip.setBackground(ripple(selected ? PRIMARY : SURFACE_TINT, 16));
        return chip;
    }

    private void setControlEnabled(TextView control, boolean enabled) {
        control.setEnabled(enabled);
        control.setAlpha(enabled ? 1f : 0.32f);
    }

    private void startNewGame() {
        startGameRound(false);
    }

    private void startRematch() {
        startGameRound(true);
    }

    private void startGameRound(boolean keepSeries) {
        game = new GameState(selectedHumanPlayers, selectedPairs);
        String[] participantNames = currentParticipantNames();
        if (!keepSeries
            || gameSeries == null
            || !Arrays.equals(gameSeries.copyParticipantNames(), participantNames)) {
            gameSeries = new GameSeries(participantNames);
        }
        gameStats = new GameStats(game.getTotalPlayerCount());
        computerMemory = new ComputerMemory(selectedDifficulty);
        gameTimer = new GameTimer(SystemClock.elapsedRealtime());
        lastGameDurationMillis = 0L;
        completedGameRecorded = false;
        resetPairDecisionClock();
        restoredBoardSlots = null;
        animateBoardEntrance = true;
        turnHandoffRequired = turnHandoffEnabled && !game.isComputerTurn();
        showGameScreen();
    }

    private void showGameScreen() {
        cancelPendingActions();
        screen = Screen.GAME;
        setWindowBackground(playerColor(game.getCurrentPlayer()));
        turnHandoffOverlay = null;
        renderedHeaderPlayer = -1;
        renderedScorePlayer = -1;
        renderedScoreFinished = false;
        renderedScores = null;
        scoreChipViews = null;
        playerChromeApplied = false;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameHost = new FrameLayout(this);
        gameHost.setBackground(gameSurroundBackground(playerColor(game.getCurrentPlayer())));

        gameRoot = verticalLayout();
        gameRoot.setBackground(gameSurroundBackground(playerColor(game.getCurrentPlayer())));
        applySafeCutoutInsets(gameRoot);
        gameHost.addView(
            gameRoot,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );

        gameHeader = verticalLayout();
        gameHeader.setPadding(dp(12), dp(8), dp(12), dp(8));
        gameRoot.addView(gameHeader, matchWrap());

        LinearLayout turnRow = horizontalLayout();
        turnRow.setGravity(Gravity.CENTER_VERTICAL);
        gameHeader.addView(turnRow, matchWrap());

        turnAvatar = new PlayerAvatarView(this);
        turnRow.addView(turnAvatar, fixed(dp(40), dp(40)));

        LinearLayout turnText = verticalLayout();
        turnRow.addView(
            turnText,
            margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 10, 0, 8, 0)
        );
        turnName = label("", 18, Color.WHITE, true);
        turnName.setSingleLine(true);
        turnName.setEllipsize(TextUtils.TruncateAt.END);
        turnText.addView(turnName, margins(matchWrap(), 0, 1, 0, 0));
        turnMessage = label("", 11, Color.WHITE, false);
        turnMessage.setSingleLine(true);
        turnMessage.setEllipsize(TextUtils.TruncateAt.END);
        turnText.addView(turnMessage, margins(matchWrap(), 0, 1, 0, 0));

        progressText = label("", 11, Color.WHITE, true);
        progressText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        progressText.setMinWidth(dp(76));
        turnRow.addView(progressText, margins(wrapWrap(), 0, 0, 8, 0));

        leaveGameButton = label("×", 23, Color.WHITE, false);
        leaveGameButton.setGravity(Gravity.CENTER);
        leaveGameButton.setContentDescription("Leave game");
        leaveGameButton.setOnClickListener(view -> confirmLeaveGame());
        turnRow.addView(leaveGameButton, fixed(dp(38), dp(38)));

        scoreScroll = new HorizontalScrollView(this);
        scoreScroll.setHorizontalScrollBarEnabled(false);
        scoreScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        gameHeader.addView(scoreScroll, margins(matchWrap(), 0, 7, 0, 0));
        scoreRow = horizontalLayout();
        scoreScroll.addView(scoreRow, wrapWrap());

        gameBoardSection = verticalLayout();
        gameBoardSection.setPadding(dp(8), dp(6), dp(8), dp(8));
        gameBoardSection.setBackground(
            gameSurroundBackground(playerColor(game.getCurrentPlayer()))
        );
        gameRoot.addView(
            gameBoardSection,
            weighted(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        );

        String[] icons = IconCatalog.iconsFor(
            selectedIconCategory,
            game.getPairCount(),
            boardLayoutSeed(),
            trickyMode
        );
        boolean wordMode = selectedIconCategory == IconCatalog.Category.WORDS;
        boolean blankMode = selectedIconCategory == IconCatalog.Category.BLANK;
        boolean rubicsMode = selectedIconCategory == IconCatalog.Category.RUBICS;
        boolean numberMode = selectedIconCategory == IconCatalog.Category.NUMBERS;
        boolean useShapeFallback = !blankMode
            && !wordMode
            && !rubicsMode
            && !numberMode
            && !CardTileView.allGlyphsSupported(icons);
        boolean colorsEnabled = useCardColors || blankMode;

        boardLayout = new BoardLayout(this);
        boardLayout.setLayoutSeed(boardLayoutSeed());
        boardLayout.setLargerCards(largerCards);
        boardLayout.setPadding(dp(7), dp(7), dp(7), dp(7));
        int activePlayerColor = playerColor(game.getCurrentPlayer());
        boardLayout.setBackground(outlined(
            gameTabletopFill(activePlayerColor),
            gameTabletopBorder(activePlayerColor),
            1,
            20
        ));
        boardLayout.setElevation(dp(themedElevation(2)));
        gameBoardSection.addView(
            boardLayout,
            weighted(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        );

        int[] pairColors = colorsEnabled
            ? CardColorCatalog.colorsFor(
                game.getPairCount(),
                trickyMode,
                boardLayoutSeed(),
                colorBlindPalette
            )
            : null;
        cardViews = new CardTileView[game.getCardCount()];
        for (int index = 0; index < game.getCardCount(); index++) {
            final int cardIndex = index;
            CardTileView card = new CardTileView(this);
            card.setCornerRadiusFraction(cardCornerRadiusFraction());
            card.setCardNumber(index + 1);
            card.setCardBackStyle(cardBackStyle);
            card.setReducedMotion(reducedMotion);
            card.setHighContrast(highContrast);
            card.setColorBlindPatterns(colorBlindPalette);
            card.setContent(
                icons[game.getPairId(index)],
                wordMode,
                useShapeFallback,
                trickyMode,
                game.getPairCount()
            );
            card.setPairColor(
                pairColors == null ? 0 : pairColors[game.getPairId(index)],
                pairColors != null
            );
            if (game.getCardState(index) == GameState.CardState.MATCHED) {
                card.removeFromTable(game.getPairId(index), false);
            } else {
                card.showState(game.getPairId(index), game.getCardState(index), false);
            }
            card.setOnClickListener(view -> onHumanCardTapped(cardIndex));
            cardViews[index] = card;
            boardLayout.addView(card);
        }

        if (restoredBoardSlots != null) {
            boardLayout.setSlotPermutation(restoredBoardSlots);
        }

        setContentView(gameHost);
        enterImmersiveMode();
        boolean shouldAnimateEntrance = animateBoardEntrance;
        animateBoardEntrance = false;
        setInputLocked(shouldAnimateEntrance
            || game.getPhase() == GameState.Phase.RESOLVING
            || game.isComputerTurn()
            || (turnHandoffEnabled && turnHandoffRequired));
        updateGameHeader(null);
        if (shouldAnimateEntrance) {
            boardLayout.animateSpread(reducedMotion ? 0L : 1000L, () -> {
                if (screen == Screen.GAME && !paused && !leaveDialogVisible) {
                    resumePendingGameAction();
                }
            });
        } else {
            gameRoot.post(this::resumePendingGameAction);
        }
    }

    private void onHumanCardTapped(int cardIndex) {
        if (screen != Screen.GAME
            || game == null
            || inputLocked
            || game.isComputerTurn()
            || paused) {
            return;
        }
        if (flipAndShowCard(cardIndex) && game.getPhase() == GameState.Phase.RESOLVING) {
            setInputLocked(true);
            updateGameHeader("Checking the pair…");
            postGameAction(
                currentPairRevealDelayMillis(REVEAL_DELAY_MS),
                this::resolveCurrentTurn
            );
        }
    }

    private long currentPairRevealDelayMillis(long matchingPairDelayMillis) {
        if (game == null || game.getPhase() != GameState.Phase.RESOLVING) {
            return matchingPairDelayMillis;
        }
        int first = game.getFirstCard();
        int second = game.getSecondCard();
        if (first < 0
            || second < 0
            || first >= game.getCardCount()
            || second >= game.getCardCount()
            || game.getPairId(first) == game.getPairId(second)) {
            return matchingPairDelayMillis;
        }
        return MissRevealDuration.millisForTenths(missRevealTenths);
    }

    private boolean flipAndShowCard(int cardIndex) {
        GameState.Phase phaseBeforeReveal = game.getPhase();
        long revealTime = SystemClock.elapsedRealtime();
        if (!game.flipCard(cardIndex)) {
            return false;
        }
        if (phaseBeforeReveal == GameState.Phase.WAITING_FIRST) {
            startPairDecisionClock(revealTime);
        } else if (phaseBeforeReveal == GameState.Phase.WAITING_SECOND) {
            completePairDecisionClock(revealTime);
        }
        int pairId = game.getPairId(cardIndex);
        computerMemory.remember(cardIndex, pairId);
        cardViews[cardIndex].showState(
            pairId,
            game.getCardState(cardIndex),
            !reducedMotion
        );
        gameFeedback.onCardFlip(cardViews[cardIndex]);
        refreshCardInteractivity();
        updateGameHeader(null);
        return true;
    }

    private void resolveCurrentTurn() {
        if (game == null || game.getPhase() != GameState.Phase.RESOLVING) {
            return;
        }
        GameState.Resolution resolution = game.resolveTurn();
        computerMemory.forgetMatchedCards(game);
        if (gameStats != null) {
            if (pendingPairDecisionDurationMillis == GameStats.NO_DECISION_TIME) {
                gameStats.recordResolution(
                    resolution.getScoringPlayer(),
                    resolution.isMatch(),
                    game.copyScores()
                );
            } else {
                gameStats.recordResolution(
                    resolution.getScoringPlayer(),
                    resolution.isMatch(),
                    game.copyScores(),
                    pendingPairDecisionDurationMillis
                );
            }
        }
        pendingPairDecisionDurationMillis = GameStats.NO_DECISION_TIME;
        int first = resolution.getFirstCard();
        int second = resolution.getSecondCard();

        if (resolution.isMatch()) {
            gameFeedback.onMatch(cardViews[first]);
            setInputLocked(true);
            Runnable completion = () -> completeSuccessfulMatch(resolution);
            if (!animateMatchedCardsToScore(
                first,
                second,
                resolution.getScoringPlayer(),
                completion
            )) {
                hideMatchedCards(first, second);
                completion.run();
            }
        } else {
            gameFeedback.onMiss(cardViews[first]);
            turnHandoffRequired = turnHandoffEnabled && !game.isComputerTurn();
            cardViews[first].showState(
                game.getPairId(first),
                game.getCardState(first),
                !reducedMotion
            );
            cardViews[second].showState(
                game.getPairId(second),
                game.getCardState(second),
                !reducedMotion
            );
            updateGameHeader("No match — " + playerName(game.getCurrentPlayer()) + " is next");
            setInputLocked(true);
            Runnable continueAfterMiss = () -> {
                updateGameHeader(null);
                resumePendingGameAction();
            };
            if (swapAfterMiss && boardLayout != null) {
                long relocationSeed = System.nanoTime()
                    ^ boardLayoutSeed()
                    ^ ((long) first << 32)
                    ^ second;
                boolean moving = boardLayout.relocateCardSlots(
                    first,
                    second,
                    relocationSeed,
                    reducedMotion ? 0L : 650L,
                    () -> {
                        if (screen == Screen.GAME && !paused && !leaveDialogVisible) {
                            boardLayout.announceForAccessibility(
                                "The revealed cards moved to new places"
                            );
                            continueAfterMiss.run();
                        }
                    }
                );
                if (!moving) {
                    postGameAction(reducedMotion ? 0L : 420L, continueAfterMiss);
                }
            } else {
                postGameAction(reducedMotion ? 0L : 420L, continueAfterMiss);
            }
        }
    }

    private boolean animateMatchedCardsToScore(
        int first,
        int second,
        int scoringPlayer,
        Runnable endAction
    ) {
        cancelMatchFlight();
        if (reducedMotion
            || gameRoot == null
            || cardViews == null
            || scoreChipViews == null
            || first < 0
            || second < 0
            || first >= cardViews.length
            || second >= cardViews.length
            || scoringPlayer < 0
            || scoringPlayer >= scoreChipViews.length) {
            return false;
        }

        CardTileView firstCard = cardViews[first];
        CardTileView secondCard = cardViews[second];
        TextView targetChip = scoreChipViews[scoringPlayer];
        if (firstCard == null
            || secondCard == null
            || targetChip == null
            || firstCard.getWidth() <= 0
            || firstCard.getHeight() <= 0
            || secondCard.getWidth() <= 0
            || secondCard.getHeight() <= 0
            || targetChip.getWidth() <= 0
            || targetChip.getHeight() <= 0
            || targetChip.getParent() != scoreRow) {
            return false;
        }

        ensureScoreChipVisible(targetChip, true);
        Rect targetRect = descendantRect(gameRoot, targetChip);
        if (targetRect.isEmpty()) {
            return false;
        }

        long generation = ++matchFlightGeneration;
        matchFlightHost = gameRoot;
        List<Animator> animators = new ArrayList<>(12);
        int[] cardIndices = {first, second};
        try {
            for (int index = 0; index < cardIndices.length; index++) {
                int cardIndex = cardIndices[index];
                CardTileView card = cardViews[cardIndex];
                int pairId = game.getPairId(cardIndex);
                card.showState(pairId, GameState.CardState.MATCHED, false);
                Rect sourceRect = descendantRect(gameRoot, card);
                if (sourceRect.isEmpty()) {
                    clearMatchFlightVisuals();
                    return false;
                }

                Bitmap bitmap = Bitmap.createBitmap(
                    card.getWidth(),
                    card.getHeight(),
                    Bitmap.Config.ARGB_8888
                );
                card.draw(new Canvas(bitmap));
                ImageView ghost = new ImageView(this);
                ghost.setImageBitmap(bitmap);
                ghost.setScaleType(ImageView.ScaleType.FIT_XY);
                int widthSpec = View.MeasureSpec.makeMeasureSpec(
                    card.getWidth(),
                    View.MeasureSpec.EXACTLY
                );
                int heightSpec = View.MeasureSpec.makeMeasureSpec(
                    card.getHeight(),
                    View.MeasureSpec.EXACTLY
                );
                ghost.measure(widthSpec, heightSpec);
                ghost.layout(0, 0, card.getWidth(), card.getHeight());
                ghost.setPivotX(card.getWidth() / 2f);
                ghost.setPivotY(card.getHeight() / 2f);
                ghost.setX(sourceRect.exactCenterX() - card.getWidth() / 2f);
                ghost.setY(sourceRect.exactCenterY() - card.getHeight() / 2f);
                ghost.setRotation(card.getRotation());

                ViewGroupOverlay overlay = matchFlightHost.getOverlay();
                overlay.add(ghost);
                matchFlightGhosts.add(ghost);
                matchFlightBitmaps.add(bitmap);

                float landingOffset = index == 0 ? -dp(2) : dp(2);
                float destinationX = targetRect.exactCenterX()
                    - card.getWidth() / 2f
                    + landingOffset;
                float destinationY = targetRect.exactCenterY()
                    - card.getHeight() / 2f;
                animators.add(ObjectAnimator.ofFloat(ghost, View.X, destinationX));
                animators.add(ObjectAnimator.ofFloat(ghost, View.Y, destinationY));
                animators.add(ObjectAnimator.ofFloat(ghost, View.SCALE_X, 0.16f));
                animators.add(ObjectAnimator.ofFloat(ghost, View.SCALE_Y, 0.16f));
                animators.add(ObjectAnimator.ofFloat(ghost, View.ALPHA, 0.12f));
                animators.add(ObjectAnimator.ofFloat(ghost, View.ROTATION, 0f));
            }
        } catch (RuntimeException ignored) {
            clearMatchFlightVisuals();
            return false;
        }

        hideMatchedCards(first, second);
        AnimatorSet flight = new AnimatorSet();
        activeMatchFlight = flight;
        flight.playTogether(animators);
        flight.setDuration(MATCH_FLIGHT_MS);
        flight.setInterpolator(new AccelerateInterpolator(1.15f));
        flight.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (generation != matchFlightGeneration || activeMatchFlight != flight) {
                    return;
                }
                activeMatchFlight = null;
                clearMatchFlightVisuals();
                if (screen == Screen.GAME
                    && game != null
                    && !paused
                    && !leaveDialogVisible) {
                    endAction.run();
                }
            }
        });
        flight.start();
        return true;
    }

    private Rect descendantRect(ViewGroup ancestor, View descendant) {
        Rect rect = new Rect(0, 0, descendant.getWidth(), descendant.getHeight());
        ancestor.offsetDescendantRectToMyCoords(descendant, rect);
        return rect;
    }

    private void hideMatchedCards(int first, int second) {
        if (cardViews == null || game == null) {
            return;
        }
        cardViews[first].removeFromTable(game.getPairId(first), false);
        cardViews[second].removeFromTable(game.getPairId(second), false);
        refreshCardInteractivity();
    }

    private void completeSuccessfulMatch(GameState.Resolution resolution) {
        if (game == null || screen != Screen.GAME) {
            return;
        }
        int scoringPlayer = resolution.getScoringPlayer();
        String scorer = playerName(scoringPlayer);
        updateGameHeader(scorer + " found a match — go again!");
        if (scoreRow != null) {
            scoreRow.announceForAccessibility(
                scorer + " scored. "
                    + game.getScore(scoringPlayer)
                    + " pairs"
            );
        }
        if (resolution.isGameComplete()) {
            postGameAction(
                reducedMotion ? 0L : AFTER_MATCH_MS + 200L,
                this::showResultsScreen
            );
            return;
        }
        postGameAction(reducedMotion ? 0L : AFTER_MATCH_MS, () -> {
            updateGameHeader(null);
            resumePendingGameAction();
        });
    }

    private void cancelMatchFlight() {
        matchFlightGeneration++;
        AnimatorSet flight = activeMatchFlight;
        activeMatchFlight = null;
        if (flight != null) {
            flight.removeAllListeners();
            flight.cancel();
        }
        clearMatchFlightVisuals();
    }

    private void clearMatchFlightVisuals() {
        ViewGroup host = matchFlightHost;
        if (host != null) {
            ViewGroupOverlay overlay = host.getOverlay();
            for (ImageView ghost : matchFlightGhosts) {
                overlay.remove(ghost);
                ghost.setImageDrawable(null);
            }
        }
        matchFlightGhosts.clear();
        matchFlightBitmaps.clear();
        matchFlightHost = null;
    }

    private void scheduleComputerFirstCard(long delay) {
        setInputLocked(true);
        updateGameHeader("Bot is studying the board…");
        postGameAction(delay, this::performComputerFirstCard);
    }

    private void performComputerFirstCard() {
        if (!game.isComputerTurn() || game.getPhase() != GameState.Phase.WAITING_FIRST) {
            return;
        }
        int choice = computerMemory.chooseFirstCard(game);
        if (choice < 0 || !flipAndShowCard(choice)) {
            return;
        }
        updateGameHeader("Bot picked one card…");
        postGameAction(COMPUTER_THINK_MS, this::performComputerSecondCard);
    }

    private void performComputerSecondCard() {
        if (!game.isComputerTurn() || game.getPhase() != GameState.Phase.WAITING_SECOND) {
            return;
        }
        int first = game.getFirstCard();
        int choice = computerMemory.chooseSecondCard(game, first, game.getPairId(first));
        if (choice < 0 || !flipAndShowCard(choice)) {
            return;
        }
        updateGameHeader("Bot is checking the pair…");
        postGameAction(
            currentPairRevealDelayMillis(REVEAL_DELAY_MS),
            this::resolveCurrentTurn
        );
    }

    private void resumePendingGameAction() {
        if (screen != Screen.GAME || game == null || paused || leaveDialogVisible) {
            return;
        }
        resumePairDecisionClock(SystemClock.elapsedRealtime());
        if (game.getPhase() == GameState.Phase.FINISHED) {
            setInputLocked(true);
            showResultsScreen();
        } else if (game.getPhase() == GameState.Phase.RESOLVING) {
            setInputLocked(true);
            updateGameHeader("Checking the pair…");
            postGameAction(
                currentPairRevealDelayMillis(500L),
                this::resolveCurrentTurn
            );
        } else if (game.isComputerTurn()) {
            setInputLocked(true);
            if (game.getPhase() == GameState.Phase.WAITING_SECOND) {
                updateGameHeader("Bot is choosing again…");
                postGameAction(COMPUTER_THINK_MS, this::performComputerSecondCard);
            } else {
                scheduleComputerFirstCard(COMPUTER_THINK_MS);
            }
        } else {
            updateGameHeader(null);
            if (turnHandoffEnabled && turnHandoffRequired) {
                showTurnHandoffOverlay();
            } else {
                turnHandoffRequired = false;
                setInputLocked(false);
            }
        }
    }

    private void showTurnHandoffOverlay() {
        if (!turnHandoffEnabled
            || !turnHandoffRequired
            || gameHost == null
            || game == null
            || game.isComputerTurn()
            || screen != Screen.GAME) {
            return;
        }
        setInputLocked(true);
        if (turnHandoffOverlay != null && turnHandoffOverlay.getParent() == gameHost) {
            return;
        }

        int player = game.getCurrentPlayer();
        int color = playerColor(player);
        int textColor = contrastTextColor(color);
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(color);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setContentDescription(
            playerName(player) + " turn. Tap to start without showing the board."
        );

        LinearLayout panel = verticalLayout();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(24), dp(22), dp(24), dp(22));
        PlayerAvatarView avatar = new PlayerAvatarView(this);
        bindPlayerAvatar(avatar, player);
        panel.addView(avatar, centered(dp(112), dp(112)));

        TextView ready = label("YOUR TURN", 13, textColor, true);
        ready.setLetterSpacing(0.12f);
        ready.setGravity(Gravity.CENTER);
        panel.addView(ready, margins(matchWrap(), 0, 18, 0, 0));
        TextView name = label(playerName(player), 32, textColor, true);
        name.setGravity(Gravity.CENTER);
        panel.addView(name, margins(matchWrap(), 0, 6, 0, 0));
        TextView prompt = label("Tap to start", 18, textColor, true);
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(dp(24), dp(13), dp(24), dp(13));
        prompt.setBackground(outlined(withAlpha(textColor, 34), textColor, 2, 18));
        panel.addView(prompt, margins(wrapWrap(), 0, 22, 0, 0));
        overlay.addView(
            panel,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        );

        overlay.setOnClickListener(view -> dismissTurnHandoffOverlay());
        turnHandoffOverlay = overlay;
        gameRoot.setImportantForAccessibility(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        );
        gameHost.addView(
            overlay,
            new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        );
        overlay.requestFocus();
        overlay.post(() -> overlay.announceForAccessibility(
            playerName(player) + " turn. Tap to start."
        ));
    }

    private void dismissTurnHandoffOverlay() {
        View overlay = turnHandoffOverlay;
        turnHandoffOverlay = null;
        if (overlay != null && overlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlay.getParent()).removeView(overlay);
        }
        if (gameRoot != null) {
            gameRoot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
        turnHandoffRequired = false;
        resumePendingGameAction();
    }

    private void updateGameHeader(String overrideMessage) {
        if (turnAvatar == null || game == null) {
            return;
        }
        int player = game.getCurrentPlayer();
        int playerBackground = playerColor(player);
        int headerText = contrastTextColor(playerBackground);
        applyActivePlayerChrome(playerBackground);
        if (renderedHeaderPlayer != player) {
            bindPlayerAvatar(turnAvatar, player);
            turnName.setTextColor(headerText);
            turnMessage.setTextColor(headerText);
            progressText.setTextColor(headerText);
            leaveGameButton.setTextColor(headerText);
            leaveGameButton.setBackground(ripple(withAlpha(headerText, 38), 12));
            turnName.setText(
                game.isComputerMode() && player == 1
                    ? "Bot • " + selectedDifficulty.getDisplayName()
                    : playerName(player)
            );
            renderedHeaderPlayer = player;
        }

        if (overrideMessage != null) {
            turnMessage.setText(overrideMessage);
        } else if (game.getPhase() == GameState.Phase.WAITING_SECOND) {
            turnMessage.setText(game.isComputerTurn() ? "Bot is choosing again…" : "Choose one more card");
        } else if (game.getPhase() == GameState.Phase.FINISHED) {
            turnMessage.setText("All pairs found");
        } else {
            turnMessage.setText(game.isComputerTurn() ? "Bot is thinking…" : "Find a matching pair");
        }

        progressText.setText(gameProgressText());
        renderScoreRow();
    }

    private void applyActivePlayerChrome(int playerBackground) {
        if (playerChromeApplied && appliedPlayerChromeColor == playerBackground) {
            return;
        }
        if (gameHost != null) {
            gameHost.setBackground(gameSurroundBackground(playerBackground));
        }
        if (gameRoot != null) {
            gameRoot.setBackground(gameSurroundBackground(playerBackground));
        }
        if (gameHeader != null) {
            gameHeader.setBackgroundColor(playerBackground);
        }
        if (gameBoardSection != null) {
            gameBoardSection.setBackground(gameSurroundBackground(playerBackground));
        }
        if (boardLayout != null) {
            boardLayout.setBackground(outlined(
                gameTabletopFill(playerBackground),
                gameTabletopBorder(playerBackground),
                1,
                20
            ));
        }
        setWindowBackground(playerBackground);
        appliedPlayerChromeColor = playerBackground;
        playerChromeApplied = true;
    }

    private GradientDrawable gameSurroundBackground(int playerColor) {
        GradientDrawable background = new GradientDrawable();
        ThemeVisualStyle style = currentThemeVisualStyle();
        if (style == ThemeVisualStyle.CLASSIC) {
            background.setColor(playerColor);
            return background;
        }
        float middleBlend;
        float endBlend;
        switch (style) {
            case GLASS:
                middleBlend = 0.38f;
                endBlend = 0.68f;
                break;
            case ARCADE:
                middleBlend = 0.58f;
                endBlend = 0.82f;
                break;
            case PAPER:
            case FLAT:
                middleBlend = 0.24f;
                endBlend = 0.48f;
                break;
            case BUBBLE:
                middleBlend = 0.32f;
                endBlend = 0.62f;
                break;
            default:
                middleBlend = 0f;
                endBlend = 0f;
                break;
        }
        background.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        background.setColors(new int[] {
            playerColor,
            GameSurfaceColors.blend(playerColor, BACKGROUND, middleBlend),
            GameSurfaceColors.blend(playerColor, BACKGROUND, endBlend)
        });
        return background;
    }

    private int gameTabletopFill(int playerColor) {
        return GameSurfaceColors.tabletop(
            tabletopMode,
            playerColor,
            SURFACE,
            darkTheme
        );
    }

    private int gameTabletopBorder(int playerColor) {
        return GameSurfaceColors.tabletopBorder(
            tabletopMode,
            playerColor,
            SURFACE,
            DIVIDER,
            darkTheme
        );
    }

    private void renderScoreRow() {
        if (scoreRow == null || scoreScroll == null || game == null) {
            return;
        }
        int playerCount = game.getTotalPlayerCount();
        boolean rebuild = scoreChipViews == null || scoreChipViews.length != playerCount;
        if (!rebuild) {
            for (TextView chip : scoreChipViews) {
                if (chip == null || chip.getParent() != scoreRow) {
                    rebuild = true;
                    break;
                }
            }
        }

        if (rebuild) {
            scoreRow.removeAllViews();
            scoreChipViews = new TextView[playerCount];
            for (int player = 0; player < playerCount; player++) {
                TextView chip = label("", 12, Color.WHITE, true);
                chip.setGravity(Gravity.CENTER);
                chip.setMinWidth(dp(62));
                chip.setMinHeight(dp(34));
                chip.setPadding(dp(11), dp(6), dp(11), dp(6));
                scoreRow.addView(chip, margins(wrapWrap(), 0, 0, 8, 0));
                scoreChipViews[player] = chip;
            }
        }

        int activePlayer = game.getCurrentPlayer();
        boolean finished = game.getPhase() == GameState.Phase.FINISHED;
        int[] scores = game.copyScores();
        boolean activePlayerChanged = renderedScorePlayer != activePlayer;
        boolean activeStyleChanged = rebuild
            || activePlayerChanged
            || renderedScoreFinished != finished;
        int activeColor = playerColor(game.getCurrentPlayer());
        int headerText = contrastTextColor(activeColor);
        int inactiveFill = headerText == Color.WHITE
            ? withAlpha(Color.BLACK, 42)
            : withAlpha(Color.WHITE, 55);
        for (int player = 0; player < playerCount; player++) {
            TextView chip = scoreChipViews[player];
            boolean scoreChanged = rebuild
                || renderedScores == null
                || renderedScores.length != scores.length
                || renderedScores[player] != scores[player];
            if (scoreChanged) {
                chip.setText(scoreBadge(player) + "  " + scores[player]);
                chip.setContentDescription(
                    playerName(player) + ", " + scores[player] + " pairs"
                );
            }
            if (activeStyleChanged) {
                boolean current = player == activePlayer && !finished;
                chip.setTextColor(current ? activeColor : headerText);
                chip.setBackground(outlined(
                    current ? headerText : inactiveFill,
                    headerText,
                    current ? 0 : 1,
                    12
                ));
            }
        }

        TextView activeChip = scoreChipViews[activePlayer];
        renderedScores = scores;
        renderedScorePlayer = activePlayer;
        renderedScoreFinished = finished;

        HorizontalScrollView capturedScroll = scoreScroll;
        LinearLayout capturedRow = scoreRow;
        TextView capturedChip = activeChip;
        int capturedPlayer = activePlayer;
        if ((rebuild || activePlayerChanged) && capturedChip != null) {
            capturedScroll.post(() -> {
                if (screen == Screen.GAME
                    && game != null
                    && !paused
                    && !leaveDialogVisible
                    && game.getCurrentPlayer() == capturedPlayer
                    && scoreScroll == capturedScroll
                    && scoreRow == capturedRow
                    && capturedChip.getParent() == capturedRow) {
                    ensureScoreChipVisible(capturedChip, false);
                }
            });
        }
    }

    private void ensureScoreChipVisible(TextView chip, boolean immediate) {
        if (scoreScroll == null
            || scoreRow == null
            || chip == null
            || chip.getParent() != scoreRow
            || scoreScroll.getWidth() <= 0
            || scoreRow.getWidth() <= 0) {
            return;
        }
        int targetX = ScoreScrollPlanner.targetX(
            scoreScroll.getScrollX(),
            scoreScroll.getWidth(),
            chip.getLeft(),
            chip.getRight(),
            scoreRow.getWidth()
        );
        if (targetX == scoreScroll.getScrollX()) {
            return;
        }
        if (immediate) {
            scoreScroll.scrollTo(targetX, 0);
        } else {
            scoreScroll.smoothScrollTo(targetX, 0);
        }
    }

    private String gameProgressText() {
        return game.getMatchedPairs() + "/" + game.getPairCount() + " pairs";
    }

    private void showResultsScreen() {
        if (game == null) {
            showSetupScreen();
            return;
        }
        recordCompletedGameIfNeeded();
        cancelPendingActions();
        screen = Screen.RESULTS;
        setWindowBackground(BACKGROUND);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        applyScreenBackground(scroll);
        applySafeCutoutInsets(scroll);

        LinearLayout content = verticalLayout();
        content.setPadding(dp(22), dp(26), dp(22), dp(32));
        scroll.addView(content, matchWrap());

        int[] scores = game.copyScores();
        List<Rankings.Group> groups = Rankings.fromScores(scores);
        Rankings.Group firstPlace = groups.get(0);
        boolean tiedForFirst = firstPlace.getPlayerIndices().size() > 1;

        TextView complete = sectionTitle("GAME COMPLETE");
        complete.setGravity(Gravity.CENTER);
        content.addView(complete, matchWrap());

        TextView trophy = label("★", 42, contrastTextColor(ACCENT), true);
        trophy.setGravity(Gravity.CENTER);
        trophy.setBackground(rounded(ACCENT, 24));
        trophy.setElevation(dp(themedElevation(5)));
        content.addView(trophy, margins(centered(dp(76), dp(76)), 0, 15, 0, 0));

        TextView title = label(
            tiedForFirst ? "It's a tie!" : groupNames(firstPlace) + " wins!",
            30,
            INK,
            true
        );
        title.setGravity(Gravity.CENTER);
        content.addView(title, margins(matchWrap(), 0, 16, 0, 0));

        TextView subtitle = label(
            tiedForFirst
                ? groupNames(firstPlace) + " share first place with "
                    + pairLabel(firstPlace.getScore())
                : pairLabel(firstPlace.getScore()) + " matched",
            15,
            MUTED,
            false
        );
        subtitle.setGravity(Gravity.CENTER);
        content.addView(subtitle, margins(matchWrap(), 0, 5, 0, 0));

        TextView gameTime = label(
            "Played in " + formatDuration(lastGameDurationMillis),
            14,
            PRIMARY_DARK,
            true
        );
        gameTime.setGravity(Gravity.CENTER);
        content.addView(gameTime, margins(matchWrap(), 0, 7, 0, 0));

        int shownGroups = 0;
        for (Rankings.Group group : groups) {
            if (group.getRank() > 3) {
                break;
            }
            content.addView(
                rankingCard(group, shownGroups == 0),
                margins(matchWrap(), 0, shownGroups == 0 ? 24 : 10, 0, 0)
            );
            shownGroups++;
        }

        TextView allScoresLabel = sectionTitle("SERIES SCOREBOARD");
        content.addView(allScoresLabel, margins(matchWrap(), 0, 24, 0, 0));

        int seriesGameCount = gameSeries == null ? 1 : gameSeries.getGameCount();
        int seriesTieCount = gameSeries == null ? 0 : gameSeries.getTiedGameCount();
        TextView seriesSummary = label(
            seriesGameCount + " game" + (seriesGameCount == 1 ? "" : "s")
                + (seriesTieCount == 0
                    ? ""
                    : "  •  " + seriesTieCount + " tied"),
            13,
            MUTED,
            false
        );
        content.addView(seriesSummary, margins(matchWrap(), 0, 5, 0, 0));

        LinearLayout allScores = verticalLayout();
        allScores.setPadding(dp(16), dp(8), dp(16), dp(8));
        allScores.setBackground(rounded(SURFACE, 20));
        allScores.setElevation(dp(themedElevation(2)));
        content.addView(allScores, margins(matchWrap(), 0, 8, 0, 0));

        for (int participant = 0; participant < scores.length; participant++) {
            LinearLayout row = horizontalLayout();
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));
            allScores.addView(row, matchWrap());

            PlayerAvatarView avatar = new PlayerAvatarView(this);
            bindPlayerAvatar(avatar, participant);
            row.addView(avatar, fixed(dp(38), dp(38)));
            LinearLayout identity = verticalLayout();
            row.addView(
                identity,
                margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 12, 0, 8, 0)
            );
            identity.addView(label(playerName(participant), 16, INK, true), matchWrap());
            identity.addView(
                label("This game: " + pairLabel(scores[participant]), 12, MUTED, false),
                margins(matchWrap(), 0, 3, 0, 0)
            );

            LinearLayout seriesResult = verticalLayout();
            seriesResult.setGravity(Gravity.END);
            int seriesWins = gameSeries == null ? 0 : gameSeries.getWinCount(participant);
            int winStreak = gameSeries == null
                ? 0
                : gameSeries.getCurrentWinStreak(participant);
            TextView wins = label(
                seriesWins + " win" + (seriesWins == 1 ? "" : "s"),
                15,
                seriesWins > 0 ? PRIMARY_DARK : MUTED,
                true
            );
            wins.setGravity(Gravity.END);
            seriesResult.addView(wins, matchWrap());
            if (winStreak > 0) {
                TextView streak = label(
                    winStreak == 1 ? "Won latest" : winStreak + " straight",
                    11,
                    SUCCESS,
                    true
                );
                streak.setGravity(Gravity.END);
                seriesResult.addView(streak, margins(matchWrap(), 0, 3, 0, 0));
            }
            row.addView(seriesResult, wrapWrap());

            if (participant < scores.length - 1) {
                View line = new View(this);
                line.setBackgroundColor(DIVIDER);
                allScores.addView(line, fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }

        addStatisticsSection(content);

        TextView again = actionButton("PLAY AGAIN", PRIMARY, contrastTextColor(PRIMARY));
        again.setOnClickListener(view -> startRematch());
        content.addView(again, margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)), 0, 24, 0, 0));

        TextView setup = actionButton(
            "CHANGE SETUP",
            SURFACE_TINT,
            darkTheme ? INK : PRIMARY_DARK
        );
        setup.setOnClickListener(view -> showSetupScreen());
        content.addView(setup, margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)), 0, 10, 0, 0));

        setContentView(scroll);
        enterImmersiveMode();
        content.post(() -> content.announceForAccessibility(
            tiedForFirst
                ? "Game complete. Tie between " + groupNames(firstPlace)
                : "Game complete. " + groupNames(firstPlace) + " won."
        ));
    }

    private void recordCompletedGameIfNeeded() {
        if (completedGameRecorded
            || game == null
            || game.getPhase() != GameState.Phase.FINISHED) {
            return;
        }
        long nowElapsed = SystemClock.elapsedRealtime();
        if (gameTimer == null) {
            gameTimer = GameTimer.restore(0L, false, nowElapsed);
        } else {
            gameTimer.pause(nowElapsed);
        }
        lastGameDurationMillis = gameTimer.getElapsedMillis(nowElapsed);
        String[] names = currentParticipantNames();
        if (gameSeries == null
            || !Arrays.equals(gameSeries.copyParticipantNames(), names)) {
            gameSeries = new GameSeries(names);
        }
        gameSeries.recordGame(game.copyScores());

        GameHistoryEntry entry = gameStats == null
            ? new GameHistoryEntry(
                System.currentTimeMillis(),
                lastGameDurationMillis,
                game.getHumanPlayerCount(),
                game.getPairCount(),
                names,
                game.copyScores(),
                null,
                null
            )
            : GameHistoryEntry.fromGameStats(
                System.currentTimeMillis(),
                lastGameDurationMillis,
                game.getHumanPlayerCount(),
                game.getPairCount(),
                names,
                game.copyScores(),
                gameStats
            );
        completedGameRecorded = true;
        historyStore.recordGame(entry);
    }

    private void addStatisticsSection(LinearLayout content) {
        if (gameStats == null) {
            return;
        }
        TextView heading = sectionTitle("PLAYER STATS");
        content.addView(heading, margins(matchWrap(), 0, 24, 0, 0));

        LinearLayout card = verticalLayout();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(SURFACE, 20));
        card.setElevation(dp(themedElevation(2)));
        content.addView(card, margins(matchWrap(), 0, 8, 0, 0));

        for (int participant = 0; participant < gameStats.getParticipantCount(); participant++) {
            if (participant > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(DIVIDER);
                card.addView(
                    divider,
                    margins(fixed(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)), 0, 9, 0, 9)
                );
            }
            TextView name = label(resultParticipantName(participant), 15, INK, true);
            card.addView(name, matchWrap());
            TextView metrics = label(
                (gameStats.getAttempts(participant) == 0
                    ? "— accuracy"
                    : gameStats.getAccuracyPercent(participant) + "% accuracy") + "  •  "
                    + "best streak " + gameStats.getLongestStreak(participant) + "  •  "
                    + gameStats.getPairs(participant) + " pairs captured  •  "
                    + (gameStats.getAverageDecisionTimeMillis(participant)
                        == GameStats.NO_DECISION_TIME
                        ? "— average decision"
                        : formatDecisionDuration(
                            gameStats.getAverageDecisionTimeMillis(participant)
                        ) + " average decision"),
                12,
                MUTED,
                false
            );
            metrics.setLineSpacing(0f, 1.08f);
            card.addView(metrics, margins(matchWrap(), 0, 3, 0, 0));
        }

        int comeback = gameStats.getComebackParticipant();
        TextView comebackText = label(
            comeback == GameStats.NO_PARTICIPANT
                ? "Comeback player: none this round"
                : "Comeback player: "
                    + resultParticipantName(comeback) + " rallied from "
                    + gameStats.getMaxDeficit(comeback) + " pair"
                    + (gameStats.getMaxDeficit(comeback) == 1 ? "" : "s") + " behind",
            13,
            comeback == GameStats.NO_PARTICIPANT ? MUTED : SUCCESS,
            true
        );
        comebackText.setPadding(0, dp(12), 0, 0);
        card.addView(comebackText, matchWrap());

        int[] quickest = gameStats.getQuickestParticipants();
        int[] slowest = gameStats.getSlowestParticipants();
        if (quickest.length == 0) {
            TextView noPace = label(
                "Decision pace: no completed timing data",
                13,
                MUTED,
                true
            );
            noPace.setPadding(0, dp(8), 0, 0);
            card.addView(noPace, matchWrap());
        } else {
            TextView quickestText = label(
                decisionPaceSummary("Quickest", quickest),
                13,
                SUCCESS,
                true
            );
            quickestText.setPadding(0, dp(8), 0, 0);
            card.addView(quickestText, matchWrap());

            TextView slowestText = label(
                decisionPaceSummary("Slowest", slowest),
                13,
                MUTED,
                true
            );
            slowestText.setPadding(0, dp(5), 0, 0);
            card.addView(slowestText, matchWrap());
        }
    }

    private String decisionPaceSummary(String role, int[] participants) {
        return role + (participants.length == 1 ? " player: " : " players: ")
            + resultParticipantNames(participants) + " — "
            + formatDecisionDuration(
                gameStats.getAverageDecisionTimeMillis(participants[0])
            ) + " average";
    }

    private String resultParticipantNames(int[] participants) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < participants.length; index++) {
            if (index > 0) {
                builder.append(index == participants.length - 1 ? " & " : ", ");
            }
            builder.append(resultParticipantName(participants[index]));
        }
        return builder.toString();
    }

    private String formatDecisionDuration(long durationMillis) {
        long safeMillis = Math.max(0L, durationMillis);
        if (safeMillis < 1_000L) {
            return safeMillis + "ms";
        }
        if (safeMillis < 60_000L) {
            long tenths = Math.round(safeMillis / 100.0);
            return (tenths / 10L) + "." + (tenths % 10L) + "s";
        }
        return formatDuration(safeMillis);
    }

    private String resultParticipantName(int participant) {
        return playerName(participant);
    }

    private View rankingCard(Rankings.Group group, boolean winner) {
        LinearLayout card = horizontalLayout();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(winner ? 17 : 13), dp(16), dp(winner ? 17 : 13));
        int winnerFill = withAlpha(ACCENT, darkTheme ? 58 : 42);
        int winnerText = darkTheme ? ACCENT : PRIMARY_DARK;
        card.setBackground(outlined(
            winner ? winnerFill : SURFACE,
            winner ? ACCENT : DIVIDER,
            winner ? 2 : 1,
            20
        ));
        card.setElevation(dp(themedElevation(winner ? 4 : 1)));

        TextView place = label(
            ordinal(group.getRank()),
            winner ? 19 : 15,
            winner ? contrastTextColor(ACCENT) : MUTED,
            true
        );
        place.setGravity(Gravity.CENTER);
        place.setBackground(rounded(winner ? ACCENT : SURFACE_TINT, 15));
        card.addView(place, fixed(dp(winner ? 58 : 48), dp(winner ? 58 : 48)));

        LinearLayout names = verticalLayout();
        card.addView(names, margins(weighted(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f), 14, 0, 8, 0));
        TextView rankLabel = label(
            ordinal(group.getRank()) + " PLACE" + (group.getPlayerIndices().size() > 1 ? " • TIE" : ""),
            10,
            winner ? winnerText : MUTED,
            true
        );
        rankLabel.setLetterSpacing(0.08f);
        names.addView(rankLabel, matchWrap());
        names.addView(label(groupNames(group), winner ? 20 : 17, INK, true), margins(matchWrap(), 0, 3, 0, 0));
        card.addView(
            label(
                pairLabel(group.getScore()),
                winner ? 18 : 15,
                winner ? winnerText : MUTED,
                true
            ),
            wrapWrap()
        );
        return card;
    }

    private String groupNames(Rankings.Group group) {
        StringBuilder builder = new StringBuilder();
        List<Integer> players = group.getPlayerIndices();
        for (int index = 0; index < players.size(); index++) {
            if (index > 0) {
                builder.append(index == players.size() - 1 ? " & " : ", ");
            }
            builder.append(resultParticipantName(players.get(index)));
        }
        return builder.toString();
    }

    private String pairLabel(int count) {
        return count + (count == 1 ? " pair" : " pairs");
    }

    private String formatDuration(long durationMillis) {
        long totalSeconds = Math.max(0L, Math.round(durationMillis / 1000.0));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private String ordinal(int rank) {
        if (rank == 1) {
            return "1ST";
        }
        if (rank == 2) {
            return "2ND";
        }
        return "3RD";
    }

    private String playerName(int playerIndex) {
        if (game != null && game.isComputerMode() && playerIndex == 1) {
            return "Bot";
        }
        if (profileStore != null && playerIndex >= 0 && playerIndex < profileStore.size()) {
            return profileStore.getDisplayNameAt(playerIndex);
        }
        return PlayerProfile.getDefaultDisplayName(playerIndex + 1);
    }

    private String[] currentParticipantNames() {
        if (game == null) {
            throw new IllegalStateException("A game is required to resolve participants");
        }
        String[] names = new String[game.getTotalPlayerCount()];
        for (int participant = 0; participant < names.length; participant++) {
            names[participant] = playerName(participant);
        }
        return names;
    }

    private String scoreBadge(int playerIndex) {
        if (game != null && game.isComputerMode() && playerIndex == 1) {
            return "BOT";
        }
        if (profileStore != null && playerIndex >= 0 && playerIndex < profileStore.size()) {
            return profileStore.get(playerIndex).getScoreBadgeLabel(playerIndex + 1);
        }
        return defaultPlayerLetter(playerIndex + 1);
    }

    private void bindPlayerAvatar(PlayerAvatarView avatar, int playerIndex) {
        if (game != null && game.isComputerMode() && playerIndex == 1) {
            avatar.setAvatar("Bot", "BOT", "", playerColor(playerIndex));
            return;
        }
        if (profileStore != null && playerIndex >= 0 && playerIndex < profileStore.size()) {
            avatar.setProfile(
                profileStore.get(playerIndex),
                playerIndex + 1,
                playerColor(playerIndex)
            );
        } else {
            avatar.setAvatar(
                PlayerProfile.getDefaultDisplayName(playerIndex + 1),
                defaultPlayerLetter(playerIndex + 1),
                "",
                playerColor(playerIndex)
            );
        }
    }

    private String defaultPlayerLetter(int oneBasedPosition) {
        return PlayerProfile.getDefaultLabel(oneBasedPosition);
    }

    private int playerColor(int playerIndex) {
        return PLAYER_COLORS[playerIndex % PLAYER_COLORS.length];
    }

    private long boardLayoutSeed() {
        long seed = 1125899906842597L;
        int[] pairIds = game.copyPairIds();
        for (int index = 0; index < pairIds.length; index++) {
            seed = seed * 31L + pairIds[index] * 17L + index;
        }
        return seed;
    }

    private int contrastTextColor(int backgroundColor) {
        return ContrastColors.blackOrWhiteFor(backgroundColor);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(
            clamp(alpha, 0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        );
    }

    private void confirmLeaveGame() {
        if (screen != Screen.GAME || leaveDialogVisible) {
            return;
        }
        pausePairDecisionClock(SystemClock.elapsedRealtime());
        suspendGameActions();
        leaveDialogVisible = true;
        setInputLocked(true);
        if (boardLayout != null) {
            // Commit any in-flight spread/swap before showing the modal. Its completion callback
            // is intentionally cancelled; Keep playing resumes from the authoritative game phase.
            boardLayout.finishBoardMotion();
        }
        int dialogTheme = darkTheme
            ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert;
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme)
            .setTitle("Leave this game?")
            .setMessage("The current board and scores will be cleared.")
            .setNegativeButton("Keep playing", (ignored, which) -> {
                leaveDialogVisible = false;
                resumePendingGameAction();
            })
            .setPositiveButton("Leave game", (ignored, which) -> {
                leaveDialogVisible = false;
                showSetupScreen();
            })
            .create();
        dialog.setOnCancelListener(ignored -> {
            leaveDialogVisible = false;
            resumePendingGameAction();
        });
        dialog.show();
        keepDialogImmersive(dialog);
    }

    @Override
    public void onBackPressed() {
        if (screen == Screen.GAME) {
            confirmLeaveGame();
        } else if (screen == Screen.RESULTS) {
            showSetupScreen();
        } else if (screen == Screen.PROFILES) {
            saveProfileNamesFromInputs();
            showSetupScreen();
        } else if (screen == Screen.ADVANCED) {
            showSetupScreen();
        } else if (screen == Screen.HISTORY) {
            showSetupScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPairRepeat();
        if (screen == Screen.GAME) {
            pausePairDecisionClock(SystemClock.elapsedRealtime());
            if (gameTimer != null) {
                gameTimer.pause(SystemClock.elapsedRealtime());
            }
            paused = true;
            suspendGameActions();
            if (boardLayout != null) {
                boardLayout.finishBoardMotion();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        if (paused) {
            paused = false;
            if (screen == Screen.GAME && game != null && !leaveDialogVisible) {
                if (gameTimer != null && game.getPhase() != GameState.Phase.FINISHED) {
                    gameTimer.resume(SystemClock.elapsedRealtime());
                }
                resumePendingGameAction();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
            resumePairDecisionClock(SystemClock.elapsedRealtime());
        } else {
            pausePairDecisionClock(SystemClock.elapsedRealtime());
            stopPairRepeat();
        }
    }

    @Override
    protected void onDestroy() {
        cancelPendingActions();
        if (gameFeedback != null) {
            gameFeedback.release();
        }
        super.onDestroy();
    }

    private void startPairDecisionClock(long nowElapsed) {
        pairDecisionElapsedMillis = 0L;
        pairDecisionSegmentStartedAtElapsed = -1L;
        pendingPairDecisionDurationMillis = GameStats.NO_DECISION_TIME;
        resumePairDecisionClock(nowElapsed);
    }

    private void completePairDecisionClock(long nowElapsed) {
        pausePairDecisionClock(nowElapsed);
        pendingPairDecisionDurationMillis = pairDecisionElapsedMillis;
        pairDecisionElapsedMillis = 0L;
    }

    private void pausePairDecisionClock(long nowElapsed) {
        if (pairDecisionSegmentStartedAtElapsed < 0L) {
            return;
        }
        pairDecisionElapsedMillis = Math.addExact(
            pairDecisionElapsedMillis,
            Math.max(0L, nowElapsed - pairDecisionSegmentStartedAtElapsed)
        );
        pairDecisionSegmentStartedAtElapsed = -1L;
    }

    private void resumePairDecisionClock(long nowElapsed) {
        if (game != null
            && game.getPhase() == GameState.Phase.WAITING_SECOND
            && pairDecisionSegmentStartedAtElapsed < 0L
            && !paused
            && !leaveDialogVisible
            && hasWindowFocus()) {
            pairDecisionSegmentStartedAtElapsed = nowElapsed;
        }
    }

    private long currentPairDecisionElapsedMillis(long nowElapsed) {
        return pairDecisionSegmentStartedAtElapsed < 0L
            ? pairDecisionElapsedMillis
            : Math.addExact(
                pairDecisionElapsedMillis,
                Math.max(0L, nowElapsed - pairDecisionSegmentStartedAtElapsed)
            );
    }

    private void resetPairDecisionClock() {
        pairDecisionElapsedMillis = 0L;
        pairDecisionSegmentStartedAtElapsed = -1L;
        pendingPairDecisionDurationMillis = GameStats.NO_DECISION_TIME;
    }

    private void suspendGameActions() {
        actionGeneration++;
        handler.removeCallbacksAndMessages(null);
        cancelMatchFlight();
    }

    private void setInputLocked(boolean locked) {
        inputLocked = locked;
        refreshCardInteractivity();
    }

    private void refreshCardInteractivity() {
        if (cardViews == null || game == null) {
            return;
        }
        boolean humanCanPlay = screen == Screen.GAME
            && !inputLocked
            && !paused
            && !game.isComputerTurn()
            && game.getPhase() != GameState.Phase.FINISHED;
        for (int index = 0; index < cardViews.length; index++) {
            CardTileView card = cardViews[index];
            GameState.CardState state = game.getCardState(index);
            boolean available = humanCanPlay && state == GameState.CardState.HIDDEN;
            card.setEnabled(available);
            card.setClickable(available);
            card.setFocusable(available);
            card.setImportantForAccessibility(
                state == GameState.CardState.MATCHED
                    ? View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    : View.IMPORTANT_FOR_ACCESSIBILITY_YES
            );
        }
    }

    private void cancelPendingActions() {
        stopPairRepeat();
        suspendGameActions();
        if (turnHandoffOverlay != null
            && turnHandoffOverlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) turnHandoffOverlay.getParent()).removeView(turnHandoffOverlay);
        }
        turnHandoffOverlay = null;
        if (gameRoot != null) {
            gameRoot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
        setInputLocked(false);
    }

    private void postGameAction(long delayMillis, Runnable action) {
        int token = actionGeneration;
        handler.postDelayed(() -> {
            if (token == actionGeneration && screen == Screen.GAME && !paused) {
                action.run();
            }
        }, delayMillis);
    }

    private void applyThemePalette() {
        if (darkTheme) {
            INK = Color.rgb(245, 246, 251);
            MUTED = Color.rgb(174, 180, 199);
            SUCCESS = Color.rgb(74, 190, 132);
            switch (themePreset) {
                case MINIMAL_DARK:
                case DARK_VIBRANT_WHITE:
                    BACKGROUND = Color.rgb(16, 17, 18);
                    SURFACE = Color.rgb(25, 27, 29);
                    SURFACE_TINT = Color.rgb(36, 39, 42);
                    INK = Color.rgb(244, 244, 242);
                    MUTED = Color.rgb(169, 173, 177);
                    PRIMARY = Color.rgb(232, 232, 227);
                    PRIMARY_DARK = Color.WHITE;
                    ACCENT = Color.rgb(202, 204, 200);
                    SUCCESS = Color.rgb(67, 169, 116);
                    DIVIDER = Color.rgb(52, 56, 60);
                    break;
                case SIMPLE:
                    BACKGROUND = Color.rgb(17, 24, 39);
                    SURFACE = Color.rgb(31, 41, 55);
                    SURFACE_TINT = Color.rgb(41, 53, 72);
                    INK = Color.rgb(245, 246, 251);
                    MUTED = Color.rgb(170, 178, 192);
                    PRIMARY = Color.rgb(96, 165, 250);
                    PRIMARY_DARK = Color.rgb(147, 197, 253);
                    ACCENT = Color.rgb(251, 191, 36);
                    SUCCESS = Color.rgb(74, 190, 132);
                    DIVIDER = Color.rgb(55, 65, 81);
                    break;
                case MIXED_COLOR:
                    BACKGROUND = Color.rgb(23, 18, 37);
                    SURFACE = Color.rgb(36, 28, 53);
                    SURFACE_TINT = Color.rgb(50, 37, 69);
                    INK = Color.rgb(248, 245, 255);
                    MUTED = Color.rgb(190, 180, 206);
                    PRIMARY = Color.rgb(167, 139, 250);
                    PRIMARY_DARK = Color.rgb(196, 181, 253);
                    ACCENT = Color.rgb(251, 113, 133);
                    SUCCESS = Color.rgb(45, 212, 191);
                    DIVIDER = Color.rgb(73, 55, 94);
                    break;
                case DARK_ORANGE:
                case DARK_VIBRANT_ORANGE:
                    BACKGROUND = Color.rgb(24, 8, 1);
                    SURFACE = Color.rgb(46, 18, 5);
                    SURFACE_TINT = Color.rgb(70, 29, 7);
                    PRIMARY = Color.rgb(255, 109, 0);
                    PRIMARY_DARK = Color.rgb(255, 183, 107);
                    ACCENT = Color.rgb(255, 225, 55);
                    DIVIDER = Color.rgb(105, 46, 12);
                    break;
                case DARK_BLUE:
                    BACKGROUND = Color.rgb(13, 20, 32);
                    SURFACE = Color.rgb(23, 34, 52);
                    SURFACE_TINT = Color.rgb(32, 48, 72);
                    PRIMARY = Color.rgb(70, 140, 255);
                    PRIMARY_DARK = Color.rgb(148, 190, 255);
                    ACCENT = Color.rgb(255, 187, 80);
                    DIVIDER = Color.rgb(49, 67, 94);
                    break;
                case DARK_ROSE:
                    BACKGROUND = Color.rgb(28, 16, 23);
                    SURFACE = Color.rgb(47, 26, 39);
                    SURFACE_TINT = Color.rgb(66, 36, 53);
                    PRIMARY = Color.rgb(234, 83, 145);
                    PRIMARY_DARK = Color.rgb(255, 158, 198);
                    ACCENT = Color.rgb(255, 192, 92);
                    DIVIDER = Color.rgb(82, 49, 68);
                    break;
                case DARK_VIBRANT_CYAN:
                    BACKGROUND = Color.rgb(1, 15, 20);
                    SURFACE = Color.rgb(5, 32, 40);
                    SURFACE_TINT = Color.rgb(7, 52, 64);
                    PRIMARY = Color.rgb(0, 221, 242);
                    PRIMARY_DARK = Color.rgb(112, 244, 255);
                    ACCENT = Color.rgb(255, 225, 55);
                    DIVIDER = Color.rgb(13, 82, 96);
                    break;
                case DARK_VIBRANT_LIME:
                    BACKGROUND = Color.rgb(8, 17, 3);
                    SURFACE = Color.rgb(20, 38, 9);
                    SURFACE_TINT = Color.rgb(34, 58, 14);
                    PRIMARY = Color.rgb(183, 240, 0);
                    PRIMARY_DARK = Color.rgb(221, 255, 105);
                    ACCENT = Color.rgb(255, 87, 125);
                    DIVIDER = Color.rgb(55, 88, 24);
                    break;
                case DARK_VIBRANT_PINK:
                    BACKGROUND = Color.rgb(24, 3, 16);
                    SURFACE = Color.rgb(47, 8, 32);
                    SURFACE_TINT = Color.rgb(72, 13, 49);
                    PRIMARY = Color.rgb(255, 45, 149);
                    PRIMARY_DARK = Color.rgb(255, 148, 204);
                    ACCENT = Color.rgb(0, 229, 255);
                    DIVIDER = Color.rgb(105, 25, 72);
                    break;
                case GLASS:
                    BACKGROUND = Color.rgb(8, 21, 33);
                    SURFACE = Color.rgb(29, 48, 72);
                    SURFACE_TINT = Color.rgb(82, 109, 135);
                    INK = Color.rgb(246, 251, 255);
                    MUTED = Color.rgb(190, 211, 225);
                    PRIMARY = Color.rgb(105, 221, 244);
                    PRIMARY_DARK = Color.rgb(165, 240, 255);
                    ACCENT = Color.rgb(255, 179, 107);
                    DIVIDER = Color.rgb(104, 142, 166);
                    break;
                case ARCADE:
                    BACKGROUND = Color.rgb(3, 7, 18);
                    SURFACE = Color.rgb(13, 20, 34);
                    SURFACE_TINT = Color.rgb(21, 34, 57);
                    INK = Color.rgb(248, 254, 255);
                    MUTED = Color.rgb(169, 193, 211);
                    PRIMARY = Color.rgb(0, 245, 212);
                    PRIMARY_DARK = Color.rgb(98, 255, 233);
                    ACCENT = Color.rgb(255, 61, 165);
                    SUCCESS = Color.rgb(183, 255, 60);
                    DIVIDER = Color.rgb(46, 91, 109);
                    break;
                case SYSTEM:
                case DARK_TEAL:
                    BACKGROUND = Color.rgb(12, 22, 23);
                    SURFACE = Color.rgb(22, 38, 39);
                    SURFACE_TINT = Color.rgb(31, 55, 55);
                    PRIMARY = Color.rgb(38, 190, 173);
                    PRIMARY_DARK = Color.rgb(112, 230, 214);
                    ACCENT = Color.rgb(255, 190, 78);
                    DIVIDER = Color.rgb(49, 73, 73);
                    break;
                case DARK_VIOLET:
                default:
                    BACKGROUND = Color.rgb(17, 19, 29);
                    SURFACE = Color.rgb(29, 33, 48);
                    SURFACE_TINT = Color.rgb(40, 44, 62);
                    PRIMARY = Color.rgb(139, 124, 255);
                    PRIMARY_DARK = Color.rgb(184, 176, 255);
                    ACCENT = Color.rgb(255, 183, 77);
                    DIVIDER = Color.rgb(57, 61, 78);
                    break;
            }
        } else {
            SURFACE = Color.WHITE;
            SUCCESS = Color.rgb(46, 157, 101);
            switch (themePreset) {
                case MINIMAL_LIGHT:
                    BACKGROUND = Color.rgb(247, 247, 245);
                    SURFACE = Color.WHITE;
                    SURFACE_TINT = Color.rgb(236, 237, 235);
                    INK = Color.rgb(32, 33, 36);
                    MUTED = Color.rgb(103, 108, 112);
                    PRIMARY = Color.rgb(48, 50, 54);
                    PRIMARY_DARK = Color.rgb(32, 33, 36);
                    ACCENT = Color.rgb(217, 219, 215);
                    SUCCESS = Color.rgb(40, 132, 90);
                    DIVIDER = Color.rgb(220, 221, 218);
                    break;
                case SIMPLE:
                    BACKGROUND = Color.rgb(246, 247, 249);
                    SURFACE = Color.WHITE;
                    SURFACE_TINT = Color.rgb(234, 240, 247);
                    INK = Color.rgb(31, 41, 55);
                    MUTED = Color.rgb(104, 115, 134);
                    PRIMARY = Color.rgb(37, 99, 235);
                    PRIMARY_DARK = Color.rgb(29, 78, 216);
                    ACCENT = Color.rgb(245, 158, 11);
                    SUCCESS = Color.rgb(46, 157, 101);
                    DIVIDER = Color.rgb(221, 227, 234);
                    break;
                case MIXED_COLOR:
                    BACKGROUND = Color.rgb(255, 247, 243);
                    SURFACE = Color.WHITE;
                    SURFACE_TINT = Color.rgb(241, 232, 255);
                    INK = Color.rgb(45, 33, 64);
                    MUTED = Color.rgb(121, 107, 136);
                    PRIMARY = Color.rgb(124, 58, 237);
                    PRIMARY_DARK = Color.rgb(91, 33, 182);
                    ACCENT = Color.rgb(219, 39, 119);
                    SUCCESS = Color.rgb(15, 152, 133);
                    DIVIDER = Color.rgb(231, 217, 238);
                    break;
                case LIGHT_ORANGE:
                    BACKGROUND = Color.rgb(252, 247, 240);
                    SURFACE_TINT = Color.rgb(252, 234, 213);
                    INK = Color.rgb(54, 37, 27);
                    MUTED = Color.rgb(119, 94, 78);
                    PRIMARY = Color.rgb(218, 101, 29);
                    PRIMARY_DARK = Color.rgb(162, 70, 17);
                    ACCENT = Color.rgb(232, 139, 38);
                    DIVIDER = Color.rgb(237, 220, 204);
                    break;
                case LIGHT_BLUE:
                    BACKGROUND = Color.rgb(243, 247, 253);
                    SURFACE_TINT = Color.rgb(224, 235, 251);
                    INK = Color.rgb(25, 43, 69);
                    MUTED = Color.rgb(82, 103, 133);
                    PRIMARY = Color.rgb(38, 103, 211);
                    PRIMARY_DARK = Color.rgb(24, 76, 161);
                    ACCENT = Color.rgb(220, 132, 27);
                    DIVIDER = Color.rgb(207, 221, 240);
                    break;
                case LIGHT_ROSE:
                    BACKGROUND = Color.rgb(253, 245, 249);
                    SURFACE_TINT = Color.rgb(249, 225, 237);
                    INK = Color.rgb(65, 31, 49);
                    MUTED = Color.rgb(126, 83, 106);
                    PRIMARY = Color.rgb(196, 48, 111);
                    PRIMARY_DARK = Color.rgb(148, 31, 81);
                    ACCENT = Color.rgb(220, 130, 35);
                    DIVIDER = Color.rgb(237, 208, 224);
                    break;
                case GLASS:
                    BACKGROUND = Color.rgb(232, 245, 255);
                    SURFACE = Color.rgb(255, 255, 255);
                    SURFACE_TINT = Color.rgb(222, 239, 249);
                    INK = Color.rgb(24, 47, 65);
                    MUTED = Color.rgb(80, 112, 132);
                    PRIMARY = Color.rgb(20, 126, 163);
                    PRIMARY_DARK = Color.rgb(11, 93, 126);
                    ACCENT = Color.rgb(217, 107, 36);
                    DIVIDER = Color.rgb(186, 218, 233);
                    break;
                case PAPER:
                    BACKGROUND = Color.rgb(242, 225, 189);
                    SURFACE = Color.rgb(255, 249, 232);
                    SURFACE_TINT = Color.rgb(243, 224, 185);
                    INK = Color.rgb(59, 42, 28);
                    MUTED = Color.rgb(121, 98, 75);
                    PRIMARY = Color.rgb(184, 76, 50);
                    PRIMARY_DARK = Color.rgb(133, 49, 31);
                    ACCENT = Color.rgb(211, 154, 32);
                    SUCCESS = Color.rgb(45, 127, 75);
                    DIVIDER = Color.rgb(199, 169, 129);
                    break;
                case SYSTEM:
                case LIGHT_TEAL:
                    BACKGROUND = Color.rgb(242, 249, 248);
                    SURFACE_TINT = Color.rgb(219, 241, 238);
                    INK = Color.rgb(25, 52, 51);
                    MUTED = Color.rgb(83, 113, 110);
                    PRIMARY = Color.rgb(0, 137, 123);
                    PRIMARY_DARK = Color.rgb(0, 103, 93);
                    ACCENT = Color.rgb(227, 146, 44);
                    DIVIDER = Color.rgb(207, 230, 227);
                    break;
                default:
                    BACKGROUND = Color.rgb(247, 245, 239);
                    SURFACE_TINT = Color.rgb(239, 237, 249);
                    INK = Color.rgb(29, 33, 55);
                    MUTED = Color.rgb(103, 111, 128);
                    PRIMARY = Color.rgb(91, 75, 219);
                    PRIMARY_DARK = Color.rgb(69, 56, 183);
                    ACCENT = Color.rgb(255, 183, 77);
                    DIVIDER = Color.rgb(229, 228, 235);
                    break;
            }
        }
        setWindowBackground(BACKGROUND);
    }

    private void showThemePicker() {
        ThemePreset[] presets = ThemePreset.selectable();
        String[] names = new String[presets.length];
        int selected = 0;
        for (int index = 0; index < presets.length; index++) {
            names[index] = presets[index].displayName;
            if (presets[index] == themePreset) {
                selected = index;
            }
        }
        int dialogTheme = darkTheme
            ? android.R.style.Theme_Material_Dialog_Alert
            : android.R.style.Theme_Material_Light_Dialog_Alert;
        AlertDialog dialog = new AlertDialog.Builder(this, dialogTheme)
            .setTitle("Choose a theme")
            .setSingleChoiceItems(names, selected, (picker, which) -> {
                themePreset = presets[which];
                darkTheme = resolveDarkTheme(themePreset);
                getSharedPreferences(SETTINGS, MODE_PRIVATE)
                    .edit()
                    .putString(THEME_PRESET, themePreset.name())
                    .putBoolean(DARK_THEME, darkTheme)
                    .apply();
                picker.dismiss();
                applyThemePalette();
                showAdvancedSettingsScreen();
            })
            .setNegativeButton("Cancel", (ignored, which) -> pendingAdvancedScrollY = -1)
            .create();
        dialog.setOnCancelListener(ignored -> pendingAdvancedScrollY = -1);
        dialog.show();
        keepDialogImmersive(dialog);
    }

    private boolean resolveDarkTheme(ThemePreset preset) {
        if (preset.mode > 0) {
            return true;
        }
        if (preset.mode == 0) {
            return false;
        }
        int nightMode = getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void enterImmersiveMode() {
        Window window = getWindow();
        if (!immersiveWindowConfigured) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                window.setAttributes(attributes);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(false);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            }
            immersiveWindowConfigured = true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            View decorView = window.getDecorView();
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                hideSystemBars(controller);
            } else {
                decorView.post(() -> {
                    WindowInsetsController attachedController = decorView.getWindowInsetsController();
                    if (attachedController != null) {
                        hideSystemBars(attachedController);
                    }
                });
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private void hideSystemBars(WindowInsetsController controller) {
        controller.hide(WindowInsets.Type.systemBars());
        controller.setSystemBarsBehavior(
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        controller.setSystemBarsAppearance(
            0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        );
    }

    private void keepDialogImmersive(AlertDialog dialog) {
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow == null) {
            return;
        }
        View decorView = dialogWindow.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dialogWindow.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                hideSystemBars(controller);
            }
        } else {
            decorView.setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());
        }
    }

    private void setWindowBackground(int color) {
        getWindow().setBackgroundDrawable(new ColorDrawable(color));
    }

    private void updateStickyStartPosition(
        FrameLayout host,
        View anchor,
        TextView button
    ) {
        if (!host.isAttachedToWindow()
            || !anchor.isAttachedToWindow()
            || anchor.getWidth() <= 0
            || anchor.getHeight() <= 0
            || host.getWidth() <= 0
            || host.getHeight() <= 0) {
            return;
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) button.getLayoutParams();
        if (params.width != anchor.getWidth()
            || params.height != anchor.getHeight()
            || params.gravity != (Gravity.TOP | Gravity.START)
            || params.leftMargin != 0
            || params.topMargin != 0
            || params.rightMargin != 0
            || params.bottomMargin != 0) {
            params.width = anchor.getWidth();
            params.height = anchor.getHeight();
            params.gravity = Gravity.TOP | Gravity.START;
            params.setMargins(0, 0, 0, 0);
            button.setLayoutParams(params);
        }

        int[] hostLocation = new int[2];
        int[] anchorLocation = new int[2];
        host.getLocationInWindow(hostLocation);
        anchor.getLocationInWindow(anchorLocation);
        int naturalLeft = anchorLocation[0] - hostLocation[0];
        int naturalTop = anchorLocation[1] - hostLocation[1];
        int safeBottom = host.getHeight() - host.getPaddingBottom();
        int targetTop = StickyStartPosition.targetTop(
            naturalTop,
            safeBottom,
            anchor.getHeight(),
            dp(14)
        );

        button.setX(naturalLeft);
        button.setY(Math.max(host.getPaddingTop(), targetTop));
        button.setVisibility(View.VISIBLE);
    }

    private void restoreScrollBeforeFirstDraw(ScrollView scroll, int scrollY) {
        scroll.getViewTreeObserver().addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    ViewTreeObserver observer = scroll.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }
                    scroll.scrollTo(0, scrollY);
                    return true;
                }
            }
        );
    }

    private void applySafeCutoutInsets(View root) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }
        int initialLeft = root.getPaddingLeft();
        int initialTop = root.getPaddingTop();
        int initialRight = root.getPaddingRight();
        int initialBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            DisplayCutout cutout = windowInsets.getDisplayCutout();
            int left = cutout == null ? 0 : cutout.getSafeInsetLeft();
            int top = cutout == null ? 0 : cutout.getSafeInsetTop();
            int right = cutout == null ? 0 : cutout.getSafeInsetRight();
            int bottom = cutout == null ? 0 : cutout.getSafeInsetBottom();
            view.setPadding(
                initialLeft + left,
                initialTop + top,
                initialRight + right,
                initialBottom + bottom
            );
            return windowInsets;
        });
        root.requestApplyInsets();
    }

    private LinearLayout verticalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontalLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout stepperRow() {
        LinearLayout row = horizontalLayout();
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView label(String text, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(
            themeFontFamily(),
            bold ? Typeface.BOLD : Typeface.NORMAL
        ));
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text, 11, PRIMARY, true);
        view.setLetterSpacing(0.12f);
        return view;
    }

    private TextView stepButton(String text) {
        TextView button = label(text, 25, PRIMARY_DARK, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(ripple(SURFACE_TINT, 16));
        button.setContentDescription(text.equals("+") ? "Increase" : "Decrease");
        return button;
    }

    private TextView stepValue() {
        TextView value = label("", 19, INK, true);
        value.setGravity(Gravity.CENTER);
        value.setBackground(outlined(SURFACE, DIVIDER, 1, 16));
        return value;
    }

    private TextView actionButton(String text, int fillColor, int textColor) {
        TextView button = label(text, 14, textColor, true);
        button.setGravity(Gravity.CENTER);
        button.setLetterSpacing(0.08f);
        button.setBackground(ripple(fillColor, 18));
        button.setElevation(dp(themedElevation(2)));
        button.setFocusable(true);
        return button;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        ThemeVisualStyle style = currentThemeVisualStyle();
        if (style == ThemeVisualStyle.BUBBLE && color == PRIMARY) {
            drawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            drawable.setColors(new int[] {PRIMARY, ACCENT});
        } else {
            drawable.setColor(themedFill(color));
        }
        drawable.setCornerRadius(dp(themedRadius(radiusDp)));
        if (style == ThemeVisualStyle.ARCADE
            && (color == SURFACE || color == SURFACE_TINT)) {
            drawable.setStroke(dp(1), withAlpha(PRIMARY, 170));
        } else if (style == ThemeVisualStyle.PAPER
            && (color == SURFACE || color == SURFACE_TINT)) {
            drawable.setStroke(dp(1), withAlpha(DIVIDER, 210));
        }
        return drawable;
    }

    private GradientDrawable outlined(int fillColor, int strokeColor, int strokeDp, int radiusDp) {
        GradientDrawable drawable = rounded(fillColor, radiusDp);
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), strokeColor);
        }
        return drawable;
    }

    private RippleDrawable ripple(int color, int radiusDp) {
        GradientDrawable content = rounded(color, radiusDp);
        if (currentThemeVisualStyle() == ThemeVisualStyle.ARCADE) {
            content.setStroke(dp(2), withAlpha(color == PRIMARY ? ACCENT : PRIMARY, 205));
        }
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(themedRadius(radiusDp)));
        int rippleColor = currentThemeVisualStyle() == ThemeVisualStyle.ARCADE
            ? withAlpha(ACCENT, 95)
            : Color.argb(45, 255, 255, 255);
        return new RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            content,
            mask
        );
    }

    private ThemeVisualStyle currentThemeVisualStyle() {
        switch (themePreset) {
            case MINIMAL_LIGHT:
            case MINIMAL_DARK:
            case DARK_VIBRANT_WHITE:
            case SIMPLE:
                return ThemeVisualStyle.FLAT;
            case MIXED_COLOR:
                return ThemeVisualStyle.BUBBLE;
            case GLASS:
                return ThemeVisualStyle.GLASS;
            case ARCADE:
                return ThemeVisualStyle.ARCADE;
            case PAPER:
                return ThemeVisualStyle.PAPER;
            default:
                return ThemeVisualStyle.CLASSIC;
        }
    }

    private String themeFontFamily() {
        switch (currentThemeVisualStyle()) {
            case ARCADE:
                return "monospace";
            case PAPER:
                return "serif";
            case BUBBLE:
                return "sans-serif-rounded";
            default:
                return "sans";
        }
    }

    private Typeface prominentButtonTypeface() {
        ThemeVisualStyle style = currentThemeVisualStyle();
        if (style == ThemeVisualStyle.ARCADE || style == ThemeVisualStyle.PAPER) {
            return Typeface.create(themeFontFamily(), Typeface.BOLD);
        }
        return Typeface.create("sans-serif-black", Typeface.NORMAL);
    }

    private int themedRadius(int requestedDp) {
        if (requestedDp <= 0) {
            return 0;
        }
        switch (currentThemeVisualStyle()) {
            case FLAT:
                return Math.min(requestedDp, 6);
            case GLASS:
                return Math.max(22, requestedDp + 6);
            case ARCADE:
                return Math.min(requestedDp, 4);
            case PAPER:
                return Math.min(requestedDp, 7);
            case BUBBLE:
                return Math.max(24, requestedDp + 10);
            default:
                return requestedDp;
        }
    }

    private int themedFill(int color) {
        switch (currentThemeVisualStyle()) {
            case GLASS:
                if (color == SURFACE) {
                    return withAlpha(color, darkTheme ? 205 : 180);
                }
                if (color == SURFACE_TINT) {
                    return withAlpha(color, darkTheme ? 178 : 155);
                }
                return color == PRIMARY ? withAlpha(color, 225) : color;
            case ARCADE:
                if (color == SURFACE) {
                    return withAlpha(color, 220);
                }
                if (color == SURFACE_TINT) {
                    return withAlpha(color, 190);
                }
                return color;
            case BUBBLE:
                if (color == SURFACE || color == SURFACE_TINT) {
                    return withAlpha(color, darkTheme ? 226 : 218);
                }
                return color;
            default:
                return color;
        }
    }

    private int themedElevation(int requestedDp) {
        switch (currentThemeVisualStyle()) {
            case FLAT:
            case ARCADE:
            case PAPER:
                return 0;
            case GLASS:
                return requestedDp + 2;
            case BUBBLE:
                return requestedDp + 1;
            default:
                return requestedDp;
        }
    }

    private float cardCornerRadiusFraction() {
        switch (currentThemeVisualStyle()) {
            case FLAT:
                return 0.075f;
            case GLASS:
                return 0.19f;
            case ARCADE:
                return 0.04f;
            case PAPER:
                return 0.07f;
            case BUBBLE:
                return 0.24f;
            default:
                return 0.13f;
        }
    }

    private void applyScreenBackground(View view) {
        GradientDrawable background;
        switch (currentThemeVisualStyle()) {
            case GLASS:
                background = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    darkTheme
                        ? new int[] {
                            Color.rgb(8, 21, 33),
                            Color.rgb(36, 27, 71),
                            Color.rgb(12, 53, 60)
                        }
                        : new int[] {
                            Color.rgb(232, 245, 255),
                            Color.rgb(247, 234, 254),
                            Color.rgb(221, 247, 240)
                        }
                );
                break;
            case ARCADE:
                background = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[] {
                        Color.rgb(3, 7, 18),
                        Color.rgb(24, 3, 46),
                        Color.rgb(0, 28, 37)
                    }
                );
                break;
            case PAPER:
                background = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {
                        Color.rgb(246, 235, 210),
                        Color.rgb(232, 208, 162)
                    }
                );
                break;
            case BUBBLE:
                background = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    darkTheme
                        ? new int[] {
                            Color.rgb(33, 12, 37),
                            Color.rgb(56, 24, 69),
                            Color.rgb(6, 43, 53)
                        }
                        : new int[] {
                            Color.rgb(255, 240, 247),
                            Color.rgb(246, 235, 255),
                            Color.rgb(234, 247, 255)
                        }
                );
                break;
            default:
                background = new GradientDrawable();
                background.setColor(BACKGROUND);
                break;
        }
        view.setBackground(background);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams fixed(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private LinearLayout.LayoutParams weighted(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }

    private LinearLayout.LayoutParams centered(int width, int height) {
        LinearLayout.LayoutParams params = fixed(width, height);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private LinearLayout.LayoutParams margins(
        LinearLayout.LayoutParams params,
        int leftDp,
        int topDp,
        int rightDp,
        int bottomDp
    ) {
        params.setMargins(dp(leftDp), dp(topDp), dp(rightDp), dp(bottomDp));
        return params;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
