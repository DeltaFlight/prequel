package net.noerd.prequel

import java.util.Date

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import net.noerd.prequel.SQLFormatterImplicits._
import java.sql.Timestamp

class ResultSetRowSpec extends FunSpec with ShouldMatchers with BeforeAndAfterEach {

  val database = TestDatabase.config

  describe("ResultSetRow") {

    it("should return the column names of the row") {
      database.transaction {
        tx =>
          val expectedColumns = Seq("id", "name")
          tx.execute("create table resultsetrowspec(id int, name varchar(265))")
          tx.execute("insert into resultsetrowspec values(?, ?)", 242, "test1")
          Database(tx.connection).select("select id, name from resultsetrowspec") {
            row =>
              row.columnNames should equal(expectedColumns)
              row.nextString
          }
      }
    }

    it("should return a Boolean") {
      database.transaction {
        tx =>
          val value1 = Some(false)
          val value2 = None
          tx.execute("create table boolean_table(c1 boolean, c2 boolean)")
          tx.execute("insert into boolean_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from boolean_table") {
            row =>
              row.nextBoolean should equal(value1)
              row.nextBoolean should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from boolean_table") {
            row =>
              row.columnBoolean("c1") should equal(value1)
              row.columnBoolean("c2") should equal(value2)
          }
      }
    }

    it("should return a Long") {
      database.transaction {
        tx =>
          val value1 = Some(12345L)
          val value2 = None
          tx.execute("create table long_table(c1 bigint, c2 bigint)")
          tx.execute("insert into long_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from long_table") {
            row =>
              row.nextLong should equal(value1)
              row.nextLong should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from long_table") {
            row =>
              row.columnLong("c1") should equal(value1)
              row.columnLong("c2") should equal(value2)
          }
      }
    }

    it("should return an Int") {
      database.transaction {
        tx =>
          val value1 = Some(12345)
          val value2 = None
          tx.execute("create table int_table(c1 int, c2 int)")
          tx.execute("insert into int_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from int_table") {
            row =>
              row.nextInt should equal(value1)
              row.nextInt should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from int_table") {
            row =>
              row.columnInt("c1") should equal(value1)
              row.columnInt("c2") should equal(value2)
          }
      }
    }

    it("should return a String") {
      database.transaction {
        tx =>
          val value1 = Some("test")
          val value2 = None
          tx.execute("create table string_table(c1 varchar(256), c2 varchar(16))")
          tx.execute("insert into string_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from string_table") {
            row =>
              row.nextString should equal(value1)
              row.nextString should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from string_table") {
            row =>
              row.columnString("c1") should equal(value1)
              row.columnString("c2") should equal(value2)
          }
      }
    }

    it("should return a Date") {
      database.transaction {
        tx =>
          val value1 = Some(new Date)
          val value2 = None
          tx.execute("create table date_table(c1 timestamp, c2 timestamp)")
          tx.execute("insert into date_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from date_table") {
            row =>
              row.nextDate.get.getTime should equal(value1.get.getTime)
              row.nextDate should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from date_table") {
            row =>
              row.columnDate("c1").get.getTime should equal(value1.get.getTime)
              row.columnDate("c2") should equal(value2)
          }
      }
    }

    it("should return a Float") {
      database.transaction {
        tx =>
          val value1 = Some(1.5f)
          val value2 = None
          tx.execute("create table float_table(c1 real, c2 real)")
          tx.execute("insert into float_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from float_table") {
            row =>
              row.nextFloat should equal(value1)
              row.nextFloat should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from float_table") {
            row =>
              row.columnFloat("c1") should equal(value1)
              row.columnFloat("c2") should equal(value2)
          }
      }
    }

    it("should return a Double") {
      database.transaction {
        tx =>
          val value1 = Some(3274832748932743.45)
          val value2 = None
          tx.execute("create table double_table(c1 real, c2 real)")
          tx.execute("insert into double_table values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from double_table") {
            row =>
              row.nextDouble should equal(value1)
              row.nextDouble should equal(value2)
          }
          Database(tx.connection).select("select c1, c2 from double_table") {
            row =>
              row.columnDouble("c1") should equal(value1)
              row.columnDouble("c2") should equal(value2)
          }
      }
    }

    it("should return a Double (2) - test for identifier") {
      database.transaction {
        tx =>
          val value1 = Some(3274832748932743.45)
          val value2 = None
          tx.execute("create table ??(c1 real, c2 real)",Identifier("double_table2"))
          tx.execute("insert into double_table2 values(?, null)", value1.get)
          Database(tx.connection).select("select c1, c2 from ??", Identifier("double_table2")) {
            row =>
              row.nextDouble should equal(value1)
              row.nextDouble should equal(value2)
          }
      }
    }
  }
}
