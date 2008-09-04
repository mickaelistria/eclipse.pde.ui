/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import junit.framework.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests that the builder correctly reports compatibility problems
 * related to member types.
 * 
 * @since 1.0
 */
public class ClassCompatibilityRestrictionTests extends ClassCompatibilityTests {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/classes/restrictions");

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.classes.restrictions.";
	
	/**
	 * Constructor
	 * @param name
	 */
	public ClassCompatibilityRestrictionTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("restrictions");
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ClassCompatibilityRestrictionTests.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.CLASS_ELEMENT_TYPE,
				IDelta.CHANGED,
				IDelta.RESTRICTIONS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "classcompat";
	}
	
	/**
	 * Tests adding a noextend annotation
	 */
	private void xAddNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoExtend.java");
		// TODO: expecting a problem
//		int[] ids = new int[] {
//			getDefaultProblemId()
//		};
//		setExpectedProblemIds(ids);
//		String[][] args = new String[1][];
//		args[0] = new String[]{"AddNoExtend"};
//		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddNoExtendI() throws Exception {
		xAddNoExtend(true);
	}	
	
	public void testAddNoExtendF() throws Exception {
		xAddNoExtend(false);
	}

	/**
	 * Tests adding a noinstantiate annotation
	 */
	private void xAddNoInstantiate(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddNoInstantiate.java");
		// TODO: expecting a problem
//		int[] ids = new int[] {
//			getDefaultProblemId()
//		};
//		setExpectedProblemIds(ids);
//		String[][] args = new String[1][];
//		args[0] = new String[]{"AddNoInstantiate"};
//		setExpectedMessageArgs(args);
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testAddNoInstantiateI() throws Exception {
		xAddNoInstantiate(true);
	}	
	
	public void testAddNoInstantiateF() throws Exception {
		xAddNoInstantiate(false);
	}	
	
	/**
	 * Tests adding a noextend annotation
	 */
	private void xFinalAddNoExtend(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("FinalAddNoExtend.java");
		// no errors expected
		performCompatibilityTest(filePath, incremental);
	}
	
	public void testFinalAddNoExtendI() throws Exception {
		xFinalAddNoExtend(true);
	}	
	
	public void testFinalAddNoExtendF() throws Exception {
		xFinalAddNoExtend(false);
	}
	
}