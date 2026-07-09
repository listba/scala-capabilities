//> using scala 3.8.3
import java.io.{FileInputStream, InputStream}
import scala.util.Using

def withFile[T](name: String)(op: InputStream => T): T =
  Using.resource(new FileInputStream(name)) { f =>
    op(f)
  }

val f = withFile("data.txt") { identity }
f.available() // boom
