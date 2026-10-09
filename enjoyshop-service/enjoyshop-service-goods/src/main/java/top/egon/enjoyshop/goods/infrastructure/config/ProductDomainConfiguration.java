package top.egon.enjoyshop.goods.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.enjoyshop.goods.domain.goods.ProductLifecycleComponent;

/** Technical assembly keeps the lifecycle Specification itself framework-independent. */
@Configuration(value = "productDomainConfiguration", proxyBeanMethods = false)
@Slf4j
public class ProductDomainConfiguration {
    @Bean("productLifecycleComponent")
    ProductLifecycleComponent productLifecycleComponent() { return new ProductLifecycleComponent(); }
}
