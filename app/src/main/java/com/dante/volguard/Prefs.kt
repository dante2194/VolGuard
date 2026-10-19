package com.dante.volguard

import android.content.Context

object Prefs {
    private fun sp(c: Context) =
        c.getSharedPreferences("volguard", Context.MODE_PRIVATE)

    fun locked(c: Context, ch: Channel) = sp(c).getBoolean("l_${ch.key}", false)
    fun setLocked(c: Context, ch: Channel, v: Boolean) =
        sp(c).edit().putBoolean("l_${ch.key}", v).apply()

    fun target(c: Context, ch: Channel, def: Int) = sp(c).getInt("t_${ch.key}", def)
    fun setTarget(c: Context, ch: Channel, v: Int) =
        sp(c).edit().putInt("t_${ch.key}", v).apply()

    /** Lowering below the locked level is allowed. Default: blocked. */
    fun allowLower(c: Context, ch: Channel) = sp(c).getBoolean("lo_${ch.key}", false)
    fun setAllowLower(c: Context, ch: Channel, v: Boolean) =
        sp(c).edit().putBoolean("lo_${ch.key}", v).apply()

    /** Raising above the locked level is allowed. Default: blocked. */
    fun allowRaise(c: Context, ch: Channel) = sp(c).getBoolean("ra_${ch.key}", false)
    fun setAllowRaise(c: Context, ch: Channel, v: Boolean) =
        sp(c).edit().putBoolean("ra_${ch.key}", v).apply()

    fun anyLocked(c: Context) = Channel.entries.any { locked(c, it) }
}
