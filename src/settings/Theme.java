package settings;

import factory.ICellFactory;
import factory.StandardBoardFactory;
import factory.ThemeBoardFactory;

/**
 * Maps visual themes to cell factories and supports switching between them.
 */
public enum Theme {
    CLASSIC,
    THEME;

    /**
     * Returns the cell factory associated with this theme.
     */
    public ICellFactory createFactory() {
        return this == THEME ? new ThemeBoardFactory() : new StandardBoardFactory();
    }

    public Theme toggled() {
        return this == CLASSIC ? THEME : CLASSIC;
    }
}
