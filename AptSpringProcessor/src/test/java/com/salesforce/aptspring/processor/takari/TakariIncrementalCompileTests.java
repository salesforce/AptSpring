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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.core.SpringVersion;

import com.salesforce.aptspring.Verified;
import com.salesforce.aptspring.processor.SpringAnnotationParser;

import io.takari.maven.testing.TestMavenRuntime;
import io.takari.maven.testing.TestResources;

@RunWith(Parameterized.class)
public class TakariIncrementalCompileTests {

  private static final String PROC = "proc";

  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final TestMavenRuntime maven = new TestMavenRuntime();

  protected final String compilerId;

  public TakariIncrementalCompileTests(String compilerId) {
    this.compilerId = compilerId;
  }

  @Parameters(name = "{0}")
  public static Iterable<Object[]> compilers() {
    return Arrays.<Object[]>asList(new Object[] { "javac" }, new Object[] { "forked-javac" }, new Object[] { "jdt" });
  }

  private Xpp3Dom newParameter(String name, String value) {
    Xpp3Dom child = new Xpp3Dom(name);
    child.setValue(value);
    return child;
  }

  @Test
  public void testBasic() throws Exception {

    // this creates a temporary copy of src/test/projects/basic test project
    File basedir = resources.getBasedir("basic");

    // create MavenProject model for the test project
    MavenProject project = maven.readMavenProject(basedir);

    // add annotation processor to the test project dependencies (i.e.
    // classpath)
    File springContext = new File(Bean.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    maven.newDependency(springContext.getCanonicalFile()).setArtifactId("spring-context").addTo(project);

    File springCore = new File(SpringVersion.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    maven.newDependency(springCore.getCanonicalFile()).setArtifactId("spring-core").addTo(project);

    File springBean = new File(BeanDefinition.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    maven.newDependency(springBean.getCanonicalFile()).setArtifactId("spring-bean").addTo(project).addTo(project);

    File verifiedApi = new File(Verified.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    maven.newDependency(verifiedApi.getCanonicalFile()).setArtifactId("verifiedApi").addTo(project);

    File verifiedImpl = new File(
        SpringAnnotationParser.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    maven.newDependency(verifiedImpl.getCanonicalFile()).setArtifactId("verifiedProcessor").addTo(project);

    MavenSession session = maven.newMavenSession(project);

    // run java compiler with annotation processing enabled
    maven.executeMojo(session, project, "compile", newParameter("compilerId", compilerId), newParameter(PROC, PROC));
    File compiledClasses = new File(basedir, "target/classes");
    File generatedSources = new File(basedir, "target/generated-sources/annotations");

    File configurationSourceFile = (new File(generatedSources,
        "com/salesforce/aptspring/ComputerHardwareConfiguration_forceInjectData.java"));
    assertThat(configurationSourceFile).exists().canRead();

    File configurationClass = (new File(compiledClasses,
        "com/salesforce/aptspring/ComputerHardwareConfiguration_forceInjectData.class"));
    assertThat(configurationClass).exists().canRead();

    maven.executeMojo(session, project, "testCompile", newParameter("compilerId", compilerId),
        newParameter(PROC, PROC));
    File compiledTestClasses = new File(basedir, "target/test-classes");
    File configurationTestClass = (new File(compiledTestClasses,
        "com/salesforce/aptspring/RootApplicationConfiguration_forceInjectData.class"));
    assertThat(configurationTestClass).exists().canRead();
  }
}
