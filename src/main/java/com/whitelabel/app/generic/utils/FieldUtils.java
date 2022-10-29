/*
 *
 */
package com.whitelabel.app.generic.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.slf4j.Logger;

import com.whitelabel.app.generic.annotation.Boost;
import com.whitelabel.app.generic.entity.GenericItem;
import com.whitelabel.app.generic.search.Params;

/**
 * From Class retrieving all fields. It works with reflections
 *
 */
public class FieldUtils {

	/** The log. */
	private static Logger log = org.slf4j.LoggerFactory.getLogger(FieldUtils.class);

	/**
	 * Gets the all fields.
	 *
	 * @param type the type
	 * @return the all fields
	 */
	public static List<Field> getAllFields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		getAllFieldsRecursive(fields, type);
		return fields;
	}

	public static Optional<Field> getFieldFromAnnotation(Class<?> type, Class annotationClass) {
		Optional<Field> fields = getAllFields(type).stream().filter(field -> {
			return field.getAnnotation(annotationClass) != null;
		}).findFirst();
		return fields;
	}

	/**
	 * Gets the all fields recursive.
	 *
	 * @param fields the fields
	 * @param type   the type
	 * @return the all fields recursive
	 */
	private static void getAllFieldsRecursive(List<Field> fields, Class<?> type) {
		List<Field> declaredFields = Arrays.asList(type.getDeclaredFields());
		for (Field declaredField : declaredFields) {
			if (!genericContainsField(declaredField, fields))
				fields.add(declaredField);
		}
		if (type.getSuperclass() != null) {
			getAllFieldsRecursive(fields, type.getSuperclass());
		}
	}

	/**
	 * Generic contains field.
	 *
	 * @param declaredField the declared field
	 * @param fields        the fields
	 * @return true, if successful
	 */
	private static boolean genericContainsField(Field declaredField, List<Field> fields) {
		for (Field field : fields) {
			if (genericEqualsFields(field, declaredField))
				return true;
		}
		return false;
	}

	/**
	 * Generic equals fields.
	 *
	 * @param obj   the obj
	 * @param other the other
	 * @return true, if successful
	 */
	private static boolean genericEqualsFields(Field obj, Field other) {
		return (obj.getName() == other.getName()) && (obj.getType() == other.getType());
	}

	/**
	 * Gets the all Generic Class Items.
	 *
	 * @param <D>
	 *
	 * @return all Generic Class Items
	 */
	public static <D> List<Class<? extends D>> getAllClassGenericItem(Class<D> subTypeOf) {
		Reflections reflections = new Reflections("com");
		Set<Class<? extends D>> classes = reflections.getSubTypesOf(subTypeOf);
		return classes.stream().collect(Collectors.toList());
	}

	public static Class getClassFromName(String nameClass) {
		try {
			return Class.forName(nameClass);
		} catch (ClassNotFoundException e) {
			log.error("Error ClassFromName", e);
		}
		return null;
	}

	/**
	 * Gets the class by name.
	 *
	 * @param <D>
	 *
	 * @return the class by name
	 */
	public static <D> List<String> getClassByName(Class<D> subTypeOf) {
		List<Class<? extends D>> allClassElasticSearchIndex = FieldUtils.getAllClassGenericItem(subTypeOf);
		return allClassElasticSearchIndex.stream().map(classType -> {
			return classType.getName();
		}).collect(Collectors.toList());
	}

	/**
	 * Creates the instance from class and values.
	 *
	 * @param <D>
	 *
	 * @param <T>
	 *
	 * @param <T>       the generic type
	 * @param typeClass the type class
	 * @param params    the params
	 * @param newObj    TODO
	 * @return the t
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static <D> D createInstanceFromClassAndValues(Class<? extends D> typeClass, Params params, D newObj)
			throws InstantiationException, IllegalAccessException {
		FactoryConverter converter = new FactoryConverter();
		if (newObj == null) {
			newObj = typeClass.newInstance();
		}
		try {
			List<String> fields = FieldUtils.getAllFields(typeClass).stream().filter(field -> {
				return params.getFields() != null && field.getName() != null
						&& params.getFields().containsKey(field.getName());
			}).map(field -> {
				return field.getName();
			}).collect(Collectors.toList());

			for (String name : fields) {
				try {
					Field nameField = null;
					try {
						nameField = typeClass.getDeclaredField(name);
					} catch (Exception exceptionField) {
						try {
							nameField = typeClass.getSuperclass().getDeclaredField(name);
						} catch (Exception superClassException) {
							log.error("createInstanceFromClassAndValues", superClassException);
						}
					}
					if (nameField != null) {
						try {
							nameField.setAccessible(true);
							nameField.set(newObj, converter.convertToObject(params.getFields().get(name),
									nameField.getType(), nameField));
						} catch (Exception e) {
							log.error("createInstanceFromClassAndValues", e);
						}
					}
				} catch (Exception e) {
					log.error("createInstanceFromClassAndValues", e);
				}
			}
		} catch (Exception e) {
			log.error("createInstanceFromClassAndValues", e);
		}
		return newObj;
	}

	/**
	 * Gets the field from instance.
	 *
	 * @param <T>       the generic type
	 * @param typeClass the type class
	 * @param obj       the obj
	 * @param nameField the name field
	 * @return the field from instance
	 */
	public static <T extends GenericItem> Object getFieldFromInstance(Class<? extends GenericItem> typeClass, T obj,
			String nameField) {
		try {
			java.lang.reflect.Field field = typeClass.getDeclaredField(nameField);
			field.setAccessible(true);
			return field.get(obj);
		} catch (Exception e) {
		}
		return "";
	}

	/**
	 * Gets the class from search params.
	 *
	 * @param <D>
	 *
	 * @param searchParams the search params
	 * @return the class from search params
	 */
	public static <D> Class<? extends D> getClassFromSearchParams(Params searchParams, Class<D> typeClass) {
		if (searchParams != null && searchParams.getFields() != null) {
			String indexName = (String) searchParams.getFields().get(GenericConstants.INDEX_ID);
			return getClassFromClassName(indexName, typeClass);
		}
		return null;

	}

	public static <D> Class<? extends D> getClassFromClassName(String nameClass, Class<D> typeClass) {
		try {
			return FieldUtils.getAllClassGenericItem(typeClass).stream().filter(classObj -> {
				String name = classObj.getName();
				if (name != null && nameClass != null) {
					return name.equals(nameClass);
				}
				return false;
			}).findFirst().get();
		} catch (NoSuchElementException e) {
		}
		return null;

	}

	/**
	 * Gets the field name boost field.
	 *
	 * @param field the field
	 * @return the field name boost field
	 */
	public static String getFieldNameBoostField(Field field) {
		Boost[] boosts = field.getAnnotationsByType(Boost.class);
		if (boosts == null || boosts.length == 0) {
			return null;
		}
		return boosts[0].name();
	}

	/**
	 * Checks if is boost field.
	 *
	 * @param field the field
	 * @return true, if is boost field
	 */
	public static boolean isBoostField(Field field) {
		Boost[] boosts = field.getAnnotationsByType(Boost.class);
		if (boosts == null || boosts.length == 0) {
			return false;
		}
		return true;
	}

	public static <D> Class<? extends D> getClassItem(Params params, Object fieldName, Class<D> classTypes) {
		Class<? extends D> classType = FieldUtils.getAllClassGenericItem(classTypes).stream().filter(classObj -> {
			String name = classObj.getName();
			String indexName = (String) fieldName;
			if (name != null && indexName != null) {
				return name.equals(indexName);
			}
			return false;
		}).findFirst().get();
		return classType;
	}
}
