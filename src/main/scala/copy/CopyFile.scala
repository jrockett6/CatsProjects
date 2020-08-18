package copy

import cats.implicits._
import cats.effect.{IO, ContextShift, IOApp}
import cats.effect.concurrent.Semaphore
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.File

import copy.CopyMethods._

object CopyFile extends App {
  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def copy(in: File, out: File): IO[Long] =
    for {
      guard <- Semaphore[IO](1)
      copy <- inputOutputStream(in, out, guard).use { case (i, o) =>
        guard.withPermit {
          transfer(i, o)
        } }
    } yield copy

  val inFile = new File("hello.txt")
  val outFile = new File("out.txt")

  val program: IO[Long] = copy(inFile, outFile)

  val res = program.attempt.unsafeRunSync().swap.getOrElse(IO(println("Success")).unsafeRunSync())
}