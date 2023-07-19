package dev.gluton.flrpc

inline fun Int.takeUnlessZero(): Int? = takeUnless { it == 0 }
inline fun UInt.takeUnlessZero(): UInt? = takeUnless { it == 0u }

/** `false` if 0, `true` otherwise */
inline fun Int.toBoolean(): Boolean = this != 0
inline fun Boolean.toInt(): Int = if (this) 1 else 0