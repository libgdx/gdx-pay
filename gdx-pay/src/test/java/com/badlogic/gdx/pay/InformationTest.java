package com.badlogic.gdx.pay;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class InformationTest {

    @Test
    public void twoObjectsWithSameFieldsShouldBeEqual() throws Exception {
        Information first = new Information("Full edition", "Access to all themes", "€ 1.00");
        Information second = new Information("Full edition", "Access to all themes", "€ 1.00");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void toStringShouldContainClassnameAndFields() throws Exception {

        Information information = new Information("Full edition", "Access to all themes", "€ 1.00");


        String string = information.toString();

        assertThat(string)
                .contains("Full edition")
                .contains("Access to all themes")
                .contains("€ 1.00")
                .contains(Information.class.getSimpleName());

    }
}