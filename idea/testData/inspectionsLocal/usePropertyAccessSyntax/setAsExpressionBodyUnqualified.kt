// WITH_RUNTIME
class C: Thread() {
    fun foo(n: String) = setName<caret>(n)
}
