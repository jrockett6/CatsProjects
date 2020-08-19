package echo

import cats.implicits._
import cats.effect.{ExitCode, IO, IOApp, Sync}
import java.net.ServerSocket
import echo.ServerMethods._


object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    def close[F[_]: Sync](socket: ServerSocket): F[Unit] =
      Sync[F].delay(socket.close()).handleErrorWith(_ => Sync[F].unit)

    for {
      _ <- IO ( new ServerSocket(args.headOption.map(_.toInt).getOrElse(4234)))
            .bracket { socket =>
              serve[IO](socket) >> IO.pure(ExitCode.Success)
            } { socket =>
              close[IO](socket) >> IO(println("socket closed"))
            }
    } yield ExitCode.Success
  }
}