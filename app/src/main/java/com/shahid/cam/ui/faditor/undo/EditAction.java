package com.shahid.cam.ui.shahideditor.undo;

import androidx.annotation.NonNull;

/**
 * Represents a single reversible edit action in the ShahidEditor editor.
 *
 * <p>Each action knows how to undo and redo itself, enabling
 * full version-control-like history for all editing operations.</p>
 */
public interface EditAction {

    /**
     * Execute the action (or re-execute for redo).
     */
    void execute();

    /**
     * Reverse the action.
     */
    void undo();

    /**
     * Human-readable description for debugging/logging.
     */
    @NonNull
    String getDescription();
}
