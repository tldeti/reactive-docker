
package test

import org.specs2.mutable.Specification
import com.kolor.docker.api.entities._
import scala.concurrent.duration._
import scala.concurrent._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.slf4j.LoggerFactory

@RunWith(classOf[JUnitRunner])
class DockerEntitySpec extends Specification {

  "Docker entities should" should {

    "accept repository tags of public docker registry " in {
      val tagWithoutVersion = RepoTagLocation.indexRepoTParse("ubuntu").get
      val tagWithVersion = RepoTagLocation.indexRepoTParse("ubuntu:latest").get

      tagWithoutVersion.repoLocation.repoName must be_!==("ubuntu")
      tagWithoutVersion.tag must_== "latest"

      tagWithVersion.repoLocation.repoName must be_==("ubuntu")
      tagWithVersion.tag must_== "latest"
    }

    "accept repository tags of private registries" in {
      val tagWithPrivateRegistryWithVersion = RepoTagLocation.indexRepoTParse("192.168.0.120:5000/test:latest").get
      tagWithPrivateRegistryWithVersion.repoLocation.noTagImage must be_==("192.168.0.120:5000/test")
      tagWithPrivateRegistryWithVersion.tag must_== "latest"

      val tagWithPrivateRegistryWithoutVersion = RepoTagLocation.indexRepoTParse("192.168.0.120:5000/test").get
      tagWithPrivateRegistryWithoutVersion.repoLocation.noTagImage must be_==("192.168.0.120:5000/test")
      tagWithPrivateRegistryWithoutVersion.tag must_== "latest"

      val tagWithPrivateRegistryDomainWithVersion = RepoTagLocation.indexRepoTParse("some.tld.com/test:latest").get
      tagWithPrivateRegistryDomainWithVersion.repoLocation.noTagImage must be_==("some.tld.com/test")
      tagWithPrivateRegistryDomainWithVersion.tag must_== "latest"
    }

  }
}