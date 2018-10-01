package net.seesharpsoft.spring.data.jpa.expression;

import javax.persistence.criteria.*;
import java.util.List;

public interface Operation extends Operand {
    Operator getOperator();

    List getOperands();

    default Expression asExpression(Root root,
                                    AbstractQuery query,
                             CriteriaBuilder builder,
                             Class targetType) {
        return getOperator().createExpression(root, query, builder, getOperands().toArray());
    }

    default Object evaluate() {
        return getOperator().evaluate(getOperands().toArray());
    }
}
