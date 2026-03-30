package com.github.hrobasti.permaclicker.common.facade;

import com.github.hrobasti.turtlelib.MessageService.MessageService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Single facade for localized message rendering delegated to TurtleLib MessageService.
 */
public final class MessageFacade {
    private final MessageService delegate;

    public MessageFacade(Class<?> resourceAnchor, Path langDir, String defaultPrefixRaw, String defaultPrefixLabel) {
        this.delegate = new MessageService(resourceAnchor, langDir, defaultPrefixRaw, defaultPrefixLabel);
    }

    public static List<String> getBundledLocales(Class<?> resourceAnchor) {
        return MessageService.getBundledLocales(resourceAnchor);
    }

    public void load(String locale) {
        delegate.load(locale);
    }

    public List<String> syncLocaleFile(String locale) {
        return delegate.syncLocaleFile(locale);
    }

    public String getLanguage() {
        return delegate.getLanguage();
    }

    public void setPrefixLabel(String label) {
        delegate.setPrefixLabel(label);
    }

    public String component(String key) {
        return delegate.component(key);
    }

    public String format(String key, Map<String, String> replacements) {
        return delegate.format(key, replacements);
    }

    public String plain(String key) {
        return delegate.plain(key);
    }

    public String plain(String key, Map<String, String> replacements) {
        return delegate.plain(key, replacements);
    }

    public void reload(Properties config) {
        delegate.reload(config);
    }

    public void reload(Map<String, ?> config) {
        delegate.reload(config);
    }
}
