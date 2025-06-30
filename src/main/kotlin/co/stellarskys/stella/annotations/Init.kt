package co.stellarskys.stella.annotations

import co.stellarskys.stella.Stella.Companion
import java.lang.annotation.*;

/**
 *
 *
 * Marks a method to be called upon mod initialization, performing any initialization logic for the class.
 * **In order for a method to be considered an initializer method, it must be public & static while having no arguments and a void return type.**
 *
 * Example usage:
 * <pre>
 * `public static void init() {
 * //do stuff
 * }
` *
</pre> *
 *
 *
 * A call to the method annotated with this annotation will be added to the [Companion.init] method at compile-time.
 *
 *
 *
 * If your method depends on another initializer method, you can use the [.priority] field to ensure that it is called after the other method.
 *
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
annotation class Init(
    /**
     * The priority of the initializer method.
     * The higher the number, the later the method will be called.
     * Use this to ensure that your initializer method is called after another initializer method if it depends on it.
     */
    val priority: Int = 0
)