/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.tooling.internal.composite;

import com.google.common.collect.Lists;
import org.gradle.api.Transformer;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.composite.ModelResult;
import org.gradle.tooling.composite.ModelResults;
import org.gradle.tooling.internal.consumer.AbstractLongRunningOperation;
import org.gradle.tooling.internal.consumer.BlockingResultHandler;
import org.gradle.tooling.internal.consumer.CompositeConnectionParameters;
import org.gradle.tooling.internal.consumer.ExceptionTransformer;
import org.gradle.tooling.internal.consumer.async.AsyncConsumerActionExecutor;
import org.gradle.tooling.internal.consumer.connection.ConsumerAction;
import org.gradle.tooling.internal.consumer.connection.ConsumerConnection;
import org.gradle.tooling.internal.consumer.parameters.ConsumerOperationParameters;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.internal.Exceptions;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class DefaultCompositeModelBuilder<T> extends AbstractLongRunningOperation<DefaultCompositeModelBuilder<T>> implements ModelBuilder<ModelResults<T>> {
    private final Class<T> modelType;
    private final AsyncConsumerActionExecutor connection;

    protected DefaultCompositeModelBuilder(Class<T> modelType, AsyncConsumerActionExecutor connection, CompositeConnectionParameters parameters) {
        super(parameters);
        this.modelType = modelType;
        this.connection = connection;
        operationParamsBuilder.setEntryPoint("CompositeModelBuilder API");
    }

    @Override
    protected DefaultCompositeModelBuilder<T> getThis() {
        return this;
    }

    @Override
    public ModelResults<T> get() throws GradleConnectionException, IllegalStateException {
        BlockingResultHandler<ModelResults> handler = new BlockingResultHandler<ModelResults>(ModelResults.class);
        get(handler);
        return handler.getResult();
    }

    @Override
    public void get(final ResultHandler<? super ModelResults<T>> handler) throws IllegalStateException {
        final ConsumerOperationParameters operationParameters = getConsumerOperationParameters();
        connection.run(new ConsumerAction<ModelResults<T>>() {
            public ConsumerOperationParameters getParameters() {
                return operationParameters;
            }

            public ModelResults<T> run(ConsumerConnection connection) {
                final Iterable<ModelResult<T>> models = connection.buildModels(modelType, operationParameters);
                return new ModelResults<T>() {
                    @Override
                    public Iterator<ModelResult<T>> iterator() {
                        return models.iterator();
                    }
                };
            }
        }, new ResultHandlerAdapter<T>(handler));
    }

    // TODO: Make all configuration methods configure underlying model builders
    private DefaultCompositeModelBuilder<T> unsupportedMethod() {
        throw new UnsupportedMethodException("Not supported for composite connections.");
    }

    @Override
    public DefaultCompositeModelBuilder<T> forTasks(String... tasks) {
        return forTasks(Lists.newArrayList(tasks));
    }

    @Override
    public DefaultCompositeModelBuilder<T> forTasks(Iterable<String> tasks) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> withArguments(String... arguments) {
        return withArguments(Lists.newArrayList(arguments));
    }

    @Override
    public DefaultCompositeModelBuilder<T> withArguments(Iterable<String> arguments) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setStandardOutput(OutputStream outputStream) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setStandardError(OutputStream outputStream) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setColorOutput(boolean colorOutput) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setStandardInput(InputStream inputStream) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setJavaHome(File javaHome) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setJvmArguments(String... jvmArguments) {
        return unsupportedMethod();
    }

    @Override
    public DefaultCompositeModelBuilder<T> setJvmArguments(Iterable<String> jvmArguments) {
        return unsupportedMethod();
    }

    private final class ResultHandlerAdapter<T> extends org.gradle.tooling.internal.consumer.ResultHandlerAdapter<ModelResults<T>> {
        ResultHandlerAdapter(ResultHandler<? super ModelResults<T>> handler) {
            super(handler, new ExceptionTransformer(new Transformer<String, Throwable>() {
                @Override
                public String transform(Throwable failure) {
                    String message = String.format("Could not fetch models of type '%s' using %s.", modelType.getSimpleName(), connection.getDisplayName());
                    if (!(failure instanceof UnsupportedMethodException) && failure instanceof UnsupportedOperationException) {
                        message += "\n" + Exceptions.INCOMPATIBLE_VERSION_HINT;
                    }
                    return message;
                }
            }));
        }
    }
}
