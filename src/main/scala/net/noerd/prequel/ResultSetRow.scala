package net.noerd.prequel

import java.util.Date

import java.sql.{Time, ResultSetMetaData, ResultSet}

import scala.collection.mutable.ArrayBuffer


/**
 * Wraps a ResultSet in a row context. The ResultSetRow gives access
 * to the current row with no possibility to change row. The data of
 * the row can be accessed though the next<Type> methods which return
 * the optional value of the next column, or through its column name.
 */
class ResultSetRow(val rs: ResultSet) {

  private val text = ArrayBuffer[String]()

  lazy val rowValues = "{" + text.mkString(",") + "}"

  /** Maintain the current position. */
  private var position = 0

  def nextBoolean: Option[Boolean] = nextValueOption(rs.getBoolean)

  def nextInt: Option[Int] = nextValueOption(rs.getInt)

  def nextLong: Option[Long] = nextValueOption(rs.getLong)

  def nextFloat: Option[Float] = nextValueOption(rs.getFloat)

  def nextDouble: Option[Double] = nextValueOption(rs.getDouble)

  def nextString: Option[String] = nextValueOption(rs.getString)

  def nextDate: Option[Date] = nextValueOption(rs.getTimestamp)

  def nextObject: Option[AnyRef] = nextValueOption(rs.getObject)

  def nextBinary: Option[Array[Byte]] = nextValueOption(rs.getBytes)

  def nextBlob: Option[java.io.InputStream] = nextValueOption(rs.getBinaryStream)

  def nextClob: Option[java.io.Reader] = nextValueOption(rs.getCharacterStream)

  def nextBigDecimal: Option[ BigDecimal ] = nextValueOption( i => BigDecimal(rs.getBigDecimal) )

  def columnBoolean(columnName: String): Option[ Boolean ] = columnValueOption(columnName, rs.getBoolean )

  def columnInt(columnName: String): Option[ Int ] = columnValueOption(columnName, rs.getInt )

  def columnLong(columnName: String): Option[ Long ] = columnValueOption(columnName, rs.getLong )

  def columnFloat(columnName: String): Option[ Float ] = columnValueOption(columnName, rs.getFloat )

  def columnDouble(columnName: String): Option[ Double ] = columnValueOption(columnName, rs.getDouble )

  def columnString(columnName: String): Option[ String ] = columnValueOption(columnName, rs.getString )

  def columnDate(columnName: String): Option[ Date ] =  columnValueOption(columnName, rs.getTimestamp )

  def columnObject(columnName: String): Option[ AnyRef ] = columnValueOption(columnName, rs.getObject )

  def columnBinary(columnName: String): Option[Array[Byte]] = columnValueOption(columnName, rs.getBytes)

  def columnBlob(columnName: String): Option[java.io.InputStream] = columnValueOption(columnName, rs.getBinaryStream)

  def columnClob(columnName: String): Option[java.io.Reader] = columnValueOption(columnName, rs.getCharacterStream)



  val columnNames: ArrayBuffer[String] = {
    val columnNames = ArrayBuffer.empty[String]
    val metaData = rs.getMetaData
    for (index <- 0.until(metaData.getColumnCount)) {
      columnNames += metaData.getColumnName(index + 1).toLowerCase
    }
    columnNames
  }

  private def incrementPosition = {
    position = position + 1
  }

  private def nextValueOption[T](f: (Int) => T): Option[T] = {
    incrementPosition
    val value = f(position)
    if (rs.wasNull) {
      None
    }else{
      Some(value)
    }
  }

  def columnValueOption[T](columnName:String, f: (String) => T ): Option[ T ] = {
    val value = f(columnName)
    if( rs.wasNull ) None
    else Some( value )
  }

  def getRowValues = {
    val s: StringBuffer = new StringBuffer(" {")
    val md: ResultSetMetaData = rs.getMetaData
    if (md.getColumnCount > 0){
      s.append(columnNames(0) + ": " + rs.getObject(1))
      (2 to md.getColumnCount) foreach { i =>
        s.append(", ").append(columnNames(i - 1) + ": " + rs.getObject(i))
      }
    }
    s.append("}").toString
  }
}

object ResultSetRow {

  def apply(rs: ResultSet): ResultSetRow = {
    new ResultSetRow(rs)
  }

}

case class Column(name: String)

/**
 * Defines a number of implicit conversion methods for the supported ColumnTypes. A call
 * to one of these methods will return the next value of the right type. The methods make
 * it easy to step through a row in order to build an object from it as shown in the example
 * below.
 *
 * Handles all types supported by Prequel as well as Option variants of those.
 *
 * import net.noerd.prequel.ResultSetRowImplicits._
 *
 * case class Person( id: Long, name: String, birthdate: DateTime )
 *
 * InTransaction { tx =>
 * tx.select( "select id, name, birthdate from people" ) { r =>
 * Person( r, r, r )
 * }
 * }
 */
object ResultSetRowImplicits {
    implicit def row2Boolean( row: ResultSetRow ) = BooleanColumnType( row ).nextValue
    implicit def row2Int( row: ResultSetRow ): Int = IntColumnType( row ).nextValue
    implicit def row2Long( row: ResultSetRow ): Long = LongColumnType( row ).nextValue
    implicit def row2Float( row: ResultSetRow ) = FloatColumnType( row ).nextValue
    implicit def row2Double( row: ResultSetRow ) = DoubleColumnType( row ).nextValue
    implicit def row2String( row: ResultSetRow ) = StringColumnType( row ).nextValue
    implicit def row2Date( row: ResultSetRow ) = DateColumnType( row ).nextValue
    implicit def row2DateTime( row: ResultSetRow ) = DateTimeColumnType( row ).nextValue
    implicit def row2Duration( row: ResultSetRow ) = DurationColumnType( row ).nextValue
    implicit def row2Binary( row: ResultSetRow ) = BinaryColumnType( row ).nextValue

    implicit def row2BooleanOption( row: ResultSetRow ) = BooleanColumnType( row ).nextValueOption
    implicit def row2IntOption( row: ResultSetRow ) = IntColumnType( row ).nextValueOption
    implicit def row2LongOption( row: ResultSetRow ) = LongColumnType( row ).nextValueOption
    implicit def row2FloatOption( row: ResultSetRow ) = FloatColumnType( row ).nextValueOption
    implicit def row2DoubleOption( row: ResultSetRow ) = DoubleColumnType( row ).nextValueOption
    implicit def row2StringOption( row: ResultSetRow ) = StringColumnType( row ).nextValueOption
    implicit def row2DateOption( row: ResultSetRow ) = DateColumnType( row ).nextValueOption
    implicit def row2DateTimeOption( row: ResultSetRow ) = DateTimeColumnType( row ).nextValueOption
    implicit def row2DurationOption( row: ResultSetRow ) = DurationColumnType( row ).nextValueOption
    implicit def row2BinaryOption( row: ResultSetRow ) = BinaryColumnType( row ).nextValueOption
}