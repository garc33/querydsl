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
package com.mysema.query.jpa.impl;

import com.mysema.query.QueryMetadata;
import com.mysema.query.jpa.AbstractJPQLSubQuery;
import com.mysema.query.jpa.JPQLCommonQuery;

/**
 * JPASubQuery is a subquery class for JPA
 *
 * @author tiwe
 *
 */
public final class JPASubQuery extends AbstractJPQLSubQuery<JPASubQuery> implements JPQLCommonQuery<JPASubQuery>{

    public JPASubQuery() {
        super();
    }

    public JPASubQuery(QueryMetadata metadata) {
        super(metadata);
    }

}
