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

import org.jboss.errai.ioc.rebind.ioc.codegen.Context;
import org.jboss.errai.ioc.rebind.ioc.codegen.Statement;
import org.jboss.errai.ioc.rebind.ioc.codegen.Variable;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.BuildCallback;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.CatchBlockBuilder;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.StatementEnd;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.TryBlockBuilder;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.CallWriter;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DeferredCallElement;
import org.jboss.errai.ioc.rebind.ioc.codegen.builder.callstack.DeferredCallback;
import org.jboss.errai.ioc.rebind.ioc.codegen.control.TryBlock;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClassFactory;

/**
 * StatementBuilder to generate try/catch/finally blocks.
 * 
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class TryBlockBuilderImpl extends AbstractStatementBuilder implements TryBlockBuilder, CatchBlockBuilder {
  private TryBlock tryBlock;

  protected TryBlockBuilderImpl(Context context, CallElementBuilder callElementBuilder) {
    super(context, callElementBuilder);
  }

  protected TryBlockBuilderImpl(Context context, CallElementBuilder callElementBuilder, TryBlock tryBlock) {
    super(context, callElementBuilder);
    this.tryBlock = tryBlock;
  }

  public BlockBuilder<CatchBlockBuilder> try_() {
    tryBlock = new TryBlock();

    appendCallElement(new DeferredCallElement(new DeferredCallback() {
      public void doDeferred(CallWriter writer, Context context, Statement statement) {
        writer.append(tryBlock.generate(Context.create(context)));
      }
    }));

    return new BlockBuilder<CatchBlockBuilder>(tryBlock.getBlock(), new BuildCallback<CatchBlockBuilder>() {
      public CatchBlockBuilder callback(Statement statement) {
        return TryBlockBuilderImpl.this;
      }
    });
  }

  public BlockBuilder<CatchBlockBuilder> catch_(Class<? extends Throwable> exceptionType, String variableName) {
    return catch_(MetaClassFactory.get(exceptionType), variableName);
  }

  public BlockBuilder<CatchBlockBuilder> catch_(MetaClass exceptionType, String variableName) {
    Variable exceptionVar = Variable.create(variableName, exceptionType);
    tryBlock.addCatchBlock(exceptionVar);

    return new BlockBuilder<CatchBlockBuilder>(tryBlock.getCatchBlock(exceptionVar),
        new BuildCallback<CatchBlockBuilder>() {

          public CatchBlockBuilder callback(Statement statement) {
            return TryBlockBuilderImpl.this;
          }
        });
  }

  public BlockBuilder<StatementEnd> finally_() {
    return new BlockBuilder<StatementEnd>(tryBlock.getFinallyBlock(), new BuildCallback<StatementEnd>() {
      public StatementEnd callback(Statement statement) {
        return TryBlockBuilderImpl.this;
      }
    });
  }
}