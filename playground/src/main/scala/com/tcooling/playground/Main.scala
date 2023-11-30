package com.tcooling.playground

import cats.effect._
import cats.syntax.all._
import java.io._

/**
 * Current progress: https://typelevel.org/cats-effect/docs/tutorial#dealing-with-cancelation
 */
object Main extends IOApp {

  /**
   * Goal: make method to copy contents of one file to another file Invoking method will not copy anything, instead
   * return an IO instance that encapsulates all side effects involved (opening/closing the file, reading/writing
   * content), to keep purity. Only when IO instance is evaluated is when the side-effectful actions will be run. No
   * exception is raised outside the IO (do not need to wrap in a try), errors embedded in IO instance. If error, IO
   * evaluation will fail and IO instance will carry the error raised.
   */

  def copyExample(origin: File, destination: File): IO[Long] = ???

  /**
   * Consider opening a stream to be a side-effectful action. Use Resource to create, use and release a resource.
   */

  def inputStream(f: File): Resource[IO, FileInputStream] =
    Resource.make {
      IO.blocking(new FileInputStream(f)) // build
    } { inStream =>
      IO.blocking(inStream.close()).handleErrorWith(_ => IO.unit) // release
    }

  def outputStream(f: File): Resource[IO, FileOutputStream] =
    Resource.make {
      IO.blocking(new FileOutputStream(f)) // build
    } { outStream =>
      IO.blocking(outStream.close()).handleErrorWith(_ => IO.unit) // release
    }

  /**
   * Can combine Resource's in for comprehensions. Can use .handleErrorWith for errors, or if want to ignore, common to
   * use .attempt.void
   */

  def inputOutputStreams(in: File, out: File): Resource[IO, (InputStream, OutputStream)] =
    for {
      inStream  <- inputStream(in)
      outStream <- outputStream(out)
    } yield inStream -> outStream

  /**
   * If something implements java.lang.AutoCloseable, can use Resource.fromAutoCloseable. Useful, but may want to log
   * something in close part.
   */

  def inputStreamAutoClose(f: File): Resource[IO, FileInputStream] =
    Resource.fromAutoCloseable(IO(new FileInputStream(f)))

  // transfer will do the real work
  def transferInitial(origin: InputStream, destination: OutputStream): IO[Long] = ???

  /**
   * transfer method will handle the logic for transferring. Even if transfer fails, both streams will be closed.
   */
  def copy(origin: File, destination: File): IO[Long] =
    inputOutputStreams(origin, destination).use {
      case (in, out) => transferInitial(in, out)
    }

  /**
   * What about bracket? Bracket within cats-effect is similar to Resource. Resource is based on Bracket. Bracket has 3
   * stages: resource acquisition/usage/release. Each stage defined by an IO instance. Fundamental property is that
   * release stage will always run, regardless of if usage stage failed. In the case of the copy method, acquire would
   * create the streams, usage will copy, then release will close the streams. Bracket is similar to try {} catch {}
   * finally {}. Resource is released if exception thrown. If exception is raised, bracket will re-raise the exception
   * after the resource is closed.
   */
  def copyUsingBracket(origin: File, destination: File): IO[Long] = {
    val inIO: IO[InputStream]   = IO(new FileInputStream(origin))
    val outIO: IO[OutputStream] = IO(new FileOutputStream(destination))

    // Go from (IO[InputStream], IO[OutputStream]) to IO[(InputStream, OutputStream)]
    val inputAndOutputStream: IO[(InputStream, OutputStream)] = (inIO, outIO).tupled

    // Call .bracket on an IO of your resource, in this case, the streams
    inputAndOutputStream.bracket { (inputStream, outputStream) =>
      transfer(inputStream, outputStream) // use
    } { (inputStream, outputStream) =>
      (IO(inputStream.close()), IO(outputStream.close())).tupled.void
        .handleErrorWith(_ => IO.unit) // close
    }
  }

  // In this case the resulting IO will raise error foo, while the bar error gets reported on a side-channel.
  // TODO: what does a side channel mean?
  val foo = new RuntimeException("Foo")
  val bar = new RuntimeException("Bar")
  val errorExample: IO[Int] = IO("resource").bracket { _ =>
    IO.raiseError(foo) // use
  } { _ =>
    IO.raiseError(bar) // release
  }

  /**
   * But there is a catch in the code above. When using bracket, if there is a problem when getting resources in the
   * first stage, then the release stage will not be run. E.g. if in the code above, the origin file is opened, then an
   * exception is thrown when opening the destination file, the origin stream will not be closed. To solve this, can use
   * separate bracket calls for each stream, nesting them like a flatMap. Resource is better for this kind of thing.
   */

  /**
   * For the transfer method, we need a loop that on each iteration, reads data from the input stream into a buffer and
   * then writes the buffer contents to an output stream. At the same time, the loop will keep a counter of the bytes
   * transferred. To reuse the same buffer, we define it outside of the main loop.
   *
   * Note: >> is similar to *> but lazy, so can be used for recursion. >> is same as first.flatMap(_ => second)
   */
  def transmit(origin: InputStream, destination: OutputStream, buffer: Array[Byte], acc: Long): IO[Long] = for {
    amount <- IO.blocking(
      origin.read(buffer, 0, buffer.length)
    ) // origin.read returns number of bytes read or -1 if no data
    count <-
      if (amount > -1)
        IO.blocking(destination.write(buffer, 0, amount)) >> transmit(origin, destination, buffer, acc + amount)
      else IO.pure(acc) // End of read stream reached (by java.io.InputStream contract), nothing to write
  } yield count         // Returns the actual amount of bytes transmitted

  /**
   * Note: assuming the InputStream above, when recursion happens, the InputStream is now at the latest position, e.g.
   * the bytes read are now "gone" and we are at the latest position.
   *
   * Could have used IO(action), but for input/output, using IO.blocking(action) is better.
   *
   * IO is a monad, hence we can sequence it in a for comprehension. IO is also stack safe, so not concerned with stack
   * overflow.
   */
  def transfer(origin: InputStream, destination: OutputStream): IO[Long] =
    transmit(origin, destination, new Array[Byte](1024 * 10), 0L)

  /**
   * IO instances execution can be cancelled (key feature of cats-effect)
   */

  override def run(args: List[String]): IO[ExitCode] = for {
    err <- errorExample
  } yield ExitCode.Success

}
