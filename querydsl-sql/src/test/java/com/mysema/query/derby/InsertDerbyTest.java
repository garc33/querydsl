package com.mysema.query.derby;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.mysema.query.Connections;
import com.mysema.query.InsertBaseTest;
import com.mysema.query.Target;
import com.mysema.query.sql.DerbyTemplates;
import com.mysema.testutil.FilteringTestRunner;
import com.mysema.testutil.Label;

@RunWith(FilteringTestRunner.class)
@Label(Target.DERBY)
public class InsertDerbyTest extends InsertBaseTest{
    
    @BeforeClass
    public static void setUp() throws Exception {
        Connections.initDerby();
    }

    @Before
    public void setUpForTest() {
        dialect = new DerbyTemplates().newLineToSingleSpace();
    }

}