package com.strobel.core;

/**
 * @author strobelm
 */
public final class HashUtilities {
    public static final int NullHashCode = 0x61E04917;

    private static final int HashPrime = 101;
    private static final int CombinedHashOffset = 5;
    private final static int MaxPrimeArrayLength = 0x7FEFFFFD;

    private HashUtilities() {
        throw new UnsupportedOperationException();
    }

    public static int combineHashCodes(final int... hashes) {
        int hash = 0;

        for (final int h : hashes) {
            hash <<= CombinedHashOffset;
            hash ^= h;
        }

        return hash;
    }

    public static int combineHashCodes(final Object... objects) {
        int hash = 0;

        for (final Object o : objects) {
            int entryHash = NullHashCode;

            if (o != null) {
                if (o instanceof Object[]) {
                    entryHash = combineHashCodes((Object[])o);
                }
                else {
                    entryHash = o.hashCode();
                }
            }

            hash <<= CombinedHashOffset;
            hash ^= entryHash;
        }

        return hash;
    }

    public static int combineHashCodes(final int hash1, final int hash2) {
        return (hash1 << CombinedHashOffset) ^ hash2;
    }

    public static int combineHashCodes(final int hash1, final int hash2, final int hash3) {
        return (((hash1 << CombinedHashOffset)
                 ^ hash2) << CombinedHashOffset)
               ^ hash3;
    }

    public static int combineHashCodes(final int hash1, final int hash2, final int hash3, final int hash4) {
        return (((((hash1 << CombinedHashOffset)
                   ^ hash2) << CombinedHashOffset)
                 ^ hash3) << CombinedHashOffset)
               ^ hash4;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5) {

        return (((((((hash1 << CombinedHashOffset)
                     ^ hash2) << CombinedHashOffset)
                   ^ hash3) << CombinedHashOffset)
                 ^ hash4) << CombinedHashOffset)
               ^ hash5;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5,
        final int hash6) {

        return (((((((((hash1 << CombinedHashOffset)
                       ^ hash2) << CombinedHashOffset)
                     ^ hash3) << CombinedHashOffset)
                   ^ hash4) << CombinedHashOffset)
                 ^ hash5) << CombinedHashOffset)
               ^ hash6;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5,
        final int hash6,
        final int hash7) {

        return (((((((((((hash1 << CombinedHashOffset)
                         ^ hash2) << CombinedHashOffset)
                       ^ hash3) << CombinedHashOffset)
                     ^ hash4) << CombinedHashOffset)
                   ^ hash5) << CombinedHashOffset)
                 ^ hash6) << CombinedHashOffset)
               ^ hash7;
    }

    public static int combineHashCodes(
        final int hash1,
        final int hash2,
        final int hash3,
        final int hash4,
        final int hash5,
        final int hash6,
        final int hash7,
        final int hash8) {

        return (((((((((((((hash1 << CombinedHashOffset)
                           ^ hash2) << CombinedHashOffset)
                         ^ hash3) << CombinedHashOffset)
                       ^ hash4) << CombinedHashOffset)
                     ^ hash5) << CombinedHashOffset)
                   ^ hash6) << CombinedHashOffset)
                 ^ hash7) << CombinedHashOffset)
               ^ hash8;
    }

    public static int combineHashCodes(final Object o1, final Object o2) {
        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode()
        );
    }

    public static int combineHashCodes(final Object o1, final Object o2, final Object o3) {
        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode()
        );
    }

    public static int combineHashCodes(final Object o1, final Object o2, final Object o3, final Object o4) {
        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode(),
            o6 == null ? NullHashCode : o6.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode(),
            o6 == null ? NullHashCode : o6.hashCode(),
            o7 == null ? NullHashCode : o7.hashCode()
        );
    }

    public static int combineHashCodes(
        final Object o1,
        final Object o2,
        final Object o3,
        final Object o4,
        final Object o5,
        final Object o6,
        final Object o7,
        final Object o8) {

        return combineHashCodes(
            o1 == null ? NullHashCode : o1.hashCode(),
            o2 == null ? NullHashCode : o2.hashCode(),
            o3 == null ? NullHashCode : o3.hashCode(),
            o4 == null ? NullHashCode : o4.hashCode(),
            o5 == null ? NullHashCode : o5.hashCode(),
            o6 == null ? NullHashCode : o6.hashCode(),
            o7 == null ? NullHashCode : o7.hashCode(),
            o8 == null ? NullHashCode : o8.hashCode()
        );
    }

    // Table of prime numbers to use as hash table sizes. 
    // A typical resize algorithm would pick the smallest prime number in this array
    // that is larger than twice the previous capacity. 
    // Suppose our Hashtable currently has capacity x and enough elements are added
    // such that a resize needs to occur. Resizing first computes 2x then finds the
    // first prime in the table greater than 2x, i.e. if primes are ordered
    // p_1, p_2, ..., p_i, ..., it finds p_n such that p_n-1 < 2x < p_n. 
    // Doubling is important for preserving the asymptotic complexity of the
    // hashtable operations such as add.  Having a prime guarantees that double 
    // hashing does not lead to infinite loops.  IE, your hash function will be 
    // h1(key) + i*h2(key), 0 <= i < size.  h2 and the size must be relatively prime.
    private final static int[] Primes = {
        3, 7, 11, 17, 23, 29, 37, 47, 59, 71, 89, 107, 131, 163, 197, 239, 293, 353, 431, 521, 631, 761, 919,
        1103, 1327, 1597, 1931, 2333, 2801, 3371, 4049, 4861, 5839, 7013, 8419, 10103, 12143, 14591,
        17519, 21023, 25229, 30293, 36353, 43627, 52361, 62851, 75431, 90523, 108631, 130363, 156437,
        187751, 225307, 270371, 324449, 389357, 467237, 560689, 672827, 807403, 968897, 1162687, 1395263,
        1674319, 2009191, 2411033, 2893249, 3471899, 4166287, 4999559, 5999471, 7199369
    };

    public static boolean isPrime(final int candidate) {
        if ((candidate & 1) != 0) {
            final int limit = (int)Math.sqrt(candidate);
            for (int divisor = 3; divisor <= limit; divisor += 2) {
                if ((candidate % divisor) == 0) {
                    return false;
                }
            }
            return true;
        }
        return (candidate == 2);
    }

    public static int getPrime(final int min) {
        VerifyArgument.isNonNegative(min, "min");

        for (final int prime : Primes) {
            if (prime >= min) {
                return prime;
            }
        }

        //outside of our predefined table.
        //compute the hard way.
        for (int i = (min | 1); i < Integer.MAX_VALUE; i += 2) {
            if (isPrime(i) && ((i - 1) % HashPrime != 0)) {
                return i;
            }
        }
        return min;
    }

    public static int getMinPrime() {
        return Primes[0];
    }

    public static int expandPrime(final int oldSize) {
        final int newSize = 2 * oldSize;

        // Allow the hashtables to grow to maximum possible size (~2G elements) before encoutering capacity overflow.
        // Note that this check works even when _items.Length overflowed thanks to the (uint) cast 
        if (Math.abs(newSize) > MaxPrimeArrayLength && MaxPrimeArrayLength > oldSize) {
            assert MaxPrimeArrayLength == getPrime(MaxPrimeArrayLength) : "Invalid MaxPrimeArrayLength";
            return MaxPrimeArrayLength;
        }

        return getPrime(newSize);
    }
}
