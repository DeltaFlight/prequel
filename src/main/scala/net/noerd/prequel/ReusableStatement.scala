package net.noerd.prequel

import java.sql._

import org.joda.time.DateTime
import scala.Array
import scala.collection.mutable.ArrayBuffer

/**
 * Wrapper around PreparedStatement making is easier to add parameters.
 *
 * The ReusableStatement can be used in two ways.
 *
 * ## Add parameters and then execute as a chain
 * statement << param1 << param2 << param3 <<!
 *
 * ## Set parameters and execute in on shot
 * statement.executeWith( param1, param2, param3 )
 */
class ReusableStatement(val wrapped: PreparedStatement, formatter: SQLFormatter) {
  private val StartIndex = 1
  private var parameterIndex = StartIndex


  /**
   * Parameters to be logged
   */
  private val params = ArrayBuffer[String]()


  val params2 = ArrayBuffer[Formattable]()
  lazy val paramsForSQLLog = params2.toList.zipWithIndex.map{ case (f: Formattable, idx: Int) => (idx + 1, f)}.toMap


  /**
   * The string representing the params to be logged
   * @return The escaped params collection
   */
  lazy val paramsToLog = if (params.size == 0) "" else " - params: {" + params.mkString(" , ") + "}"

  var timeElapsed = ""

  def time[R](f: => R): (R, Long) = {
    timeElapsed = ""
    val t0 = System.nanoTime
    val r = f
    val t1 = System.nanoTime
    timeElapsed = " - { time: " + ((t1 - t0) / 1000000) + " ms }"
    (r, t1 - t0)
  }

  /**
   * Adds the param to the query and returns this so that it
   * possible to chain several calls together
   * @return self to allow for chaining calls
   */
  def <<(param: Formattable): ReusableStatement = {
    params2 += param
    param.addTo(this)
    this
  }

  /**
   * Alias of execute() included to look good with the <<
   * @return the number of affected records
   */
  def <<!(): Int = execute()

  /**
   * Executes the statement with the previously set parameters
   * @return the number of affected records
   */
  def execute(): Int = {
    parameterIndex = StartIndex
    time {
      wrapped.executeUpdate()
    }._1
  }

  /**
   * Executes the query statement with the previously set parameters
   * @return the ResultSet of the query
   */
  def select(): ResultSet = {
    parameterIndex = StartIndex
    time {
      wrapped.executeQuery()
    }._1
  }

  /**
   * Sets all parameters and executes the statement
   * @return the number of affected records
   */
  def executeWith(params: Formattable*): Int = {
    params.foreach(this << _)
    execute
  }

  /**
   * Sets all parameters and executes the query statement
   * @return the ResultSet of the query
   */
  def selectWith(params: Formattable*): ResultSet = {
    params.foreach(this << _)
    select
  }

  /**
   * Add a String to the current parameter index
   */
  def addString(value: String) = {
    params += formatter.escapeString(value)
    addValue(() =>
      wrapped.setString(parameterIndex, formatter.escapeString(value))
    )
  }

  /**
   * Add a Date to the current parameter index. This is done by setTimestamp which
   * looses the Timezone information of the DateTime
   */
  def addDateTime(value: DateTime): Unit = {
    params += new Timestamp(value.getMillis).toString
    addValue(() =>
      wrapped.setTimestamp(parameterIndex, new Timestamp(value.getMillis))
    )
  }

  /**
   * Add a Date to the current parameter index. This is done by setTimestamp which
   * looses the Timezone information of the DateTime
   */
  def addDate(value: Date): Unit = {
    params += new Timestamp(value.getTime).toString
    addValue(() =>
      wrapped.setDate(parameterIndex, value)
    )
  }

  /**
   * Add Blob (stream of bytes) to the current parameter index
   */
  def addBlob(value: java.io.InputStream): Unit = {
    params += value.toString
    addValue(() => wrapped.setBinaryStream(parameterIndex, value))
  }

  /**
   * Add Clob (stream of characters) to the current parameter index
   */
  def addClob(value: java.io.Reader): Unit = {
    params += value.toString
    addValue(() => wrapped.setCharacterStream(parameterIndex, value))
  }

  /**
   * Add Binary (array of bytes) to the current parameter index
   */
  def addBinary(value: Array[Byte]): Unit = {
    params += value.toString
    addValue(() => wrapped.setBytes(parameterIndex, value))
  }

  /**
   * Add a Boolean to the current parameter index
   */
  def addBoolean(value: Boolean): Unit = {
    params += value.toString
    addValue(() => wrapped.setBoolean(parameterIndex, value))
  }

  /**
   * Add a Long to the current parameter index
   */
  def addLong(value: Long): Unit = {
    params += value.toString
    addValue(() => wrapped.setLong(parameterIndex, value))
  }

  /**
   * Add a Int to the current parameter index
   */
  def addInt(value: Int): Unit = {
    params += value.toString
    addValue(() => wrapped.setInt(parameterIndex, value))
  }

  /**
   * Add a Float to the current parameter index
   */
  def addFloat(value: Float): Unit = {
    params += value.toString
    addValue(() => wrapped.setFloat(parameterIndex, value))
  }

  /**
   * Add a Double to the current parameter index
   */
  def addDouble(value: Double): Unit = {
    params += value.toString
    addValue(() => wrapped.setDouble(parameterIndex, value))
  }

  /**
   * Add a BigDecimal to the current parameter index
   */
  def addBigDecimal( value: BigDecimal ): Unit = {
    params += value.toString
    addValue(() => wrapped.setBigDecimal(parameterIndex, value.bigDecimal))
  }

  /**
   * Add Null to the current parameter index
   */
  def addNull(): Unit = {
    params += "null"
    addValue(() => wrapped.setNull(parameterIndex, Types.NULL))
  }


  private def addValue(f: () => Unit) = {
    f.apply
    parameterIndex = parameterIndex + 1
  }

  private[prequel] def close() = wrapped.close()
}