package net.noerd.prequel


import java.util.Properties
import org.slf4j.LoggerFactory
import java.util.regex.{Matcher, Pattern}
import scala.StringBuilder
import java.text.SimpleDateFormat

/**
 * Created with IntelliJ IDEA.
 * User: GiovanniCaruso
 * Date: 10/26/13
 * Time: 6:40 PM
 */
object SQLLogHandler {

  /* the logger */
  val sqllogger = LoggerFactory.getLogger("SQLLOG")
  /**
   * drives this class to print or not based on the property 'toLog' in file 'prequelous.properties'
   */
  var toLog = true
  /**
   * drives this class to print the time elapsed or not based on the property 'toTime' in file 'prequelous.properties'
   */
  var toTime = true
  /**
   * drives the choice between executable or not formatted sql statement
   */
  var printExecutableSql = true
  /**
   * drives the choice of print or not the resultset row values
   */
  var printRowValues = true

  val loader: ClassLoader = this.getClass.getClassLoader

  /**
   * Search for the file 'prequelous.properties' in the classpath and if, reads it
   */
  CloseUtil.closeAfterUse(loader.getResourceAsStream("prequelous.properties")) {
    in =>
      val props = new Properties(System.getProperties)
      if (in != null) {
        props.load(in)

        initToLog(props)
        initToTime(props)
        initPrintExcecutableSql(props)
        initPrintRowValues(props)
      }
  }

  /**
   * prints this entry in the log file in the format: [SQL] - {params} - {timeElapsed}
   *
   * @param sql the string passed to the statement
   * @param params the string that represents ' - {params} -' in the log
   * @param time the string that represents ' - {timeElapsed}' in the log
   */
  def createLogEntry(sql: String, params: String, time: String) = {
    if(toLog){
      val log = new StringBuilder(sql)
      log.append(params)
      if (toTime)
        log.append(time)
      sqllogger.info(log.toString)
    }
  }

  /**
   * prints this entry in oracle dialect. the string can be executed as is on a SQL terminal
   *
   * @param sql the string passed to the statement
   * @param params the Map of Formattables to pass to the statement
   * @param time the string that represents ' - {timeElapsed}' in the log
   */
  def createLogEntry(sql: String, params: scala.collection.immutable.Map[Int, Formattable], time: String) = {
    if (toLog){
      val log = new StringBuilder(createSqlLogEntry(sql, params))
      if (toTime)
        log.append(time)
      sqllogger.info(log.toString)
    }
  }

  def createRowLog(row: ResultSetRow, sql: String, params: scala.collection.immutable.Map[Int, Formattable]) = {
    if(printRowValues)
      sqllogger.info("Cursor read for sql {" + createSqlLogEntry(sql,params) + "} --> " + row.getRowValues)
  }

  def createRowLog(row: ResultSetRow) = {
    if(printRowValues)
      sqllogger.info("Cursor values: " +  row.getRowValues)
  }

  /**
   * creates the executable string for this log entry
   *
   * @param sqlOuter the original sql string passed to the statement
   * @param parameters the Map of Formattables to pass to the statement
   * @return the executable sql string
   */
  private def createSqlLogEntry(sqlOuter: String, parameters: scala.collection.immutable.Map[Int, Formattable]): String = {
    var sql = sqlOuter
    val s: StringBuffer = new StringBuffer
    if (sql != null) {
      var questionMarkCount: Int = 1

      val p: Pattern = Pattern.compile("\\?")
      val m: Matcher = p.matcher(sql)
      val stringBuffer: StringBuffer = new StringBuffer
      while (m.find) {
        m.appendReplacement(stringBuffer, formatParameter(parameters.get(questionMarkCount)))
        questionMarkCount += 1
      }
      sql = String.valueOf(m.appendTail(stringBuffer))
      s.append(sql).append(";")
    }
    s.toString
  }

  /**
   * format params for execution
   *
   * @param param the Formattable to be escaped
   * @return the plain representation of this Formattable
   */
  def formatParameter(param: Option[Formattable]): String =
    param match {
    case None => "null"
    case Some(p) if p.isInstanceOf[DateTimeFormattable] => "to_timestamp(" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(p.asInstanceOf[DateTimeFormattable].value.toDate) + ", 'yyyy-MM-dd hh24:mi:ss.ff3')"
    case Some(p) => p.escaped(SQLFormatter.DefaultSQLFormatter)
  }

  private def initToLog(props: Properties) {
    val textOption = Option(props.getProperty("prequelous.text"))
    if (textOption.isDefined)
      if (textOption.get.equalsIgnoreCase("false"))
        toLog = false
  }

  private def initToTime(props: Properties) {
    val textOption = Option(props.getProperty("prequelous.time"))
    if (textOption.isDefined)
      if (textOption.get.equalsIgnoreCase("false"))
        toTime = false
  }

  private def initPrintExcecutableSql(props: Properties) {
    val textOption = Option(props.getProperty("prequelous.executable-log-format"))
    if (textOption.isDefined)
      if (textOption.get.equalsIgnoreCase("false"))
        printExecutableSql = false
  }

  private def initPrintRowValues(props: Properties) {
    val textOption = Option(props.getProperty("prequelous.row-print"))
    if (textOption.isDefined)
      if (textOption.get.equalsIgnoreCase("false"))
        printRowValues = false
  }
}
