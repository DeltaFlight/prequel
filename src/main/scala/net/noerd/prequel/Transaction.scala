package net.noerd.prequel

import java.sql.Connection


import net.noerd.prequel.RichConnection.conn2RichConn

/**
 * A Transaction is normally created by the InTransaction object and can be
 * used to execute one or more queries against the database. Once the block
 * passed to InTransaction is succesfully executed the transaction is auto-
 * matically committed. And if some exception is throws during execution the 
 * transaction is rollbacked. 
 *
 * @throws SQLException all methods executing queries will throw SQLException 
 *                      if the query was not properly formatted or something went wrong in
 *                      the database during execution.
 *
 * @throws IllegalFormatException: Will be throw by all method if the format 
 *                               string is invalid or if there is not enough parameters.
 */
class Transaction(val connection: Connection, val formatter: SQLFormatter) {


  /**
   * Executes the given query and returns the number of affected records
   *
   * @param sql query that must not return any records
   * @param params are the optional parameters used in the query
   * @return the number of affected records
   */
  def execute(sql: String, params: Formattable*): Int = {
    /*
    connection.usingStatement { statement =>
        statement.executeUpdate( formatter.formatSeq( sql, params.toSeq ) )
    }
    */
    val (sql2, params2) = formatter.formatSeq(sql, params.toSeq)
    connection.usingReusableStatement(sql2, formatter, false) {
      statement =>
        statement.executeWith(params2.toSeq: _*)
    }
  }

  /**
   * Will pass a ReusableStatement to the given block. This block
   * may add parameters to the statement and execute it multiple times.
   * The statement will be automatically closed onced the block returns.
   *
   * Example:
   * tx.executeBatch( "insert into foo values(?)" ) { statement =>
   * items.foreach { statement.executeWith( _ ) }
   * }
   *
   * @return the result of the block
   * @throws SQLException if the query is missing parameters when executed
   *                      or if they are of the wrong type.
   */
  def executeBatch[T](sql: String, generateKeys: Boolean = false)(block: (ReusableStatement) => T): T = {
    connection.usingReusableStatement(sql, formatter, generateKeys)(block)
  }

  /**
   * Rollbacks the Transaction.
   *
   * @throws SQLException if transaction could not be rollbacked
   */
  def rollback(): Unit = connection.rollback()

  /**
   * Commits all changed done in the Transaction.
   *
   * @throws SQLException if transaction could not be committed.
   */
  def commit(): Unit = connection.commit()

  /**
   * Verify if connection is still alive
   */
  def active_? = !connection.isClosed

}

object Transaction {

  def apply(conn: Connection, formatter: SQLFormatter) = new Transaction(conn, formatter)
}