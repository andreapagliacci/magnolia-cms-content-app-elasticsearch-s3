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

import com.whitelabel.app.generic.annotation.Boost;
import com.whitelabel.app.generic.entity.GenericItem;
import com.whitelabel.app.generic.others.LogStatus;
import com.whitelabel.app.generic.search.Params;
import com.whitelabel.app.generic.service.RepositoryService;

/**
 * From Class retrieving all fields. It works with reflections
 *
 */
public class GenericClassConverter {
	private RepositoryService repositoryService;

	public GenericClassConverter(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	/**
	 * Gets the all fields.
	 *
	 * @param type the type
	 * @return the all fields
	 */
	public List<Field> getAllFields(Class<?> type) {
		if (repositoryService.getCacheHelper().containsConverterClassKey(type)) {
			return repositoryService.getCacheHelper().getConverterClass(type);
		} else {
			List<Field> fields = new ArrayList<>();
			getAllFieldsRecursive(fields, type);
			repositoryService.getCacheHelper().putConverterClass(type, fields);
			return fields;
		}

	}

	public Optional<Field> getFieldFromAnnotation(Class<?> type, Class annotationClass) {
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
	private void getAllFieldsRecursive(List<Field> fields, Class<?> type) {
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
	private boolean genericContainsField(Field declaredField, List<Field> fields) {
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
	private boolean genericEqualsFields(Field obj, Field other) {
		return (obj.getName() == other.getName()) && (obj.getType() == other.getType());
	}

	/**
	 * Gets the all Generic Class Items.
	 *
	 * @param <D>
	 *
	 * @return all Generic Class Items
	 */
	public <D> List<Class<? extends D>> getAllClassGenericItem(Class<D> subTypeOf) {
		if (repositoryService.getCacheHelper().containsAllSubTypeOfClassKey(subTypeOf)) {
			return repositoryService.getCacheHelper().getAllSubTypeOfClass(subTypeOf);
		} else {
			Reflections reflections = new Reflections("com");
			Set<Class<? extends D>> classes = reflections.getSubTypesOf(subTypeOf);
			List<Class<? extends D>> list = classes.stream().collect(Collectors.toList());
			repositoryService.getCacheHelper().putAllSubTypeOfClass(subTypeOf, list);
			return list;
		}

	}

	public Class getClassFromName(String nameClass) {
		try {
			return Class.forName(nameClass);
		} catch (ClassNotFoundException e) {
			repositoryService.getLogService().logger(LogStatus.ERROR, "Error ClassFromName",
					GenericClassConverter.class, e);
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
	public <D> List<String> getClassByName(Class<D> subTypeOf) {
		List<Class<? extends D>> allClassElasticSearchIndex = getAllClassGenericItem(subTypeOf);
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
	public <D> D createInstanceFromClassAndValues(Class<? extends D> typeClass, Params params, D newObj)
			throws InstantiationException, IllegalAccessException {
		if (newObj == null) {
			newObj = typeClass.newInstance();
		}
		try {
			List<String> fields = getAllFields(typeClass).stream().filter(field -> {
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
							repositoryService.getLogService().logger(LogStatus.ERROR,
									"createInstanceFromClassAndValues", GenericClassConverter.class,
									superClassException);
						}
					}
					if (nameField != null) {
						try {
							nameField.setAccessible(true);
							nameField.set(newObj, repositoryService.getFactoryConverter()
									.convertToObject(params.getFields().get(name), nameField.getType(), nameField));
						} catch (Exception e) {
							repositoryService.getLogService().logger(LogStatus.ERROR,
									"createInstanceFromClassAndValues", GenericClassConverter.class, e);
						}
					}
				} catch (Exception e) {
					repositoryService.getLogService().logger(LogStatus.ERROR, "createInstanceFromClassAndValues",
							GenericClassConverter.class, e);
				}
			}
		} catch (Exception e) {
			repositoryService.getLogService().logger(LogStatus.ERROR, "createInstanceFromClassAndValues",
					GenericClassConverter.class, e);
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
	public <T extends GenericItem> Object getFieldFromInstance(Class<? extends GenericItem> typeClass, T obj,
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
	 * Gets the class from params.
	 *
	 * @param <D>
	 *
	 * @param params the params
	 * @return the class from params
	 */
	public <D> Class<? extends D> getClassFromParams(Params searchParams, Class<D> typeClass) {
		if (searchParams != null && searchParams.getFields() != null) {
			String indexName = (String) searchParams.getFields().get(GenericConstants.INDEX_ID);
			repositoryService.getLogService().logger(LogStatus.ERROR,
					"[getClassFromParams]" + indexName + typeClass.getName(), GenericClassConverter.class, null);

			return getClassFromClassName(indexName, typeClass);
		}
		return null;
	}

	public <D> Class<? extends D> getClassFromClassName(String nameClass, Class<D> typeClass) {
		try {
			return getAllClassGenericItem(typeClass).stream().filter(classObj -> {
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
	public String getFieldNameBoostField(Field field) {
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
	public boolean isBoostField(Field field) {
		Boost[] boosts = field.getAnnotationsByType(Boost.class);
		if (boosts == null || boosts.length == 0) {
			return false;
		}
		return true;
	}

	public <D> Class<? extends D> getClassItem(Params params, Object fieldName, Class<D> classTypes) {
		Class<? extends D> classType = getAllClassGenericItem(classTypes).stream().filter(classObj -> {
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
