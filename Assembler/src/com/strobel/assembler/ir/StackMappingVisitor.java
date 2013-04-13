/*
 * StackMappingVisitor.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.ir;

import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class StackMappingVisitor implements MethodVisitor {
    private final MethodVisitor _innerVisitor;

    private int _maxLocals;
//    private int _maxStack;
    private List<FrameValue> _stack = new ArrayList<>();
    private List<FrameValue> _locals = new ArrayList<>();

    public StackMappingVisitor() {
        _innerVisitor = null;
    }

    public StackMappingVisitor(final MethodVisitor innerVisitor) {
        _innerVisitor = innerVisitor;
    }

    public final Frame buildFrame() {
        return new Frame(
            FrameType.New,
            _locals.toArray(new FrameValue[_locals.size()]),
            _stack.toArray(new FrameValue[_stack.size()])
        );
    }

    public final int getStackSize() {
        return _stack == null ? 0 : _stack.size();
    }

    public final int getLocalCount() {
        return _locals == null ? 0 : _locals.size();
    }

    public final FrameValue getStackValue(final int offset) {
        VerifyArgument.inRange(0, getStackSize(), offset, "offset");
        return _stack.get(_stack.size() - offset - 1);
    }

    public final FrameValue getLocalValue(final int slot) {
        VerifyArgument.inRange(0, getLocalCount(), slot, "slot");
        return _locals.get(slot);
    }

    public final FrameValue[] getStackSnapshot() {
        if (_stack == null || _stack.isEmpty()) {
            return FrameValue.EMPTY_VALUES;
        }

        return _stack.toArray(new FrameValue[_stack.size()]);
    }

    public final FrameValue[] getLocalsSnapshot() {
        if (_locals == null || _locals.isEmpty()) {
            return FrameValue.EMPTY_VALUES;
        }

        return _locals.toArray(new FrameValue[_locals.size()]);
    }

    @Override
    public boolean canVisitBody() {
        return true;
    }

    @Override
    public InstructionVisitor visitBody(final MethodBody body) {
        if (_innerVisitor != null && _innerVisitor.canVisitBody()) {
            return new InstructionAnalyzer(body, _innerVisitor.visitBody(body));
        }
        else {
            return new InstructionAnalyzer(body);
        }
    }

    @Override
    public void visitEnd() {
        if (_innerVisitor != null) {
            _innerVisitor.visitEnd();
        }
    }

    @Override
    public void visitFrame(final Frame frame) {
        VerifyArgument.notNull(frame, "frame");

        if (frame.getFrameType() != FrameType.New) {
            throw Error.stackMapperCalledWithUnexpandedFrame(frame.getFrameType());
        }

        if (_innerVisitor != null) {
            _innerVisitor.visitFrame(frame);
        }

        if (_locals != null) {
            _locals.clear();
            _stack.clear();
        }
        else {
            _locals = new ArrayList<>();
            _stack = new ArrayList<>();
        }

        for (final FrameValue frameValue : frame.getLocalValues()) {
            _locals.add(frameValue);

            if (frameValue.getType() == FrameValueType.Double ||
                frameValue.getType() == FrameValueType.Long) {

                _locals.add(FrameValue.TOP);
            }
        }

        for (final FrameValue frameValue : frame.getStackValues()) {
            _stack.add(frameValue);

            if (frameValue.getType() == FrameValueType.Double ||
                frameValue.getType() == FrameValueType.Long) {

                _stack.add(FrameValue.TOP);
            }
        }
    }

    @Override
    public void visitLineNumber(final Instruction instruction, final int lineNumber) {
        if (_innerVisitor != null) {
            _innerVisitor.visitLineNumber(instruction, lineNumber);
        }
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        if (_innerVisitor != null) {
            _innerVisitor.visitAttribute(attribute);
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
        if (_innerVisitor != null) {
            _innerVisitor.visitAnnotation(annotation, visible);
        }
    }

    @Override
    public void visitParameterAnnotation(final int parameter, final CustomAnnotation annotation, final boolean visible) {
        if (_innerVisitor != null) {
            _innerVisitor.visitParameterAnnotation(parameter, annotation, visible);
        }
    }

    protected final FrameValue get(final int local) {
        _maxLocals = Math.max(_maxLocals, local);
        return local < _locals.size() ? _locals.get(local) : FrameValue.TOP;
    }

    protected final void set(final int local, final FrameValue value) {
        _maxLocals = Math.max(_maxLocals, local);

        if (_locals == null) {
            _locals = new ArrayList<>();
            _stack = new ArrayList<>();
        }

        while (local >= _locals.size()) {
            _locals.add(FrameValue.TOP);
        }

        _locals.set(local, value);
    }

    protected final void set(final int local, final TypeReference type) {
        _maxLocals = Math.max(_maxLocals, local);

        if (_locals == null) {
            _locals = new ArrayList<>();
            _stack = new ArrayList<>();
        }

        while (local >= _locals.size()) {
            _locals.add(FrameValue.TOP);
        }

        switch (type.getSimpleType()) {
            case Boolean:
            case Byte:
            case Character:
            case Short:
            case Integer:
                _locals.set(local, FrameValue.INTEGER);
                break;

            case Long:
                _locals.set(local, FrameValue.LONG);
                _locals.set(local + 1, FrameValue.TOP);
                break;

            case Float:
                _locals.set(local, FrameValue.FLOAT);
                break;

            case Double:
                _locals.set(local, FrameValue.DOUBLE);
                _locals.set(local + 1, FrameValue.TOP);
                break;

            case Object:
            case Array:
            case TypeVariable:
            case Wildcard:
                _locals.set(local, FrameValue.makeReference(type));
                break;

            case Void:
                throw new IllegalArgumentException("Cannot set local to type void.");
        }
    }

    protected final FrameValue pop() {
        return _stack.remove(_stack.size() - 1);
    }

    protected final FrameValue peek() {
        return _stack.get(_stack.size() - 1);
    }

    protected final void pop(final int count) {
        final int size = _stack.size();
        final int end = size - count;

        for (int i = size - 1; i >= end; i--) {
            _stack.remove(i);
        }
    }

    protected final void push(final TypeReference type) {
        if (_stack == null) {
            _locals = new ArrayList<>();
            _stack = new ArrayList<>();
        }

        switch (type.getSimpleType()) {
            case Boolean:
            case Byte:
            case Character:
            case Short:
            case Integer:
                _stack.add(FrameValue.INTEGER);
                break;

            case Long:
                _stack.add(FrameValue.LONG);
                _stack.add(FrameValue.TOP);
                break;

            case Float:
                _stack.add(FrameValue.FLOAT);
                break;

            case Double:
                _stack.add(FrameValue.DOUBLE);
                _stack.add(FrameValue.TOP);
                break;

            case Object:
            case Array:
            case TypeVariable:
            case Wildcard:
                _stack.add(FrameValue.makeReference(type));
                break;

            case Void:
                break;
        }
    }

    protected final void push(final FrameValue value) {
        if (_stack == null) {
            _locals = new ArrayList<>();
            _stack = new ArrayList<>();
        }
        _stack.add(value);
    }

    private final class InstructionAnalyzer implements InstructionVisitor {
        private final InstructionVisitor _innerVisitor;
        private final MethodBody _body;

        private boolean _afterExecute;

        private InstructionAnalyzer(final MethodBody body) {
            this(body, null);
        }

        private InstructionAnalyzer(final MethodBody body, final InstructionVisitor innerVisitor) {
            _body = VerifyArgument.notNull(body, "body");
            _innerVisitor = innerVisitor;

            if (body.getMethod().isConstructor()) {
                set(0, FrameValue.UNINITIALIZED_THIS);
            }
        }

        @Override
        public void visit(final Instruction instruction) {
            if (_innerVisitor != null) {
                _innerVisitor.visit(instruction);
            }

            instruction.accept(this);
            execute(instruction);

            _afterExecute = true;

            try {
                instruction.accept(this);
            }
            finally {
                _afterExecute = false;
            }
        }

        @Override
        public void visit(final OpCode code) {
            if (_afterExecute) {
                if (code.isStore()) {
                    set(OpCodeHelpers.getLoadStoreMacroArgumentIndex(code), _temp.isEmpty() ? pop() : _temp.pop());
                }
            }
            else if (code.isLoad()) {
                push(get(OpCodeHelpers.getLoadStoreMacroArgumentIndex(code)));
            }
        }

        @Override
        public void visitConstant(final OpCode code, final TypeReference value) {
        }

        @Override
        public void visitConstant(final OpCode code, final int value) {
        }

        @Override
        public void visitConstant(final OpCode code, final long value) {
        }

        @Override
        public void visitConstant(final OpCode code, final float value) {
        }

        @Override
        public void visitConstant(final OpCode code, final double value) {
        }

        @Override
        public void visitConstant(final OpCode code, final String value) {
        }

        @Override
        public void visitBranch(final OpCode code, final Instruction target) {
        }

        @Override
        public void visitVariable(final OpCode code, final VariableReference variable) {
            if (_afterExecute) {
                if (code.isStore()) {
                    set(variable.getSlot(), _temp.isEmpty() ? pop() : _temp.pop());
                }
            }
            else if (code.isLoad()) {
                push(get(variable.getSlot()));
            }
        }

        @Override
        public void visitVariable(final OpCode code, final VariableReference variable, final int operand) {
        }

        @Override
        public void visitType(final OpCode code, final TypeReference type) {
        }

        @Override
        public void visitMethod(final OpCode code, final MethodReference method) {
        }

        @Override
        public void visitDynamicCallSite(final OpCode opCode, final DynamicCallSite callSite) {
        }

        @Override
        public void visitField(final OpCode code, final FieldReference field) {
        }

        @Override
        public void visitLabel(final Label label) {
        }

        @Override
        public void visitSwitch(final OpCode code, final SwitchInfo switchInfo) {
        }

        @Override
        public void visitEnd() {
        }

        private final Stack<FrameValue> _temp = new Stack<>();

        private void execute(final Instruction instruction) {
            final OpCode code = instruction.getOpCode();

            _temp.clear();

            switch (code.getStackBehaviorPop()) {
                case Pop0:
                    break;

                case Pop1:
                    _temp.push(pop());
                    break;

                case Pop2:
                case Pop1_Pop1:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case Pop1_Pop2:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case Pop1_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case Pop2_Pop1:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case Pop2_Pop2:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopI4:
                    _temp.push(pop());
                    break;

                case PopI8:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopR4:
                    _temp.push(pop());
                    break;

                case PopR8:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopA:
                    _temp.push(pop());
                    break;

                case PopI4_PopI4:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopI4_PopI8:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopI8_PopI8:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopR4_PopR4:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopR8_PopR8:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopI4_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopI4_PopI4_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopI8_PopI4_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopR4_PopI4_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopR8_PopI4_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopA_PopI4_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case PopA_PopA:
                    _temp.push(pop());
                    _temp.push(pop());
                    break;

                case VarPop: {
                    switch (code) {
                        case INVOKEVIRTUAL:
                        case INVOKESPECIAL:
                        case INVOKESTATIC:
                        case INVOKEINTERFACE: {
                            final MethodReference method = instruction.getOperand(0);
                            final List<ParameterDefinition> parameters = method.getParameters();

                            if (code == OpCode.INVOKESPECIAL) {
                                final FrameValue firstParameter = _stack.get(parameters.size());

                                if (firstParameter.getType() == FrameValueType.UninitializedThis) {
                                    set(0, FrameValue.makeReference(_body.getMethod().getDeclaringType()));
                                }
                                else if (firstParameter.getType() == FrameValueType.Uninitialized) {
                                    set(0, FrameValue.makeReference(method.getDeclaringType()));
                                }
                            }

                            for (final ParameterDefinition parameter : parameters) {
                                final TypeReference parameterType = parameter.getParameterType();

                                switch (parameterType.getSimpleType()) {
                                    case Long:
                                    case Double:
                                        _temp.push(pop());
                                        _temp.push(pop());
                                        break;

                                    default:
                                        _temp.push(pop());
                                        break;
                                }
                            }

                            if (code != OpCode.INVOKESTATIC) {
                                _temp.push(pop());
                            }

                            break;
                        }

                        case INVOKEDYNAMIC: {
/*
                            final DynamicCallSite callSite = instruction.getOperand(0);
                            final IMethodSignature method = callSite.getBootstrapMethod();
                            final List<ParameterDefinition> parameters = method.getParameters();

                            for (final ParameterDefinition parameter : parameters) {
                                final TypeReference parameterType = parameter.getParameterType();

                                switch (parameterType.getSimpleType()) {
                                    case Long:
                                    case Double:
                                        _temp.push(pop());
                                        _temp.push(pop());
                                        break;

                                    default:
                                        _temp.push(pop());
                                        break;
                                }
                            }
*/

                            break;
                        }

                        case ATHROW: {
                            _temp.push(pop());
                            while (!_stack.isEmpty()) {
                                pop();
                            }
                            break;
                        }

                        case MULTIANEWARRAY: {
                            final int dimensions = ((Number) instruction.getOperand(1)).intValue();

                            for (int i = 0; i < dimensions; i++) {
                                _temp.push(pop());
                            }

                            break;
                        }
                    }

                    break;
                }
            }

            if (code.isArrayLoad()) {
                push(((TypeReference)_temp.pop().getParameter()).getElementType());
                return;
            }

            switch (code.getStackBehaviorPush()) {
                case Push0:
                    break;

                case Push1: {
                    switch (code) {
                        case LDC:
                        case LDC_W: {
                            final Object op = instruction.getOperand(0);
                            if (op instanceof String) {
                                push(MetadataSystem.instance().lookupType("java/lang/String"));
                            }
                            else if (op instanceof TypeReference) {
                                push(MetadataSystem.instance().lookupType("java/lang/Class"));
                            }
                            else {
                                if (op instanceof Long) {
                                    push(FrameValue.LONG);
                                    push(FrameValue.TOP);
                                }
                                else if (op instanceof Float) {
                                    push(FrameValue.INTEGER);
                                }
                                else if (op instanceof Double) {
                                    push(FrameValue.DOUBLE);
                                    push(FrameValue.TOP);
                                }
                                else if (op instanceof Integer) {
                                    push(FrameValue.INTEGER);
                                }
                            }
                            break;
                        }

                        case GETFIELD:
                        case GETSTATIC: {
                            final FieldReference field = instruction.getOperand(0);
                            push(field.getFieldType());
                            break;
                        }
                    }
                    break;
                }

                case Push1_Push1: {
                    switch (code) {
                        case DUP: {
                            final FrameValue value = _temp.pop();
                            push(value);
                            push(value);
                            break;
                        }

                        case SWAP: {
                            final FrameValue t2 = _temp.pop();
                            final FrameValue t1 = _temp.pop();
                            push(t2);
                            push(t1);
                            break;
                        }
                    }
                    break;
                }

                case Push1_Push1_Push1: {
                    final FrameValue t2 = _temp.pop();
                    final FrameValue t1 = _temp.pop();
                    push(t1);
                    push(t2);
                    push(t1);
                    break;
                }

                case Push1_Push2_Push1: {
                    final FrameValue t3 = _temp.pop();
                    final FrameValue t2 = _temp.pop();
                    final FrameValue t1 = _temp.pop();
                    push(t1);
                    push(t3);
                    push(t2);
                    push(t1);
                    break;
                }

                case Push2: {
                    final Number constant = instruction.getOperand(0);
                    if (constant instanceof Double) {
                        push(FrameValue.DOUBLE);
                        push(FrameValue.TOP);
                    }
                    else {
                        push(FrameValue.LONG);
                        push(FrameValue.TOP);
                    }
                    break;
                }

                case Push2_Push2:{
                    final FrameValue t2 = _temp.pop();
                    final FrameValue t1 = _temp.pop();
                    push(t2);
                    push(t1);
                    push(t2);
                    push(t1);
                    break;
                }

                case Push2_Push1_Push2: {
                    final FrameValue t3 = _temp.pop();
                    final FrameValue t2 = _temp.pop();
                    final FrameValue t1 = _temp.pop();
                    push(t2);
                    push(t1);
                    push(t3);
                    push(t2);
                    push(t1);
                    break;
                }

                case Push2_Push2_Push2: {
                    final FrameValue t4 = _temp.pop();
                    final FrameValue t2 = _temp.pop();
                    final FrameValue t3 = _temp.pop();
                    final FrameValue t1 = _temp.pop();
                    push(t2);
                    push(t1);
                    push(t4);
                    push(t3);
                    push(t2);
                    push(t1);
                    break;
                }

                case PushI4: {
                    push(FrameValue.INTEGER);
                    break;
                }

                case PushI8: {
                    push(FrameValue.LONG);
                    push(FrameValue.TOP);
                    break;
                }

                case PushR4: {
                    push(FrameValue.FLOAT);
                    break;
                }

                case PushR8: {
                    push(FrameValue.DOUBLE);
                    push(FrameValue.TOP);
                    break;
                }

                case PushA: {
                    switch (code) {
                        case NEW:
                            push(FrameValue.makeUninitializedReference(instruction));
                            break;

                        case NEWARRAY:
                        case ANEWARRAY:
                            push(instruction.<TypeReference>getOperand(0).makeArrayType());
                            break;

                        case CHECKCAST:
                        case MULTIANEWARRAY:
                            push(instruction.<TypeReference>getOperand(0));
                            break;

                        case ACONST_NULL:
                            push(FrameValue.NULL);
                            break;

                        default:
                            push(pop());
                            break;
                    }
                    break;
                }

                case VarPush: {
                    final IMethodSignature signature;

                    if (code == OpCode.INVOKEDYNAMIC) {
                        signature = instruction.<DynamicCallSite>getOperand(0).getMethodType();
                    }
                    else {
                        signature = instruction.<MethodReference>getOperand(0);
                    }

                    final TypeReference returnType = signature.getReturnType();

                    switch (returnType.getSimpleType()) {
                        case Boolean:
                        case Byte:
                        case Character:
                        case Short:
                        case Integer:
                            push(FrameValue.INTEGER);
                            break;

                        case Long:
                            push(FrameValue.LONG);
                            push(FrameValue.TOP);
                            break;

                        case Float:
                            push(FrameValue.FLOAT);
                            break;

                        case Double:
                            push(FrameValue.DOUBLE);
                            push(FrameValue.TOP);
                            break;

                        case Object:
                        case Array:
                        case TypeVariable:
                        case Wildcard:
                            push(FrameValue.makeReference(returnType));
                            break;

                        case Void:
                            break;
                    }

                    break;
                }
            }
        }
    }
}
