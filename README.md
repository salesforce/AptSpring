### Status ###

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7e9fe7e2a2534e9dacddaf15a9fc27e4)](https://www.codacy.com/app/rexhoffman/AptSpring?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=salesforce/AptSpring&amp;utm_campaign=Badge_Grade)
[![codebeat badge](https://codebeat.co/badges/a0528ed4-185e-4ac2-90c5-a93477656a7a)](https://codebeat.co/projects/github-com-salesforce-aptspring-master)

### About AptSpring ###

This project checks that spring beans in your project adhere to certain properties when you add the ```@Verified``` annotation to your ```@Configuration``` class.

#### Duplicate bean name and cycle free ####

Some of those properties include all beans being explicitly named, all names are unique, and all autowiring includes names via ```@Qualified``` annotations.  Cycles are prevented as well as duplicate bean names.  

#### Type Safety ####

Return types of ```@Bean``` methods are verified against ```@Autowired``` inputs to bean methods.

#### Disallows "Interesting" practices ####

No autowiring in to ```@Configuration``` instances.
No non literal static member variables on ```@Configuration``` instances.
No autowiring an unknown list of instances by type... instead encourages the construction of named lists.
No ```@ComponentScan```, instead use ```@Import``` to allow the annotation processor to analyze your graph.

#### Safety ####

By defining the spring graph in a type safe and introspectable manner the relevant parts of a spring graph are encoded in the java type system.  Your code will be more readable, more refactorable, and less error prone even if your app has to scale to thousands of beans.

### Quick Start: Get the goodness. ###

In your pom file, include the version of spring you wish to use (3+ should be fine)

Include these dependencies:

```xml
        <dependency>
            <groupId>com.salesforce.aptspring</groupId>
            <artifactId>AptSpringAPI</artifactId>
            <version>${apt.spring.version}</version>
        </dependency>
        <dependency>
            <groupId>com.salesforce.aptspring</groupId>
            <artifactId>AptSpringProcessor</artifactId>
            <version>${apt.spring.version}</version>
            <scope>provided</scope>
        </dependency>
```

Include the ```@Verified``` annotation on your spring ```@Configuration``` classes.

Use the takari lifecycle and takari-m2e support for best performance and file handling.
You can also configure the APT manually in eclipse if you are not using m2e, but eclipse's default APT handling will not support referencing a ```@Configuration``` class in another eclipse project/maven module.   You would be limited to having on one eclipse project.

The error messages will help clean up your code and keep it clean.

A full list of checks can be found on the javadoc of the ```@Verified``` annotation class.

In short, no cycles, fully defined decoupled spring graphs, with clear entry point for all system properties.

No guarantee you wont have runtime problems.

The guarantee is that those problems will be a lot simpler than the one you would otherwise get out of spring.

### Enforcement ###
Please see [@Verified Annotation](./AptSpringAPI/src/main/java/com/salesforce/aptspring/Verified.java#L35) for a full list of the
constraints the ForceInjectSpringAPT processor enforces, and what it may enforce in the future.
