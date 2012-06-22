package com.strobel.reflection.emit;

/**
 * @author strobelm
 */
public final class Label {
    @SuppressWarnings("PackageVisibleField")
    final int label;

    Label(final int label) {
        this.label = label;
    }

    int getLabelValue() {
        return this.label;
    }

    public int hashCode() {
        return this.label;
    }

    public boolean equals(final Object o) {
        return o instanceof Label &&
               equals((Label)o);
    }

    public boolean equals(final Label other) {
        return other != null &&
               other.label == this.label;
    }
}