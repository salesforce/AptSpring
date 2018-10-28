/*
 * Copyright © 2017, Salesforce.com, Inc
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
package com.salesforce.apt.graph.model.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.salesforce.apt.graph.model.DefinitionModel;
import com.salesforce.apt.graph.model.errors.ErrorType;
import com.salesforce.apt.graph.model.storage.classpath.ClasspathUrlResourceLoader;
import com.salesforce.apt.graph.model.storage.classpath.FileStore;
import com.salesforce.apt.graph.model.storage.classpath.GsonDefinitionModelStore;
import com.salesforce.apt.graph.model.storage.classpath.TestFileStore;
import com.salesforce.apt.graph.processing.DefinitionContentInspector;
import com.salesforce.apt.graph.test.cycle.definitions.TestErrorListener;
import com.salesforce.apt.graph.types.impl.ReflectionAssignabilityUtils;

public class TestResourceLoader {
  
  private static final String NO_PERMS = "---------";


  private static final String SIMPLE_MODEL = "simple.Model";

 
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private static DefinitionModelStore getDefinitionModelStore(File root) {
    TestFileStore fileStore = new TestFileStore(root);
    ResourceLoader loader = new ClasspathUrlResourceLoader(root);
    return new GsonDefinitionModelStore(loader, fileStore);
  }
  
  @Test
  public void testSimpleLoadNotFound() {
    ResourceLoader loader = new ClasspathUrlResourceLoader();
    List<Resource> resources = loader.getEntries("notFound");
    assertThat(resources).hasSize(0);
  }
  
 
  
  @Test
  public void testSimpleRoundTrip() throws IOException {
    File root = testFolder.newFolder();
    DefinitionModelStore definitionStore = getDefinitionModelStore(root);
    DefinitionModel model = new DefinitionModel(SIMPLE_MODEL);
    model.addDependencyNameToSha256("test1", "ABCDEF1234567890");
    definitionStore.store(model);
    List<DefinitionModel> foundModels = definitionStore.lookup(SIMPLE_MODEL);
    assertThat(foundModels).hasSize(1);
    assertThat(foundModels.get(0).getSha256()).isNotNull();
    assertThat(foundModels.get(0).getIdentity()).isEqualTo(model.getIdentity());
    assertThat(foundModels.get(0).getSha256()).isEqualTo(model.getSha256());
    assertThat(foundModels.get(0).getDependencyNameToSha256()).containsEntry("test1", "ABCDEF1234567890");
    cleanUp(root);
  }
  
  
  @Test
  public void testSha256AndSourceLocationAreNotStored() throws IOException {
    File root = testFolder.newFolder();
    TestFileStore fileStore = new TestFileStore(root);
    ClassLoader classLoader = new URLClassLoader(new URL[] {root.toURI().toURL()},
        Thread.currentThread().getContextClassLoader());
    ResourceLoader loader = new ClasspathUrlResourceLoader(classLoader);
    DefinitionModelStore definitionStore = new GsonDefinitionModelStore(loader, fileStore);
    DefinitionModel model = new DefinitionModel(SIMPLE_MODEL);
    definitionStore.store(model);

    List<Resource> resources = loader.getEntries(SIMPLE_MODEL);
    assertThat(resources).hasSize(1);
    assertThat(toString(resources.get(0).getInputStream()))
       .doesNotContain("sha256")
       .doesNotContain("sourceLocation")
       .contains("\"elementLocation\": \"simple.Model\"");
    cleanUp(root);
  }
  
  /**
   * Doesn't appear to work in docker, will never work in windows.
   * @throws IOException if any of the setup fails
   */
  @Test
  public void testBrokenEnvironment() throws IOException {
    File root = testFolder.newFolder();
    File workingDir = new File(root, FileStore.STANDARD.getPath() + File.separator);
    DefinitionModel model = new DefinitionModel(SIMPLE_MODEL);
    Files.createDirectories(workingDir.toPath(),
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(NO_PERMS)));
    if (PosixFilePermissions.fromString(NO_PERMS).equals(Files.getPosixFilePermissions(workingDir.toPath()))) {
      TestFileStore fileStore = new TestFileStore(root);
      assertThatThrownBy(() -> fileStore.store(model)).isInstanceOf(IOException.class)
        .hasMessage("could not write directories for model storage");
      Files.setPosixFilePermissions(workingDir.toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
    }
    cleanUp(root);
  }
  
  /**
   * Doesn't appear to work in docker, will never work in windows.
   * @throws IOException if any of the setup fails
   */
  @Test
  public void failedStorage() throws IOException {
    File root = testFolder.newFolder();
    File workingDir = new File(root, FileStore.STANDARD.getPath() + File.separator);
    Files.createDirectories(workingDir.toPath(),
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(NO_PERMS)));
    
    if (PosixFilePermissions.fromString(NO_PERMS).equals(Files.getPosixFilePermissions(workingDir.toPath()))) {
      DefinitionModel model1 = new DefinitionModel(SIMPLE_MODEL);
      TestErrorListener el = new TestErrorListener();
  
      TestFileStore fileStore = new TestFileStore(workingDir);
      ResourceLoader loader = new ClasspathUrlResourceLoader(workingDir);
      DefinitionModelStore store = new GsonDefinitionModelStore(loader, fileStore);
      Set<DefinitionModel> models = new HashSet<>();
      models.add(model1);
      new DefinitionContentInspector()
        .inspectDefinitionGraph(models, el, new ReflectionAssignabilityUtils(), store);
      assertThat(el.getErrors()).hasSize(1);
      assertThat(el.getErrors().get(0).getMessage()).isEqualTo(ErrorType.COULD_NOT_STORE);
      Files.setPosixFilePermissions(workingDir.toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
    }
    cleanUp(root);
  }
  
  
  private String toString(InputStream inputStream) throws IOException {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
      result.write(buffer, 0, length);
    }
    result.flush();
    result.close();
    return result.toString(StandardCharsets.UTF_8.name());
  }

  private void cleanUp(File file) throws IOException {    
    Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }
      
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
