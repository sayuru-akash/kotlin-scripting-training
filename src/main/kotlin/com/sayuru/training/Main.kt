package com.sayuru.training

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val path = System.getProperty("user.dir")

    val inputStream: InputStream = File("$path/src/main/resources/SAMPLE.dat").inputStream()
//    val inputStream: InputStream = File("$args[0]").inputStream()

    val infoCSVPath = "$path/src/main/resources/INFO.csv"
    val tradeCSVPath = "$path/src/main/resources/TRADE.csv"
    val exTradeCSVPath = "$path/src/main/resources/EXTRADE.csv"

    val infoWriter = Files.newBufferedWriter(Paths.get(infoCSVPath))
    val tradeWriter = Files.newBufferedWriter(Paths.get(tradeCSVPath))
    val exTradeWriter = Files.newBufferedWriter(Paths.get(exTradeCSVPath))

    val lineList = mutableListOf<String>()
    inputStream.bufferedReader().forEachLine { lineList.add(it) }

    val headerList = lineList[0]

    if (headerList.take(5) != "HEADR") {
        printErr("Invalid Header")
        return
    }
    val head = Headr(headerList)
    when (head.headerVersion) {
        "0004".toLong(), "0005".toLong() -> {}
        else -> {
            printErr("Invalid Header Version")
            return
        }
    }
    if (head.headerVersion < 5 && head.fileComment.isNotEmpty()) {
        printErr("Invalid Header Comment")
        return
    }

//    println(head)

    val tradeList = lineList.filter { it.take(5) == "TRADE" }
    val sortedTradeList =
        tradeList.sortedByDescending { it.substring(35, 50).toBigDecimal() * it.substring(50, 61).toBigDecimal() }

    val tradeQty = tradeList.size
    when (tradeQty) {
        0 -> {
            println("No Trades")
        }

        else -> {}
    }

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
        val trade = Trade(sortedTradeList[i])
//        println(trade)
        when (trade.tradeDirection) {
            "B" -> {}
            "S" -> {}
            else -> {
                printErr("Invalid Trade Direction")
            }
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
        for (j in 0 until 4) {
            if (!trade.tradeBuyer[j].isLetterOrDigit() && trade.tradeBuyer[j] != '_') {
                printErr("Invalid Trade Buyer @ trade $i , buyer $j")
            }
            if (!trade.tradeSeller[j].isLetterOrDigit() && trade.tradeSeller[j] != '_') {
                printErr("Invalid Trade Seller @ trade $i , seller $j")
            }
        }

        tradeCSVPrinter.printRecord(
            trade.tradeDateTime,
            trade.tradeDirection,
            trade.tradeItemID,
            trade.tradePrice,
            trade.tradeQuantity,
            trade.tradeBuyer,
            trade.tradeSeller,
            trade.tradeComment
        )
        tradeCSVPrinter.flush()
    }

    val exTradeList = lineList.filter { it.take(5) == "EXTRD" }
    val sortedExTradeList =
        exTradeList.sortedByDescending { it.substring(42, 57).toBigDecimal() * it.substring(57, 68).toBigDecimal() }

    val exTradeQty = exTradeList.size
    when (exTradeQty) {
        0 -> {
            println("No Extra Trades")
        }

        else -> {}
    }

    val extrdCSVPrinter = CSVPrinter(
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
        val exTrade = Extrd(sortedExTradeList[i])
//        println(exTrade)
        if (exTrade.tradeVersion != "0001".toLong()) {
            printErr("Invalid ExTrade Version @ extrade $i")
        }
        when (exTrade.tradeDirection) {
            "BUY_" -> {}
            "SELL" -> {}
            else -> {
                printErr("Invalid ExTrade Direction @ extrade $i")
            }
        }
        for (j in 0 until 3) {
            if (!exTrade.tradeItemID[j].isUpperCase()) {
                printErr("Invalid ExTrade Item ID @ extrade $i , item $j")
            }
        }
        for (j in 3 until 12) {
            if (!exTrade.tradeItemID[j].isDigit() && !exTrade.tradeItemID[j].isUpperCase()) {
                printErr("Invalid ExTrade Item ID @ extrade $i , item $j")
            }
        }
        if (exTrade.tradeQuantity <= 0) {
            printErr("Invalid ExTrade Quantity @ extrade $i")
        }
        for (j in 0 until 4) {
            if (!exTrade.tradeBuyer[j].isLetterOrDigit() && exTrade.tradeBuyer[j] != '_') {
                printErr("Invalid ExTrade Buyer @ extrade $i , buyer $j")
            }
            if (!exTrade.tradeSeller[j].isLetterOrDigit() && exTrade.tradeSeller[j] != '_') {
                printErr("Invalid ExTrade Seller @ extrade $i , seller $j")
            }
        }

        extrdCSVPrinter.printRecord(
            exTrade.tradeVersion,
            exTrade.tradeDateTime,
            exTrade.tradeDirection,
            exTrade.tradeItemID,
            exTrade.tradePrice,
            exTrade.tradeQuantity,
            exTrade.tradeBuyer,
            exTrade.tradeSeller,
            exTrade.nestedTags
        )
        extrdCSVPrinter.flush()
    }

    val allTradeCount: Long = tradeQty.toLong() + exTradeQty.toLong()

    val footerList = lineList[lineList.size - 1]

    if (footerList.take(5) != "FOOTR") {
        printErr("Invalid Footer")
        return
    }
    val foot = Footr(footerList)
    if (foot.tradeCount != allTradeCount) {
        printErr("Invalid Footer Trade Count")
        return
    }
    if (head.headerVersion != "0005".toLong() && !foot.tradeCharCount.equals(null)) {
        printErr("Invalid Footer Trade Char Count Exists")
        return
    }

//    println(foot)

    val csvPrinter = CSVPrinter(
        infoWriter, CSVFormat.Builder.create().setHeader(
            "Header Version",
            "File Creation Date and Time",
            "File Comment, Total Number of Trades and ExTrades",
            "Number of Characters in Trade and Extrade Structures"
        ).build()
    )
    csvPrinter.printRecord(
        head.headerVersion,
        head.fileCreation,
        head.fileComment,
        foot.tradeCount,
        foot.tradeCharCount
    )

    csvPrinter.flush()
    csvPrinter.close()
}

data class Headr(val headr: String) {
    var headerVersion: Long = headr.drop(5).take(4).toLong()
    var fileCreation: String = headr.drop(9).take(17)
    var fileComment: String = headr.drop(30).take(headr.length - 30)
}

//open class AnyTrade {
//    var tradeDateTime: String = ""
//    var tradeDirection: String = ""
//    var tradeItemID : String = ""
//    var tradePrice : BigDecimal = BigDecimal.ZERO
//    var tradeQuantity : Int = 0
//    var tradeBuyer : String = ""
//    var tradeSeller : String = ""
//}

data class Trade(val trade: String) {
    var tradeDateTime: String = trade.drop(5).take(17)
    var tradeDirection: String = trade.drop(22).take(1)
    var tradeItemID: String = trade.drop(23).take(12)
    var tradePrice: BigDecimal = trade.drop(35).take(15).toBigDecimal()
    var tradeQuantity: Long = trade.drop(50).take(11).toLong()
    var tradeBuyer: String = trade.drop(61).take(4)
    var tradeSeller: String = trade.drop(65).take(4)
    var tradeComment: String = trade.drop(69).take(32)
}

data class Extrd(val extrd: String) {
    var tradeVersion: Long = extrd.drop(5).take(4).toLong()
    var tradeDateTime: String = extrd.drop(9).take(17)
    var tradeDirection: String = extrd.drop(26).take(4)
    var tradeItemID: String = extrd.drop(30).take(12)
    var tradePrice: BigDecimal = extrd.drop(42).take(15).toBigDecimal()
    var tradeQuantity: Long = extrd.drop(57).take(11).toLong()
    var tradeBuyer: String = extrd.drop(68).take(4)
    var tradeSeller: String = extrd.drop(72).take(4)
    var nestedTags = extrd.drop(76).take(extrd.length - 76)
}

data class Footr(val footr: String) {
    var tradeCount: Long = footr.drop(5).take(10).toLong()
    var tradeCharCount: Long = footr.drop(15).take(10).toLong()
}

fun printErr(errorMsg: String) {
    System.err.println(errorMsg)
}