package factory;

import model.CellModel;

/**
 * Abstracts cell creation so board logic does not depend on a concrete cell theme.
 */
public interface ICellFactory {
    /**
     * Creates a new cell in its initial hidden, unflagged state.
     */
    CellModel createCell();
}
