// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
@JvmDefaultCompatibility
interface Test {
    @JvmDefault
    fun test(): String {
        return "OK"
    }
}

class TestClass : Test {

}

fun box(): String {
    val defaultImpls = java.lang.Class.forName(Test::class.java.canonicalName + "\$DefaultImpls")

    val declaredMethod = defaultImpls.getDeclaredMethod("test", Test::class.java)
    return declaredMethod.invoke(null, TestClass()) as String
}
