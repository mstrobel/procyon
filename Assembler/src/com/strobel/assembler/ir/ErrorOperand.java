package com.strobel.assembler.ir;

/**
 * @author Mike Strobel
 */
public final class ErrorOperand {
    private final String _message;

    ErrorOperand(final String message) {
        _message = message;
    }

    @Override
    public String toString() {
        if (_message != null) {
            return _message;
        }

        return "!!! BAD OPERAND !!!";
    }
}
