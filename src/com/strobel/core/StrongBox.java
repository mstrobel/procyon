package com.strobel.core;

import com.strobel.reflection.Type;
import com.strobel.reflection.Types;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("PublicField")
public final class StrongBox<T> implements IStrongBox {
    public T value;

    public StrongBox() {}

    public StrongBox(final T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object value) {
        this.value = (T) value;
    }

    public static Type<? extends IStrongBox> getBoxType(final Type<?> type) {
        switch (VerifyArgument.notNull(type, "type").getKind()) {
            case BOOLEAN:
                return Types.BooleanBox;
            case BYTE:
                return Types.ByteBox;
            case SHORT:
                return Types.ShortBox;
            case INT:
                return Types.IntegerBox;
            case LONG:
                return Types.LongBox;
            case CHAR:
                return Types.CharacterBox;
            case FLOAT:
                return Types.FloatBox;
            case DOUBLE:
                return Types.DoubleBox;
            default:
                return Types.StrongBox.makeGenericType(type);
        }
    }
}
