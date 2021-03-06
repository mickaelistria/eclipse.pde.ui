/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.tags;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;


/**
 * Tests the builder to make sure it correctly finds and reports
 * unsupported tag usage.
 * 
 * @since 1.0
 */
public abstract class TagTest extends ApiBuilderTest {

	protected static IPath WORKSPACE_PATH = new Path("tagproject/src/a/b/c");
	protected static IPath WORKSPACE_PATH_DEFAULT = new Path("tagproject/src");
	
	/**
	 * Constructor
	 */
	public TagTest(String name) {
		super(name);
	}
	
	/**
	 * Sets the message arguments we are expecting for the given test, the number of times denoted
	 * by count
	 * @param tagname
	 * @param context
	 * @param count
	 */
	protected void setExpectedMessageArgs(String tagname, String context, int count) {
		String[][] args = new String[count][];
		for(int i = 0; i < count; i++) {
			args[i] = new String[] {tagname, context};
		}
		setExpectedMessageArgs(args);
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			InvalidClassTagTests.class,
			ValidClassTagTests.class,
			InvalidInterfaceTagTests.class,
			ValidInterfaceTagTests.class,
			InvalidFieldTagTests.class,
			ValidFieldTagTests.class,
			InvalidMethodTagTests.class,
			ValidMethodTagTests.class,
			InvalidEnumTagTests.class,
			InvalidAnnotationTagTests.class,
			InvalidDuplicateTagsTests.class
		};
		return classes;
	}
	
	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder tests...
		Class[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite", new Class[0]);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null, new Object[0]);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				continue;
			}
			suite.addTest((Test) test);
		}
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(TagTest.class.getName());
		collectTests(suite);
		return suite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		//only care about unsupported tags
		enableUnsupportedTagOptions(true);
		
		//disable the rest
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}
	
	/**
	 * Returns an array composed only of the specified number of {@link #PROBLEM_ID}
	 * @param problemcount
	 * @return an array of {@link #PROBLEM_ID} of the specified size, or an empty array if the specified
	 * size is < 1
	 */
	protected int[] getDefaultProblemSet(int problemcount) {
		if(problemcount < 1) {
			return new int[0];
		}
		int[] array = new int[problemcount];
		int defaultproblem = getDefaultProblemId();
		for(int i = 0; i < problemcount; i++) {
			array[i] = defaultproblem;
		}
		return array;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return new Path("tags");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "tagproject";
	}
	
	/**
	 * Deploys a build test for API Javadoc tags using the given source file,
	 * looking for problems specified from {@link #getExpectedProblemIds()()}
	 * @param sourcename
	 * @param incremental if an incremental build should take place
	 * @param usedefault if the default package should be used or not
	 */
	protected void deployTagTest(String sourcename, boolean incremental, boolean usedefault) {
		try {
			IPath path = WORKSPACE_PATH.append(sourcename);
			if(usedefault) {
				path = WORKSPACE_PATH_DEFAULT.append(sourcename);
			}
			createWorkspaceFile(path, TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(sourcename));
			if(incremental) {
				incrementalBuild();
			}
			else {
				fullBuild();
			}
			expectingNoJDTProblemsFor(path);
			ApiProblem[] problems = getEnv().getProblemsFor(path, null);
			assertProblems(problems);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
			env.setRevertSourcePath(null);
		}
		super.setUp();
		IProject project = getEnv().getWorkspace().getRoot().getProject(getTestingProjectName());
		if (!project.exists()) {
			// populate the workspace with initial plug-ins/projects
			createExistingProjects("tagprojects", true, true, false);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(false);
		}
	}
}
