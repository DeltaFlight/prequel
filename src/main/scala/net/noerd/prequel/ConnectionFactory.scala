package net.noerd.prequel

import javax.sql.DataSource
import javax.naming.InitialContext
import scala.collection.mutable.ArrayBuffer

import org.joda.time.DateTime
import org.joda.time.Duration

import net.noerd.prequel.RichConnection.conn2RichConn
import net.noerd.prequel.ResultSetRowImplicits.row2Long
import net.noerd.prequel.ResultSetRowImplicits.row2Int
import net.noerd.prequel.ResultSetRowImplicits.row2Boolean
import net.noerd.prequel.ResultSetRowImplicits.row2String
import net.noerd.prequel.ResultSetRowImplicits.row2Float
import net.noerd.prequel.ResultSetRowImplicits.row2Double
import net.noerd.prequel.ResultSetRowImplicits.row2DateTime
import net.noerd.prequel.ResultSetRowImplicits.row2Duration
import net.noerd.prequel.ResultSetRowImplicits.row2InputStream
import net.noerd.prequel.ResultSetRowImplicits.row2CharacterInputStream


import org.slf4j.LoggerFactory
import scala.util.Try

final case class Database(val jndiNameOrConnection: Any) {
  val logger = LoggerFactory.getLogger("Database")

  /**
   * Datasource generated connections have to be returned to pool
   */
  def closeable_? = jndiNameOrConnection.isInstanceOf[String]

  val connection = getConnection

  /**
   * Get from datasource or return the passed connection
   *
   * @throws Error if jndiNameOrConnection is a String and doesn't match any named datasource in context
   */
  private def getConnection: java.sql.Connection = jndiNameOrConnection match {

    case jndiName: String => {
      /*val dataSource: Option[DataSource] = {
        try {
          Some(new InitialContext().lookup(jndiName).asInstanceOf[DataSource])
        } catch {
          case th: Throwable =>
            logger.error(th.getMessage, th)
            None
        }
      }
      val conn = dataSource match {
        case ds: Some[DataSource] => ds.get.getConnection
        case None => throw new Error("Invalid jndi name!")
      }
      conn
    }     */
      val otherJndiName =
        if (jndiName.startsWith("jdbc/"))
          "java:comp/env/" + jndiName
        else if (jndiName.startsWith("java:comp/env/"))
          jndiName.substring("java:comp/env/".length + 1)
        else
          jndiName
      val dataSource = Try(new InitialContext().lookup(jndiName).asInstanceOf[DataSource])
      if (dataSource.isSuccess)
        dataSource.get.getConnection
      else {
        val otherDataSource = Try(new InitialContext().lookup(otherJndiName).asInstanceOf[DataSource])
        if (otherDataSource.isSuccess)
          otherDataSource.get.getConnection
        else
          throw new Error("Invalid jndi name: " + jndiName)
      }

    }
    case conn: java.sql.Connection => conn
    case _ => throw new Error("jndiName or java.sql.Connection")
  }

  /**
   * Execute the block in a transaction against the db defined by
   * the configuration.
   *
   * If the block is executed succesfully the transaction
   * will be committed but if an exception is throw it will be rolled back
   * immediately and rethrow the exception.
   *
   * @throws Any Exception that the block may generate.
   * @throws SQLException if the connection could not be committed, rollbacked
   *                      or closed.
   */
  def transaction[T](block: (Transaction) => T) = InTransaction(block, this)

  /**
   * Returns all records returned by the query after being converted by the
   * given block. All objects are kept in memory to this method is no suited
   * for very big result sets. Use selectAndProcess if you need to process
   * bigger datasets.
   *
   * @param sql query that should return records
   * @param params are the optional parameters used in the query
   * @param block is a function converting the row to something else
   */
  def select[T](sql: String, params: Formattable*)(block: ResultSetRow => T): Seq[T] = {
    val results = new ArrayBuffer[T]
    _selectIntoBuffer(Some(results), sql, params.toSeq)(block)
    results
  }

  /**
   * Executes the query and passes each row to the given block. This method
   * does not keep the objects in memory and returns Unit so the row needs to
   * be fully processed in the block.
   *
   * @param sql query that should return records
   * @param params are the optional parameters used in the query
   * @param block is a function fully processing each row
   */
  def selectAndProcess(sql: String, params: Formattable*)(block: ResultSetRow => Unit): Unit = {
    _selectIntoBuffer(None, sql, params.toSeq)(block)
  }

  /**
   * Returns the first record returned by the query after being converted by the
   * given block. If the query does not return anything None is returned.
   *
   * @param sql query that should return records
   * @param params are the optional parameters used in the query
   * @param block is a function converting the row to something else
   */
  def selectHeadOption[T](sql: String, params: Formattable*)(block: ResultSetRow => T): Option[T] = {
    select(sql, params.toSeq: _*)(block).headOption
  }

  /**
   * Return the head record from a query that must be guaranteed to return at least one record.
   * The query may return more records but those will be ignored.
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @param block is a function converting the returned row to something useful.
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectHead[T](sql: String, params: Formattable*)(block: ResultSetRow => T): T = {
    select(sql, params.toSeq: _*)(block).head
  }

  /**
   * Convience method for intepreting the first column of the first record as a long
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a long
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectLong(sql: String, params: Formattable*): Long = {
    selectHead(sql, params.toSeq: _*)(row2Long)
  }

  /**
   * Convience method for intepreting the first column of the first record as a Int
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a Int
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectInt(sql: String, params: Formattable*): Int = {
    selectHead(sql, params.toSeq: _*)(row2Int)
  }

  /**
   * Convience method for intepreting the first column of the first record as a Boolean
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a Boolean
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectBoolean(sql: String, params: Formattable*): Boolean = {
    selectHead(sql, params.toSeq: _*)(row2Boolean)
  }

  /**
   * Convience method for intepreting the first column of the first record as a String
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a String
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectString(sql: String, params: Formattable*): String = {
    selectHead(sql, params.toSeq: _*)(row2String)
  }

  /**
   * Convience method for intepreting the first column of the first record as a Float
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a Float
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectFloat(sql: String, params: Formattable*): Float = {
    selectHead(sql, params.toSeq: _*)(row2Float)
  }

  /**
   * Convience method for intepreting the first column of the first record as a Double
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a Double
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectDouble(sql: String, params: Formattable*): Double = {
    selectHead(sql, params.toSeq: _*)(row2Double)
  }

  /**
   * Convience method for intepreting the first column of the first record as a DateTime
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a DateTime
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectDateTime(sql: String, params: Formattable*): DateTime = {
    selectHead(sql, params.toSeq: _*)(row2DateTime)
  }

  /**
   * Convience method for intepreting the first column of the first record as a Duration
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be intepreted as a Duration
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectDuration(sql: String, params: Formattable*): Duration = {
    selectHead(sql, params.toSeq: _*)(row2Duration)
  }

  /**
   * Convenience method for interpreting the first column of the first record as a LOB
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be interpreted as a java.io.InputStream
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectLOB(sql: String, params: Formattable*): java.io.InputStream = {
    selectHead(sql, params.toSeq: _*)(row2InputStream)
  }

  /**
   * Convenience method for interpreting the first column of the first record as a BLOB
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be interpreted as a java.io.InputStream
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectBlob(sql: String, params: Formattable*): java.io.InputStream = {
    selectHead(sql, params.toSeq: _*)(row2InputStream)
  }

  /**
   * Convenience method for interpreting the first column of the first record as a CLOB
   *
   * @param sql is a query that must return at least one record
   * @param params are the optional parameters of the query
   * @throws RuntimeException if the value is null
   * @throws SQLException if the value in the first column could not be interpreted as a java.io.InputStream
   * @throws NoSuchElementException if the query did not return any records.
   */
  def selectClob(sql: String, params: Formattable*): java.io.Reader = {
    selectHead(sql, params.toSeq: _*)(row2CharacterInputStream)
  }

  private def _selectIntoBuffer[T](
                                    buffer: Option[ArrayBuffer[T]],
                                    sql: String, params: Seq[Formattable])(block: (ResultSetRow) => T): Unit = {
    try {

      val (sql2, params2) = SQLFormatter.DefaultSQLFormatter.formatSeq(sql, params.toSeq)
      connection.usingReusableStatement(sql2, SQLFormatter.DefaultSQLFormatter, false) {
          statement =>
            val rs = statement.selectWith(params2: _*)
            val append = buffer.isDefined
            while (rs.next) {
              val row = ResultSetRow(rs)
              val value = block(row)
              SQLLogHandler.createRowLog(row, sql, statement.paramsForSQLLog)
              if (append) buffer.get.append(value)
            }
        }
    } catch {
      case th: Throwable => {
        logger.error(th.getLocalizedMessage, th)
        throw th
      }
    } finally {
      if ( closeable_? ){
        connection.close
      }
    }
  }

}


object CloseUtil {
  val logger = LoggerFactory.getLogger("CloseUtil")

  /**
   * just ensures that something with a close() method gets closed after it is used
   */
  def closeAfterUse[Closeable <: {def close() : Unit}, B](closeable: Closeable)(block: Closeable => B): B =
    try {
      block(closeable)
    } finally {
      if(closeable != null)
        closeable.close
    }
}
