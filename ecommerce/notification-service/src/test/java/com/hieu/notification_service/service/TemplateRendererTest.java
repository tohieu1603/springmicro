package com.hieu.notification_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link TemplateRenderer} placeholder substitution. No Spring.
 */
@DisplayName("TemplateRenderer (unit)")
class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    @DisplayName("replaces {{ key }} placeholders, tolerating inner whitespace")
    void replacesPlaceholders() {
        var vars = Map.of("name", "Alice", "orderId", "42");

        String out = renderer.render("Hi {{ name }}, order {{orderId}} shipped.", vars);

        assertThat(out).isEqualTo("Hi Alice, order 42 shipped.");
    }

    @Test
    @DisplayName("leaves an unknown placeholder untouched")
    void unknownPlaceholderUntouched() {
        String out = renderer.render("Hello {{ missing }}!", Map.of("name", "Bob"));

        assertThat(out).isEqualTo("Hello {{ missing }}!");
    }

    @Test
    @DisplayName("treats $ and \\ in replacement values literally (no regex group expansion)")
    void literalReplacement() {
        var vars = Map.of("amount", "$5 \\ off");

        String out = renderer.render("Discount: {{ amount }}", vars);

        assertThat(out).isEqualTo("Discount: $5 \\ off");
    }

    @Test
    @DisplayName("returns the template unchanged when vars map is empty")
    void emptyVarsReturnsTemplate() {
        String template = "Nothing {{ here }} changes";

        assertThat(renderer.render(template, Map.of())).isSameAs(template);
    }

    @Test
    @DisplayName("returns null template unchanged")
    void nullTemplate() {
        assertThat(renderer.render(null, Map.of("a", "b"))).isNull();
    }

    @Test
    @DisplayName("returns the template unchanged when vars map is null")
    void nullVarsReturnsTemplate() {
        String template = "Keep {{ x }}";

        assertThat(renderer.render(template, null)).isSameAs(template);
    }

    @Test
    @DisplayName("replaces repeated occurrences of the same placeholder")
    void repeatedPlaceholder() {
        var vars = new HashMap<String, String>();
        vars.put("u", "Sam");

        String out = renderer.render("{{u}} & {{ u }}", vars);

        assertThat(out).isEqualTo("Sam & Sam");
    }
}
