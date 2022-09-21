package com.sayuru.training

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

fun main() {
    val startTime = System.currentTimeMillis()
    val path = System.getProperty("user.dir")

    val inputStream: InputStream = File("$path/src/main/resources/SAMPLE.dat").inputStream()
//    val inputStream: InputStream = File("$args[0]").inputStream()

    val infoCSVPath = "$path/src/main/resources/INFO.csv"
    val tradeCSVPath = "$path/src/main/resources/TRADE.csv"
    val exTradeCSVPath = "$path/src/main/resources/EX_TRADE.csv"

    val lineList = mutableListOf<String>()
    inputStream.bufferedReader().forEachLine { lineList.add(it) }

    if (lineList.first().take(5) != "HEADR") {
        printErr("Invalid Header")
        return
    }

    val headerLine = lineList.first()

    if (headerLine.length < 27) {
        printErr("Invalid Header Length")
        return
    }

    val head = Header(headerLine)

    if (head.headerVersion != "0004".toLong() && head.headerVersion != "0005".toLong()) {
        printErr("Invalid Header Version")
        return
    }
    if (head.headerVersion < 5 && head.formattedFileComment.isNotEmpty()) {
        printErr("Invalid Header Comment")
        return
    }
    if (head.formattedFileComment.length != head.fileCommentAllowedLength) {
        printErr("Invalid Header Comment Size")
        return
    }

//    println(head)

    val tradeList = lineList.filter { it.take(5) == "TRADE" }
    val sortedTradeList =
        tradeList.sortedByDescending { it.substring(35, 50).toBigDecimal() * it.substring(50, 61).toBigDecimal() }

    val tradeQty = tradeList.size

    if (tradeQty == 0) {
        println("No Trades")
        return
    }

    val tradeWriter = Files.newBufferedWriter(Paths.get(tradeCSVPath))

    val tradeCSVPrinter = CSVPrinter(
        tradeWriter, CSVFormat.Builder.create().setHeader(
            "Trade Date and Time",
            "Direction",
            "Item ID",
            "Price",
            "Quantity",
            "Buyer",
            "Seller",
            "Comment"
        ).build()
    )

    for (i in 0 until tradeQty) {
        if (sortedTradeList[i].length < 69) {
            printErr("Invalid Trade @ trade $i")
        }

        val trade = Trade(sortedTradeList[i])

//        println(trade)

        if (trade.tradeDirection != "B" && trade.tradeDirection != "S") {
            printErr("Invalid Trade Direction @ trade $i")
            return
        }
        for (j in 0 until 3) {
            if (!trade.tradeItemID[j].isUpperCase()) {
                printErr("Invalid Trade Item ID @ trade $i , item $j")
            }
        }
        for (j in 3 until 12) {
            if (!trade.tradeItemID[j].isDigit() && !trade.tradeItemID[j].isUpperCase()) {
                printErr("Invalid Trade Item ID @ trade $i , item $j")
            }
        }
        if (trade.tradeQuantity <= 0) {
            printErr("Invalid Trade Quantity @ trade $i")
        }
        if (!isStringContainsLatinCharactersAndUnderscoreOnly(trade.tradeBuyer)) {
            printErr("Invalid Trade Buyer ID @ trade $i")
        }
        if (!isStringContainsLatinCharactersAndUnderscoreOnly(trade.tradeSeller)) {
            printErr("Invalid Trade Seller ID @ trade $i")
        }

        try {
            tradeCSVPrinter.printRecord(
                trade.formattedDateTime,
                trade.tradeDirection,
                trade.tradeItemID,
                trade.tradePrice,
                trade.tradeQuantity,
                trade.tradeBuyer,
                trade.tradeSeller,
                trade.tradeComment
            )
            tradeCSVPrinter.flush()
        } catch (e: Exception) {
            printErr("Error writing to TRADE.csv")
        }
    }

    val exTradeList = lineList.filter { it.take(5) == "EXTRD" }
    val sortedExTradeList =
        exTradeList.sortedByDescending { it.substring(42, 57).toBigDecimal() * it.substring(57, 68).toBigDecimal() }

    val exTradeQty = exTradeList.size
    if (exTradeQty == 0) {
        println("No Ex-Trades")
        return
    }

    val exTradeWriter = Files.newBufferedWriter(Paths.get(exTradeCSVPath))
    val exTradeCSVPrinter = CSVPrinter(
        exTradeWriter, CSVFormat.Builder.create().setHeader(
            "Trade Version",
            "Date & Time",
            "Direction",
            "Item ID",
            "Price",
            "Quantity",
            "Buyer",
            "Seller",
            "Nested Tags"
        ).build()
    )

    for (i in 0 until exTradeQty) {
        if (sortedExTradeList[i].length < 30) {
            printErr("Invalid Ex-Trade @ ex-trade $i")
        }

        val exTrade = ExTrade(sortedExTradeList[i])

//        println(exTrade)

        if (exTrade.tradeVersion != "0001".toLong()) {
            printErr("Invalid ExTrade Version @ exTrade $i")
        }
        if (exTrade.tradeDirection != "BUY_" && exTrade.tradeDirection != "SELL") {
            printErr("Invalid ExTrade Direction @ exTrade $i")
        }

        for (j in 0 until 3) {
            if (!exTrade.tradeItemID[j].isUpperCase()) {
                printErr("Invalid ExTrade Item ID @ exTrade $i , item $j")
            }
        }
        for (j in 3 until 12) {
            if (!exTrade.tradeItemID[j].isDigit() && !exTrade.tradeItemID[j].isUpperCase()) {
                printErr("Invalid ExTrade Item ID @ exTrade $i , item $j")
            }
        }
        if (exTrade.tradeQuantity <= 0) {
            printErr("Invalid ExTrade Quantity @ exTrade $i")
        }
        if (!isStringContainsLatinCharactersAndUnderscoreOnly(exTrade.tradeBuyer)) {
            printErr("Invalid ExTrade Buyer @ exTrade $i")
        }

        try {
            exTradeCSVPrinter.printRecord(
                exTrade.tradeVersion,
                exTrade.formattedDateTime,
                exTrade.tradeDirection,
                exTrade.tradeItemID,
                exTrade.tradePrice,
                exTrade.tradeQuantity,
                exTrade.tradeBuyer,
                exTrade.tradeSeller,
                exTrade.nestedTags
            )
            exTradeCSVPrinter.flush()
        } catch (e: Exception) {
            printErr("Error writing to EX_TRADE.csv")
        }
    }

    val allTradeCount: Long = tradeQty.toLong() + exTradeQty.toLong()

    if (lineList.last().take(5) != "FOOTR") {
        printErr("Invalid Footer")
        return
    }

    val footerLine = lineList.last()
    if (footerLine.length < 15) {
        printErr("Invalid Footer Length")
        return
    }

    val foot = Footer(footerLine)

    if (foot.tradeCount != allTradeCount) {
        printErr("Invalid Footer Trade Count")
        return
    }
    if (head.headerVersion != ("0005").toLong() && !foot.tradeCharCount.equals(null)) {
        printErr("Invalid Footer Trade Char Count Exists")
        return
    }

//    println(foot)

    val infoWriter = Files.newBufferedWriter(Paths.get(infoCSVPath))
    val csvPrinter = CSVPrinter(
        infoWriter, CSVFormat.Builder.create().setHeader(
            "Header Version",
            "File Creation Date and Time",
            "File Comment",
            "Total Number of Trades and ExTrades",
            "Number of Characters in Trade and ExTrade Structures"
        ).build()
    )
    try {
        csvPrinter.printRecord(
            head.headerVersion,
            head.fileCreation,
            head.formattedFileComment,
            foot.tradeCount,
            foot.tradeCharCount
        )

        csvPrinter.flush()
    } catch (e: Exception) {
        printErr("Error writing to INFO.csv")
    }
    csvPrinter.close()

    for (i in 0 until lineList.size) {
        if (lineList[i].take(5) != "HEADR" && lineList[i].take(5) != "FOOTR" && lineList[i].take(5) != "TRADE" && lineList[i].take(
                5
            ) != "EXTRD"
        ) {
            printErr("Invalid Line Detected @ line $i")
        }
    }

    println("Execution completed! Check the output files for the results.\nExecution time: ${System.currentTimeMillis() - startTime} ms")
}

data class Header(val header: String) {
    var headerVersion: Long = header.substring(5, 9).toLong()
    var fileCreation: String = header.substring(9, 26)
    private var fileComment: String = header.substring(26, header.length)
    var fileCommentAllowedLength: Int = fileComment.substringAfter("{").substringBefore("}").toInt()
    var formattedFileComment: String = fileComment.substringAfter("}")
}

open class AnyTrade {
    val dateTimeFormatIdentifier: DateTimeFormatter? = DateTimeFormatterBuilder()
        .appendPattern("yyyyMMddHHmmssSSS")
        .toFormatter()
    val dateTimeFormatter: DateTimeFormatter? = DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .toFormatter()
}

data class Trade(val trade: String) : AnyTrade() {
    private var tradeDateTime: String = trade.substring(5, 22)
    var formattedDateTime: String = LocalDateTime.parse(tradeDateTime, dateTimeFormatIdentifier).format(
        dateTimeFormatter
    )
    var tradeDirection: String = trade.substring(22, 23)
    var tradeItemID: String = trade.substring(23, 35)
    var tradePrice: BigDecimal = trade.substring(35, 50).toBigDecimal().divide(BigDecimal(10000))
    var tradeQuantity: Long = trade.substring(50, 61).toLong()
    var tradeBuyer: String = trade.substring(61, 65)
    var tradeSeller: String = trade.substring(65, 69)
    var tradeComment: String = trade.substring(69, 101).trimStart().replace("""[\\,/]""".toRegex(), "")
}

data class ExTrade(val exTrade: String) : AnyTrade() {
    var tradeVersion: Long = exTrade.substring(5, 9).toLong()
    private var tradeDateTime: String = exTrade.substring(9, 26)
    var formattedDateTime: String = LocalDateTime.parse(tradeDateTime, dateTimeFormatIdentifier).format(
        dateTimeFormatter
    )
    var tradeDirection: String = exTrade.substring(26, 30)
    var tradeItemID: String = exTrade.substring(30, 42)
    var tradePrice: BigDecimal = exTrade.substring(42, 57).toBigDecimal().divide(BigDecimal(10000))
    var tradeQuantity: Long = exTrade.substring(57, 68).toLong()
    var tradeBuyer: String = exTrade.substring(68, 72)
    var tradeSeller: String = exTrade.substring(72, 76)
    var nestedTags: String = exTrade.substring(76, exTrade.length).trimStart()
}

data class Footer(val footer: String) {
    var tradeCount: Long = footer.substring(5, 15).toLong()
    var tradeCharCount: Long = footer.substring(15, 25).toLong()
}

fun isStringContainsLatinCharactersAndUnderscoreOnly(iStringToCheck: String): Boolean {
    return iStringToCheck.matches("^[a-zA-Z_]+$".toRegex())
}

fun printErr(errorMsg: String) {
    System.err.println(errorMsg)
}