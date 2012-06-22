package com.strobel.reflection.emit;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.Type;

import java.util.Arrays;

/**
 * @author strobelm
 */
@SuppressWarnings("PackageVisibleField")
final class __ExceptionInfo {

    final static int None             = 0x0000;  //COR_ILEXCEPTION_CLAUSE_NONE
    final static int Filter           = 0x0001;  //COR_ILEXCEPTION_CLAUSE_FILTER
    final static int Finally          = 0x0002;  //COR_ILEXCEPTION_CLAUSE_FINALLY
    final static int Fault            = 0x0004;  //COR_ILEXCEPTION_CLAUSE_FAULT
    final static int PreserveStack    = 0x0004;  //COR_ILEXCEPTION_CLAUSE_PRESERVESTACK

    final static int State_Try = 0;
    final static int State_Filter =1;
    final static int State_Catch = 2;
    final static int State_Finally = 3;
    final static int State_Fault = 4;
    final static int State_Done = 5;

    int    _startAddr;
    int[]  _filterAddr;
    int[]  _catchAddr;
    int[]  _catchEndAddr;
    int[]  _type;
    Type[] _catchClass;
    Label  _endLabel;
    Label  _finallyEndLabel;
    int    _endAddr;
    int    _endFinally;
    int    _currentCatch;

    int _currentState;


    //This will never get called.  The values exist merely to keep the
    //compiler happy.
    private __ExceptionInfo() {
        _startAddr = 0;
        _filterAddr = null;
        _catchAddr = null;
        _catchEndAddr = null;
        _endAddr = 0;
        _currentCatch = 0;
        _type = null;
        _endFinally = -1;
        _currentState = State_Try;
    }

    __ExceptionInfo(final int startAddr, final Label endLabel) {
        _startAddr=startAddr;
        _endAddr=-1;
        _filterAddr=new int[4];
        _catchAddr=new int[4];
        _catchEndAddr=new int[4];
        _catchClass=new Type[4];
        _currentCatch=0;
        _endLabel=endLabel;
        _type=new int[4];
        _endFinally=-1;
        _currentState = State_Try;
    }

    private static Type[] enlargeArray(final Type[] incoming)
    {
        return Arrays.copyOf(incoming, incoming.length * 2);
    }

    private void markHelper(
        final int catchOrfilterAddr,      // the starting address of a clause
        final int catchEndAddr,           // the end address of a previous catch clause. Only use when finally is following a catch
        final Type catchClass,             // catch exception type
        final int type)                   // kind of clause
    {
        if (_currentCatch >= _catchAddr.length) {
            _filterAddr = BytecodeGenerator.enlargeArray(_filterAddr);
            _catchAddr = BytecodeGenerator.enlargeArray(_catchAddr);
            _catchEndAddr = BytecodeGenerator.enlargeArray(_catchEndAddr);
            _catchClass = __ExceptionInfo.enlargeArray(_catchClass);
            _type = BytecodeGenerator.enlargeArray(_type);
        }

        if (type == Filter) {
            _type[_currentCatch] = type;
            _filterAddr[_currentCatch] = catchOrfilterAddr;
            _catchAddr[_currentCatch] = -1;

            if (_currentCatch > 0) {
                assert _catchEndAddr[_currentCatch - 1] == -1
                    : "_catchEndAddr[_currentCatch - 1] == -1";

                _catchEndAddr[_currentCatch - 1] = catchOrfilterAddr;
            }
        }
        else {
            // catch or Fault clause
            _catchClass[_currentCatch] = catchClass;

            if (_type[_currentCatch] != Filter) {
                _type[_currentCatch] = type;
            }

            _catchAddr[_currentCatch] = catchOrfilterAddr;

            if (_currentCatch > 0) {
                if (_type[_currentCatch] != Filter) {
                    assert _catchEndAddr[_currentCatch - 1] == -1
                        : "_catchEndAddr[_currentCatch - 1] == -1";

                    _catchEndAddr[_currentCatch - 1] = catchEndAddr;
                }
            }

            _catchEndAddr[_currentCatch] = -1;
            _currentCatch++;
        }

        if (_endAddr == -1) {
            _endAddr = catchOrfilterAddr;
        }
    }

    void markFilterAddr(final int filterAddr) {
        _currentState = State_Filter;
        markHelper(filterAddr, filterAddr, null, Filter);
    }

    void markFaultAddr(final int faultAddr) {
        _currentState = State_Fault;
        markHelper(faultAddr, faultAddr, null, Fault);
    }

    void markCatchAddr(final int catchAddr, final Type catchException) {
        _currentState = State_Catch;
        markHelper(catchAddr, catchAddr, catchException, None);
    }

    void markFinallyAddr(final int finallyAddr, final int endCatchAddr) {
        if (_endFinally != -1) {
            throw new IllegalArgumentException("Too many finally clauses.");
        }
        else {
            _currentState = State_Finally;
            _endFinally = finallyAddr;
        }
        markHelper(finallyAddr, endCatchAddr, null, Finally);
    }

    void done(final int endAddr) {
        assert _currentCatch > 0
            : "_currentCatch > 0";

        assert _catchAddr[_currentCatch - 1] > 0
            : "_catchAddr[_currentCatch - 1] > 0";

        assert _catchEndAddr[_currentCatch - 1] == -1
            : "_catchEndAddr[_currentCatch - 1] == -1";

        _catchEndAddr[_currentCatch - 1] = endAddr;
        _currentState = State_Done;
    }

    int getStartAddress() {
        return _startAddr;
    }

    int getEndAddress() {
        return _endAddr;
    }

    int getFinallyEndAddress() {
        return _endFinally;
    }

    Label getEndLabel() {
        return _endLabel;
    }

    int[] getFilterAddresses() {
        return _filterAddr;
    }

    int[] getCatchAddresses() {
        return _catchAddr;
    }

    int[] getCatchEndAddresses() {
        return _catchEndAddr;
    }

    Type[] getCatchClass() {
        return _catchClass;
    }

    int getNumberOfCatches() {
        return _currentCatch;
    }

    int[] getExceptionTypes() {
        return _type;
    }

    void setFinallyEndLabel(final Label lbl) {
        _finallyEndLabel = lbl;
    }

    Label getFinallyEndLabel() {
        return _finallyEndLabel;
    }

    // Specifies whether exc is an inner exception for "this".  The way
    // its determined is by comparing the end address for the last catch
    // clause for both exceptions.  If they're the same, the start address
    // for the exception is compared.
    // WARNING: This is not a generic function to determine the innerness
    // of an exception.  This is somewhat of a mis-nomer.  This gives a
    // random result for cases where the two exceptions being compared do
    // not having a nesting relation.
    boolean isInner(final __ExceptionInfo exc) {
        VerifyArgument.notNull(exc, "exc");

        assert _currentCatch > 0
            : "_currentCatch > 0";

        assert exc._currentCatch > 0
            : "exc._currentCatch > 0";

        final int exclast = exc._currentCatch - 1;
        final int last = _currentCatch - 1;

        if (exc._catchEndAddr[exclast] < _catchEndAddr[last]) {
            return true;
        }
        else if (exc._catchEndAddr[exclast] == _catchEndAddr[last]) {
            assert exc.getEndAddress() != getEndAddress()
                : "exc.getEndAddress() != getEndAddress()";

            if (exc.getEndAddress() > getEndAddress()) {
                return true;
            }
        }
        return false;
    }

    // 0 indicates in a try block
    // 1 indicates in a filter block
    // 2 indicates in a catch block
    // 3 indicates in a finally block
    // 4 indicates done
    int getCurrentState() {
        return _currentState;
    }
}
