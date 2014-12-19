package test

import java.util.concurrent.TimeUnit._

import com.kolor.docker.api.entities.{DockerAuth, RepTWithNS, PublicRepositoryTag$}
import com.kolor.docker.api.{privateRegistry$, Docker, DockerClient}
import com.kolor.docker.dsl._
import com.kolor.docker.dsl.Dockerfile
import org.slf4j.LoggerFactory
import org.specs2.execute.AsResult
import org.specs2.specification.{AroundOutside, Scope}
import play.api.libs.iteratee.Iteratee

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by tldeti on 14-12-12.
 */
object PrivateRegistryTestUtil {

  val log = LoggerFactory.getLogger(getClass())
  class PrivateRegistryContext extends Scope {
    implicit val docker = Docker("localhost")
    implicit lazy val registry = privateRegistry("192.168.0.120", 5000)
    implicit val timeout = 60 seconds
  }

  trait DockerPrivateEnv[T] extends AroundOutside[T] {
    implicit val docker = Docker("localhost", 2375)
    implicit val registry = privateRegistry("192.168.0.120", 5000)
    implicit val timeout = 60 seconds
  }



  def image:DockerEnv[PublicRepositoryTag] = new DockerEnv[PublicRepositoryTag] {
    val cmd = Seq("/bin/sh", "-c", "while true; do echo hello world; sleep 1; done")
    val repoTag = PublicRepositoryTag.create("busybox", Some("latest"))


    // create a context
    def around[T: AsResult](t: => T) = {
      try {
        val file = Dockerfile from repoTag.toString  entering BashWrapper("while true; do echo hello world; sleep 1; done")
        val q = Await.result(docker.dockerfileBuild(file,"test/busy"),timeout)
        log.info(s"prepare image context - pulling busybox:latest ...")
        AsResult(t)
      } finally {
        //Await.result(docker.imageRemove(env.imageName), timeout)
        log.info(s"shutdown & cleaned up image context")
      }
    }

    // prepare a valid ImageEnv
    def outside: PublicRepositoryTag = repoTag
  }
}
