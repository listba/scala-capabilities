//> using scala 3.8.3
//> using option -language:experimental.captureChecking
//> using option -Ycc-verbose
//> import language.experimental.captureChecking
import java.io.{FileInputStream, InputStream}
import scala.util.Using

def withFile[T](name: String)(op: InputStream^ => T): T =
  Using.resource(new FileInputStream(name)) { f =>
    op(f)
  }


trait Foo{ val x: Int }
def mkFoo(in: InputStream^): Foo = new Foo { val x = 42 }

withFile("data.txt"): in =>
  val foo = mkFoo(in)
  foo.x