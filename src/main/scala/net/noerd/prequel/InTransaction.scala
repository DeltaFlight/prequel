package net.noerd.prequel


import org.slf4j.LoggerFactory

/**
 * InTransaction is a factory object for Transaction instances. Given the
 * DatabaseConfig an existing pool is used or a new one is created that will
 * be reused for the same configuration the next time it's used.
 */
object InTransaction {

  val logger = LoggerFactory.getLogger("InTransaction")

  /**
   * Given a block and a DatabaseConfig a Transaction will be created and
   * passed to the block. If the block is executed succesfully the transaction
   * will be committed but if an exception is throw it will be rollbacked
   * immediately and rethrow the exception.
   *
   * @throws Any Exception that the block may generate.
   * @throws SQLException if the connection could not be committed, rollbacked
   *                      or closed.
   */
  def apply[T](block: (Transaction) => T, config: DatabaseConfig): T = {
    val transaction = TransactionFactory.newTransaction(config)

    try {
      val value = block(transaction)
      transaction.commit()
      value
    } catch {
      case th: Throwable => {
        transaction.rollback()
        throw th
      }
    } finally {
      transaction.connection.close()
    }
  }

  /**
   * Given a block and a Database a Transaction will be created and
   * passed to the block. If the block is executed succesfully the transaction
   * will be committed but if an exception is throw it will be rollbacked
   * immediately and rethrow the exception.
   *
   * The transaction starts with autocommit -> false and when finished do autocommit -> true
   * If the underline connection is datasource generated (database.closeable_? == true) this connection is closed
   *
   * @throws Any Exception that the block may generate.
   * @throws SQLException if the connection could not be committed, rollbacked
   *                      or closed.
   */
  def apply[T](block: (Transaction) => T, database: Database): T = {
    val transaction = Transaction(database.connection, SQLFormatter.DefaultSQLFormatter)
    database.connection.setAutoCommit(false)
    try {
      val value = block(transaction)
      transaction.commit()
      value
    } catch {
      case th: Throwable => {
        logger.error(th.getMessage, th)
        if (transaction.active_?)
          transaction.rollback()
        throw th
      }
    } finally {
      database.connection.setAutoCommit(true)
      if (database.closeable_? && transaction.active_?) database.connection.close
    }

  }

  /**
   * Given a block and a Connection a Transaction will be created and
   * passed to the block. If the block is executed succesfully the transaction
   * will be committed but if an exception is throw it will be rollbacked
   * immediately and rethrow the exception.
   *
   * @throws Any Exception that the block may generate.
   * @throws SQLException if the connection could not be committed, rollbacked
   *                      or closed.
   */
  def apply[T](block: (Transaction) => T, conn: java.sql.Connection): T = {
    val transaction = Transaction(conn, SQLFormatter.DefaultSQLFormatter)
    transaction.connection.setAutoCommit(false)
    try {
      val value = block(transaction)
      transaction.commit()
      value
    } catch {
      case th: Throwable => {
        transaction.rollback()
        throw th
      }
    } finally {
      transaction.connection.setAutoCommit(true)

    }
  }
}