package com.hieu.notification_service.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal template renderer that replaces {@code {{ key }}} placeholders
 * with values from a supplied map. No external dependency needed for v1.
 */
@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");

    /** Replace all {@code {{ key }}} tokens in {@code template} using {@code vars}. */
    public String render(String template, Map<String, String> vars) {
        if (template == null || vars == null || vars.isEmpty()) return template;
        Matcher m = PLACEHOLDER.matcher(template);
        var sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String replacement = vars.getOrDefault(key, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
