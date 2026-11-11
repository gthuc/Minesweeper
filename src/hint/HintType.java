package hint;

/**
 * Distinguishes proven safe cells, proven mines, and heuristic suggestions.
 */
public enum HintType {
    CERTAIN_SAFE,
    CERTAIN_MINE,
    PROBABLE
}
