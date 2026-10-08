package net.kroet.permaclicker.common.update;

/**
 * Newest release found on one platform; {@code url} may be null.
 */
public record ProviderRelease(String version, String url) {
}
