package edu.iu.terracotta.security.app.roles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Every /api endpoint states its role with exactly one annotation from this package, and
 * src/test/resources/controller-roles.txt records which. A new endpoint with no role annotation
 * fails here, and so does changing an endpoint's role without updating that file - run this test
 * with -DupdateControllerRoles=true to rewrite it, then review the diff.
 */
class ControllerRoleCoverageTest {

    private static final Path SNAPSHOT = Paths.get("src/test/resources/controller-roles.txt");
    // a role named in a @PreAuthorize expression, e.g. 'INSTRUCTOR'
    private static final Pattern QUOTED_ROLE = Pattern.compile("'([A-Z_]+)'");
    private static final List<Class<? extends Annotation>> ROLE_ANNOTATIONS = List.of(
        InstructorOrHigher.class,
        LearnerOrHigher.class,
        RoleCheckedInHandler.class,
        NoRoleRequired.class
    );

    @Test
    void everyApiEndpointHasExactlyOneRoleAnnotationMatchingTheSnapshot() throws Exception {
        List<String> missingOrAmbiguous = new ArrayList<>();
        List<String> actual = new ArrayList<>();

        for (Class<?> controller : apiControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }

                List<String> roles = ROLE_ANNOTATIONS.stream()
                    .filter(method::isAnnotationPresent)
                    .map(Class::getSimpleName)
                    .toList();
                String endpoint = endpoint(controller, method);

                if (roles.size() != 1) {
                    missingOrAmbiguous.add(endpoint + " has " + roles);
                }

                // method security only intercepts calls it can proxy
                assertTrue(Modifier.isPublic(method.getModifiers()), endpoint + " must be public for its role annotation to be enforced");

                actual.add(endpoint + " -> " + String.join(",", roles));
            }
        }

        assertTrue(missingOrAmbiguous.isEmpty(), "Each /api endpoint needs exactly one role annotation: " + missingOrAmbiguous);

        List<String> sorted = actual.stream().sorted().toList();

        if (Boolean.getBoolean("updateControllerRoles")) {
            Files.write(SNAPSHOT, sorted, StandardCharsets.UTF_8);
        }

        List<String> expected = Files.readAllLines(SNAPSHOT, StandardCharsets.UTF_8).stream()
            .filter(line -> !line.isBlank())
            .toList();

        assertEquals(
            String.join("\n", expected),
            String.join("\n", sorted),
            "Endpoint roles differ from " + SNAPSHOT + " - if the change is intended, rerun with -DupdateControllerRoles=true and review the diff"
        );
    }

    // the annotations name roles in @PreAuthorize strings, which can't reference ApiRole - so a
    // role renamed there would otherwise only show up as endpoints refusing everyone
    @Test
    void roleAnnotationsOnlyNameRolesTheFilterGrants() {
        List<String> granted = Arrays.stream(ApiRole.values()).map(Enum::name).toList();

        for (Class<? extends Annotation> annotation : List.of(InstructorOrHigher.class, LearnerOrHigher.class)) {
            String expression = annotation.getAnnotation(PreAuthorize.class).value();
            Matcher roles = QUOTED_ROLE.matcher(expression);
            int count = 0;

            while (roles.find()) {
                count++;
                assertTrue(granted.contains(roles.group(1)), annotation.getSimpleName() + " names role " + roles.group(1) + ", which ApiRole doesn't grant");
            }

            assertTrue(count > 0, annotation.getSimpleName() + " names no roles: " + expression);
        }
    }

    // the roles these annotations check are only granted by the /api token filter
    // (ApiOAuthProviderProcessingFilter), so outside /api they'd refuse every caller
    @Test
    void noEndpointOutsideApiUsesARoleAnnotation() throws Exception {
        List<String> misplaced = new ArrayList<>();

        for (Class<?> controller : controllers(false)) {
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(InstructorOrHigher.class) || method.isAnnotationPresent(LearnerOrHigher.class)) {
                    misplaced.add(endpoint(controller, method));
                }
            }
        }

        assertTrue(misplaced.isEmpty(), "Role annotations only work under /api, so these would refuse everyone: " + misplaced);
    }

    private static List<Class<?>> apiControllers() throws ClassNotFoundException, IOException {
        return controllers(true);
    }

    private static List<Class<?>> controllers(boolean api) throws ClassNotFoundException, IOException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<Class<?>> controllers = new ArrayList<>();

        for (BeanDefinition definition : scanner.findCandidateComponents("edu.iu.terracotta.controller")) {
            Class<?> controller = Class.forName(definition.getBeanClassName());
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            boolean underApi = mapping != null && Arrays.stream(mapping.path()).anyMatch(path -> path.startsWith("api") || path.startsWith("/api"));

            if (underApi == api) {
                controllers.add(controller);
            }
        }

        return controllers;
    }

    private static String endpoint(Class<?> controller, Method method) {
        return controller.getSimpleName() + "#" + method.getName()
            + Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(",", "(", ")"));
    }

}
