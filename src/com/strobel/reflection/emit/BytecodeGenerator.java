package com.strobel.reflection.emit;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.BindingFlags;
import com.strobel.reflection.ConstructorInfo;
import com.strobel.reflection.FieldInfo;
import com.strobel.reflection.MethodBuilder;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * @author strobelm
 */
@SuppressWarnings(
    {
        "PointlessBitwiseExpression",
        "PointlessArithmeticExpression",
        "UnusedDeclaration",
        "PackageVisibleField"
    })
public class BytecodeGenerator {

    final static int DefaultFixupArraySize = 64;
    final static int DefaultLabelArraySize = 16;
    final static int DefaultExceptionArraySize = 8;

    private BytecodeStream _bytecodeStream;

    private int[] _labelList;
    private int _labelCount;

    private __FixupData[] _fixupData;

    private int _fixupCount;

    private int[] _rvaFixupList;
    private int _rvaFixupCount;

    private int[] _relocateFixupList;
    private int _relocateFixupCount;

    private int _exceptionCount;
    private int _currExcStackCount;
    private __ExceptionInfo[] _exceptions;           //This is the list of all of the exceptions in this BytecodeStream.
    private __ExceptionInfo[] _currExcStack;         //This is the stack of exceptions which we're currently in.

    ScopeTree _scopeTree;           // this variable tracks all debugging scope information

    MethodBuilder _methodBuilder;
    int _localCount;
//    SignatureHelper             _localSignature;

    private int _maxStackSize = 0;     // Maximum stack size not counting the exceptions.

    private int _maxMidStack = 0;      // Maximum stack size for a given basic block.
    private int _maxMidStackCur = 0;   // Running count of the maximum stack size for the current basic block.

    public Label defineLabel() {
        // Declares a new Label.  This is just a token and does not yet represent any
        // particular location within the stream.  In order to set the position of the
        // label within the stream, you must call markLabel().

        if (_labelList == null) {
            _labelList = new int[DefaultLabelArraySize];
        }

        if (_labelCount >= _labelList.length) {
            _labelList = enlargeArray(_labelList);
        }

        _labelList[_labelCount] = -1;

        return new Label(_labelCount++);
    }

    public void markLabel(final Label label) {
        // Defines a label by setting the position where that label is found
        // within the stream.  Verifies the label is not defined more than once.

        final int labelIndex = label.getLabelValue();

        // This should never happen.
        if (labelIndex < 0 || labelIndex >= _labelList.length) {
            throw Error.badLabel();
        }

        if (_labelList[labelIndex] != -1) {
            throw Error.labelAlreadyDefined();
        }

        _labelList[labelIndex] = _bytecodeStream.getLength();
    }

    public LocalBuilder declareLocal(final Type localType) {
        VerifyArgument.notNull(localType, "localType");

        // Declare a local of type "local". The current active lexical scope
        // will be the scope that local will live.

        final LocalBuilder localBuilder;
        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        if (methodBuilder.isTypeCreated()) {
            // cannot change method after its containing type has been created 
            throw Error.typeHasBeenCreated();
        }

        if (methodBuilder.isFinished()) {
            throw Error.methodIsFinished();
        }

        // add the localType to local signature 
//        _localSignature.AddArgument(localType, pinned);

        localBuilder = new LocalBuilder(_localCount, localType, methodBuilder);

        _localCount++;

        return localBuilder;
    }

    public void emit(final OpCode opCode) {
        ensureCapacity(opCode.getSizeWithOperands());
        internalEmit(opCode);
    }

    public void emit(final OpCode opCode, final byte arg) {
        emit(opCode);
        emitByteOperand(arg);
    }

    public void emit(final OpCode opCode, final short arg) {
        emit(opCode);
        emitShortOperand(arg);
    }

    public void emit(final OpCode opCode, final int arg) {
        emit(opCode);
        emitIntOperand(arg);
    }

    public void emit(final OpCode opCode, final long arg) {
        emit(opCode);
        emitLongOperand(arg);
    }

    public void emit(final OpCode opCode, final float arg) {
        emit(opCode);
        emitFloatOperand(arg);
    }

    public void emit(final OpCode opCode, final double arg) {
        emit(opCode);
        emitDoubleOperand(arg);
    }

    public void emit(final OpCode opCode, final String arg) {
        emit(opCode);
        emitString(arg);
    }

    public void emit(final OpCode opCode, final Type<?> type) {
        VerifyArgument.notNull(type, "type");

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int typeToken = methodBuilder.getDeclaringType().getTypeToken(type);

        emit(opCode, typeToken);
    }

    public void emit(final OpCode opCode, final ConstructorInfo constructor) {
        VerifyArgument.notNull(constructor, "constructor");

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int constructorToken = methodBuilder.getDeclaringType().getMethodToken(constructor);

        emit(opCode, constructorToken);
    }

    public void emit(final OpCode opCode, final MethodInfo method) {
        VerifyArgument.notNull(method, "method");

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int methodToken = methodBuilder.getDeclaringType().getMethodToken(method);

        emit(opCode, methodToken);
    }

    public void emit(final OpCode opCode, final FieldInfo field) {
        throw ContractUtils.unreachable();
    }

    public void emit(final OpCode opCode, final Label label) {
        // Puts opCode onto the stream and leaves space to include label when fix-ups
        // are done.  Labels are created using BytecodeGenerator.defineLabel() and their
        // location within the stream is fixed by using BytecodeGenerator.defineLabel().
        //
        // opCode must represent a branch instruction (although we don't explicitly
        // verify this).  Since branches are relative instructions, label will be
        // replaced with the correct offset to branch during the fixup process.

        final int tempVal = label.getLabelValue();

        emit(opCode);

        if (opCode.getOperandType() == OperandType.Branch) {
            addFixup(label, _bytecodeStream.getLength(), 2);
            _bytecodeStream.putShort(0);
        }
        else {
            addFixup(label, _bytecodeStream.getLength(), 4);
            _bytecodeStream.putInt(0);
        }
    }

    public void emit(final OpCode opCode, final LocalBuilder local) {
        // Puts the opcode onto the bytecode stream followed by the information
        // for local variable local.

        VerifyArgument.notNull(opCode, "opCode");
        VerifyArgument.notNull(local, "local");

        final int localIndex = local.getLocalIndex();

        if (local.getMethodBuilder() != _methodBuilder) {
            throw Error.unmatchedLocal();
        }

        final OpCode optimalOpCode;

        if (opCode.getOperandType() == OperandType.Local) {
            if (opCode.getCode() <= OpCode.ALOAD.getCode()) {
                optimalOpCode = getLocalLoadOpCode(local.getLocalType(), localIndex);
            }
            else {
                optimalOpCode = getLocalStoreOpCode(local.getLocalType(), localIndex);
            }
        }
        else {
            optimalOpCode = opCode;
        }

        emit(optimalOpCode);

        if (optimalOpCode.getOperandType() == OperandType.Local) {
            emitByteOperand((byte)localIndex);
        }
    }

    public void emitCall(final OpCode opCode, final MethodInfo method) {
        VerifyArgument.notNull(method, "method");

        switch (opCode) {
            case INVOKEDYNAMIC:
            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEVIRTUAL:
                break;

            default:
                throw Error.invokeOpCodeRequired();
        }

        int stackChange = opCode.getStackChange();

        if (method.getReturnType() != PrimitiveTypes.Void) {
            ++stackChange;
        }

        stackChange -= method.getParameters().size();

        emit(opCode, method);

        updateStackSize(opCode, stackChange);
    }

    public void emitNew(final ConstructorInfo constructor) {
        VerifyArgument.notNull(constructor, "constructor");

        final Type type = constructor.getDeclaringType();

        if (type.containsGenericParameters()) {
            throw Error.cannotInstantiateUnboundGenericType(type);
        }

        emit(OpCode.NEW, type);
        emit(OpCode.DUP);
        emit(OpCode.INVOKESPECIAL, constructor);
    }

    public void emitNew(final Type<?> type, final Type... parameterTypes) {
        VerifyArgument.notNull(type, "type");

        final ConstructorInfo constructor = type.getConstructor(parameterTypes);

        if (constructor == null) {
            throw Error.constructorNotFound();
        }

        emitNew(constructor);
    }

    protected void emitLoadConstant(final int token) {
        if (token < Byte.MIN_VALUE || token > Byte.MAX_VALUE) {
            emit(OpCode.LDC_W);
            emitShortOperand(token);
        }
        else {
            emit(OpCode.LDC);
            emitByteOperand(token);
        }
    }

    protected void emitLoadLongConstant(final int token) {
        emit(OpCode.LDC2_W);
        emitShortOperand(token);
    }

    public void emitNull() {
        emit(OpCode.ACONST_NULL);
    }

    public void emitLoad(final LocalBuilder local) {
        emit(
            getLocalLoadOpCode(
                local.getLocalType(),
                local.getLocalIndex()
            ),
            local
        );
    }

    public void emitStore(final LocalBuilder local) {
        emit(
            getLocalStoreOpCode(
                local.getLocalType(),
                local.getLocalIndex()
            ),
            local
        );
    }

    public void emitDefaultValue(final Type<?> type) {
        VerifyArgument.notNull(type, "type");

        switch (type.getKind()) {
            case BOOLEAN:
                emit(OpCode.ICONST_0);
                break;

            case BYTE:
                emit(OpCode.ICONST_0);
                emit(OpCode.I2B);
                break;

            case SHORT:
                emit(OpCode.ICONST_0);
                emit(OpCode.I2S);
                break;

            case INT:
                emit(OpCode.ICONST_0);
                break;

            case LONG:
                emit(OpCode.LCONST_0);
                break;

            case CHAR:
                emit(OpCode.ICONST_0);
                emit(OpCode.I2C);
                break;

            case FLOAT:
                emit(OpCode.FCONST_0);
                break;

            case DOUBLE:
                emit(OpCode.DCONST_0);
                break;

            case NULL:
            case ARRAY:
            case DECLARED:
            case ERROR:
            case TYPEVAR:
                emit(OpCode.ACONST_NULL);
                break;

            default:
                throw Error.invalidType(type);
        }
    }

    public void emitFieldGet(final FieldInfo field) {
        VerifyArgument.notNull(field, "field");

        if (field.isStatic()) {
            emit(OpCode.GETSTATIC, field);
        }
        else {
            emit(OpCode.GETFIELD, field);
        }
    }

    public void emitFieldSet(final FieldInfo field) {
        VerifyArgument.notNull(field, "field");

        if (field.isStatic()) {
            emit(OpCode.PUTSTATIC, field);
        }
        else {
            emit(OpCode.PUTFIELD, field);
        }
    }

    public void emitBoolean(final boolean value) {
        emit(value ? OpCode.ICONST_1 : OpCode.ICONST_0);
    }

    public void emitByte(final byte value) {
        emit(OpCode.BIPUSH, value);
    }

    public void emitCharacter(final char value) {
        if (value <= Byte.MAX_VALUE) {
            emitByte((byte)value);
        }
        else {
            emitShort((short)value);
        }
    }

    public void emitShort(final short value) {
        emit(OpCode.SIPUSH, value);
    }

    public void emitInteger(final int value) {
        switch (value) {
            case -1:
                emit(OpCode.ICONST_M1);
                return;
            case 0:
                emit(OpCode.ICONST_0);
                return;
            case 1:
                emit(OpCode.ICONST_1);
                return;
            case 2:
                emit(OpCode.ICONST_2);
                return;
            case 3:
                emit(OpCode.ICONST_3);
                return;
            case 4:
                emit(OpCode.ICONST_4);
                return;
            case 5:
                emit(OpCode.ICONST_5);
                return;
        }

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int constantToken = methodBuilder.getDeclaringType().getConstantToken(value);

        emitLoadConstant(constantToken);
    }

    public void emitLong(final long value) {
        if (value == 0L) {
            emit(OpCode.LCONST_0);
            return;
        }

        if (value == 1L) {
            emit(OpCode.LCONST_1);
            return;
        }

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int constantToken = methodBuilder.getDeclaringType().getConstantToken(value);

        emitLoadLongConstant(constantToken);
    }

    public void emitFloat(final float value) {
        if (value == 0f) {
            emit(OpCode.FCONST_0);
            return;
        }

        if (value == 1f) {
            emit(OpCode.FCONST_1);
            return;
        }

        if (value == 2f) {
            emit(OpCode.FCONST_2);
            return;
        }

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int constantToken = methodBuilder.getDeclaringType().getConstantToken(value);

        emitLoadConstant(constantToken);
    }

    public void emitDouble(final double value) {
        if (value == 0d) {
            emit(OpCode.DCONST_0);
            return;
        }

        if (value == 1d) {
            emit(OpCode.DCONST_1);
            return;
        }

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int constantToken = methodBuilder.getDeclaringType().getConstantToken(value);

        emitLoadLongConstant(constantToken);
    }

    public void emitString(final String value) {
        if (value == null) {
            emitNull();
            return;
        }

        final MethodBuilder methodBuilder = _methodBuilder;

        if (methodBuilder == null) {
            throw Error.bytecodeGeneratorNotOwnedByMethodBuilder();
        }

        final int stringToken = methodBuilder.getDeclaringType().getStringToken(value);

        emitLoadConstant(stringToken);
    }

    public void emitBox(final Type<?> type) {
        final Type<?> boxedType;
        final Type<?> primitiveType;

        if (type.isPrimitive()) {
            boxedType = TypeUtils.getBoxedType(type);
        }
        else if (TypeUtils.isAutoUnboxed(type)) {
            boxedType = type;
        }
        else {
            return;
        }

        primitiveType = TypeUtils.getUnderlyingPrimitive(type);

        final MethodInfo valueOfMethod = boxedType.getMethod(
            "valueOf",
            BindingFlags.PublicStatic,
            primitiveType
        );

        if (valueOfMethod != null) {
            emitCall(OpCode.INVOKESTATIC, valueOfMethod);
            return;
        }

        final ConstructorInfo constructor = boxedType.getConstructor(primitiveType);

        if (constructor != null) {
            emitNew(constructor);
            return;
        }

        throw Error.boxFailure(boxedType);
    }

    public void emitUnbox(final Type<?> type) {
        final Type<?> boxedType;
        final Type<?> primitiveType;

        if (type.isPrimitive()) {
            boxedType = TypeUtils.getBoxedType(type);
        }
        else if (TypeUtils.isAutoUnboxed(type)) {
            boxedType = type;
        }
        else {
            return;
        }

        primitiveType = TypeUtils.getUnderlyingPrimitive(boxedType);

        final MethodInfo unboxMethod;
        final Set<BindingFlags> unboxMethodFlags = BindingFlags.PublicInstance;

        switch (primitiveType.getKind()) {
            case BOOLEAN:
                unboxMethod = boxedType.getMethod("booleanValue", unboxMethodFlags);
                break;

            case BYTE:
                unboxMethod = boxedType.getMethod("byteValue", unboxMethodFlags);
                break;

            case SHORT:
                unboxMethod = boxedType.getMethod("shortValue", unboxMethodFlags);
                break;

            case INT:
                unboxMethod = boxedType.getMethod("intValue", unboxMethodFlags);
                break;

            case LONG:
                unboxMethod = boxedType.getMethod("longValue", unboxMethodFlags);
                break;

            case CHAR:
                unboxMethod = boxedType.getMethod("charValue", unboxMethodFlags);
                break;

            case FLOAT:
                unboxMethod = boxedType.getMethod("floatValue", unboxMethodFlags);
                break;

            case DOUBLE:
                unboxMethod = boxedType.getMethod("doubleValue", unboxMethodFlags);
                break;

            default:
                return;
        }

        emitCall(OpCode.INVOKEVIRTUAL, unboxMethod);
    }

    void emitByteOperand(final int value) {
        _bytecodeStream.putByte(value);
    }

    void emitCharOperand(final char value) {
        _bytecodeStream.putShort(value);
    }

    void emitShortOperand(final int value) {
        _bytecodeStream.putShort(value);
    }

    void emitIntOperand(final int value) {
        _bytecodeStream.putInt(value);
    }

    void emitLongOperand(final long value) {
        _bytecodeStream.putLong(value);
    }

    void emitFloatOperand(final float value) {
        emitIntOperand(Float.floatToIntBits(value));
    }

    void emitDoubleOperand(final double value) {
        emitLongOperand(Double.doubleToRawLongBits(value));
    }

    void internalEmit(final OpCode opCode) {
        if (opCode.getSize() == 1) {
            _bytecodeStream.putByte((byte)(opCode.getCode() & 0xFF));
        }
        else {
            _bytecodeStream.putByte((byte)((opCode.getCode() >> 16) & 0xFF));
            _bytecodeStream.putByte((byte)((opCode.getCode() >> 0) & 0xFF));
        }
        updateStackSize(opCode, opCode.getStackChange());
    }

    static byte getByteOperand(final byte[] codes, final int index) {
        return codes[index];
    }

    static char getCharOperand(final byte[] codes, final int index) {
        final int hi = ((codes[index + 0] & 0xFF) << 8);
        final int lo = ((codes[index + 1] & 0xFF) << 0);
        return (char)(hi + lo);
    }

    static short getShortOperand(final byte[] codes, final int index) {
        final int hi = ((codes[index + 0] & 0xFF) << 8);
        final int lo = ((codes[index + 1] & 0xFF) << 0);
        return (short)(hi + lo);
    }

    static int getIntOperand(final byte[] codes, final int index) {
        final int hh = ((codes[index + 0] & 0xFF) << 24);
        final int hl = ((codes[index + 1] & 0xFF) << 16);
        final int lh = ((codes[index + 2] & 0xFF) << 8);
        final int ll = ((codes[index + 3] & 0xFF) << 0);
        return hh + hl + lh + ll;
    }

    static long getLongOperand(final byte[] codes, final int index) {
        return ((long)getIntOperand(codes, index) << 32) +
               ((long)getIntOperand(codes, index) << 0);
    }

    static float getFloatOperand(final byte[] codes, final int index) {
        return Float.intBitsToFloat(getIntOperand(codes, index));
    }

    static double getDoubleOperand(final byte[] codes, final int index) {
        return Double.longBitsToDouble(getIntOperand(codes, index));
    }

    static void putByteOperand(final byte[] codes, final int index, final byte value) {
        codes[index] = value;
    }

    static void putCharOperand(final byte[] codes, final int index, final char value) {
        codes[index + 0] = (byte)((value >> 8) & 0xFF);
        codes[index + 1] = (byte)((value >> 0) & 0xFF);
    }

    static void putShortOperand(final byte[] codes, final int index, final short value) {
        codes[index + 0] = (byte)((value >> 8) & 0xFF);
        codes[index + 1] = (byte)((value >> 0) & 0xFF);
    }

    static void putIntOperand(final byte[] codes, final int index, final int value) {
        codes[index + 0] = (byte)((value >> 24) & 0xFF);
        codes[index + 1] = (byte)((value >> 16) & 0xFF);
        codes[index + 2] = (byte)((value >> 8) & 0xFF);
        codes[index + 3] = (byte)((value >> 0) & 0xFF);
    }

    static void putLongOperand(final byte[] codes, final int index, final long value) {
        codes[index + 0] = (byte)((value >> 56) & 0xFF);
        codes[index + 1] = (byte)((value >> 48) & 0xFF);
        codes[index + 2] = (byte)((value >> 40) & 0xFF);
        codes[index + 3] = (byte)((value >> 32) & 0xFF);
        codes[index + 4] = (byte)((value >> 24) & 0xFF);
        codes[index + 5] = (byte)((value >> 16) & 0xFF);
        codes[index + 6] = (byte)((value >> 8) & 0xFF);
        codes[index + 7] = (byte)((value >> 0) & 0xFF);
    }

    static void putFloatOperand(final byte[] codes, final int index, final float value) {
        putIntOperand(codes, index, Float.floatToRawIntBits(value));
    }

    static void putDoubleOperand(final byte[] codes, final int index, final double value) {
        putLongOperand(codes, index, Double.doubleToRawLongBits(value));
    }

    private void addFixup(final Label label, final int position, final int operandSize) {
        // Notes the label, position, and instruction size of a new fixup.  Expands
        // all of the fixup arrays as appropriate. 

        if (_fixupData == null) {
            _fixupData = new __FixupData[DefaultFixupArraySize];
        }

        if (_fixupCount >= _fixupData.length) {
            _fixupData = enlargeArray(_fixupData);
        }

        _fixupData[_fixupCount].fixupPosition = position;
        _fixupData[_fixupCount].fixupLabel = label;
        _fixupData[_fixupCount].operandSize = operandSize;

        _fixupCount++;
    }

    void ensureCapacity(final int size) {
        _bytecodeStream.ensureCapacity(size);
    }

    void updateStackSize(final OpCode opCode, final int stackChange) {
        // Updates internal variables for keeping track of the stack size
        // requirements for the function.  stackChange specifies the amount 
        // by which the stack size needs to be updated. 

        // Special case for the Return.  Returns pops 1 if there is a 
        // non-void return value.

        // Update the running stack size.  _maxMidStack specifies the maximum
        // amount of stack required for the current basic block irrespective of 
        // where you enter the block.
        _maxMidStackCur += stackChange;

        if (_maxMidStackCur > _maxMidStack) {
            _maxMidStack = _maxMidStackCur;
        }
        else if (_maxMidStackCur < 0) {
            _maxMidStackCur = 0;
        }

        // If the current instruction signifies end of a basic, which basically
        // means an unconditional branch, add _maxMidStack to _maxStackSize. 
        // _maxStackSize will eventually be the sum of the stack requirements for
        // each basic block. 

        if (opCode.endsUnconditionalJumpBlock()) {
            _maxStackSize += _maxMidStack;
            _maxMidStack = 0;
            _maxMidStackCur = 0;
        }
    }

    private int getLabelPosition(final Label label) {
        // Gets the position in the stream of a particular label.
        // Verifies that the label exists and that it has been given a value.

        final int index = label.getLabelValue();

        if (index < 0 || index >= _labelCount) {
            throw Error.badLabel();
        }

        if (_labelList[index] < 0) {
            throw Error.badLabelContent();
        }

        return _labelList[index];
    }

    byte[] bakeByteArray() {
        // bakeByteArray() is a package private function designed to be called by
        // MethodBuilder to do all of the fix-ups and return a new byte array
        // representing the byte stream with labels resolved, etc. 

        final int newSize;
        final byte[] newBytes;

        int updateAddress;

        if (_currExcStackCount != 0) {
            throw Error.unclosedExceptionBlock();
        }

        if (_bytecodeStream.getLength() == 0) {
            return null;
        }

        newSize = _bytecodeStream.getLength();
        newBytes = Arrays.copyOf(_bytecodeStream.getData(), newSize);

        // Do the fix-ups.  This involves iterating over all of the labels and replacing
        // them with their proper values.
        for (int i = 0; i < _fixupCount; i++) {
            updateAddress = getLabelPosition(_fixupData[i].fixupLabel) -
                            (_fixupData[i].fixupPosition + _fixupData[i].operandSize);

            // Handle single byte instructions
            // Throw an exception if they're trying to store a jump in a single byte instruction that doesn't fit.
            if (_fixupData[i].operandSize == 2) {
                // Verify that our two-byte arg will fit into a Short.
                if (updateAddress < Short.MIN_VALUE || updateAddress > Short.MAX_VALUE) {
                    throw Error.illegalTwoByteBranch(_fixupData[i].fixupPosition, updateAddress);
                }

                putShortOperand(newBytes, _fixupData[i].fixupPosition, (short)updateAddress);
            }
            else {
                // Emit the four-byte arg.
                putIntOperand(newBytes, _fixupData[i].fixupPosition, updateAddress);
            }
        }

        return newBytes;
    }

    static int[] enlargeArray(final int[] incoming) {
        return Arrays.copyOf(
            VerifyArgument.notNull(incoming, "incoming"),
            incoming.length * 2
        );
    }

    static <T> T[] enlargeArray(final T[] incoming) {
        return Arrays.copyOf(
            incoming,
            incoming.length * 2
        );
    }

    static byte[] enlargeArray(final byte[] incoming) {
        return Arrays.copyOf(
            VerifyArgument.notNull(incoming, "incoming"),
            incoming.length * 2
        );
    }

    static byte[] enlargeArray(final byte[] incoming, final int requiredSize) {
        return Arrays.copyOf(
            VerifyArgument.notNull(incoming, "incoming"),
            requiredSize
        );
    }

    static OpCode getLocalLoadOpCode(final Type<?> type, final int localIndex) {
        switch (type.getKind()) {
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
                switch (localIndex) {
                    case 0:
                        return OpCode.ILOAD_0;
                    case 1:
                        return OpCode.ILOAD_1;
                    case 2:
                        return OpCode.ILOAD_2;
                    case 3:
                        return OpCode.ILOAD_3;
                    default:
                        return OpCode.ILOAD;
                }

            case LONG:
                switch (localIndex) {
                    case 0:
                        return OpCode.LLOAD_0;
                    case 1:
                        return OpCode.LLOAD_1;
                    case 2:
                        return OpCode.LLOAD_2;
                    case 3:
                        return OpCode.LLOAD_3;
                    default:
                        return OpCode.LLOAD;
                }

            case FLOAT:
                switch (localIndex) {
                    case 0:
                        return OpCode.FLOAD_0;
                    case 1:
                        return OpCode.FLOAD_1;
                    case 2:
                        return OpCode.FLOAD_2;
                    case 3:
                        return OpCode.FLOAD_3;
                    default:
                        return OpCode.FLOAD;
                }

            case DOUBLE:
                switch (localIndex) {
                    case 0:
                        return OpCode.DLOAD_0;
                    case 1:
                        return OpCode.DLOAD_1;
                    case 2:
                        return OpCode.DLOAD_2;
                    case 3:
                        return OpCode.DLOAD_3;
                    default:
                        return OpCode.DLOAD;
                }

            default:
                switch (localIndex) {
                    case 0:
                        return OpCode.ALOAD_0;
                    case 1:
                        return OpCode.ALOAD_1;
                    case 2:
                        return OpCode.ALOAD_2;
                    case 3:
                        return OpCode.ALOAD_3;
                    default:
                        return OpCode.ALOAD;
                }
        }
    }

    static OpCode getLocalStoreOpCode(final Type<?> type, final int localIndex) {
        switch (type.getKind()) {
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
                switch (localIndex) {
                    case 0:
                        return OpCode.ISTORE_0;
                    case 1:
                        return OpCode.ISTORE_1;
                    case 2:
                        return OpCode.ISTORE_2;
                    case 3:
                        return OpCode.ISTORE_3;
                    default:
                        return OpCode.ISTORE;
                }

            case LONG:
                switch (localIndex) {
                    case 0:
                        return OpCode.LSTORE_0;
                    case 1:
                        return OpCode.LSTORE_1;
                    case 2:
                        return OpCode.LSTORE_2;
                    case 3:
                        return OpCode.LSTORE_3;
                    default:
                        return OpCode.LSTORE;
                }

            case FLOAT:
                switch (localIndex) {
                    case 0:
                        return OpCode.FSTORE_0;
                    case 1:
                        return OpCode.FSTORE_1;
                    case 2:
                        return OpCode.FSTORE_2;
                    case 3:
                        return OpCode.FSTORE_3;
                    default:
                        return OpCode.FSTORE;
                }

            case DOUBLE:
                switch (localIndex) {
                    case 0:
                        return OpCode.DSTORE_0;
                    case 1:
                        return OpCode.DSTORE_1;
                    case 2:
                        return OpCode.DSTORE_2;
                    case 3:
                        return OpCode.DSTORE_3;
                    default:
                        return OpCode.DSTORE;
                }

            default:
                switch (localIndex) {
                    case 0:
                        return OpCode.ASTORE_0;
                    case 1:
                        return OpCode.ASTORE_1;
                    case 2:
                        return OpCode.ASTORE_2;
                    case 3:
                        return OpCode.ASTORE_3;
                    default:
                        return OpCode.ASTORE;
                }
        }
    }
}

