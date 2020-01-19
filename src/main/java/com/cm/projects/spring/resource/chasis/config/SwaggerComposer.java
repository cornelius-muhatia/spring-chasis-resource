/*
 * Copyright 2019 Cornelius M.
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
package com.cm.projects.spring.resource.chasis.config;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.OperationModelsProviderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.RequestMappingContext;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import com.cm.projects.spring.resource.chasis.annotations.ApiChasisEntity;

/**
 * @author Cornelius M
 * @version 1.0.0 10/6/19
 */
public class SwaggerComposer implements OperationModelsProviderPlugin, OperationBuilderPlugin {

    /**
     * Temporal folder for storing the generated java classes
     */
    private final Path tempPath;
    /**
     * Event logs handler
     */
    private final Logger log;

    public SwaggerComposer() {
        try {
            this.tempPath = Files.createTempDirectory("swagger-composer");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.log = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void apply(RequestMappingContext context) {
        Optional<ApiChasisEntity> chasisParam = context.findAnnotation(ApiChasisEntity.class);
        if (chasisParam.isPresent()) {
            this.addCustomModel(context, chasisParam.get().value());
        }


    }

    private void addCustomModel(RequestMappingContext context, Class clazz) {
        try {
            log.debug("Found chasis param annotation creating custom model for class {}", clazz);
            StringBuilder postStringBuilder = new StringBuilder("public class " + clazz.getSimpleName() + "Post{");
            StringBuilder putStringBuilder = new StringBuilder("public class " + clazz.getSimpleName() + "Put{");
            for (Field f : clazz.getDeclaredFields()) {
                Id id = f.getAnnotation(Id.class);
                Column column = f.getAnnotation(Column.class);
                JoinColumn joinColumn = f.getAnnotation(JoinColumn.class);
                if(id != null){
                    putStringBuilder.append("public ").append(f.getType().getCanonicalName())
                            .append(" ").append(f.getName()).append(";");
                }else if(column != null){
                    if(column.insertable()) {
                        postStringBuilder.append("public ").append(f.getType().getCanonicalName())
                                .append(" ").append(f.getName()).append(";");
                    }
                    if(column.updatable()){
                        putStringBuilder.append("public ").append(f.getType().getCanonicalName())
                                .append(" ").append(f.getName()).append(";");
                    }
                }else if(joinColumn != null){
                    if(joinColumn.insertable()) {
                        postStringBuilder.append("public ").append(f.getType().getCanonicalName())
                                .append(" ").append(f.getName()).append(";");
                    }
                    if(joinColumn.updatable()){
                        putStringBuilder.append("public ").append(f.getType().getCanonicalName())
                                .append(" ").append(f.getName()).append(";");
                    }
                } else{
                    postStringBuilder.append("public ").append(f.getType().getCanonicalName())
                                .append(" ").append(f.getName()).append(";");
                    putStringBuilder.append("public ").append(f.getType().getCanonicalName())
                                .append(" ").append(f.getName()).append(";");
                }

            }
            Path tempClazz = Files.write(Paths.get(tempPath.toString() + "/" + clazz.getSimpleName() + "Post.java"),
                    postStringBuilder.toString().getBytes());
            ByteArrayOutputStream errOutput = new ByteArrayOutputStream();
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler.run(null, null, errOutput, tempClazz.toString()) != 0) {
                throw new IOException("Failed to compile temp class " + new String(errOutput.toByteArray()));
            }
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{tempPath.toUri().toURL()});
            Class demo = Class.forName(clazz.getSimpleName() + "Post", true, classLoader);
            context.operationModelsBuilder().addInputParam(demo);
        } catch (IOException | ClassNotFoundException e) {
            log.warn("Failed to create custom model for swagger documentation. This swagger documentation", e);
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return Objects.equals(delimiter, DocumentationType.SWAGGER_2);
    }

    @Override
    public void apply(OperationContext context) {
        ArrayList<Parameter> params = new ArrayList<>();        
        if(context.findAnnotation(ApiChasisEntity.class).isPresent()) {
            Class oClazz = context.findAnnotation(ApiChasisEntity.class).get().value();
            log.info("Found chasis param handling custom documentation");
            if (context.httpMethod().equals(HttpMethod.POST)) {
                params.add(new ParameterBuilder()
                        .name(oClazz.getSimpleName())
                        .description("")
                        .modelRef(new ModelRef(oClazz.getSimpleName() + "Post"))
                        .parameterType("body")
                        .required(true)
                        .build());
            }
        }
        context.operationBuilder()
                .parameters(params)
                .build();
    }
}
