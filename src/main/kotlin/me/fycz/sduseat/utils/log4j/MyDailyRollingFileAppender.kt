/*
 * This file is part of Sdu-Seat
 * Sdu-Seat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sdu-Seat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sdu-Seat.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C)  2022~2023 fengyuecanzhu
 */

package me.fycz.sduseat.utils.log4j

import org.apache.log4j.FileAppender
import org.apache.log4j.Layout
import org.apache.log4j.helpers.CountingQuietWriter
import org.apache.log4j.helpers.LogLog
import org.apache.log4j.spi.LoggingEvent
import java.io.File
import java.io.IOException
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author fengyue
 * @date 2022/6/3 10:56
 */
class MyDailyRollingFileAppender : FileAppender {
    /**
     * The default maximum file size is 10MB.
     */
    protected var maxFileSize = 10 * 1024 * 1024
    /** Returns the value of the **DatePattern** option.  */
    /**
     * The **DatePattern** takes a string in the same format as expected by
     * [SimpleDateFormat]. This options determines the rollover schedule.
     */
    /**
     * The date pattern. By default, the pattern is set to "'.'yyyy-MM-dd" meaning daily
     * rollover.
     */
    var datePattern: String? = "'.'yyyy-MM-dd"
    /**
     * The **MaxBackupIndex** option determines how many backup files are kept before
     * the oldest is erased. This option takes a positive integer value. If set to zero,
     * then there will be no backup files and the log file will be renamed to the value of
     * the scheduledFilename variable when the next interval is entered.
     */
    /**
     * There is one backup file by default.
     */
    var maxBackupIndex = 1

    /**
     * The log file will be renamed to the value of the scheduledFilename variable when
     * the next interval is entered. For example, if the rollover period is one hour, the
     * log file will be renamed to the value of "scheduledFilename" at the beginning of
     * the next hour. The precise time when a rollover occurs depends on logging activity.
     */
    private var scheduledFilename: String? = null

    /**
     * The next time we estimate a rollover should occur.
     */
    private var nextCheck = System.currentTimeMillis() - 1
    private val now = Date()
    private var sdf: SimpleDateFormat? = null
    private val rollingCalendar = MyRollingCalendar()
    var checkPeriod = TOP_OF_TROUBLE

    /**
     * The default constructor does nothing.
     */
    constructor() {}

    /**
     * Instantiate a `DailyRollingFileAppender` and open the file designated by
     * `filename`. The opened filename will become the ouput destination for
     * this appender.
     */
    constructor(layout: Layout?, filename: String?, datePattern: String?) : super(layout, filename, true) {
        this.datePattern = datePattern
        activateOptions()
    }

    override fun activateOptions() {
        super.activateOptions()
        LogLog.debug("Max backup file kept: $maxBackupIndex.")
        if (datePattern != null && fileName != null) {
            now.time = System.currentTimeMillis()
            sdf = SimpleDateFormat(datePattern)
            val type = computeCheckPeriod()
            printPeriodicity(type)
            rollingCalendar.type = type
            val file = File(fileName)
            scheduledFilename = fileName + sdf!!.format(Date(file.lastModified()))
        } else {
            LogLog.error("Either File or DatePattern options are not set for appender [$name].")
        }
    }

    fun printPeriodicity(type: Int) {
        when (type) {
            TOP_OF_MINUTE -> LogLog.debug("Appender [[+name+]] to be rolled every minute.")
            TOP_OF_HOUR -> LogLog.debug("Appender [$name] to be rolled on top of every hour.")
            HALF_DAY -> LogLog.debug("Appender [$name] to be rolled at midday and midnight.")
            TOP_OF_DAY -> LogLog.debug("Appender [$name] to be rolled at midnight.")
            TOP_OF_WEEK -> LogLog.debug("Appender [$name] to be rolled at start of week.")
            TOP_OF_MONTH -> LogLog.debug("Appender [$name] to be rolled at start of every month.")
            else -> LogLog.warn("Unknown periodicity for appender [[+name+]].")
        }
    }

    fun computeCheckPeriod(): Int {
        val rollingPastCalendar = MyRollingCalendar(gmtTimeZone, Locale.ENGLISH)
        // set sate to 1970-01-01 00:00:00 GMT
        val epoch = Date(0)
        if (datePattern == null) {
            return TOP_OF_TROUBLE
        }
        for (i in TOP_OF_MINUTE..TOP_OF_MONTH) {
            val simpleDateFormat = SimpleDateFormat(datePattern)
            simpleDateFormat.timeZone = gmtTimeZone // do all date formatting in
            // GMT
            val r0 = simpleDateFormat.format(epoch)
            rollingPastCalendar.type = i
            val next = Date(rollingPastCalendar.getNextCheckMillis(epoch))
            val r1 = simpleDateFormat.format(next)

            // System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
            if (r0 != null && r1 != null && r0 != r1) {
                return i
            }
        }
        return TOP_OF_TROUBLE // Deliberately head for trouble...
    }

    /**
     * Rollover the current file to a new file.
     */
    @Throws(IOException::class)
    fun rollOver() {
        /* Compute filename, but only if datePattern is specified */
        if (datePattern == null) {
            errorHandler.error("Missing DatePattern option in rollOver().")
            return
        }
        val datedFilename = fileName + sdf!!.format(now)
        // It is too early to roll over because we are still within the
        // bounds of the current interval. Rollover will occur once the
        // next interval is reached.
        if (scheduledFilename == datedFilename) {
            return
        }

        // close current file, and rename it to datedFilename
        closeFile()
        val target = File(scheduledFilename)
        if (target.exists()) {
            target.delete()
        }
        var file = File(fileName)
        val result = file.renameTo(target)
        if (result) {
            LogLog.debug("$fileName -> $scheduledFilename")

            // If maxBackups <= 0, then there is no file renaming to be done.
            if (maxBackupIndex > 0) {
                // Delete the oldest file, to keep system happy.
                file = File(fileName + dateBefore())

                // 删除很久以前的历史log文件
                deleteAncientFilesIfExists(file)
                if (file.exists()) {
                    file.delete()
                }
            }
        } else {
            LogLog.error("Failed to rename [[+fileName+]] to [[+scheduledFilename+]].")
        }
        try {
            // This will also close the file. This is OK since multiple close operations
            // are safe.
            this.setFile(fileName, false, bufferedIO, bufferSize)
        } catch (e: IOException) {
            errorHandler.error("setFile($fileName, false) call failed.")
        }
        scheduledFilename = datedFilename
    }

    /**
     * 删除很久以前没有删除的日志文件（如果存在的话）
     *
     * @param oldestFile
     */
    private fun deleteAncientFilesIfExists(oldestFile: File) {
        // 找出久远日志文件列表
        val ancientfiles = oldestFile.parentFile.listFiles { pathname ->
            (pathname.path.replace("\\\\".toRegex(), "/").startsWith(fileName.replace("\\\\".toRegex(), "/"))
                    && pathname.name < oldestFile.name)
        }

        // 删除久远日志文件列表
        ancientfiles?.forEach {
            // 如果文件比配置的最老日期还老的话，删掉
            it.delete()
        }
    }

    private fun dateBefore(): String {
        var dataAnte = ""
        if (datePattern != null) {
            val simpleDateFormat = SimpleDateFormat(datePattern)
            dataAnte = simpleDateFormat
                .format(Date(rollingCalendar.getPastCheckMillis(Date(), maxBackupIndex)))
        }
        return dataAnte
    }

    /**
     * This method differentiates DailyRollingFileAppender from its super class.
     *
     *
     * Before actually logging, this method will check whether it is time to do a
     * rollover. If it is, it will schedule the next rollover time and then rollover.
     */
    override fun subAppend(event: LoggingEvent) {
        // 根据文件大小roll over
        rollOverBySize()

        // 根据时间roll over
        rollOverByTime()
        super.subAppend(event)
    }

    /**
     * 根据文件大小roll over
     */
    private fun rollOverBySize() {
        val currentTimeMillis = System.currentTimeMillis()
        now.time = currentTimeMillis
        if (fileName != null && qw != null) {
            val size = (qw as CountingQuietWriter).count
            if (size >= maxFileSize) {
                // close current file, and rename it
                closeFile()
                val rollingFileName = fileName + sdf!!.format(now) + '.' + currentTimeMillis
                File(fileName).renameTo(File(rollingFileName))
                try {
                    // This will also close the file. This is OK since multiple close
                    // operations
                    // are safe.
                    this.setFile(fileName, false, bufferedIO, bufferSize)
                } catch (e: IOException) {
                    errorHandler.error("setFile($fileName, false) call failed.")
                }
            }
        }
    }

    /**
     * 根据时间roll over
     */
    private fun rollOverByTime() {
        val currentTime = System.currentTimeMillis()
        if (currentTime < nextCheck) {
            return
        }
        now.time = currentTime
        nextCheck = rollingCalendar.getNextCheckMillis(now)
        try {
            rollOver()
        } catch (ioe: IOException) {
            LogLog.error("rollOver() failed.", ioe)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    override fun setFile(fileName: String, append: Boolean, bufferedIO: Boolean, bufferSize: Int) {
        super.setFile(fileName, append, this.bufferedIO, this.bufferSize)
        if (append) {
            val f = File(fileName)
            (qw as CountingQuietWriter).count = f.length()
        }
    }

    override fun setQWForFiles(writer: Writer) {
        qw = CountingQuietWriter(writer, errorHandler)
    }

    companion object {
        // The code assumes that the following constants are in a increasing
        // sequence.
        const val TOP_OF_TROUBLE = -1
        const val TOP_OF_MINUTE = 0
        const val TOP_OF_HOUR = 1
        const val HALF_DAY = 2
        const val TOP_OF_DAY = 3
        const val TOP_OF_WEEK = 4
        const val TOP_OF_MONTH = 5

        /**
         * The gmtTimeZone is used only in computeCheckPeriod() method.
         */
        val gmtTimeZone = TimeZone.getTimeZone("GMT")
    }
}

/**
 * MyRollingCalendar is a helper class to DailyMaxRollingFileAppender. Given a periodicity
 * type and the current time, it computes the past maxBackupIndex date.
 */
internal class MyRollingCalendar : GregorianCalendar {
    var type = TOP_OF_TROUBLE

    constructor() : super() {}
    constructor(tz: TimeZone?, locale: Locale?) : super(tz, locale) {}

    fun getPastCheckMillis(now: Date?, maxBackupIndex: Int): Long {
        return getPastDate(now, maxBackupIndex).time
    }

    fun getPastDate(now: Date?, maxBackupIndex: Int): Date {
        setTime(now)
        when (type) {
            TOP_OF_MINUTE -> this[MINUTE] = this[MINUTE] - maxBackupIndex
            TOP_OF_HOUR -> this[HOUR_OF_DAY] = this[HOUR_OF_DAY] - maxBackupIndex
            HALF_DAY -> {
                val hour = get(HOUR_OF_DAY)
                if (hour < 12) {
                    this[HOUR_OF_DAY] = 12
                } else {
                    this[HOUR_OF_DAY] = 0
                }
                this[DAY_OF_MONTH] = this[DAY_OF_MONTH] - maxBackupIndex
            }
            TOP_OF_DAY -> this[DATE] = this[DATE] - maxBackupIndex
            TOP_OF_WEEK -> {
                this[DAY_OF_WEEK] = firstDayOfWeek
                this[WEEK_OF_YEAR] = this[WEEK_OF_YEAR] - maxBackupIndex
            }
            TOP_OF_MONTH -> this[MONTH] = this[MONTH] - maxBackupIndex
            else -> throw IllegalStateException("Unknown periodicity type.")
        }
        return getTime()
    }

    fun getNextCheckMillis(now: Date?): Long {
        return getNextCheckDate(now).time
    }

    fun getNextCheckDate(now: Date?): Date {
        setTime(now)
        when (type) {
            TOP_OF_MINUTE -> {
                this[SECOND] = 0
                this[MILLISECOND] = 0
                add(MINUTE, 1)
            }
            TOP_OF_HOUR -> {
                this[MINUTE] = 0
                this[SECOND] = 0
                this[MILLISECOND] = 0
                add(HOUR_OF_DAY, 1)
            }
            HALF_DAY -> {
                this[MINUTE] = 0
                this[SECOND] = 0
                this[MILLISECOND] = 0
                val hour = get(HOUR_OF_DAY)
                if (hour < 12) {
                    this[HOUR_OF_DAY] = 12
                } else {
                    this[HOUR_OF_DAY] = 0
                    add(DAY_OF_MONTH, 1)
                }
            }
            TOP_OF_DAY -> {
                this[HOUR_OF_DAY] = 0
                this[MINUTE] = 0
                this[SECOND] = 0
                this[MILLISECOND] = 0
                add(DATE, 1)
            }
            TOP_OF_WEEK -> {
                this[DAY_OF_WEEK] = firstDayOfWeek
                this[HOUR_OF_DAY] = 0
                this[MINUTE] = 0
                this[SECOND] = 0
                this[MILLISECOND] = 0
                add(WEEK_OF_YEAR, 1)
            }
            TOP_OF_MONTH -> {
                this[DATE] = 1
                this[HOUR_OF_DAY] = 0
                this[MINUTE] = 0
                this[SECOND] = 0
                this[MILLISECOND] = 0
                add(MONTH, 1)
            }
            else -> throw IllegalStateException("Unknown periodicity type.")
        }
        return getTime()
    }

    companion object {
        private const val serialVersionUID = 1L
        const val TOP_OF_TROUBLE = -1
        const val TOP_OF_MINUTE = 0
        const val TOP_OF_HOUR = 1
        const val HALF_DAY = 2
        const val TOP_OF_DAY = 3
        const val TOP_OF_WEEK = 4
        const val TOP_OF_MONTH = 5
    }
}