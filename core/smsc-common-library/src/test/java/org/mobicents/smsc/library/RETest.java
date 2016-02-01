package org.mobicents.smsc.library;

import static org.testng.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testng.annotations.Test;

/**
*
* @author sergey vetyutnev
* 
*/
public class RETest {
    @Test(groups = { "RegularExpr" })
    public void testRegularExpr() {
//        String expr = "^([1-9][1-9]|[0-9][1-9]|[1-9][0-9]).*";
        String expr = "^00[0-9].*";
        Pattern p = Pattern.compile(expr);
        Matcher m = p.matcher("0011");
        boolean b1 = m.matches();

        m = p.matcher("01");
        boolean b2 = m.matches();

        m = p.matcher("1");
        boolean b3 = m.matches();

        m = p.matcher("0");
        boolean b4 = m.matches();

        m = p.matcher("012222");
        boolean b5 = m.matches();

        m = p.matcher("1022222");
        boolean b6 = m.matches();

        m = p.matcher("11223232");
        boolean b7 = m.matches();

        m = p.matcher("100212");
        boolean b8 = m.matches();

        m = p.matcher("0000000");
        boolean b9 = m.matches();

        m = p.matcher("");
        boolean b0 = m.matches();
    }

    @Test(groups = { "RegularExpr" })
    public void testRegularExpr2() {
        String expr = "^(152)|(AWCC)$";
        Pattern p = Pattern.compile(expr);

        Matcher m = p.matcher("152");
        boolean b1 = m.matches();

        m = p.matcher("15");
        boolean b2 = m.matches();

        m = p.matcher("1152");
        boolean b3 = m.matches();

        m = p.matcher("1522");
        boolean b4 = m.matches();

        m = p.matcher("152152");
        boolean b5 = m.matches();

        m = p.matcher("AWCC");
        boolean b6 = m.matches();

        m = p.matcher("1AWCC");
        boolean b7 = m.matches();

        m = p.matcher("AWCC152");
        boolean b8 = m.matches();

        m = p.matcher("0000000");
        boolean b9 = m.matches();

        m = p.matcher("");
        boolean b0 = m.matches();

        m = p.matcher("1");
        boolean b10 = m.matches();

        m = p.matcher("2");
        boolean b11 = m.matches();

        m = p.matcher("152152");
        boolean b12 = m.matches();
    }

    @Test(groups = { "RegularExpr" })
    public void testRegularExpr3() {
        String expr = "^(152)|(AWCC)|(Shayeri)$";
        Pattern p = Pattern.compile(expr);

        Matcher m = p.matcher("152");
        boolean b1 = m.matches();
        assertTrue(b1);

        m = p.matcher("15");
        boolean b2 = m.matches();
        assertFalse(b2);

        m = p.matcher("1152");
        boolean b3 = m.matches();
        assertFalse(b3);

        m = p.matcher("1522");
        boolean b4 = m.matches();
        assertFalse(b4);

        m = p.matcher("152152");
        boolean b5 = m.matches();
        assertFalse(b5);

        m = p.matcher("AWCC");
        boolean b6 = m.matches();
        assertTrue(b6);

        m = p.matcher("1AWCC");
        boolean b7 = m.matches();
        assertFalse(b7);

        m = p.matcher("AWCC152");
        boolean b8 = m.matches();
        assertFalse(b8);

        m = p.matcher("0000000");
        boolean b9 = m.matches();
        assertFalse(b9);

        m = p.matcher("");
        boolean b0 = m.matches();
        assertFalse(b0);

        m = p.matcher("1");
        boolean b10 = m.matches();
        assertFalse(b10);

        m = p.matcher("2");
        boolean b11 = m.matches();
        assertFalse(b11);

        m = p.matcher("152152");
        boolean b12 = m.matches();
        assertFalse(b12);

        m = p.matcher("Shayeri");
        boolean b13 = m.matches();
        assertTrue(b13);

        m = p.matcher("hayeri");
        boolean b14 = m.matches();
        assertFalse(b14);

        m = p.matcher("Shayer");
        boolean b15 = m.matches();
        assertFalse(b15);

    }

    @Test(groups = { "RegularExpr" })
    public void testRegularExpr4() {
        String expr = "^27.*";
        Pattern p = Pattern.compile(expr);
        Matcher m = p.matcher("2711111");
        boolean b1 = m.matches();

        m = p.matcher("2");
        boolean b2 = m.matches();

        m = p.matcher("27");
        boolean b3 = m.matches();

        m = p.matcher("3700000");
        boolean b4 = m.matches();

        m = p.matcher("");
        boolean b5 = m.matches();

        m = p.matcher("200700");
        boolean b6 = m.matches();
    }

    @Test(groups = { "RegularExpr" })
    public void testRegularExpr5() {
        String expr = "^[0-9a-zA-Z \n]*"; // "^.*"
        Pattern p = Pattern.compile(expr);
        Matcher m = p.matcher("2711111");
        boolean b1 = m.matches();

        m = p.matcher("2711111\n");
        boolean b2 = m.matches();

        m = p.matcher("27 11111\n");
        boolean b3 = m.matches();
    }
}
