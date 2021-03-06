/*
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.query.sql;

import com.mysema.query.types.Ops;

/**
 * PostgresTemplates is an SQL dialect for PostgreSQL
 *
 * <p>tested with PostgreSQL 8.4 and 9.1</p>
 *
 * @author tiwe
 *
 */
public class PostgresTemplates extends SQLTemplates {

    public PostgresTemplates() {
        this('\\', false);
    }
    
    public PostgresTemplates(boolean quote) {
        this('\\', quote);
    }

    public PostgresTemplates(char escape, boolean quote) {
        super("\"", escape, quote);
        setDummyTable(null);
        // type mappings
        addClass2TypeMappings("numeric(3,0)", Byte.class);
        addClass2TypeMappings("double precision", Double.class);

        // String
        add(Ops.MATCHES, "{0} ~ {1}");
        add(Ops.INDEX_OF, "strpos({0},{1})-1");
        add(Ops.INDEX_OF_2ARGS, "strpos({0},{1})-1"); //FIXME
        add(Ops.StringOps.LOCATE,  "strpos({1},{0})");
        add(Ops.StringOps.LOCATE2, "strpos(repeat('^',{2s}-1) || substr({1},{2s}),{0})");

        // like without escape
        if (escape == '\\') {
            add(Ops.LIKE, "{0} like {1}");
            add(Ops.ENDS_WITH, "{0} like {%1}");
            add(Ops.ENDS_WITH_IC, "{0l} like {%%1}");
            add(Ops.STARTS_WITH, "{0} like {1%}");
            add(Ops.STARTS_WITH_IC, "{0l} like {1%%}");
            add(Ops.STRING_CONTAINS, "{0} like {%1%}");
            add(Ops.STRING_CONTAINS_IC, "{0l} like {%%1%%}");    
        }        
        
        // Number
        add(Ops.MathOps.RANDOM, "random()");
        add(Ops.MathOps.LN, "ln({0})");
        add(Ops.MathOps.LOG, "log({1},{0})");
        add(Ops.MathOps.COSH, "(exp({0}) + exp({0} * -1)) / 2");
        add(Ops.MathOps.COTH, "(exp({0} * 2) + 1) / (exp({0} * 2) - 1)");
        add(Ops.MathOps.SINH, "(exp({0}) - exp({0} * -1)) / 2");
        add(Ops.MathOps.TANH, "(exp({0} * 2) - 1) / (exp({0} * 2) + 1)");

        // Date / time
        add(Ops.DateTimeOps.YEAR, "extract(year from {0})");
        add(Ops.DateTimeOps.YEAR_MONTH, "extract(year from {0}) * 100 + extract(month from {0})");
        add(Ops.DateTimeOps.MONTH, "extract(month from {0})");
        add(Ops.DateTimeOps.WEEK, "extract(week from {0})");
        add(Ops.DateTimeOps.DAY_OF_MONTH, "extract(day from {0})");
        add(Ops.DateTimeOps.DAY_OF_WEEK, "extract(dow from {0}) + 1");
        add(Ops.DateTimeOps.DAY_OF_YEAR, "extract(doy from {0})");
        add(Ops.DateTimeOps.HOUR, "extract(hour from {0})");
        add(Ops.DateTimeOps.MINUTE, "extract(minute from {0})");
        add(Ops.DateTimeOps.SECOND, "extract(second from {0})");
        
//        add(Ops.DateTimeOps.DATE_ADD, "timestamp {0} + interval '{1s} {2s}s'");

    }

}
