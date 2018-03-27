// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    comparable(select(getShort(), 1))
}

fun <T> select(x: T, y: T): T = x

fun <R : Comparable<R>> comparable(f: R) {}

fun getShort(): Short = 1