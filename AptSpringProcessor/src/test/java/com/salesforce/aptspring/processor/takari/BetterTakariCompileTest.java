/*
 * Copyright © 2017, Saleforce.com, Inc
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.aptspring.processor.takari;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

/**
 * TODO:  OMG:  FIXME:
 * This is actually busted as it runs against the last packaged version of the jar.....
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.3.9"})
@SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
public class BetterTakariCompileTest {

  @Rule
  public final TestResources resources = new TestResources();
  
  public final MavenRuntime maven;

  public BetterTakariCompileTest(MavenRuntimeBuilder mavenBuilder) throws Exception {
    this.maven = mavenBuilder.withCliOptions("-B", "-U", "-e").build();
  }

  @Test
  public void testPackageMultimodule() throws Exception {
    File basedir = resources.getBasedir("basic");
    maven.forProject(basedir)
        .execute("package") //
        .assertErrorFreeLog();

    TestResources.assertFilesPresent(basedir,
        "target/classes/com/salesforce/aptspring/ComputerHardwareConfiguration_forceInjectData.class");
    TestResources.assertFilesPresent(basedir,
        "target/generated-sources/annotations/com/salesforce/aptspring/ComputerHardwareConfiguration_forceInjectData.java");
    TestResources.assertFilesPresent(basedir,
        "target/test-classes/com/salesforce/aptspring/RootApplicationConfiguration_forceInjectData.class");
    TestResources.assertFilesPresent(basedir,
        "target/generated-test-sources/test-annotations/com/salesforce/aptspring/RootApplicationConfiguration_forceInjectData.java");
  }
}