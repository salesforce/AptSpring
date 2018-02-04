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
package com.salesforce.aptspring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This marker annotation may only be used on Spring @Bean LITE and @Component classes.
 * Combined with the AptSpringProcessor annotation processor to prevent errors from creeping in to your spring graph!
 * <p>
 * The APT parser will generate errors if any of the following restrictions are violated:
 * </p>
 * <p>
 * Restrictions on @Bean Lite mode class definitions are:
 * </p>
 * <ul>
 *  <li>All methods on the class are @Bean methods.</li>
 *  <li>The class has a public no-parameter constructor.</li>
 *  <li>The class must be the top level class in a the java file.</li>
 *  <li>No uses of @ComponentScan on the classes, instead use @Import</li>
 *  <li>All fields on the class must be {@code "private static final 'Type' 'name' = 'LITERAL_VALUE'";}</li>
 *  <li>The class must not be annotated with @Component or @Configuration</li>
 * </ul>
 * <p>
 * Bean Methods signatures have the following restrictions:
 * </p>
 * <ul>
 *  <li>@Bean annotations define at least one name for the bean.</li>
 *  <li>@Bean methods return an object (not void, not un-boxed values)</li>
 *  <li>@Bean methods are public</li>
 *  <li>@Bean methods are not static, final, native, or abstract (support may be added for BeanFactoryPostProcessors)</li>
 *  <li>@Bean method parameters must have a @Qualifier of the bean name they expect as input or an @Value 
 *  of the property they expect spring to inject (system property or other configuration).</li>
 * </ul>
 * <p>
 * With the above restrictions in place, the following checks become feasible, and also occur at compile time:
 * </p>
 * <ul>
 *  <li>Cycles in @Import class</li>
 *  <li>Cycles in @Bean or @Component dependency definitions</li>
 *  <li>Duplicate names in the directed acyclic graph of @Bean or @Component defined instances</li>
 *  <li>Detect missing beans by their name reference</li>
 *  <li>Allow classes to declare beans they expect (these are the only missing beans a graph may have),
 *   see {@link Verified#expectedBeans()}</li>
 *  <li>Detect when one of those expected beans is not used in the graph and flag it as an error.</li>
 *  <li>Detect missing classes from an @Import</li>
 *  <li>only @Verified classes may be @Imported in to a @Verified class</li>
 *  <li>Detect when the declared output type of an @Bean method does not satisfy the type expected by
 *   uses of bean's qualified name in an injection point.</li>
 *  <li>Ensure expected beans passed in types are of compatible types.</li>
 * </ul>
 * <p>
 * Restrictions on the use of @Value annotations.
 * </p>
 * <ul>
 *   <li>@Value may only be used on @Bean method parameters or @Component constructors.</li>  
 *   <li>@Value parameters and @Qualifier parameters may not be mixed on the same @Bean method or @Component constructor</li>
 *   <li>It is highly encouraged that @Bean methods or @Component constructors with @Value parameters only have one parameters (more are allowed)</li>
 *   <li>@Value annotations may only be used on classes that are marked as {@link Verified#root()} = true</li>
 *   <li>No class may mark a {@link Verified#root()} = true class as an @Import</li>
 *   <li>Instead @Import files should use {@link Verified#expectedBeans()} to expect beans that will be required
 *   at runtime that will contain the properties they need to run</li>
 *   <li>This is to encourage re-usable, testable @Bean lite classes, and to consolidate where all system properties are read
 *   in to one location, namely the {@link Verified#root()} = true class</li>
 * </ul>
 * <p>
 * Restrictions on @Component classes:
 * </p>
 * <ul>
 *   <li>May only have one constructor, or one constructor marked @Autowired if it has multiple constructors.</li>
 *   <li>All fields must be private static final literals, of private final variables set during instance construction.</li>  
 * </ul>  
 * <p>
 * MetaAnnotations are supported.  @AliasFor will work as expected.
 * </p>
 * <p>
 * Future checks to be added to the project will include but are not limited to:
 * </p>
 * <ul>
 *   <li>No usage of banned classes {classes that contain static references to the Spring Context or system properties}</li>
 *   <li>don't call any methods on the injected dependencies to a bean method in the bean method or object constructor, this
 *   violates the https://en.wikipedia.org/wiki/Law_of_Demeter and makes code more coupled and error prone, instead only inject what you
 *   need at runtime.</li>
 * </ul>
 * <p>
 * Work I'd like to finish soon but likely wont have time:
 * </p>
 * <ul>
 *  <li>Prune unneeded data from the persisted storage of @Bean lite classes data allowing for faster cycle detect.</li>
 * </ul>
 * <p>
 * The processing is incremental, meaning that a file is generated to a target directory and read when available.
 * This allows for decent performance on large graphs touching hundreds of files by preventing duplicate work.  SHA-256 digests
 * are used to make sure no one gets clever and tries to swap out a jar underneath the working application.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE })
public @interface Verified {
  
  /**
   * List of beans that are expected to be provided by an external Configuration that imports the Configuration class annotated with
   * the list of expected beans.
   * @return the list of expected beans.
   */
  String[] expectedBeans() default {};
  
  /**
   * Only @Configuration files that are marked as "root" nodes may have @Value annotations used in their @Configuration. 
   * Root @Configuration nodes may not be imported by other @Configuration nodes.
   * @return whether or not this is a root @Configuration bean.
   */
  boolean root() default false;
}
