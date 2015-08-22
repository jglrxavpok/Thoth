package thoth.testing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ListConditions {

    /**
     * Compares each element from <code>actual</code> to <code>expected</code> ones. If a difference is found,
     * an {@link java.lang.AssertionError AssertionError} is raised.<br/>
     * First, this method checks if the list have the same length. If not, an error is raised.<br/>
     * Otherwise, equality is checked via calls to {@link java.lang.Object#equals(Object)} like the following:<br/>
     * {@code (elem == null && otherElem == null) || elem.equals(otherElem)} with elem being an element from <code>expected</code> and
     * otherElem from <code>actual</code>
     * @param message
     *                The message to show if the lists are not equal
     * @param expected
     *                 The list of expected values
     * @param actual
     *              The actual list of values
     * @param <T>
     *            The type of the content of the lists, must be the same for the two lists
     */
    public static <T> void assertListEquals(Object message, List<T> expected, List<T> actual) {
        if(expected.size() != actual.size()) {
            throw new AssertionError(message);
        }
        for(int i = 0;i<expected.size();i++) {
            T expectedElem = expected.get(i);
            T actualElem = actual.get(i);
            boolean condition = expectedElem == null && actualElem == null || expectedElem.equals(actualElem);
            if(!condition) {
                throw new AssertionError(message);
            }
        }
    }

    /**
     * Shorthand for {@link ListConditions#assertListEquals(Object, List, List)} where the message is autogenerated
     * @see ListConditions#assertListEquals(Object, List, List)
     */
    public static <T> void assertListEquals(List<T> expected, List<T> actual) {
        assertListEquals("Lists are not equal, expected: "+toString(expected) + "; found: "+toString(actual), expected, actual);
    }

    /**
     * Creates a list from <code>elements</code>. Should only be used inside tests.
     * @param elements
     *                The elements to compose the list
     * @param <T>
     *            The type of the elements inside the list
     * @return
     *        A list containing each element of <code>elements</code>
     */
    @SafeVarargs
    public static <T> List<T> quickList(T... elements) {
        List<T> result = new LinkedList<T>();
        Collections.addAll(result, elements);
        return result;
    }

    private static <T> String toString(List<T> list) {
        StringBuilder builder = new StringBuilder();
        for(int i = 0;i<list.size();i++) {
            if(i != 0)
                builder.append(", ");
            builder.append(list.get(i).toString());
        }
        return builder.toString();
    }
}
