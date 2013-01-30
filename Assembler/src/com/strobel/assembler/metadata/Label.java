package com.strobel.assembler.metadata;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:36 PM
 */
public final class Label {
    @SuppressWarnings("PackageVisibleField")
    int index;

    public Label(final int label) {
        this.index = label;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public int hashCode() {
        return this.index;
    }

    public boolean equals(final Object o) {
        return o instanceof Label &&
               equals((Label)o);
    }

    public boolean equals(final Label other) {
        return other != null &&
               other.index == this.index;
    }
}
