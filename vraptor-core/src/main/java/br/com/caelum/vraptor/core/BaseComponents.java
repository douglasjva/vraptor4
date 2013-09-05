/***
 * Copyright (c) 2009 Caelum - www.caelum.com.br/opensource
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.caelum.vraptor.core;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.util.AnnotationLiteral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.iogi.Instantiator;
import br.com.caelum.iogi.spi.DependencyProvider;
import br.com.caelum.iogi.spi.ParameterNamesProvider;
import br.com.caelum.vraptor.Controller;
import br.com.caelum.vraptor.Convert;
import br.com.caelum.vraptor.Intercepts;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.Validator;
import br.com.caelum.vraptor.config.ApplicationConfiguration;
import br.com.caelum.vraptor.config.Configuration;
import br.com.caelum.vraptor.controller.ControllerNotFoundHandler;
import br.com.caelum.vraptor.controller.DefaultControllerNotFoundHandler;
import br.com.caelum.vraptor.controller.DefaultMethodNotAllowedHandler;
import br.com.caelum.vraptor.controller.MethodNotAllowedHandler;
import br.com.caelum.vraptor.deserialization.DefaultDeserializers;
import br.com.caelum.vraptor.deserialization.Deserializer;
import br.com.caelum.vraptor.deserialization.Deserializers;
import br.com.caelum.vraptor.deserialization.Deserializes;
import br.com.caelum.vraptor.deserialization.DeserializesHandler;
import br.com.caelum.vraptor.deserialization.FormDeserializer;
import br.com.caelum.vraptor.deserialization.JsonDeserializer;
import br.com.caelum.vraptor.deserialization.XMLDeserializer;
import br.com.caelum.vraptor.deserialization.XStreamXMLDeserializer;
import br.com.caelum.vraptor.http.DefaultControllerTranslator;
import br.com.caelum.vraptor.http.DefaultFormatResolver;
import br.com.caelum.vraptor.http.EncodingHandlerFactory;
import br.com.caelum.vraptor.http.FormatResolver;
import br.com.caelum.vraptor.http.ParameterNameProvider;
import br.com.caelum.vraptor.http.ParametersProvider;
import br.com.caelum.vraptor.http.ParanamerNameProvider;
import br.com.caelum.vraptor.http.UrlToControllerTranslator;
import br.com.caelum.vraptor.http.iogi.InstantiatorWithErrors;
import br.com.caelum.vraptor.http.iogi.IogiParametersProvider;
import br.com.caelum.vraptor.http.iogi.VRaptorDependencyProvider;
import br.com.caelum.vraptor.http.iogi.VRaptorInstantiator;
import br.com.caelum.vraptor.http.iogi.VRaptorParameterNamesProvider;
import br.com.caelum.vraptor.http.route.DefaultRouter;
import br.com.caelum.vraptor.http.route.DefaultTypeFinder;
import br.com.caelum.vraptor.http.route.Evaluator;
import br.com.caelum.vraptor.http.route.JavaEvaluator;
import br.com.caelum.vraptor.http.route.NoRoutesConfiguration;
import br.com.caelum.vraptor.http.route.PathAnnotationRoutesParser;
import br.com.caelum.vraptor.http.route.Router;
import br.com.caelum.vraptor.http.route.RoutesConfiguration;
import br.com.caelum.vraptor.http.route.RoutesParser;
import br.com.caelum.vraptor.http.route.TypeFinder;
import br.com.caelum.vraptor.interceptor.ControllerLookupInterceptor;
import br.com.caelum.vraptor.interceptor.DefaultSimpleInterceptorStack;
import br.com.caelum.vraptor.interceptor.DefaultTypeNameExtractor;
import br.com.caelum.vraptor.interceptor.DeserializingInterceptor;
import br.com.caelum.vraptor.interceptor.ExceptionHandlerInterceptor;
import br.com.caelum.vraptor.interceptor.ExecuteMethodInterceptor;
import br.com.caelum.vraptor.interceptor.FlashInterceptor;
import br.com.caelum.vraptor.interceptor.ForwardToDefaultViewInterceptor;
import br.com.caelum.vraptor.interceptor.InstantiateInterceptor;
import br.com.caelum.vraptor.interceptor.InterceptorRegistry;
import br.com.caelum.vraptor.interceptor.OutjectResult;
import br.com.caelum.vraptor.interceptor.ParameterIncluderInterceptor;
import br.com.caelum.vraptor.interceptor.ParametersInstantiatorInterceptor;
import br.com.caelum.vraptor.interceptor.SimpleInterceptorStack;
import br.com.caelum.vraptor.interceptor.TopologicalSortedInterceptorRegistry;
import br.com.caelum.vraptor.interceptor.TypeNameExtractor;
import br.com.caelum.vraptor.interceptor.download.DownloadInterceptor;
import br.com.caelum.vraptor.interceptor.multipart.CommonsUploadMultipartInterceptor;
import br.com.caelum.vraptor.interceptor.multipart.DefaultMultipartConfig;
import br.com.caelum.vraptor.interceptor.multipart.DefaultServletFileUploadCreator;
import br.com.caelum.vraptor.interceptor.multipart.MultipartConfig;
import br.com.caelum.vraptor.interceptor.multipart.MultipartInterceptor;
import br.com.caelum.vraptor.interceptor.multipart.NullMultipartInterceptor;
import br.com.caelum.vraptor.interceptor.multipart.Servlet3MultipartInterceptor;
import br.com.caelum.vraptor.interceptor.multipart.ServletFileUploadCreator;
import br.com.caelum.vraptor.ioc.ControllerHandler;
import br.com.caelum.vraptor.ioc.ConverterHandler;
import br.com.caelum.vraptor.ioc.InterceptorStereotypeHandler;
import br.com.caelum.vraptor.proxy.JavassistProxifier;
import br.com.caelum.vraptor.proxy.Proxifier;
import br.com.caelum.vraptor.restfulie.RestHeadersHandler;
import br.com.caelum.vraptor.restfulie.headers.DefaultRestDefaults;
import br.com.caelum.vraptor.restfulie.headers.DefaultRestHeadersHandler;
import br.com.caelum.vraptor.restfulie.headers.RestDefaults;
import br.com.caelum.vraptor.serialization.DefaultRepresentationResult;
import br.com.caelum.vraptor.serialization.HTMLSerialization;
import br.com.caelum.vraptor.serialization.I18nMessageSerialization;
import br.com.caelum.vraptor.serialization.JSONPSerialization;
import br.com.caelum.vraptor.serialization.JSONSerialization;
import br.com.caelum.vraptor.serialization.NullProxyInitializer;
import br.com.caelum.vraptor.serialization.ProxyInitializer;
import br.com.caelum.vraptor.serialization.RepresentationResult;
import br.com.caelum.vraptor.serialization.XMLSerialization;
import br.com.caelum.vraptor.serialization.xstream.NullConverter;
import br.com.caelum.vraptor.serialization.xstream.XStreamConverters;
import br.com.caelum.vraptor.serialization.xstream.XStreamJSONPSerialization;
import br.com.caelum.vraptor.serialization.xstream.XStreamJSONSerialization;
import br.com.caelum.vraptor.serialization.xstream.XStreamXMLSerialization;
import br.com.caelum.vraptor.validator.BeanValidator;
import br.com.caelum.vraptor.validator.DefaultBeanValidator;
import br.com.caelum.vraptor.validator.DefaultValidator;
import br.com.caelum.vraptor.validator.MessageConverter;
import br.com.caelum.vraptor.validator.MessageInterpolatorFactory;
import br.com.caelum.vraptor.validator.MethodValidatorFactoryCreator;
import br.com.caelum.vraptor.validator.MethodValidatorInterceptor;
import br.com.caelum.vraptor.validator.NullBeanValidator;
import br.com.caelum.vraptor.validator.Outjector;
import br.com.caelum.vraptor.validator.ReplicatorOutjector;
import br.com.caelum.vraptor.validator.ValidatorCreator;
import br.com.caelum.vraptor.validator.ValidatorFactoryCreator;
import br.com.caelum.vraptor.view.AcceptHeaderToFormat;
import br.com.caelum.vraptor.view.DefaultAcceptHeaderToFormat;
import br.com.caelum.vraptor.view.DefaultHttpResult;
import br.com.caelum.vraptor.view.DefaultLogicResult;
import br.com.caelum.vraptor.view.DefaultPageResult;
import br.com.caelum.vraptor.view.DefaultPathResolver;
import br.com.caelum.vraptor.view.DefaultRefererResult;
import br.com.caelum.vraptor.view.DefaultStatus;
import br.com.caelum.vraptor.view.DefaultValidationViewsFactory;
import br.com.caelum.vraptor.view.EmptyResult;
import br.com.caelum.vraptor.view.FlashScope;
import br.com.caelum.vraptor.view.HttpResult;
import br.com.caelum.vraptor.view.LogicResult;
import br.com.caelum.vraptor.view.PageResult;
import br.com.caelum.vraptor.view.PathResolver;
import br.com.caelum.vraptor.view.RefererResult;
import br.com.caelum.vraptor.view.SessionFlashScope;
import br.com.caelum.vraptor.view.Status;
import br.com.caelum.vraptor.view.ValidationViewsFactory;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * List of base components to vraptor.<br/>
 * Those components should be available with any chosen ioc implementation.
 *
 * @author guilherme silveira
 */
public class BaseComponents {

    static final Logger logger = LoggerFactory.getLogger(BaseComponents.class);

    private final static Map<Class<?>, Class<?>> APPLICATION_COMPONENTS = classMap(
    		EncodingHandlerFactory.class, 	EncodingHandlerFactory.class,
    		AcceptHeaderToFormat.class, 	DefaultAcceptHeaderToFormat.class,
    		Converters.class, 				DefaultConverters.class,
            InterceptorRegistry.class, 		TopologicalSortedInterceptorRegistry.class,
            InterceptorHandlerFactory.class,DefaultInterceptorHandlerFactory.class,
            MultipartConfig.class, 			DefaultMultipartConfig.class,
            UrlToControllerTranslator.class, 	DefaultControllerTranslator.class,
            Router.class, 					DefaultRouter.class,
            TypeNameExtractor.class, 		DefaultTypeNameExtractor.class,
            ControllerNotFoundHandler.class, 	DefaultControllerNotFoundHandler.class,
            MethodNotAllowedHandler.class,	DefaultMethodNotAllowedHandler.class,
            RoutesConfiguration.class, 		NoRoutesConfiguration.class,
            Deserializers.class,			DefaultDeserializers.class,
            Proxifier.class, 				JavassistProxifier.class,
            ParameterNameProvider.class, 	ParanamerNameProvider.class,
            TypeFinder.class, 				DefaultTypeFinder.class,
            RoutesParser.class, 			PathAnnotationRoutesParser.class,
            Routes.class,					DefaultRoutes.class,
            RestDefaults.class,				DefaultRestDefaults.class,
            Evaluator.class,				JavaEvaluator.class,
            StaticContentHandler.class,		DefaultStaticContentHandler.class,
            SingleValueConverter.class,     NullConverter.class,
            ProxyInitializer.class,			NullProxyInitializer.class
    );

    private static final Map<Class<?>, Class<?>> REQUEST_COMPONENTS = classMap(
    			InterceptorStack.class, 						DefaultInterceptorStack.class,
    			SimpleInterceptorStack.class,                DefaultSimpleInterceptorStack.class,
            MethodInfo.class, 						MethodInfo.class,
            LogicResult.class, 								DefaultLogicResult.class,
            PageResult.class, 								DefaultPageResult.class,
            HttpResult.class, 								DefaultHttpResult.class,
            RefererResult.class, 							DefaultRefererResult.class,
            PathResolver.class, 							DefaultPathResolver.class,
            ValidationViewsFactory.class,					DefaultValidationViewsFactory.class,
            Result.class, 									DefaultResult.class,
            Validator.class, 								DefaultValidator.class,
            Outjector.class, 								ReplicatorOutjector.class,
            DownloadInterceptor.class, 						DownloadInterceptor.class,
            EmptyResult.class, 								EmptyResult.class,
            ExecuteMethodInterceptor.class, 				ExecuteMethodInterceptor.class,
            ExceptionHandlerInterceptor.class,              ExceptionHandlerInterceptor.class,
            ExceptionMapper.class,                          DefaultExceptionMapper.class,
            FlashInterceptor.class, 						FlashInterceptor.class,
            ForwardToDefaultViewInterceptor.class, 			ForwardToDefaultViewInterceptor.class,
            InstantiateInterceptor.class, 					InstantiateInterceptor.class,
            DeserializingInterceptor.class, 				DeserializingInterceptor.class,
            JsonDeserializer.class,							JsonDeserializer.class,
            FormDeserializer.class,							FormDeserializer.class,
            Localization.class, 							JstlLocalization.class,
            OutjectResult.class, 							OutjectResult.class,
            ParametersInstantiatorInterceptor.class, 		ParametersInstantiatorInterceptor.class,
            ControllerLookupInterceptor.class, 				ControllerLookupInterceptor.class,
            Status.class,									DefaultStatus.class,
            XMLDeserializer.class,			                XStreamXMLDeserializer.class,
            XMLSerialization.class,							XStreamXMLSerialization.class,
            JSONSerialization.class,						XStreamJSONSerialization.class,
            JSONPSerialization.class,						XStreamJSONPSerialization.class,
            HTMLSerialization.class,						HTMLSerialization.class,
            I18nMessageSerialization.class,					I18nMessageSerialization.class,
            RepresentationResult.class,						DefaultRepresentationResult.class,
            FormatResolver.class,							DefaultFormatResolver.class,
            Configuration.class,							ApplicationConfiguration.class,
            RestHeadersHandler.class,						DefaultRestHeadersHandler.class,
            FlashScope.class,								SessionFlashScope.class,
            XStreamConverters.class,                        XStreamConverters.class,
            MessageConverter.class,							MessageConverter.class,
            ParameterIncluderInterceptor.class,					ParameterIncluderInterceptor.class
    );


	private static final HashMap<Class<? extends Annotation>, StereotypeInfo> STEREOTYPES_INFO = new HashMap<Class<? extends Annotation>,StereotypeInfo>();
    static {
    		STEREOTYPES_INFO.put(Controller.class,new StereotypeInfo(Controller.class,ControllerHandler.class,new AnnotationLiteral<ControllerQualifier>() {}));
    		STEREOTYPES_INFO.put(Convert.class,new StereotypeInfo(Convert.class,ConverterHandler.class,new AnnotationLiteral<ConvertQualifier>() {}));
    		STEREOTYPES_INFO.put(Deserializes.class,new StereotypeInfo(Deserializes.class,DeserializesHandler.class,new AnnotationLiteral<DeserializesQualifier>() {}));
    		STEREOTYPES_INFO.put(Intercepts.class,new StereotypeInfo(Intercepts.class,InterceptorStereotypeHandler.class,new AnnotationLiteral<InterceptsQualifier>() {}));

    }

    private static final Set<Class<? extends Deserializer>> DESERIALIZERS = Collections.<Class<? extends Deserializer>>singleton(XMLDeserializer.class);


    public static Set<Class<? extends Deserializer>> getDeserializers() {
		return DESERIALIZERS;
	}

    public static Map<Class<?>, Class<?>> getApplicationScoped() {
        APPLICATION_COMPONENTS.put(DependencyProvider.class, VRaptorDependencyProvider.class);

        // try put beanval 1.1 or beanval 1.0 if available
        if (isClassPresent("javax.validation.executable.ExecutableValidator")) {
            APPLICATION_COMPONENTS.put(ValidatorCreator.class, ValidatorCreator.class);
            APPLICATION_COMPONENTS.put(ValidatorFactoryCreator.class, ValidatorFactoryCreator.class);
            APPLICATION_COMPONENTS.put(MethodValidatorFactoryCreator.class, MethodValidatorFactoryCreator.class);
            APPLICATION_COMPONENTS.put(MessageInterpolatorFactory.class, MessageInterpolatorFactory.class);
        } else if (isClassPresent("javax.validation.Validation")) {
            APPLICATION_COMPONENTS.put(ValidatorCreator.class, ValidatorCreator.class);
            APPLICATION_COMPONENTS.put(ValidatorFactoryCreator.class, ValidatorFactoryCreator.class);
            APPLICATION_COMPONENTS.put(MessageInterpolatorFactory.class, MessageInterpolatorFactory.class);
        }

    	return Collections.unmodifiableMap(APPLICATION_COMPONENTS);
    }

    public static Map<Class<?>, Class<?>> getRequestScoped() {
        // try put beanval 1.1 or beanval 1.0 if available
        if (isClassPresent("javax.validation.executable.ExecutableValidator")) {
            REQUEST_COMPONENTS.put(BeanValidator.class, DefaultBeanValidator.class);
            REQUEST_COMPONENTS.put(MethodValidatorInterceptor.class, MethodValidatorInterceptor.class);
        } else if (isClassPresent("javax.validation.Validation")) {
            REQUEST_COMPONENTS.put(BeanValidator.class, DefaultBeanValidator.class);
        } else {
            REQUEST_COMPONENTS.put(BeanValidator.class, NullBeanValidator.class);
        }

        if (isClassPresent("org.apache.commons.fileupload.FileItem")) {
            REQUEST_COMPONENTS.put(MultipartInterceptor.class, CommonsUploadMultipartInterceptor.class);
            REQUEST_COMPONENTS.put(ServletFileUploadCreator.class, DefaultServletFileUploadCreator.class);
        } else if (isClassPresent("javax.servlet.http.Part")) {
            REQUEST_COMPONENTS.put(MultipartInterceptor.class, Servlet3MultipartInterceptor.class);
        } else {
    	    logger.warn("There is neither commons-fileupload nor servlet3 handlers registered. " +
    	    		"If you are willing to upload a file, please add the commons-fileupload in " +
    	    		"your classpath or use a Servlet 3 Container");
            REQUEST_COMPONENTS.put(MultipartInterceptor.class, NullMultipartInterceptor.class);
    	}

        REQUEST_COMPONENTS.put(ParametersProvider.class, IogiParametersProvider.class);
        REQUEST_COMPONENTS.put(ParameterNamesProvider.class, VRaptorParameterNamesProvider.class);
        REQUEST_COMPONENTS.put(InstantiatorWithErrors.class, VRaptorInstantiator.class);
        REQUEST_COMPONENTS.put(Instantiator.class, VRaptorInstantiator.class);

        return Collections.unmodifiableMap(REQUEST_COMPONENTS);
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Set<StereotypeInfo> getStereotypesInfo() {
    		return new HashSet<StereotypeInfo>(STEREOTYPES_INFO.values());
    }

    public static Set<Class<? extends Annotation>> getStereotypes() {
    		Set<StereotypeInfo> stereotypesInfo = getStereotypesInfo();
    		HashSet<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();
    		for (StereotypeInfo stereotypeInfo : stereotypesInfo) {
    			stereotypes.add(stereotypeInfo.getStereotype());
		}
    		return stereotypes;
    }
    public static Map<Class<? extends Annotation>,StereotypeInfo> getStereotypesInfoMap() {
    		return STEREOTYPES_INFO;
    }

    private static Map<Class<?>, Class<?>> classMap(Class<?>... items) {
        HashMap<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
        Iterator<Class<?>> it = Arrays.asList(items).iterator();
        while (it.hasNext()) {
            Class<?> key = it.next();
            Class<?> value = it.next();
            if (value == null) {
                throw new IllegalArgumentException("The number of items should be even.");
            }
            map.put(key, value);
        }
        return map;
    }


}