package net.noerd.prequel

object TransactionFactory {

  def newTransaction(config: DatabaseConfig): Transaction = {
    Transaction(
      ConnectionPools.getOrCreatePool(config).getConnection,
      config.sqlFormatter
    )
  }

  def newTransaction(conn: java.sql.Connection): Transaction = {
    Transaction(conn, SQLFormatter.DefaultSQLFormatter)
  }
}
