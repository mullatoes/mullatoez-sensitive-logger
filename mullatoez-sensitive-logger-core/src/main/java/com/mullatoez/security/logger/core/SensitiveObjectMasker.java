package com.mullatoez.security.logger.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.time.temporal.Temporal;
import java.util.*;

public class SensitiveObjectMasker {

    private static final int DEFAULT_MAX_DEPTH = 5;

    private final MaskingStrategy maskingStrategy;

    public SensitiveObjectMasker() {
        this.maskingStrategy = new MaskingStrategy();
    }

    public Object mask(Object source, boolean maskFull) {
        MaskingMode defaultMode = maskFull ? MaskingMode.FULL : MaskingMode.PARTIAL;
        return maskValue(source, false, defaultMode, 0, new IdentityHashMap<>());
    }

    private Object maskValue(
            Object value,
            boolean sensitive,
            MaskingMode mode,
            int depth,
            IdentityHashMap<Object, Boolean> visited
    ) {
        if (value == null) {
            return null;
        }

        if (sensitive && isSimpleValue(value)) {
            return maskingStrategy.mask(value, mode);
        }

        if (isSimpleValue(value)) {
            return value;
        }

        if (depth > DEFAULT_MAX_DEPTH) {
            return "[MAX_DEPTH_REACHED]";
        }

        if (visited.containsKey(value)) {
            return "[CIRCULAR_REFERENCE]";
        }

        visited.put(value, Boolean.TRUE);

        if (value instanceof Collection<?> collection) {
            return maskCollection(collection, sensitive, mode, depth, visited);
        }

        if (value instanceof Map<?, ?> map) {
            return maskMap(map, mode, depth, visited);
        }

        Class<?> clazz = value.getClass();

        if (clazz.isRecord()) {
            return maskRecord(value, clazz, mode, depth, visited);
        }

        return maskPojo(value, clazz, mode, depth, visited);
    }

    private List<Object> maskCollection(
            Collection<?> collection,
            boolean sensitive,
            MaskingMode mode,
            int depth,
            IdentityHashMap<Object, Boolean> visited
    ) {
        List<Object> masked = new ArrayList<>();

        for (Object item : collection) {
            masked.add(maskValue(item, sensitive, mode, depth + 1, visited));
        }

        return masked;
    }

    private Map<String, Object> maskMap(
            Map<?, ?> map,
            MaskingMode mode,
            int depth,
            IdentityHashMap<Object, Boolean> visited
    ) {
        Map<String, Object> masked = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());

            /*
             * Strict rule:
             * Map keys are not used to guess sensitivity.
             * Only annotated DTO/record fields are masked.
             */
            masked.put(key, maskValue(entry.getValue(), false, mode, depth + 1, visited));
        }

        return masked;
    }

    private Map<String, Object> maskRecord(
            Object record,
            Class<?> clazz,
            MaskingMode defaultMode,
            int depth,
            IdentityHashMap<Object, Boolean> visited
    ) {
        Map<String, Object> masked = new LinkedHashMap<>();

        for (RecordComponent component : clazz.getRecordComponents()) {
            try {
                String name = component.getName();
                Object value = component.getAccessor().invoke(record);

                Sensitive sensitiveAnnotation = component.getAnnotation(Sensitive.class);
                boolean sensitive = sensitiveAnnotation != null;
                MaskingMode fieldMode = resolveMode(sensitiveAnnotation, defaultMode);

                masked.put(name, maskValue(value, sensitive, fieldMode, depth + 1, visited));
            } catch (Exception ex) {
                masked.put(component.getName(), "[UNREADABLE_FIELD]");
            }
        }

        return masked;
    }

    private Map<String, Object> maskPojo(
            Object pojo,
            Class<?> clazz,
            MaskingMode defaultMode,
            int depth,
            IdentityHashMap<Object, Boolean> visited
    ) {
        Map<String, Object> masked = new LinkedHashMap<>();

        for (Field field : getAllFields(clazz)) {
            if (shouldSkipField(field)) {
                continue;
            }

            try {
                field.setAccessible(true);

                String name = field.getName();
                Object value = field.get(pojo);

                Sensitive sensitiveAnnotation = field.getAnnotation(Sensitive.class);
                boolean sensitive = sensitiveAnnotation != null;
                MaskingMode fieldMode = resolveMode(sensitiveAnnotation, defaultMode);

                masked.put(name, maskValue(value, sensitive, fieldMode, depth + 1, visited));
            } catch (Exception ex) {
                masked.put(field.getName(), "[UNREADABLE_FIELD]");
            }
        }

        return masked;
    }

    private MaskingMode resolveMode(Sensitive sensitiveAnnotation, MaskingMode defaultMode) {
        if (sensitiveAnnotation == null || sensitiveAnnotation.mode() == SensitiveMode.DEFAULT) {
            return defaultMode;
        }

        if (sensitiveAnnotation.mode() == SensitiveMode.FULL) {
            return MaskingMode.FULL;
        }

        return MaskingMode.PARTIAL;
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields;
    }

    private boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();

        return Modifier.isStatic(modifiers)
                || Modifier.isTransient(modifiers)
                || field.isSynthetic();
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof UUID
                || value instanceof Date
                || value instanceof Temporal;
    }
}