package com.strobel;

import com.strobel.reflection.Type;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author Mike Strobel
 */
public class Test {
    public static void main(final String[] args) {
        final Type hashMap = Type.of(HashMap.class);

        System.out.println(hashMap.getName());
        System.out.println(hashMap.getSignature());
        System.out.println(hashMap.getErasedSignature());
        System.out.println(hashMap.getBriefDescription());
        System.out.println(hashMap.getFullDescription());

        final Type boundHashMap = Type.of(HashMap.class)
                                      .makeGenericType(Type.of(String.class), Type.of(UUID.class))
                                      .makeArrayType();

        System.out.println(boundHashMap.getName());
        System.out.println(boundHashMap.getSignature());
        System.out.println(boundHashMap.getErasedSignature());
        System.out.println(boundHashMap.getBriefDescription());
        System.out.println(boundHashMap.getFullDescription());
    }
}
