package net.seesharpsoft.spring.test.mock;

import org.mockito.invocation.InvocationOnMock;

import javax.persistence.criteria.CriteriaQuery;

import static org.mockito.Mockito.mock;

public class CriteriaQueryMockBuilder {

    public static CriteriaQuery newCriteriaQuery() {
        CriteriaQuery builderMock = mock(CriteriaQuery.class, new CriteriaQueryMockDefaultAnswer());
        return builderMock;
    }

    private static class CriteriaQueryMockDefaultAnswer extends MockAnswer {

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Object result = super.answer(invocation);
            if (result != UNDEFINED) {
                return result;
            }

            Class<?> returnType = invocation.getMethod().getReturnType();

            if (returnType.isAssignableFrom(CriteriaQuery.class)) {
                return invocation.getMock();
            }

            throw new UnsupportedOperationException();
        }

    }

}
