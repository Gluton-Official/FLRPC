
inline fun Int.takeUnlessZero(): Int? = takeUnless { it == 0 }
inline fun UInt.takeUnlessZero(): UInt? = takeUnless { it == 0u }
