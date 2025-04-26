package copy.com.gtnewhorizon.gtnhlib.hash;

/**
 * The 32-bit version of <a href="http://www.isthe.com/chongo/tech/comp/fnv/index.html">Fowler/Noll/Vo 1-alternate</a>
 * hash. To use this class, you need to keep track of a single {@code int} worth of state yourself, for example:
 *
 * <pre>
 * int hash = Fnv1a32.initialState();
 * hash = Fnv1a32.hashStep(hash, value1);
 * hash = Fnv1a32.hashStep(hash, value2);
 * hash = Fnv1a32.hashStep(hash, value3);
 * </pre>
 *
 * The final hash is just the state after performing all the hashing steps, there is no separate finalization step
 * needed.
 */
@SuppressWarnings("unused") // Public API
public final class Fnv1a32 {

    /** The FNV_prime constant for this hash function */
    public static final int PRIME32 = 0x100_0193;
    /** The offset_basis constant for this hash function */
    public static final int OFFSET_BASIS32 = 0x811C_9DC5;

    /**
     * @return The value to assign to the state variable to begin constructing a new hash.
     */
    public static int initialState() {
        return OFFSET_BASIS32;
    }

    /**
     * Hashes a single byte into the given state, and returns the updated state.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(final int prevState, final byte value) {
        return (prevState ^ value) * PRIME32;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Booleans are converted to
     * a 0/1 byte first.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(final int prevState, final boolean value) {
        return hashStep(prevState, value ? (byte) 1 : (byte) 0);
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Chars are hashed like
     * their underlying short representation.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(final int prevState, final char value) {
        return hashStep(prevState, (short) value);
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Integers are hashed in
     * order from most to least significant byte.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(int prevState, final short value) {
        prevState = hashStep(prevState, (byte) ((value >> 8) & 0xFF));
        prevState = hashStep(prevState, (byte) (value & 0xFF));
        return prevState;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Integers are hashed in
     * order from most to least significant byte.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(int prevState, final int value) {
        prevState = hashStep(prevState, (byte) ((value >> 24) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 16) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 8) & 0xFF));
        prevState = hashStep(prevState, (byte) (value & 0xFF));
        return prevState;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Integers are hashed in
     * order from most to least significant byte.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(int prevState, final long value) {
        prevState = hashStep(prevState, (byte) ((value >> 56) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 48) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 40) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 32) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 24) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 16) & 0xFF));
        prevState = hashStep(prevState, (byte) ((value >> 8) & 0xFF));
        prevState = hashStep(prevState, (byte) (value & 0xFF));
        return prevState;
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Floats are hashed in
     * order from most to least significant byte of their integer representation.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(final int prevState, final float value) {
        return hashStep(prevState, Float.floatToRawIntBits(value));
    }

    /**
     * Hashes a single value into the given state byte by byte, and returns the updated state. Floats are hashed in
     * order from most to least significant byte of their integer representation.
     *
     * @param prevState The state to update
     * @param value     The value to hash into the new state
     * @return The new hash state, computed by hashing the value into the previous state
     */
    public static int hashStep(final int prevState, final double value) {
        return hashStep(prevState, Double.doubleToLongBits(value));
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final byte[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final boolean[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final char[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final short[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final int[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final long[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final float[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes an array of values by hashing each individual element.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final double[] array) {
        for (var element : array) {
            prevState = hashStep(prevState, element);
        }
        return prevState;
    }

    /**
     * Hashes any {@link CharSequence} (including {@link String}) by hashing each individual code unit in it.
     *
     * @param prevState The state to update
     * @param array     The array to hash
     * @return The new hash state, computed by hashing the values into the previous state
     */
    public static int hashStep(int prevState, final CharSequence array) {
        final int length = array.length();
        for (int i = 0; i < length; i++) {
            prevState = hashStep(prevState, array.charAt(i));
        }
        return prevState;
    }
}
