package net.seesharpsoft.spring.suite.boot;

import net.seesharpsoft.UnhandledSwitchCaseException;
import net.seesharpsoft.spring.data.jpa.SpecificationConverter;
import net.seesharpsoft.spring.data.jpa.expression.Dialects;
import net.seesharpsoft.spring.data.web.OffsetLimitPageHandlerMethodArgumentResolver;
import net.seesharpsoft.spring.data.web.SpecificationHandlerMethodArgumentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ConfigurationProperties.class)
public class SuiteConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    ConfigurationProperties properties;
    
    @Bean
    @Conditional(SpecificationConverterEnabledCondition.class)
    Converter<String, Specification> specificationConverter(@Lazy ConversionService conversionService) {
        switch (properties.getExpressionDialect()) {
            case OData:
                return new SpecificationConverter(Dialects.ODATA, conversionService);
            case Default:
                return new SpecificationConverter(Dialects.JAVA, conversionService);
            case None:
                throw new UnsupportedOperationException();
            default:
                throw new UnhandledSwitchCaseException(properties.getExpressionDialect());
        }
    }
    
    @Bean
    @ConditionalOnProperty(name = ConfigurationProperties.WEB_RESOLVER_SPECIFICATION)
    SpecificationHandlerMethodArgumentResolver specificationHandlerMethodArgumentResolver(Converter<String, Specification> specificationConverter) {
        return new SpecificationHandlerMethodArgumentResolver(specificationConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = ConfigurationProperties.WEB_RESOLVER_LIMIT_OFFSET)
    SortHandlerMethodArgumentResolver sortHandlerMethodArgumentResolver() {
        return new SortHandlerMethodArgumentResolver();
    }
    
    @Bean
    @ConditionalOnProperty(name = ConfigurationProperties.WEB_RESOLVER_LIMIT_OFFSET)
    OffsetLimitPageHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver(@Lazy SortHandlerMethodArgumentResolver sortHandlerMethodArgumentResolver) {
        return new OffsetLimitPageHandlerMethodArgumentResolver(sortHandlerMethodArgumentResolver);
    }
}
