import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed

@OptIn(ExperimentalForeignApi::class)
interface NativePointerHolder<T : CStructVar> {
    val nativePointer: CPointer<T>

    companion object {
        protected inline fun <reified T : CStructVar, R> NativePointerHolder<T>.use(block: T.() -> R) =
            nativePointer.pointed.block()
    }
}

