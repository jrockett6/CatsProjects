package copy

import cats.implicits._
import cats.effect.{IO, Resource}
import cats.effect.concurrent.Semaphore
import java.io.{File, FileInputStream, FileOutputStream}

object CopyMethods {

  def inputStream(f: File, guard: Semaphore[IO]): Resource[IO, FileInputStream] =
    Resource.make {
      IO(new FileInputStream(f))
    } { inStream =>
      guard.withPermit {
        IO(inStream.close()).handleErrorWith(_ => IO.unit)
      }
    }

  def outputStream(f: File, guard: Semaphore[IO]): Resource[IO, FileOutputStream] =
    Resource.make {
      IO(new FileOutputStream(f))
    } { outStream =>
      guard.withPermit {
        IO(outStream.close()).handleErrorWith(_ => IO.unit)
      }
    }

  def inputOutputStream(in: File, out: File, guard: Semaphore[IO]): Resource[IO, (FileInputStream, FileOutputStream)] =
    for {
      x <- inputStream(in, guard)
      y <- outputStream(out, guard)
    } yield (x, y)

  def transmit(in: FileInputStream, out: FileOutputStream, buffer: Array[Byte], acc: Long): IO[Long] =
    for {
      amount <- IO(in.read(buffer, 0, buffer.length))
      count <-
        if (amount > -1)
          IO(out.write(buffer, 0, amount)) >> transmit(in, out, buffer, acc + amount)
        else
          IO.pure(acc)
    } yield count

  def transfer(origin: FileInputStream, destination: FileOutputStream): IO[Long] =
    for {
      buffer <- IO(new Array[Byte](1024 * 10))
      total <- transmit(origin, destination, buffer, 0L)
    } yield total
}