package net.seesharpsoft.spring.data.jpa.expression;

import net.seesharpsoft.spring.data.jpa.expression.Operator.NAry;
import net.seesharpsoft.commons.TriFunction;
import org.springframework.util.Assert;

import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Operators {

    private Operators() {
        // static
    }

    public static final int compareTo(Comparable x, Comparable y) {
        if (x == null || y == null) {
            return 0;
        }
        return x.compareTo(y);
    }

    public static final boolean compareTo(Comparable x, Comparable y, Function<Integer, Boolean> reduce) {
        return reduce.apply(compareTo(x, y));
    }

    protected static final Number calculate(Number x, Number y, MathContext mc, TriFunction<BigDecimal, BigDecimal, MathContext, BigDecimal> function) {
        BigDecimal xDecimal, yDecimal;
        Class targetClass = null;

        if (x == null) {
            xDecimal = BigDecimal.valueOf(0.0);
        } else {
            xDecimal = BigDecimal.valueOf(x.doubleValue());
            targetClass = x.getClass();
        }
        if (y == null) {
            yDecimal = BigDecimal.valueOf(0.0);
        } else {
            yDecimal = BigDecimal.valueOf(y.doubleValue());
            if (targetClass == null) {
                targetClass = y.getClass();
            }
        }
        if (targetClass == null) {
            targetClass = Integer.class;
        }

        BigDecimal result = function.apply(xDecimal, yDecimal, mc);

        if (targetClass.equals(Double.class)) {
            return result.doubleValue();
        }
        if (targetClass.equals(Float.class)) {
            return result.floatValue();
        }
        if (targetClass.equals(Long.class)) {
            return result.longValueExact();
        }
        if (targetClass.equals(Integer.class)) {
            return result.intValueExact();
        }
        if (targetClass.equals(Short.class)) {
            return result.shortValueExact();
        }
        if (targetClass.equals(Byte.class)) {
            return result.byteValueExact();
        }
        return result;
    }

    public static final Operator AND = new Operators.Binary<>("&&", 40, CriteriaBuilder::and, Boolean::logicalAnd);
    public static final Operator OR = new Operators.Binary<>("||", 30, CriteriaBuilder::or, Boolean::logicalOr);
    public static final Operator NOT = new Operators.Unary<Boolean, Boolean>("!", 140, CriteriaBuilder::not, x -> !x) {
        @Override
        public Associativity getAssociativity() {
            return Associativity.RIGHT;
        }
    };

    public static final Operator GREATER_THAN = new Operators.Binary<Comparable, Comparable, Boolean>(">", 90,
            CriteriaBuilder::greaterThan,
            (x, y) -> compareTo(x, y, r -> r > 0));
    public static final Operator GREATER_THAN_OR_EQUALS = new Operators.Binary<Comparable, Comparable, Boolean>(">=", 90,
            CriteriaBuilder::greaterThanOrEqualTo,
            (x, y) -> compareTo(x, y, r -> r >= 0));
    public static final Operator LESS_THAN = new Operators.Binary<Comparable, Comparable, Boolean>("<", 90,
            CriteriaBuilder::lessThan,
            (x, y) -> compareTo(x, y, r -> r < 0));
    public static final Operator LESS_THAN_OR_EQUALS = new Operators.Binary<Comparable, Comparable, Boolean>("<=", 90,
            CriteriaBuilder::lessThanOrEqualTo,
            (x, y) -> compareTo(x, y, r -> r <= 0));
    public static final Operator IN = new Operators.Binary<>("IN", 90,
            (criteriaBuilder, left, right) -> criteriaBuilder.in(left).value(right),
            (Object needle, Object collection) -> {
                if (collection == null) {
                    return false;
                }
                Iterable iterable = null;
                if (collection instanceof Iterable) {
                    iterable = (Iterable)collection;
                } else {
                    iterable = Arrays.asList(collection);
                }
                for (Object value : iterable) {
                    if (Objects.equals(needle, value)) {
                        return true;
                    }
                }
                return false;
            });

    public static final Operator ADD = new Numerical("+", 110, CriteriaBuilder::sum, BigDecimal::add);
    public static final Operator SUB = new Numerical("-", 110, CriteriaBuilder::diff, BigDecimal::subtract);
    public static final Operator MUL = new Numerical("*", 120, CriteriaBuilder::prod, BigDecimal::multiply);
    public static final Operator DIV = new Numerical("/", 120, CriteriaBuilder::quot, BigDecimal::divide);
    public static final Operator MOD = new Numerical("%", 125, CriteriaBuilder::mod, BigDecimal::remainder);

    public static final Operator EQUALS = new Operators.Base("==", NAry.BINARY, 80) {
        @Override
        public Object evaluate(Object... operands) {
            Assert.isTrue(operands == null || operands.length == 2, "exactly two operands expected for binary operator!");
            Operand leftOperand = operands == null ? null : Operands.from(operands[0]);
            Operand rightOperand = operands == null ? null : Operands.from(operands[1]);
            Object left = leftOperand == null ? null : leftOperand.evaluate();
            Object right = rightOperand == null ? null : rightOperand.evaluate();
            return Objects.equals(left, right);
        }

        @Override
        public Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Object... operands) {
            Assert.isTrue(operands == null || operands.length == 2, "exactly two operands expected for binary operator!");
            Operand leftOperand = operands == null ? null : Operands.from(operands[0]);
            Operand rightOperand = operands == null ? null : Operands.from(operands[1]);
            Expression left = leftOperand == null ? null : leftOperand.asExpression(root, query, builder);
            Expression right = rightOperand == null ? null : rightOperand.asExpression(root, query, builder);

            if (left instanceof Join || right instanceof Join) {
                query.distinct(true);
            }

            if (rightOperand == null && leftOperand == null) {
                return builder.and();
            } else if (leftOperand == null) {
                return Iterable.class.isAssignableFrom(right.getJavaType()) ? builder.isEmpty(right) : builder.isNull(right);
            } else if (rightOperand == null) {
                return Iterable.class.isAssignableFrom(left.getJavaType()) ? builder.isEmpty(left) : builder.isNull(left);
            }
            return builder.equal(left, right);
        }
    };

    public static final Operator NOT_EQUALS = new Operators.Base("!=", NAry.BINARY, 80) {
        @Override
        public Object evaluate(Object... operands) {
            return !(boolean)EQUALS.evaluate(operands);
        }

        @Override
        public Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Object... operands) {
            Assert.isTrue(operands.length == 2, "exactly two operands expected for binary operator!");
            return builder.not(EQUALS.createExpression(root, query, builder, operands));
        }
    };

    public static final Operator IS_SUBSTRING = new LikeOperatorBase("includes", 90) {
        @Override
        protected Object evaluate(String leftOperand, String rightOperand) {
            return leftOperand.contains(rightOperand);
        }

        @Override
        protected Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Expression left, String right) {
            return builder.like(left, String.format("\\%%s\\%", right));
        }
    };
    public static final Operator STARTS_WITH = new LikeOperatorBase("startsWith", 90) {
        @Override
        protected Object evaluate(String leftOperand, String rightOperand) {
            return leftOperand.startsWith(rightOperand);
        }

        @Override
        protected Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Expression left, String right) {
            return builder.like(left, String.format("%s\\%", right));
        }
    };
    public static final Operator ENDS_WITH = new LikeOperatorBase("endsWith", 90) {
        @Override
        protected Object evaluate(String leftOperand, String rightOperand) {
            return leftOperand.endsWith(rightOperand);
        }

        @Override
        protected Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Expression left, String right) {
            return builder.like(left, String.format("\\%%s", right));
        }
    };

    protected abstract static class LikeOperatorBase extends Operators.Base {

        public LikeOperatorBase(String name, int precedence) {
            super(name, NAry.BINARY, precedence);
        }

        protected abstract Object evaluate(String leftOperand, String rightOperand);

        @Override
        public final Object evaluate(Object... operands) {
            Assert.isTrue(operands == null || operands.length == 2, "exactly two operands expected for binary operator!");
            Operand leftOperand = operands == null ? null : Operands.from(operands[0]);
            Operand rightOperand = operands == null ? null : Operands.from(operands[1]);
            Object leftValue = leftOperand == null ? null : leftOperand.evaluate();
            Object rightValue = rightOperand == null ? null : rightOperand.evaluate();

            return evaluate(leftValue == null ? "" : leftValue.toString(), rightValue == null ? "" : rightValue.toString());
        }

        protected abstract Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Expression left, String right);

        @Override
        public Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Object... operands) {
            Assert.isTrue(operands.length == 2, "exactly two operands expected for binary operator!");
            Operand leftOperand = operands == null ? null : Operands.from(operands[0]);
            Expression left = leftOperand == null ? null : leftOperand.asExpression(root, query, builder);
            Operand rightOperand = operands == null ? null : Operands.from(operands[1]);
            Object rightValue = rightOperand == null ? null : rightOperand.evaluate();

            return createExpression(root, query, builder, left, rightValue == null ? "" : rightValue.toString());
        }
    }

    public static class Unary<T, R> extends Operators.Base {
        private final BiFunction<CriteriaBuilder, Expression, Expression> expressionFunction;
        private final Function<T, R> evaluationFunction;

        public Unary(String name,
                     int precedence,
                     BiFunction<CriteriaBuilder, Expression, Expression> expressionFunction,
                     Function<T, R> evaluationFunction) {
            super(name, NAry.UNARY, precedence);
            this.expressionFunction = expressionFunction;
            this.evaluationFunction = evaluationFunction;
        }

        protected R evaluate(T operand) {
            Assert.notNull(this.evaluationFunction, "evaluationFunction must be defined!");
            return evaluationFunction.apply(operand);
        }

        @Override
        public final Object evaluate(Object... operands) {
            Assert.isTrue(operands == null || operands.length == 1, "exactly one operand expected for unary operator!");
            Operand operand = operands == null ? null : Operands.from(operands[0]);
            return this.evaluate(operand == null ? null : (T)operand.evaluate());
        }

        protected Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Expression expression) {
            Assert.notNull(this.expressionFunction, "expressionFunction must be defined!");
            return expressionFunction.apply(builder, expression);
        }

        @Override
        public final Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Object... operands) {
            Assert.isTrue(operands == null || operands.length == 1, "exactly one operand expected for unary operator!");
            Expression expression = operands == null || operands[0] == null ? null : Operands.from(operands[0]).asExpression(root, query, builder);
            return createExpression(root, query, builder, expression);
        }
    }

    /**
     * Base class for numerical operations like addition, subtraction, modulo, etc.
     */
    public static class Numerical extends Binary<Number, Number, Number> {
        public static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

        public Numerical(String name, int precedence,
                         TriFunction<CriteriaBuilder, Expression, Expression, Expression> expressionFunction,
                         TriFunction<BigDecimal, BigDecimal, MathContext, BigDecimal> evaluationFunction) {
            super(name, precedence, expressionFunction, (x, y) -> calculate(x, y, MATH_CONTEXT, evaluationFunction));
        }
    }

    /**
     * Base class for binary operators.
     * @param <T> left operand type
     * @param <U> right operand type
     * @param <R> result type
     */
    public static class Binary<T, U, R> extends Operators.Base {
        private final TriFunction<CriteriaBuilder, Expression, Expression, Expression> expressionFunction;
        private final BiFunction<T, U, R> evaluationFunction;

        public Binary(String name,
                      int precedence,
                      TriFunction<CriteriaBuilder, Expression, Expression, Expression> expressionFunction,
                      BiFunction<T, U, R> evaluationFunction) {
            super(name, NAry.BINARY, precedence);
            this.expressionFunction = expressionFunction;
            this.evaluationFunction = evaluationFunction;
        }

        protected R evaluate(T leftOperand, U rightOperand) {
            Assert.notNull(this.evaluationFunction, "evaluationFunction must be defined!");
            return evaluationFunction.apply(leftOperand, rightOperand);
        }

        @Override
        public final Object evaluate(Object... operands) {
            Assert.isTrue(operands == null || operands.length == 2, "exactly two operands expected for binary operator!");
            Operand leftOperand = operands == null ? null : Operands.from(operands[0]);
            Operand rightOperand = operands == null ? null : Operands.from(operands[1]);
            Object left = leftOperand == null ? null : leftOperand.evaluate();
            Object right = rightOperand == null ? null : rightOperand.evaluate();
            return this.evaluate((T)left, (U)right);
        }

        protected Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Expression left, Expression right) {
            Assert.notNull(this.expressionFunction, "expressionFunction must be defined!");
            return expressionFunction.apply(builder, left, right);
        }

        @Override
        public final Expression createExpression(Root root, CriteriaQuery query, CriteriaBuilder builder, Object... operands) {
            Assert.isTrue(operands == null || operands.length == 2, "exactly two operands expected for binary operator!");
            Operand leftOperand = operands == null ? null : Operands.from(operands[0]);
            Operand rightOperand = operands == null ? null : Operands.from(operands[1]);
            Expression left = leftOperand == null ? null : leftOperand.asExpression(root, query, builder);
            Expression right = rightOperand == null ? null : rightOperand.asExpression(root, query, builder);

            if (left instanceof Join || right instanceof Join) {
                query.distinct(true);
            }
            return createExpression(root, query, builder, left, right);
        }
    }

    /**
     * Base class for operator implementations.
     */
    public static abstract class Base implements Operator {
        private final String name;
        private final NAry nary;
        private final int precedence;

        public Base(String name, NAry nary, int precedence) {
            this.name = name;
            this.nary = nary;
            this.precedence = precedence;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public int getPrecedence() {
            return this.precedence;
        }

        @Override
        public Associativity getAssociativity() {
            return Associativity.LEFT;
        }

        @Override
        public NAry getNAry() {
            return this.nary;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Base)) {
                return false;
            }

            return Objects.equals(this.name, ((Base) other).name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.name);
        }
    }
}