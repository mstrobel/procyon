package com.strobel.expressions;

/**
 * @author Mike Strobel
 */
interface IArgumentProvider {
    int getArgumentCount();
    Expression getArgument(final int index);
}
