package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 3:21 PM
 */
public final class SwitchInfo {
    private final int[] _keys;
    private final Instruction _defaultTarget;
    private final Instruction[] _targets;

    public SwitchInfo(final Instruction defaultTarget, final Instruction[] targets) {
        _keys = null;
        _defaultTarget = defaultTarget;
        _targets = targets;
    }

    public SwitchInfo(final int[] keys, final Instruction defaultTarget, final Instruction[] targets) {
        _keys = keys;
        _defaultTarget = defaultTarget;
        _targets = targets;
    }

    public final int[] getKeys() {
        return _keys;
    }

    public final Instruction getDefaultTarget() {
        return _defaultTarget;
    }

    public final Instruction[] getTargets() {
        return _targets;
    }
}
