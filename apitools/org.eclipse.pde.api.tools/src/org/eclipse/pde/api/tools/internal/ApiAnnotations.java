/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Base implementation of the {@linkplain IApiAnnotations} interface
 * 
 * @since 1.0.0
 */
public class ApiAnnotations implements IApiAnnotations {

	public static final int VISIBILITY_MASK = 0x000F;
	public static final int RESTRICTIONS_MASK = 0x01F0;
	public static final int ADDED_PROFILE_MASK = 0xFFE00;
	public static final int REMOVED_PROFILE_MASK = 0x7FF00000;
	public static final int OFFSET_VISIBILITY = 0;
	public static final int OFFSET_RESTRICTIONS = 4;
	public static final int OFFSET_ADDED_PROFILE = 9;
	public static final int OFFSET_REMOVED_PROFILE = 20;

	private int bits;
	
	/**
	 * Constructs API annotations.
	 * 
	 * @param visibility the visibility of an element. See {@linkplain VisibilityModifiers} for visibility constants
	 * @param restrictions the restrictions for an element. See {@linkplain RestrictionModifiers} for restriction kind constants
	 */
	public ApiAnnotations(int visibility, int restrictions, int addedProfile, int removedProfile) {
		this.bits = (visibility << OFFSET_VISIBILITY)
					| (restrictions << OFFSET_RESTRICTIONS)
					| (addedProfile  << OFFSET_ADDED_PROFILE)
					| (removedProfile  << OFFSET_REMOVED_PROFILE);
	}
	
	public ApiAnnotations(int visibility, int restrictions) {
		this.bits = (visibility << OFFSET_VISIBILITY)
			| (restrictions << OFFSET_RESTRICTIONS);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getRestrictions()
	 */
	public int getRestrictions() {
		return (this.bits & RESTRICTIONS_MASK) >> OFFSET_RESTRICTIONS;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getVisibility()
	 */
	public int getVisibility() {
		return (this.bits & VISIBILITY_MASK) >> OFFSET_VISIBILITY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getAddedProfile()
	 */
	public int getAddedProfile() {
		return (this.bits & ADDED_PROFILE_MASK) >> OFFSET_ADDED_PROFILE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiAnnotations#getRemovedProfile()
	 */
	public int getRemovedProfile() {
		return (this.bits & REMOVED_PROFILE_MASK) >> OFFSET_REMOVED_PROFILE;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		String visibility = null;
		switch (getVisibility()) {
			case VisibilityModifiers.API:
				visibility = "API"; //$NON-NLS-1$
				break;
			case VisibilityModifiers.SPI:
				visibility = "SPI"; //$NON-NLS-1$
				break;
			case VisibilityModifiers.PRIVATE_PERMISSIBLE:
				visibility = "PRIVATE PERMISSIBLE"; //$NON-NLS-1$
				break;
			case VisibilityModifiers.PRIVATE:
				visibility = "PRIVATE"; //$NON-NLS-1$
				break;
			case 0:
				visibility = "INHERITED"; //$NON-NLS-1$
				break;
			default:
				visibility = "<unknown visibility>"; //$NON-NLS-1$
				break;
		}
		buffer.append(visibility);
		buffer.append(" / "); //$NON-NLS-1$
		int restrictions = getRestrictions();
		buffer.append(Util.getRestrictionKind(restrictions));
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof ApiAnnotations) {
			ApiAnnotations desc = (ApiAnnotations) obj;
			return
				this.bits == desc.bits;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.bits;
	}
	
}
