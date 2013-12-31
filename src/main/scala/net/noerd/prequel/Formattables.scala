package net.noerd.prequel

import java.util.{Locale, Date}

import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Wrap your optional value in NullComparable to compare with null if None.
 *
 * Note: The '=' operator is added during formatting so don't include it in your SQL
 */
class NullComparable(val value: Option[Formattable]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    value.map("=" + _.escaped(formatter)).getOrElse("is null")
  }

  override def addTo(statement: ReusableStatement): Unit = {
    sys.error("incompatible with prepared statements")
  }
}

object NullComparable {
  def apply(value: Option[Formattable]) = new NullComparable(value)
}

/**
 * Wrap your optional value in Nullable to have it converted to null if None
 */
class Nullable(val value: Option[Formattable]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    value.map(_.escaped(formatter)).getOrElse("null")
  }

  override def addTo(statement: ReusableStatement): Unit = {
    value.map(statement << _).getOrElse(statement.addNull)
  }
}

object Nullable {
  def apply(value: Option[Formattable]) = new Nullable(value)
}

/**
 * Wrap a parameter string in an Identifier to avoid escaping
 */
class Identifier(val value: String) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    value
  }

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addString(value)
  }
}

object Identifier {
  def apply(value: String) = new Identifier(value)
}

//
// String
//
class StringFormattable(val value: String) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    formatter.toSQLString(value)
  }

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addString(value)
  }
}

object StringFormattable {
  def apply(value: String) = new StringFormattable(value)
}

class StringFormattableOption(val value: Option[String]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    formatter.toSQLString(value.getOrElse(""))
  }

  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(s) =>  statement.addString(s)
      case None => statement.addNull
    }
   
  }
}

object StringFormattableOption {
  def apply(value: Option[String]) = new StringFormattableOption(value)
}

//
// Boolean
// 
class BooleanFormattable(val value: Boolean) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addBoolean(value)
  }
}

object BooleanFormattable {
  def apply(value: Boolean) = new BooleanFormattable(value)
}

class BooleanFormattableOption(val value: Option[Boolean]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  override def addTo(statement: ReusableStatement): Unit = {
   value match {
      case Some(b) =>  statement.addBoolean(b)
      case None => statement.addNull
    }
  }
}

object BooleanFormattableOption {
  def apply(value: Option[Boolean]) = new BooleanFormattableOption(value)
}
//
// Long
//
class LongFormattable(val value: Long) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addLong(value)
  }
}

object LongFormattable {
  def apply(value: Long) = new LongFormattable(value)
}

class LongFormattableOption(val value: Option[Long]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  override def addTo(statement: ReusableStatement): Unit = {
   value match {
      case Some(l) =>  statement.addLong(l)
      case None => statement.addNull
    }
  }
}

object LongFormattableOption {
  def apply(value: Option[Long]) = new LongFormattableOption(value)
}
//
// Int
//
class IntFormattable(val value: Int) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addInt(value)
  }
}

object IntFormattable {
  def apply(value: Int) = new IntFormattable(value)
}

class IntFormattableOption(val value: Option[Int]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(i) =>  statement.addInt(i)
      case None => statement.addNull
    }
  }
}

object IntFormattableOption {
  def apply(value: Option[Int]) = new IntFormattableOption(value)
}
//
// Float
//
class FloatFormattable(val value: Float) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = "%f".formatLocal(Locale.US, value)

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addFloat(value)
  }
}

object FloatFormattable {
  def apply(value: Float) = new FloatFormattable(value)
}

class FloatFormattableOption(val value: Option[Float]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = 
    value match {
    case Some(f) => "%f".formatLocal(Locale.US, f)
    case None => ""
  }

  override def addTo(statement: ReusableStatement): Unit = {
     value match {
      case Some(f) =>  statement.addFloat(f)
      case None => statement.addNull
    }
  }
}

object FloatFormattableOption {
  def apply(value: Option[Float]) = new FloatFormattableOption(value)
}
//
// Double
//
class DoubleFormattable(val value: Double) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = "%f".formatLocal(Locale.US, value)

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addDouble(value)
  }
}

object DoubleFormattable {
  def apply(value: Double) = new DoubleFormattable(value)
}

class DoubleFormattableOption(val value: Option[Double]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = 
 value match {
    case Some(d) => "%f".formatLocal(Locale.US, d)
    case None => ""
  }

  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(d) =>  statement.addDouble(d)
      case None => statement.addNull
    }
  }
}

object DoubleFormattableOption {
  def apply(value: Option[Double]) = new DoubleFormattableOption(value)
}
//
// DateTime
//
class DateTimeFormattable(val value: DateTime)
  extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    formatter.toSQLString(formatter.timeStampFormatter.print(value))
  }

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addDateTime(value)
  }
}

object DateTimeFormattable {
  def apply(value: DateTime) = {
    new DateTimeFormattable(value)
  }

  def apply(value: Date) = {
    new DateTimeFormattable(new DateTime(value))
  }
}

class DateTimeFormattableOption(val value: Option[DateTime])
  extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    value match {
      case Some(datetime) => formatter.toSQLString(formatter.timeStampFormatter.print(datetime))
      case None => ""
    }
    
  }

  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(d) =>  statement.addDateTime(d)
      case None => statement.addNull
    }
  }
}

object DateTimeFormattableOption {
  
  def apply(value: Option[DateTime]) = {
    new DateTimeFormattableOption(value)
  }

  
}

// Option[Date]

class DateFormattableOption(val value: Option[Date])
  extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    value match {
      case Some(datetime) => formatter.toSQLString(formatter.timeStampFormatter.print(datetime.getTime))
      case None => ""
    }
  }

  override def addTo(statement: ReusableStatement): Unit = {
   value match {
      case Some(d) =>  statement.addDateTime(new DateTime(d))
      case None => statement.addNull
    }
  }
}

object DateFormattableOption {  

  def apply(value: Option[Date]) = {
    value match {
      case Some(date) => 
        val dateTime = new DateTime(date)
        new DateTimeFormattableOption(Some(dateTime))
      case None => new DateTimeFormattableOption(None)
    }
        
  
  }
} 

//
// Duration
//
/**
 * Formats an Duration object by converting it to milliseconds.
 */
class DurationFormattable(val value: Duration)
  extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.getMillis.toString

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addLong(value.getMillis)
  }
}

object DurationFormattable {
  def apply(value: Duration) = new DurationFormattable(value)
}

class DurationFormattableOption(val value: Option[Duration])
  extends Formattable {
  override def escaped(formatter: SQLFormatter): String = 
    value match {
    case Some(d) => d.getMillis.toString
    case None => ""
  }
    

  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(d) =>  statement.addLong(d.getMillis)
      case None => statement.addNull
    }
  }
}

object DurationFormattableOption {
  def apply(value: Option[Duration]) = new DurationFormattableOption(value)
}
//
// Binary
//
class BinaryFormattable(val value: Array[Byte]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    formatter.toSQLString(formatter.binaryFormatter.print(value))
  }

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addBinary(value)
  }
}

object BinaryFormattable {
  def apply(value: Array[Byte]) = new BinaryFormattable(value)
}

class BinaryFormattableOption(val value: Option[Array[Byte]]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = {
    value match {
      case Some(a) => formatter.toSQLString(formatter.binaryFormatter.print(a))
      case None => ""
    }
    
  }

  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(a) =>  statement.addBinary(a)
      case None => statement.addNull
    }
  }
}

object BinaryFormattableOption {
  def apply(value: Option[Array[Byte]]) = new BinaryFormattableOption(value)
}
//
// Blob
//
class BlobFormattable(val value: java.io.InputStream) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  /*{
      formatter.toSQLString(formatter.binaryFormatter.print(""+value.available()))
    }*/
  override def addTo(statement: ReusableStatement): Unit = {
    CloseUtil.closeAfterUse(value) {
      value =>
        statement.addBlob(value)
    }
  }
}

object BlobFormattableOption {
  def apply(value: Option[java.io.InputStream]) = new BlobFormattableOption(value)
}

class BlobFormattableOption(val value: Option[java.io.InputStream]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  /*{
      formatter.toSQLString(formatter.binaryFormatter.print(""+value.available()))
    }*/
  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(inputstream) => CloseUtil.closeAfterUse(inputstream) { i =>
        statement.addBlob(i)
      }
      case None => statement.addNull
    }
  }
}

object BlobFormattable {
  def apply(value: java.io.InputStream) = new BlobFormattable(value)
}

//
// Clob
//
class ClobFormattable(val value: java.io.Reader) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  /*{
      formatter.toSQLString(formatter.binaryFormatter.print(""+value.available()))
    }*/
  override def addTo(statement: ReusableStatement): Unit = {
    CloseUtil.closeAfterUse(value) {
      value =>
        statement.addClob(value)
    }
  }
}

object ClobFormattable {
  def apply(value: java.io.Reader) = new ClobFormattable(value)
}

class ClobFormattableOption(val value: Option[java.io.Reader]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = value.toString

  /*{
      formatter.toSQLString(formatter.binaryFormatter.print(""+value.available()))
    }*/
  override def addTo(statement: ReusableStatement): Unit = {
    value match {
      case Some(reader) => CloseUtil.closeAfterUse(reader) { r =>
        statement.addClob(r)
      }
      case None => statement.addNull
    }
    
    
  }
}

object ClobFormattableOption {
  def apply(value: Option[java.io.Reader]) = new ClobFormattableOption(value)
}

//
// BigDecimal
//
class BigDecimalFormattable( val value: BigDecimal )
  extends Formattable {
  override def escaped( formatter: SQLFormatter ): String = {
    formatter.toSQLString( value.toString() )
  }
  override def addTo( statement: ReusableStatement ): Unit = {
    statement.addBigDecimal( value )
  }
}
object BigDecimalFormattable{
  def apply( value: BigDecimal ) = {
    new BigDecimalFormattable( value )
  }
}

class BigDecimalFormattableOption( val value: Option[BigDecimal] )
  extends Formattable {
  override def escaped( formatter: SQLFormatter ): String = {
    formatter.toSQLString( value.toString() )
  }
  override def addTo( statement: ReusableStatement ): Unit = value match {
    case Some(bigDecimal) => statement.addBigDecimal( bigDecimal )
    case None => statement.addNull()
  }
}
object BigDecimalFormattableOption{
  def apply( value: Option[BigDecimal] ) = {
    new BigDecimalFormattableOption( value )
  }
}

/*class ListFormattable[A](val list: List[A]) extends Formattable {
  override def escaped(formatter: SQLFormatter): String = list map {
    case s: String => formatter.toSQLString(s)   
    case a: Any         => a

  } mkString(",")

  override def addTo(statement: ReusableStatement): Unit = {
    statement.addString(list map {
      case s: String => SQLFormatter().toSQLString(s)
      case a: Any    => a

    } mkString(","))
    
  }
  
  //def value = list.head
}
object ListFormattable {
  def apply[A](list: List[A]) = new ListFormattable(list)
}*/
