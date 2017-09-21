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
 * This marker annotation may only be used on Spring @Configuration and @Component classes.
 * Combined with the AptSpringProcessor annotation processor to prevent errors from creeping in to your spring graph!
 * <p>
 * The APT parser will generate errors if any of the following restrictions are violated:
 * </p>
 * <p>
 * Restrictions on @Configuration class definitions are:
 * </p>
 * <ul>
 *  <li>All methods on an @Configuration class are @Bean methods.</li>
 *  <li>The @Configuration class has a public no-parameter constructor.</li>
 *  <li>The @Configuration class must be the top level class in a the java file.</li>
 *  <li>No uses of @ComponentScan on @Configuration classes, instead use @Import</li>
 *  <li>All fields on the @Configuration annotation must be "private static final {Type} {name} = {LITERAL_VALUE}";</li>
 * </ul>
 * <p>
 * Bean Methods signatures have the following restrictions:
 * </p>
 * <ul>
 *  <li>@Bean annotations define at least one name for the bean.</li>
 *  <li>@Bean methods return an object (not void, not un-boxed values)</li>
 *  <li>@Bean methods are public</li>
 *  <li>@Bean methods are not static, final, native, or abstract</li>
 *  <li>@Bean method parameters must have a @Qualifier of the bean name they expect as input or an @Value 
 *  of the property they expect spring to inject (system property or other configuration).</li>
 * </ul>
 * <p>
 * With the above restrictions in place, the following checks become feasible, and also occur at compile time:
 * </p>
 * <ul>
 *  <li>Cycles in @Configuration @Import class</li>
 *  <li>Cycles in @Bean dependency definitions</li>
 *  <li>Duplicate @Bean names in the directed acyclic graph of @Configuration classes</li>
 *  <li>Detect missing beans by their name reference</li>
 *  <li>Allow @Configuration classes to declare beans they expect (these are the only missing beans a graph may have),
 *   see {@link Verified#expectedBeans()}</li>
 *  <li>Detect when one one of those expected beans is not used in the graph and flag it as an error.</li>
 *  <li>Detect missing @Configuration classes from an @Import</li>
 *  <li>only @Verified @Configuration classes may be @Imported in to a @Verified @Configuration class</li>
 *  <li>Detect when the declared output type of an @Bean method does not satisfy the type expected by
 *   uses of that @Bean method.</li>
 *  <li>Ensure expect beans passed in types are of compatible types (few hours)</li>
 * </ul>
 * <p>
 * Restrictions on the use of @Value annotations.
 * </p>
 * <ul>
 *   <li>@Value may only be used on method parameters.</li>  
 *   <li>@Value parameters and @Qualifier parameters may not be mixed on the same @Bean method</li>
 *   <li>It is highly encouraged that @Bean methods with @Value parameters only have one parameters (more are allowed)</li>
 *   <li>@Value annotations may only be used on @Configuration classes that are marked as {@link Verified#root()} = true</li>
 *   <li>No @Configuration class may mark an {@link Verified#root()} = true @Configuration class as an @Import</li>
 *   <li>Instead @Import @Configuration files should use {@link Verified#expectedBeans()} to expect beans that will be required
 *   at runtime that will contain they properties they need to run</li>
 *   <li>This is to encourage re-usable, testable @Configuration classes, and to consolidate where all system properties are read
 *   in to one location, namely the {@link Verified#root()} = true @Configuration class</li>
 * </ul>
 * <p>
 * Restrictions on @Component classes that are not @Configuration classes.
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
 *   violates the Liskov Substitution Principle and makes code more coupled and error prone, instead only inject what you
 *   need at runtime.</li>
 * </ul>
 * <p>
 * Work I'd like to finish soon but likely wont have time:
 * </p>
 * <ul>
 *  <li>Prune unneeded data from the persisted storage of @Configuration data allowing for faster cycle detect.</li>
 * </ul>
 * <p>
 * The processing is incremental, meaning that a json file is generated in to a target directory and read when available.
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
