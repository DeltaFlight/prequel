package net.noerd.prequel

import java.util.Date

import org.apache.commons.lang.StringEscapeUtils.escapeSql

import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import scala.util.matching.Regex.Match
import java.util.regex.Matcher

/**
 * Currently a private class responsible for formatting SQL used in 
 * transactions (@see Transaction). It does properly format standards 
 * classes like DateTime, Floats, Longs and Integers as well as some 
 * SQL specific classes like Nullable, NullComparable and Identifier. 
 * See their documentation for more info on how to use them.
 */
class SQLFormatter(
                    val timeStampFormatter: DateTimeFormatter,
                    val binaryFormatter: BinaryFormatter
                    ) {
  private val sqlQuote = "'"

  def format(sql: String, params: Formattable*): (String, Seq[Formattable]) = formatSeq(sql, params.toSeq)

  val PlaceholderPattern = """\?\?""".r

  /**
   * Formats the provided statement by escaping all parameter values and replacing every pre statement placeholder (represented by double question marks '??') with the corresponding parameter value.
   * @param sql SQL statement with optional pre statement placeholders (represented by '??')
   * @param params Sequence of parameters. All pre statement placeholders will be replaced by the corresponding parameter value.
   * @return A pair consisting of the formatted statement and any parameters not already used for pre statement placeholders.
   */
  def formatSeq(sql: String, params: Seq[Formattable]): (String, Seq[Formattable]) = {
    var i = 0
    val mapper = (m: Match) => {
      if (i < params.length) {
        val replacement = Some(Matcher.quoteReplacement(params.apply(i).escaped(this)))
        i = i + 1
        replacement
      } else None
    }
    (PlaceholderPattern replaceSomeIn(sql, mapper), params.drop(i))
  }

  /**
   * Escapes  "'" and "\" in the string for use in a sql query
   */
  def escapeString(str: String): String = escapeSql(str).replace("\\", "\\\\")

  /**
   * Quotes the passed string according to the formatter
   */
  def quoteString(str: String): String = {
    val sb = new StringBuilder
    sb.append(sqlQuote).append(str).append(sqlQuote)
    sb.toString
  }

  /**
   * Escapes and quotes the given string
   */
  def toSQLString(str: String): String = quoteString(escapeString(str))
}

object SQLFormatter {
  /**
   * SQLFormatter for dbs supporting ISODateTimeFormat
   */
  val DefaultSQLFormatter = SQLFormatter()
  /**
   * SQLFormatter for usage with HSQLDB.
   */
  val HSQLDBSQLFormatter = SQLFormatter(
    DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSS")
  )

  private[prequel] def apply(
                              timeStampFormatter: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis,
                              binaryFormatter: BinaryFormatter = new BinaryFormatter()
                              ) = {
    new SQLFormatter(timeStampFormatter, binaryFormatter)
  }
}

class BinaryFormatter {
  /**
   * Print to hexadecimal format
   */
  def print(value: Array[Byte]): String = value.map("%02X" format _).mkString
}

object SQLFormatterImplicits {
  implicit def string2Formattable(wrapped: String) = StringFormattable(wrapped)
  implicit def string2FormattableOption(wrapped: Option[String]) = StringFormattableOption(wrapped)

  implicit def boolean2Formattable(wrapped: Boolean) = BooleanFormattable(wrapped)
  implicit def boolean2FormattableOption(wrapped: Option[Boolean]) = BooleanFormattableOption(wrapped)

  implicit def long2Formattable(wrapped: Long) = LongFormattable(wrapped)
  implicit def long2FormattableOption(wrapped: Option[Long]) = LongFormattableOption(wrapped)

  implicit def int2Formattable(wrapped: Int) = IntFormattable(wrapped)
  implicit def int2FormattableOption(wrapped: Option[Int]) = IntFormattableOption(wrapped)

  implicit def float2Formattable(wrapped: Float) = FloatFormattable(wrapped)
  implicit def float2FormattableOption(wrapped: Option[Float]) = FloatFormattableOption(wrapped)

  implicit def double2Formattable(wrapped: Double) = DoubleFormattable(wrapped)
  implicit def double2FormattableOption(wrapped: Option[Double]) = DoubleFormattableOption(wrapped)

  implicit def dateTime2Formattable(wrapped: DateTime) = DateTimeFormattable(wrapped)
  implicit def dateTime2FormattableOption(wrapped: Option[DateTime]) = DateTimeFormattableOption(wrapped)

  //implicit def date2Formattable(wrapped: Date) = DateFormattable(wrapped)
  implicit def date2Formattable(wrapped: Date) = DateTimeFormattable(wrapped)
  implicit def date2FormattableOption(wrapped: Option[Date]) = DateFormattableOption(wrapped)

  implicit def duration2Formattable(wrapped: Duration) = new DurationFormattable(wrapped)
  implicit def duration2FormattableOption(wrapped: Option[Duration]) = new DurationFormattableOption(wrapped)

  implicit def binary2Formattable(wrapped: Array[Byte]) = new BinaryFormattable(wrapped)
  implicit def binary2FormattableOption(wrapped: Option[Array[Byte]]) = new BinaryFormattableOption(wrapped)

  implicit def blob2Formattable[A](wrapped: java.io.InputStream) = new BlobFormattable(wrapped)
  implicit def blob2FormattableOption[A](wrapped: Option[java.io.InputStream]) = new BlobFormattableOption(wrapped)
  
  implicit def clob2Formattable[A](wrapped: java.io.Reader) = new ClobFormattable(wrapped)
  implicit def clob2FormattableOption[A](wrapped: Option[java.io.Reader]) = new ClobFormattableOption(wrapped)

  implicit def bigDecimalFormattable( wrapped: BigDecimal ) = new BigDecimalFormattable( wrapped )
  implicit def bigDecimalFormattableOption( wrapped: BigDecimal ) = new BigDecimalFormattable( wrapped )

  implicit def null2Formattable[A](wrapped: Option[Formattable]) = new Nullable(wrapped)
    
}