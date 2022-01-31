package generated;

import java.lang.invoke.MethodHandles;

public final class PackageAccess {
    public static MethodHandles.Lookup defaultPackage() {
        return LazyInit.LOOKUP;
    }

    private final static class LazyInit {
        final static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    }
}
