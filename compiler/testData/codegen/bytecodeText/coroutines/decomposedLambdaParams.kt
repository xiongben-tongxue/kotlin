// WITH_RUNTIME
// WITH_COROUTINES
// TREAT_AS_ONE_FILE
import helpers.*
import kotlin.coroutines.experimental.*

data class Data(val x: String, val y: Int)

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun main(args: Array<String>) {
    builder {
        foo(Data("A", 1)) { (x_param, y_param) ->
            println("$x_param / $y_param")
        }
    }
}

suspend fun foo(data: Data, body: suspend (Data) -> Unit) {
    body(data)
}

// 5 LOCALVARIABLE data Ljava/lang/Object;
// 2 LOCALVARIABLE throwable Ljava/lang/Throwable;
// 1 LOCALVARIABLE x_param Ljava/lang/String;
// 1 LOCALVARIABLE y_param I