package copy

import cats.implicits._
import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.effect.concurrent.Semaphore
//import scala.concurrent.ExecutionContext.Implicits.global
import java.io.File
import copy.CopyMethods._

object CopyFile extends IOApp {
//  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def copy(in: File, out: File): IO[Long] =
    for {
      guard <- Semaphore[IO](1)
      copy <- inputOutputStream(in, out, guard).use { case (i, o) =>
        guard.withPermit {
          transfer(i, o)
        } }
    } yield copy

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- if(args.length < 2) IO.raiseError(new IllegalArgumentException("need input and output filenames"))
          else IO.unit
      iFile = new File(args(0))
      oFile = new File(args(1))
      _ <- copy(iFile, oFile)
      _ <- IO(println("File copied successfully!"))
    } yield ExitCode.Success
}