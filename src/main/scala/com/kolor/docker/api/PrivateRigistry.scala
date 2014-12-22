package com.kolor.docker.api

import com.kolor.docker.api.entities._
import com.ning.http.client.Response
import dispatch.{Http, host, _}
import play.api.libs.json.Format

import scala.concurrent.ExecutionContext


final case class NoIndexRegistry(host: String, port: Int, version: String)

object NoIndexRegistry extends NoIndexRegistryFunc{
  def apply(host: String, port: Int): NoIndexRegistry =
    new NoIndexRegistry(host, port, "v1")
}

object PrivateRegistryEndpoint {
  def baseReq(implicit registry: NoIndexRegistry) =
    host(registry.host, registry.port) / registry.version secure

  def deleteRepoTag(r: NoIndexRepoTagLocation)(implicit registry: NoIndexRegistry) = {
    baseReq / "repositories" / r.repoLocation.namespace / r.repoLocation.toString / "tags" / r.tag
  }

  def deleteRepo(r: NoIndexRepoLocation)(implicit registry: NoIndexRegistry) = {
    baseReq / "repositories" / r.namespace / r.toString / "tags"
  }
}


/**
 * Created by tldeti on 14-12-10.
 */
trait NoIndexRegistryFunc {
  import com.kolor.docker.api.json.Formats._
  def pRRequest(req: Req)(implicit exec:ExecutionContext):Future[Either[PrivateRegistryException,Response]] =
    Http(req).map(resp =>
      resp match {
        case x if x.getStatusCode == 200 => Right(x)
        case x if x.getStatusCode == 500 => Left(PRInternalException(resp.getResponseBody))
        case x if x.getStatusCode == 401 => Left(NoAuthException(resp.getResponseBody))
        case x if x.getStatusCode == 404 => Left(NotFoundException(resp.getResponseBody))
        case x => Left(WTFException(resp.getResponseBody, resp.getStatusCode))
      }
    ).recover {
      case x: Throwable =>
        Left(PRRequestException(x.getMessage, Some(x)))
    }

  private def authHeaderMap(auth:DockerAuth) = auth match {
    case DockerAnonymousAuth => Map()
    case data => Map("Authorization" -> s"Basic ${data.pRBase64Encoded}")
  }


  def deleteRepoTag(r:NoIndexRepoTagLocation)(
    implicit d: NoIndexRegistry, auth: DockerAuth,exec:ExecutionContext
    ):Future[Either[PrivateRegistryException,Unit]] = {
    val req = PrivateRegistryEndpoint.deleteRepoTag(r).DELETE <:< authHeaderMap(auth)
    pRRequest(req).map(x =>
      x.right.map(_=>())
    )
  }

  /**
   * from https://github.com/docker/docker-registry/issues/45 , delete request is
   * DELETE /v1/repositories/<namespace>/<path:repository>/tags , not
   * DELETE /v1/repositories/<namespace>/<path:repository>/   which in specs
   *
   * note: this only delete repo's all tag. need shell to delete relate no tag image.
   */
  def deleteRepo(r:NoIndexRepoLocation)(
    implicit d: NoIndexRegistry, auth: DockerAuth,exec:ExecutionContext): Future[Either[PrivateRegistryException, Unit]] = {
    val req = PrivateRegistryEndpoint.deleteRepo(r).DELETE <:< authHeaderMap(auth)
    pRRequest(req).map { x=>
      x.right.map(_=>())
    }
  }


}
