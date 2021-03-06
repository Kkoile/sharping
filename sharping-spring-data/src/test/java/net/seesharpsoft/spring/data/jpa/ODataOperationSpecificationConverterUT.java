package net.seesharpsoft.spring.data.jpa;

import net.seesharpsoft.spring.data.jpa.expression.Dialects;
import net.seesharpsoft.spring.data.jpa.expression.Operands;
import net.seesharpsoft.spring.data.jpa.expression.Operations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.jpa.domain.Specification;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

public class ODataOperationSpecificationConverterUT {

    private SpecificationConverter converter;

    @Before
    public void beforeEach() {
        converter = new SpecificationConverter(Dialects.ODATA, DefaultConversionService.getSharedInstance());
    }

    @Test
    public void converter_should_handle_null_value() {
        Specification result = converter.convert(null);
        assertThat(result, instanceOf(Specification.class));
    }

    @Test
    public void converter_should_treat_null_value_as_true_filter() {
        Specification result = converter.convert(null);
        assertThat(result, equalTo(StaticSpecification.TRUE));
    }

    @Test
    public void converter_should_treat_empty_value_as_true_filter() {
        Specification result = converter.convert("");
        assertThat(result, equalTo(StaticSpecification.TRUE));
    }

    @Test
    public void converter_should_parse_simple_expression() {
        Specification result = converter.convert("a eq 1");
        assertThat(result, equalTo(new OperationSpecification(Operations.equals(Operands.asReference("a"), Operands.from(1)))));
    }

}
