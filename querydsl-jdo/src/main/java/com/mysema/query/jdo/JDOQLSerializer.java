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
package com.mysema.query.jdo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.annotation.Nullable;

import com.google.common.primitives.Primitives;
import com.mysema.query.JoinExpression;
import com.mysema.query.QueryMetadata;
import com.mysema.query.support.SerializerBase;
import com.mysema.query.types.Constant;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Operation;
import com.mysema.query.types.Operator;
import com.mysema.query.types.Ops;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.ParamNotSetException;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.expr.Param;
import com.mysema.query.types.expr.SimpleOperation;

/**
 * JDOQLSerializer serializes Querydsl queries and expressions into JDOQL strings
 *
 * @author tiwe
 *
 */
public final class JDOQLSerializer extends SerializerBase<JDOQLSerializer> {

    private static final String COMMA = ", ";

    private static final String FROM = "\nFROM ";

    private static final String GROUP_BY = "\nGROUP BY ";

    private static final String HAVING = "\nHAVING ";

    private static final String ORDER_BY = "\nORDER BY ";

    private static final String PARAMETERS = "\nPARAMETERS ";

    private static final String RANGE = "\nRANGE ";

    private static final String SELECT = "SELECT ";

    private static final String SELECT_COUNT = "SELECT count(";

    private static final String SELECT_COUNT_THIS = "SELECT count(this)\n";

    private static final String SELECT_DISTINCT = "SELECT DISTINCT ";

    private static final String SELECT_UNIQUE = "SELECT UNIQUE ";

    private static final String THIS = "this";

    private static final String VARIABLES = "\nVARIABLES ";

    private static final String WHERE = "\nWHERE ";
    
    private static Comparator<Map.Entry<Object,String>> comparator = new Comparator<Map.Entry<Object,String>>() {
        @Override
        public int compare(Entry<Object, String> o1, Entry<Object, String> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    };

    private final Expression<?> candidatePath;

    private final List<Object> constants = new ArrayList<Object>();

    private final Stack<Map<Object,String>> constantToLabel = new Stack<Map<Object,String>>();
    
    public JDOQLSerializer(JDOQLTemplates templates, Expression<?> candidate) {
        super(templates);
        this.candidatePath = candidate;
        this.constantToLabel.push(new HashMap<Object,String>());
    }

    public Expression<?> getCandidatePath() {
        return candidatePath;
    }

    public List<Object> getConstants() {
        return constants;
    }
    
    @Override
    public Map<Object,String> getConstantToLabel() {
        return constantToLabel.peek();
    }

    @SuppressWarnings("unchecked")
    private <T> Expression<?> regexToLike(Operation<T> operation) {
        List<Expression<?>> args = new ArrayList<Expression<?>>();
        for (Expression<?> arg : operation.getArgs()) {
            if (!arg.getType().equals(String.class)) {
                args.add(arg);
            }else if (arg instanceof Constant) {
                args.add(regexToLike(arg.toString()));
            }else if (arg instanceof Operation) {
                args.add(regexToLike((Operation)arg));
            } else {
                args.add(arg);
            }
        }
        return SimpleOperation.create(
                operation.getType(),
                operation.getOperator(),
                args.<Expression<?>>toArray(new Expression[args.size()]));
    }

    private Expression<?> regexToLike(String str) {
        return ConstantImpl.create(str.replace(".*", "%").replace(".", "_"));
    }

    @SuppressWarnings("unchecked")
    public void serialize(QueryMetadata metadata, boolean forCountRow, boolean subQuery) {
        List<? extends Expression<?>> select = metadata.getProjection();
        List<JoinExpression> joins = metadata.getJoins();
        Expression<?> source = joins.get(0).getTarget();
        Predicate where = metadata.getWhere();
        List<? extends Expression<?>> groupBy = metadata.getGroupBy();
        Predicate having = metadata.getHaving();
        List<OrderSpecifier<?>> orderBy = metadata.getOrderBy();

        constantToLabel.push(new HashMap<Object,String>());
        
        // select
        boolean skippedSelect = false;
        if (forCountRow) {
            if (joins.size() == 1 && !subQuery) {
                append(SELECT_COUNT_THIS);
            } else {
                append(SELECT_COUNT);
                boolean first = true;
                for (JoinExpression je : joins) {
                    if (!first) {
                        append(COMMA);
                    }
                    handle(je.getTarget());
                    first = false;
                }
                append(")");
            }

        } else if (!select.isEmpty()) {
            if (metadata.isDistinct()) {
                append(SELECT_DISTINCT);
            }else if (metadata.isUnique() && !subQuery) {
                append(SELECT_UNIQUE);
            } else {
                append(SELECT);
            }
            if (select.size() >1 || !select.get(0).equals(source) || metadata.isDistinct()) {
                handle(COMMA, select);    
            } else {
                skippedSelect = true;
            }
        }

        // from
        append(skippedSelect ? FROM.substring(1) : FROM);
        if (source instanceof Operation && subQuery) {
            handle(source);
        } else {
            append(source.getType().getName());
            if (!source.equals(candidatePath)) {
                append(" ").handle(source);
            }
        }

        // where
        if (where != null) {
            append(WHERE).handle(where);
        }

        // variables
        if (joins.size() > 1) {
            serializeVariables(joins);
        }

        // parameters
        if (!getConstantToLabel().isEmpty()) {
            serializeParameters(metadata.getParams());
        }

        // group by
        if (!groupBy.isEmpty()) {
            append(GROUP_BY).handle(COMMA, groupBy);
        }

        // having
        if (having != null) {
            append(HAVING).handle(having);
        }

        // order by
        if (!orderBy.isEmpty() && !forCountRow) {
            append(ORDER_BY);
            boolean first = true;
            for (OrderSpecifier<?> os : orderBy) {
                if (!first) {
                    append(COMMA);
                }
                handle(os.getTarget());
                append(" " + os.getOrder());
                first = false;
            }
        }

        // range
        if (!forCountRow && metadata.getModifiers().isRestricting()) {
            Long limit = metadata.getModifiers().getLimit();
            Long offset = metadata.getModifiers().getOffset();
            serializeModifiers(limit, offset);
        }

        constantToLabel.pop();
        
    }

    private void serializeModifiers(@Nullable Long limit, @Nullable Long offset) {
        append(RANGE);
        if (offset != null) {
            append(String.valueOf(offset));
            if (limit != null) {
                append(COMMA);
                append(String.valueOf(offset + limit));
            }
        } else {
            append("0, ").append(String.valueOf(limit));
        }
    }

    private void serializeParameters(Map<ParamExpression<?>, Object> params) {
        append(PARAMETERS);
        boolean first = true;
        List<Map.Entry<Object, String>> entries = new ArrayList<Map.Entry<Object, String>>(getConstantToLabel().entrySet());
        Collections.sort(entries, comparator);
        for (Map.Entry<Object, String> entry : entries) {
            if (!first) {
                append(COMMA);
            }
            if (Param.class.isInstance(entry.getKey())) {
                Object constant = params.get(entry.getKey());
                if (constant == null) {
                    throw new ParamNotSetException((Param<?>) entry.getKey());
                }
                constants.add(constant);
                append(((Param<?>)entry.getKey()).getType().getName());
            } else {
                constants.add(entry.getKey());
                append(entry.getKey().getClass().getName());
            }
            append(" ").append(entry.getValue());
            first = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void serializeVariables(List<JoinExpression> joins) {
        append(VARIABLES);
        for (int i = 1; i < joins.size(); i++) {
            JoinExpression je = joins.get(i);
            if (i > 1) {
                append("; ");
            }

            // type specifier
            if (je.getTarget() instanceof EntityPath) {
                EntityPath<?> pe = (EntityPath<?>) je.getTarget();
                if (pe.getMetadata().getParent() == null) {
                    append(pe.getType().getName()).append(" ");
                }
            }
            handle(je.getTarget());
        }
    }

    @Override
    public Void visit(Path<?> path, Void context) {
        if (path.equals(candidatePath)) {
            append(THIS);
        } else {
            super.visit(path, context);
        }
        return null;
    }

    @Override
    public Void visit(SubQueryExpression<?> query, Void context) {
        append("(");
        serialize(query.getMetadata(), false, true);
        append(")");
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void visitOperation(Class<?> type, Operator<?> operator, List<? extends Expression<?>> args) {
        if (operator.equals(Ops.INSTANCE_OF)) {
            handle(args.get(0)).append(" instanceof ");
            append(((Constant<Class<?>>) args.get(1)).getConstant().getName());

        } else if (operator.equals(Ops.LIKE) || operator.equals(Ops.LIKE_ESCAPE)) {
            super.visitOperation(type, Ops.MATCHES, 
                Arrays.asList(args.get(0), ExpressionUtils.likeToRegex((Expression<String>) args.get(1), false)));
            
        // exists    
        } else if (operator.equals(Ops.EXISTS) && args.get(0) instanceof SubQueryExpression) {
            SubQueryExpression subQuery = (SubQueryExpression) args.get(0);
            append("(");
            serialize(subQuery.getMetadata(), true, true);
            append(") > 0");

        // not exists    
        } else if (operator.equals(Ops.NOT) && args.get(0) instanceof Operation 
                && ((Operation)args.get(0)).getOperator().equals(Ops.EXISTS)) {    
            SubQueryExpression subQuery = (SubQueryExpression) ((Operation)args.get(0)).getArg(0);
            append("(");
            serialize(subQuery.getMetadata(), true, true);
            append(") == 0");
                
        } else if (operator.equals(Ops.NUMCAST)) {
            Class<?> clazz = ((Constant<Class<?>>)args.get(1)).getConstant();
            if (Number.class.isAssignableFrom(clazz) && Primitives.isWrapperType(clazz)) {
                clazz = Primitives.unwrap(clazz);
            }
            append("(",clazz.getSimpleName(),")").handle(args.get(0));

        } else {
            super.visitOperation(type, operator, args);
        }
    }
    

}
