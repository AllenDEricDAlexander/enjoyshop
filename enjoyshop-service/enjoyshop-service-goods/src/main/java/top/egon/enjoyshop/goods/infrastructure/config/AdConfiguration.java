package top.egon.enjoyshop.goods.infrastructure.config;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.enjoyshop.goods.domain.ad.AdEligibilitySpecification;

@Configuration(value = "adConfiguration", proxyBeanMethods = false)
@EnableConfigurationProperties(AdProperties.class)
public class AdConfiguration {

    private static final String AD_PROPERTIES_BEAN =
            "app.ad-top.egon.enjoyshop.goods.infrastructure.config.AdProperties";

    @Bean("adClock")
    public Clock adClock() {
        return Clock.systemUTC();
    }

    @Bean("adEligibilitySpecification")
    public AdEligibilitySpecification adEligibilitySpecification() {
        return new AdEligibilitySpecification();
    }

    @Bean("adAllowedPositions")
    public List<String> adAllowedPositions(@Qualifier(AD_PROPERTIES_BEAN) AdProperties properties) {
        return List.copyOf(properties.getAllowedPositions());
    }

    @Bean("adImagePublicBase")
    public URI adImagePublicBase(@Value("${app.object-storage.public-url-base:}") String publicUrlBase) {
        return publicUrlBase == null || publicUrlBase.isBlank() ? URI.create("") : URI.create(publicUrlBase.trim());
    }
}
