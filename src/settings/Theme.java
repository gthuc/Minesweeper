package settings;

import factory.ICellFactory;
import factory.StandardBoardFactory;
import factory.ThemeBoardFactory;

public enum Theme {
    CLASSIC,
    THEME;

    public ICellFactory createFactory() {
        return this == THEME ? new ThemeBoardFactory() : new StandardBoardFactory();
    }

    public Theme toggled() {
        return this == CLASSIC ? THEME : CLASSIC;
    }
}
