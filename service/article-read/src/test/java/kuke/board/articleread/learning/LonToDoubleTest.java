package kuke.board.articleread.learning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class LonToDoubleTest {

    @Test
    void longToDoubleTest() {
        long longValue = 111_111_111_111_111_111L;
        System.out.println("longValue = " + longValue);
        double doubleValue = longValue;
        System.out.println("doubleValue = " + new BigDecimal(doubleValue).toString());
        long longValue2 = (long) doubleValue;
        System.out.println("longValue2 = " + longValue2);
    }
}
