package com.hyperos.weather.widget

internal data class LunarDate(val day: Int, val month: Int, val year: Int)

internal object LunarCalendar {
    fun convert(dd: Int, mm: Int, yy: Int): LunarDate {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = getNewMoonDay(k + 1)
        if (monthStart > dayNumber) monthStart = getNewMoonDay(k)
        var a11 = getLunarMonth11(yy)
        var b11 = a11
        var lunarYear = yy
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = getLunarMonth11(yy - 1)
        } else {
            lunarYear = yy + 1
            b11 = getLunarMonth11(yy + 1)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = ((monthStart - a11) / 29).toInt()
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11)
            if (diff >= leapMonthDiff) lunarMonth = diff + 10
        }
        if (lunarMonth > 12) lunarMonth -= 12
        if (lunarMonth >= 11 && diff < 4) lunarYear--
        return LunarDate(lunarDay, lunarMonth, lunarYear)
    }

    private fun getLunarMonth11(yy: Int): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toInt()
        var nm = getNewMoonDay(k)
        val sunLong = getSunLongitude(nm)
        if (sunLong >= 9) nm = getNewMoonDay(k - 1)
        return nm
    }

    private fun getLeapMonthOffset(a11: Int): Int {
        val k = floor(0.5 + (a11 - 2415021.076998695) / 29.530588853).toInt()
        var last = 0
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i))
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i))
        } while (arc != last && i < 15)
        return i - 1
    }

    private fun getNewMoonDay(k: Int): Int {
        val T = k / 1236.85
        val dr = Math.PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T * T - 0.000000155 * T * T * T
        jd1 += 0.00033 * sin((166.56 + 132.87 * T - 0.009173 * T * T) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * T * T - 0.00000347 * T * T * T
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T * T + 0.00001236 * T * T * T
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * T * T - 0.00000239 * T * T * T
        var c1 = (0.1734 - 0.000393 * T) * sin(m * dr) + 0.0021 * sin(2 * m * dr)
        c1 -= 0.4068 * sin(mpr * dr) + 0.0161 * sin(2 * mpr * dr)
        c1 -= 0.0004 * sin(3 * mpr * dr)
        c1 += 0.0104 * sin(2 * f * dr) - 0.0051 * sin((m + mpr) * dr)
        c1 -= 0.0074 * sin((m - mpr) * dr) + 0.0004 * sin((2 * f + m) * dr)
        c1 -= 0.0004 * sin((2 * f - m) * dr) - 0.0006 * sin((2 * f + mpr) * dr)
        c1 += 0.0010 * sin((2 * f - mpr) * dr) + 0.0005 * sin((2 * mpr + m) * dr)
        val dt = if (T < -11) {
            0.001 + 0.000839 * T + 0.0002261 * T * T - 0.00000845 * T * T * T - 0.000000081 * T * T * T * T
        } else {
            -0.000278 + 0.000265 * T + 0.000262 * T * T
        }
        return floor(jd1 + c1 - dt + 0.5).toInt()
    }

    private fun getSunLongitude(jdn: Int): Int {
        val dr = Math.PI / 180
        val t = (jdn - 2451545.0 - 0.5) / 36525
        val t2 = t * t
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(2 * dr * m) + 0.000290 * sin(3 * dr * m)
        val l = (l0 + dl) * dr
        return floor((l / Math.PI * 6)).toInt() % 12
    }

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        return if (yy > 1582 || (yy == 1582 && (mm > 10 || (mm == 10 && dd >= 15)))) {
            dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        } else {
            dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        }
    }

    private fun floor(x: Double) = kotlin.math.floor(x)
    private fun sin(x: Double) = kotlin.math.sin(x)
}
