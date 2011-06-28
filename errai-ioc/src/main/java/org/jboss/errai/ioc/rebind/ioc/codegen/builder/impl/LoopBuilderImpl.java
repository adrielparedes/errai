/*
 * Copyright 2011 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc.codegen.builder.impl;

import org.jboss.errai.ioc.rebind.ioc.codegen.BlockStatement;
import org.jboss.errai.ioc.rebind.ioc.codegen.BooleanExpression;
import org.jboss.errai.ioc.rebind.ioc.codegen.BooleanOperator;
import org.jboss.errai.ioc.rebind.ioc.codegen.Context;
import org.jboss.errai.ioc.rebind.ioc.codegen.Statement;
import org.jboss.errai.ioc.rebind.ioc.codegen.Variable;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.BuildCallback;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.ContextualLoopBuilder;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.LoopBuilder;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.StatementEnd;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.WhileBuilder;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.CallWriter;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.ConditionalBlockCallElement;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DeferredCallElement;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DeferredCallback;
import org.jboss.errai.ioc.rebind.ioc.codegen.control.DoWhileLoop;
import org.jboss.errai.ioc.rebind.ioc.codegen.control.ForLoop;
import org.jboss.errai.ioc.rebind.ioc.codegen.control.ForeachLoop;
import org.jboss.errai.ioc.rebind.ioc.codegen.control.WhileLoop;
import org.jboss.errai.ioc.rebind.ioc.codegen.literal.NullLiteral;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClassFactory;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.codegen.util.GenUtil;

/**
 * StatementBuilder to generate loops.
 * 
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class LoopBuilderImpl extends AbstractStatementBuilder implements ContextualLoopBuilder, LoopBuilder {

  protected LoopBuilderImpl(Context context, CallElementBuilder callElementBuilder) {
    super(context, callElementBuilder);
  }

  // foreach loop
  @Override
  public BlockBuilderImpl<StatementEnd> foreach(String loopVarName) {
    return foreach(loopVarName, (MetaClass) null);
  }

  @Override
  public BlockBuilderImpl<StatementEnd> foreach(String loopVarName, Class<?> loopVarType) {
    return foreach(loopVarName, MetaClassFactory.get(loopVarType));
  }

  private BlockBuilderImpl<StatementEnd> foreach(final String loopVarName, final MetaClass loopVarType) {
    final BlockStatement body = new BlockStatement();

    appendCallElement(new DeferredCallElement(new DeferredCallback() {
      @Override
      public void doDeferred(CallWriter writer, Context context, Statement statement) {
        GenUtil.assertIsIterable(statement);
        Variable loopVar = createForEachLoopVar(statement, loopVarName, loopVarType);
        String collection = writer.getCallString();
        writer.reset();
        writer.append(new ForeachLoop(loopVar, collection, body).generate(Context.create(context)));
      }
    }));

    return createLoopBody(body);
  }

  // do while loop
  @Override
  public BlockBuilderImpl<WhileBuilder> do_() {
    final BlockStatement body = new BlockStatement();

    return new BlockBuilderImpl<WhileBuilder>(body, new BuildCallback<WhileBuilder>() {
      @Override
      public WhileBuilder callback(Statement statement) {
        return new WhileBuilder() {

          @Override
          public StatementEnd while_(final BooleanExpression condition) {
            appendCallElement(new ConditionalBlockCallElement(new DoWhileLoop(condition, body)));
            return LoopBuilderImpl.this;
          }

          @Override
          public StatementEnd while_() {
            while_(new BooleanExpressionBuilder());
            return LoopBuilderImpl.this;
          }

          @Override
          public StatementEnd while_(BooleanOperator op, Statement rhs) {
            while_(new BooleanExpressionBuilder(rhs, op));
            return LoopBuilderImpl.this;
          }

          @Override
          public StatementEnd while_(BooleanOperator op, Object rhs) {
            return while_(op, GenUtil.generate(context, rhs));
          }
        };
      }
    });
  }

  // while loop
  @Override
  public BlockBuilderImpl<StatementEnd> while_() {
    return while_(new BooleanExpressionBuilder());
  }

  @Override
  public BlockBuilderImpl<StatementEnd> while_(BooleanOperator op, Object rhs) {
    return while_(op, GenUtil.generate(context, rhs));
  }

  @Override
  public BlockBuilderImpl<StatementEnd> while_(BooleanOperator op, Statement rhs) {
    if (rhs == null)
      rhs = NullLiteral.INSTANCE;
    return while_(new BooleanExpressionBuilder(rhs, op));
  }

  @Override
  public BlockBuilderImpl<StatementEnd> while_(final BooleanExpression condition) {
    final BlockStatement body = new BlockStatement();
    appendCallElement(new ConditionalBlockCallElement(new WhileLoop(condition, body)));
    return createLoopBody(body);
  }

  // for loop
  @Override
  public BlockBuilderImpl<StatementEnd> for_(BooleanExpression condition) {
    return for_((Statement) null, condition);
  }

  @Override
  public BlockBuilderImpl<StatementEnd> for_(Statement initializer, BooleanExpression condition) {
    return for_(initializer, condition, null);
  }

  @Override
  public BlockBuilderImpl<StatementEnd> for_(final Statement initializer, final BooleanExpression condition,
      final Statement countingExpression) {
    
    final BlockStatement body = new BlockStatement();
    appendCallElement(new ConditionalBlockCallElement(new ForLoop(condition, body, initializer, countingExpression)));
    return createLoopBody(body);
  }

  private BlockBuilderImpl<StatementEnd> createLoopBody(BlockStatement body) {
    return new BlockBuilderImpl<StatementEnd>(body, new BuildCallback<StatementEnd>() {
      @Override
      public StatementEnd callback(Statement statement) {
        return LoopBuilderImpl.this;
      }
    });
  }

  private Variable createForEachLoopVar(Statement collection, String loopVarName, MetaClass providedLoopVarType) {
    // infer the loop variable type
    MetaClass loopVarType = MetaClassFactory.get(Object.class);
    MetaParameterizedType parameterizedType = collection.getType().getParameterizedType();
    if (parameterizedType != null && parameterizedType.getTypeParameters().length != 0) {
      loopVarType = (MetaClass) parameterizedType.getTypeParameters()[0];
    }
    else if (collection.getType().getComponentType() != null) {
      loopVarType = collection.getType().getComponentType();
    }

    // try to use the provided loop variable type if possible (assignable from the inferred type)
    if (providedLoopVarType != null) {
      GenUtil.assertAssignableTypes(loopVarType, providedLoopVarType);
      loopVarType = providedLoopVarType;
    }

    Variable loopVar = Variable.create(loopVarName, loopVarType);
    context.addVariable(loopVar);
    return loopVar;
  }
}