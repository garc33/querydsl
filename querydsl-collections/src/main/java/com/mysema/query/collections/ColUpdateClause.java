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
package com.mysema.query.collections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mysema.query.dml.UpdateClause;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.util.BeanMap;

/**
 * ColUpdateClause is an implementation of the UpdateClause interface for Querydsl Collections
 *
 * @author tiwe
 *
 * @param <T>
 */
public class ColUpdateClause<T> implements UpdateClause<ColUpdateClause<T>> {

    private final Path<T> expr;

    private final Map<Path<?>, Object> paths = new HashMap<Path<?>, Object>();

    private final ColQuery query;

    public ColUpdateClause(QueryEngine qe, Path<T> expr, Iterable<? extends T> col){
        this.query = new ColQueryImpl(qe).from(expr, col);
        this.expr = expr;
    }

    public ColUpdateClause(Path<T> expr, Iterable<? extends T> col){
        this(DefaultQueryEngine.DEFAULT, expr, col);
    }

    @Override
    public long execute() {
        int rv = 0;
        for (T match : query.list(expr)) {
            BeanMap beanMap = new BeanMap(match);
            for (Map.Entry<Path<?>,Object> entry : paths.entrySet()) {
                // TODO : support deep updates as well
                String propertyName = entry.getKey().getMetadata().getExpression().toString();
                beanMap.put(propertyName, entry.getValue());
            }
            rv++;
        }
        return rv;
    }

    @Override
    public <U> ColUpdateClause<T> set(Path<U> path, U value) {
        paths.put(path, value);
        return this;
    }
    

    @Override
    public <U> ColUpdateClause<T> set(Path<U> path, Expression<? extends U> expression) {
        // TODO : implement
        throw new UnsupportedOperationException();
    }
    
    @Override
    public <U> ColUpdateClause<T> setNull(Path<U> path){
        paths.put(path, null);
        return this;
    }

    @Override
    public ColUpdateClause<T> set(List<? extends Path<?>> p, List<?> v) {
        for (int i = 0; i < p.size(); i++) {
            paths.put(p.get(i), v.get(i));
        }
        return this;
    }

    @Override
    public ColUpdateClause<T> where(Predicate... o) {
        query.where(o);
        return this;
    }
    
    @Override
    public String toString(){
        return "update " + query;
    }


}
