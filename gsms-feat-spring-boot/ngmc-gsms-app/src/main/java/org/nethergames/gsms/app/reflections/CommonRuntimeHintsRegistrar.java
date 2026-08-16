package org.nethergames.gsms.app.reflections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.novemberain.quartz.mongodb.MongoDBJobStore;
import com.novemberain.quartz.mongodb.cluster.KamikazeErrorHandler;
import io.fabric8.kubernetes.api.model.Config;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.VersionInfo;
import org.nethergames.common.mongodb.repo.impl.ExtendedMongoRepositoryImpl;
import org.quartz.simpl.SimpleInstanceIdGenerator;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.providers.SpringWebProvider;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ImportRuntimeHints(CommonRuntimeHintsRegistrar.class)
public class CommonRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	private static final Logger logger = LoggerFactory.getLogger(CommonRuntimeHintsRegistrar.class);

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.resources().registerPattern("META-INF/services/io.fabric8.kubernetes.api.model.KubernetesResource");
		hints.resources().registerPattern("META-INF/services/io.fabric8.kubernetes.client.http.HttpClient$Factory");
		hints.resources().registerPattern("META-INF/native-image/vertx-version.txt");
		hints.resources().registerPattern("META-INF/vertx/vertx-version.txt");
		hints.reflection().registerType(VersionInfo.class, MemberCategory.values());
		hints.reflection().registerType(Config.class, MemberCategory.values());
		hints.reflection().registerType(IntOrString.class, MemberCategory.values());
		hints.reflection().registerType(SimpleInstanceIdGenerator.class, MemberCategory.values());
		hints.reflection().registerType(MongoDBJobStore.class, MemberCategory.values());
		hints.reflection().registerType(KamikazeErrorHandler.class, MemberCategory.values());

		hints.reflection().registerType(
				TypeReference.of("org.springdoc.core.providers.SpringWebProvider$$SpringCGLIB$$0"),
				builder -> builder.withField("CGLIB$FACTORY_DATA"));

		hints.reflection().registerType(
				TypeReference.of("org.springdoc.core.providers.SpringWebProvider$$SpringCGLIB$$0"),
				builder -> builder.withField("CGLIB$CALLBACK_FILTER"));

		hints.reflection().registerType(
				TypeReference.of("org.springdoc.core.providers.SpringWebProvider"),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_METHODS));

		registerClients(hints);
		registerJacksonKubernetesModels(hints);
	}

	private void registerClients(RuntimeHints hints) {
		Class<Client> clazz = Client.class;
		Reflections reflections = new Reflections(clazz.getPackage().getName(), clazz);
		Set<Class<? extends Client>> clients = new HashSet<>(reflections.getSubTypesOf(Client.class));
		clients.add(Client.class);

		for (Class<?> client : clients) {
			hints.reflection().registerType(client, MemberCategory.values());
			logger.info("[registerClients] registering {} for reflection", client.getName());
		}
	}

	private void registerJacksonKubernetesModels(RuntimeHints hints) {
		Class<KubernetesResource> clazz = KubernetesResource.class;
		Reflections reflections = new Reflections(clazz.getPackage().getName(), clazz);
		Set<Class<? extends KubernetesResource>> kubernetesModels = reflections.getSubTypesOf(KubernetesResource.class);

		Set<Class<?>> combined = new HashSet<>();
		combined.addAll(kubernetesModels);
		combined.addAll(resolveSerializationClasses(JsonSerialize.class, reflections));
		combined.addAll(resolveSerializationClasses(JsonDeserialize.class, reflections));

		for (Class<?> model : combined) {
			hints.reflection().registerType(model, MemberCategory.values());
			logger.info("[registerJacksonKubernetesModels] registering {} for reflection", model.getName());
		}
	}

	/**
	 * Extracts Jacksons Deserializer / Serializers specified in the classes annotations
	 */
	private <R extends Annotation> List<Class<?>> resolveSerializationClasses(Class<R> annotationClass, Reflections reflections) {
		List<Class<?>> result = new ArrayList<>();
		try {
			Method method = annotationClass.getMethod("using");
			Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotationClass);

			for (Class<?> clazzWithAnnotation : classes) {
				R annotation = clazzWithAnnotation.getAnnotation(annotationClass);
				if (annotation != null) {
					Object usingClass = method.invoke(annotation);
					if (usingClass instanceof Class<?>) {
						result.add((Class<?>) usingClass);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error resolving serialization classes for annotation {}", annotationClass.getName(), e);
		}
		return result;
	}
}