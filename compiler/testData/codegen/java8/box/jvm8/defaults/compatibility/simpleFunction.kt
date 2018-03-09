// !API_VERSION: 1.3
// FILE: Simple.java

public interface Simple extends KInterface {
    default String test() {
        return KInterface.DefaultImpls.test(this);
    }
}

// FILE: Foo.java
public class Foo implements Simple {

}

// FILE: main.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME

@JvmDefaultCompatibility
interface KInterface  {
    @JvmDefault
    fun test(): String {
        return "OK"
    }
}


fun box(): String {
    return Foo().test()
}