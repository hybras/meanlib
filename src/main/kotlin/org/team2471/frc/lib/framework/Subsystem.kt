@file:Suppress("EXPERIMENTAL_API_USAGE")

package org.team2471.frc.lib.framework

import edu.wpi.first.networktables.NetworkTableInstance
import kotlinx.coroutines.Job
import org.team2471.frc.lib.framework.internal.EventHandler

/**
 * An individually requirable component of your robot.
 *
 * Optionally a [defaultFunction] may be provided. If the [defaultFunction] is provided, the [Subsystem]
 * is enabled, and there are no coroutines using this subsystem, a new coroutine will be launched that
 * calls the [defaultFunction]. This fulfills the same role as default commands in the wpilib command
 * system.
 *
 * Note that coroutines calling the [defaultFunction] implicitly requires this [Subsystem], so unless
 * the [defaultFunction] is called elsewhere in your code, it is not necessary to [use] this [Subsystem]
 * inside of it.
 *
 * Subsystems are disabled by default, so [enable] must be called before the subsystem can be used.
 *
 * @see use
 */
open class Subsystem(
    /**
     * The name of your Subsystem, required for debugging purposes.
     */
    val name: String,
    /**
     * An optional function that is run whenever the subsystem is enabled and unused.
     */
    internal val defaultFunction: (suspend () -> Unit)? = null
) {
    private val table = NetworkTableInstance.getDefault().getTable("Subsystems").getSubTable(name)
    private val enabledEntry = table.getEntry("Enabled")

    internal var activeJob: Job? = null

    /**
     * Whether or not the [Subsystem] is enabled.
     *
     * @see enable
     * @see disable
     */
    var isEnabled: Boolean = false
        internal set(value) {
            field = value
            enabledEntry.setBoolean(value)
        }

    /**
     * An optionally overloadable method. This method is automatically run whenever any [use] call completes,
     * regardless of if it completed or canceled.
     */
    open fun reset() { /* NOOP */ }

    /**
     * Enables the [Subsystem].
     *
     * If the [Subsystem] was previously disabled, the [defaultFunction] will be run in a different coroutine, if it
     * was provided.
     *
     * Note that enables are asynchronous, so it may take some time for the [Subsystem] to be enabled.
     */
    fun enable() = EventHandler.enableSubsystem(this)

    /**
     * Disables the [Subsystem].
     *
     * If the [Subsystem] was previously enabled, the current coroutine using it will be canceled, if there is one.
     *
     * Note that disables are asynchronous, so it may take some time for the [Subsystem] to be disabled.
     */
    fun disable() = EventHandler.disableSubsystem(this)

    init {
        enabledEntry.setBoolean(false)
    }
}

/**
 * Attempts to run the provided [body] with exclusive access to all provided [subsystems]. This fulfills
 * the same role as a Command in wpilib's command system.
 *
 * If [cancelConflicts] is set to false and one of the [subsystems] is being used by another coroutine,
 * an exception will be thrown and the provided [body] will not be invoked. Otherwise all coroutines requiring
 * any of [subsystems] will be cancelled and completed before the [body] is invoked.
 *
 * Use calls are re-entrant, meaning if a coroutine is using subsystems A and B calls [use] with subsystems B and C,
 * the code inside the nested [use] call's [body] will effectively be using subsystems A, B, and C, instead of
 * cancelling itself.
 */
suspend fun <R> use(vararg subsystems: Subsystem, cancelConflicts: Boolean = true, body: suspend () -> R) =
    EventHandler.useSubsystems(setOf(*subsystems), cancelConflicts, body)
