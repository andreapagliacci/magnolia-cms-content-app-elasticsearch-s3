/*
 *
 */
package com.whitelabel.app.event;

import java.io.Serializable;

import com.whitelabel.app.generic.ui.table.RowId;

/**
 * The Interface RowIdChangeEvent.
 */
public interface RowIdChangeEvent extends Serializable {

	/**
	 * Gets the old row id.
	 *
	 * @return the old row id
	 */
	public RowId getOldRowId();

	/**
	 * Gets the new row id.
	 *
	 * @return the new row id
	 */
	public RowId getNewRowId();
}
