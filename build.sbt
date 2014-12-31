organization := "org.almoehi"

name := "reactive-docker"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)

publishMavenStyle := true

//crossScalaVersions := Seq("2.10.0", "2.11.0")

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


//wartremoverErrors ++= Warts.allBut(Wart.DefaultArguments)

parallelExecution in Test := false

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>http://github.com/almoehi/reactive-docker</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:almoehi/reactive-docker.git</url>
    <connection>scm:git:git@github.com:almoehi/reactive-docker.git</connection>
  </scm>
  <developers>
    <developer>
      <id>almoehi</id>
      <name>alm oehi</name>
      <url>http://github.com/almoehi/</url>
    </developer>
  </developers>)
scalacOptions ++= Seq("-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-language:postfixOps,higherKinds,existentials",
  "-unchecked",
  "-Xcheckinit",
//  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-value-discard",
  "-Ywarn-infer-any"
)

// scalacOptions in (Compile, console) += "-Xlog-implicits"


  javacOptions ++= Seq("-target", "1.6", "-source","1.6")

val logbackVer = "1.1.2"

libraryDependencies ++= Seq(
            //"org.scalaz.stream" %% "scalaz-stream" % "0.3.1",
            "com.netaporter" %% "scala-uri" % "0.4.4",
            "com.typesafe.play" %% "play-json" % "2.3.7",
            "com.typesafe.play" %% "play-iteratees" % "2.3.7",
            // "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
            "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
            "org.specs2" %% "specs2" % "2.4.15" % "test",
            "ch.qos.logback" % "logback-core" % logbackVer,
            "ch.qos.logback" % "logback-classic" % logbackVer,
            "org.apache.commons" % "commons-compress" % "1.9",
            "org.parboiled" %% "parboiled" % "2.0.1",
            "org.scalaz" %% "scalaz-core" % "7.1.0"
  )

transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

// see https://github.com/typesafehub/scalalogging/issues/23
testOptions in Test += Tests.Setup(classLoader =>
  classLoader
    .loadClass("org.slf4j.LoggerFactory")
    .getMethod("getLogger", classLoader.loadClass("java.lang.String"))
    .invoke(null, "ROOT")
)

resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    Resolver.typesafeRepo("releases"),
    Resolver.typesafeRepo("snapshots")
    //"Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    //"spray repo" at "http://repo.spray.io"
    )


testOptions in Test += Tests.Argument("-oDF")
