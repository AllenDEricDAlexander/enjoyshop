package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("adUrlValidator")
@Slf4j
@RequiredArgsConstructor
public class AdUrlValidator implements ConstraintValidator<ValidAdUrl, String> {

    private static final Set<String> SENSITIVE_QUERY_PARTS = Set.of(
            "token", "secret", "password", "credential", "authorization", "signature", "auth",
            "accesskey", "access_key", "access-key", "sessiontoken", "session_token");

    @Qualifier("adImagePublicBase")
    private final URI imagePublicBase;

    private boolean image;

    @Override
    public void initialize(ValidAdUrl annotation) {
        image = annotation.image();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        int maxLength = image ? 512 : 1024;
        if (value.length() > maxLength || value.codePoints().anyMatch(Character::isISOControl)) {
            return false;
        }
        URI candidate;
        try {
            candidate = new URI(value);
        } catch (URISyntaxException invalidUri) {
            return false;
        }
        if (candidate.getUserInfo() != null || containsSensitiveQuery(candidate.getRawQuery())
                || hasUnsafePath(candidate)) {
            return false;
        }
        return image ? imageIsWithinConfiguredBase(candidate) : targetIsAllowed(candidate, value);
    }

    private boolean imageIsWithinConfiguredBase(URI candidate) {
        if (imagePublicBase == null || !isHttps(candidate) || !isHttps(imagePublicBase)
                || imagePublicBase.getUserInfo() != null || imagePublicBase.getRawQuery() != null
                || imagePublicBase.getRawFragment() != null || hasUnsafePath(imagePublicBase)
                || !candidate.getHost().equalsIgnoreCase(imagePublicBase.getHost())
                || effectivePort(candidate) != effectivePort(imagePublicBase)) {
            return false;
        }
        String basePath = imagePublicBase.getPath();
        String candidatePath = candidate.getPath();
        if (basePath == null || candidatePath == null) {
            return false;
        }
        String prefix = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        return candidatePath.length() > prefix.length() && candidatePath.startsWith(prefix + "/");
    }

    private static boolean targetIsAllowed(URI candidate, String rawValue) {
        if (candidate.isAbsolute()) {
            return isHttps(candidate);
        }
        String rawPath = candidate.getRawPath();
        return candidate.getRawAuthority() == null && rawPath != null
                && rawValue.startsWith("/") && !rawValue.startsWith("//")
                && !rawPath.startsWith("//");
    }

    private static boolean isHttps(URI uri) {
        return uri.isAbsolute() && "https".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null && uri.getRawAuthority() != null;
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : 443;
    }

    private static boolean hasUnsafePath(URI uri) {
        String rawPath = uri.getRawPath();
        String path = uri.getPath();
        if (rawPath == null || path == null || rawPath.toLowerCase(Locale.ROOT).contains("%2f")
                || rawPath.toLowerCase(Locale.ROOT).contains("%5c") || path.contains("\\")) {
            return true;
        }
        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSensitiveQuery(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        for (String parameter : query.split("&")) {
            String name = parameter.split("=", 2)[0];
            String decodedName;
            try {
                decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            } catch (IllegalArgumentException invalidEncoding) {
                return true;
            }
            if (SENSITIVE_QUERY_PARTS.stream().anyMatch(decodedName::contains)) {
                return true;
            }
        }
        return false;
    }
}
