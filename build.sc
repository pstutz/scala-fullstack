import $file.webpack
import ammonite.ops._
import coursier.maven.MavenRepository
import mill._
import mill.scalajslib._
import mill.scalalib._
import mill.scalalib.scalafmt._
import webpack.ScalaJSWebpackModule

trait CommonScalaModule extends ScalaModule with ScalafmtModule {
  def scalaVersion: T[String] = "2.13.2"

  def scalaJSVersion: T[String] = "1.1.0"

  def booPickleVersion = "1.3.2"

  def slothVersion = "0.3.0"

  def osLibVersion = "0.6.2"

  def akkaHttpVersion = "10.1.12"

  def akkaStreamVersion = "2.6.5"

  def covenantVersion = "master-SNAPSHOT"

  def scalaJsDomVersion = "1.0.0"

  def laminarVersion = "0.9.1"

  def uuidVersion = "8.1.0"

  def postCssLoaderVersion = "3.0.0"

  def styleLoaderVersion = "1.2.1"

  def tailwindCssVersion = "1.4.6"

  def autoPrefixerVersion = "9.8.4"

  override def scalacOptions = super.scalacOptions() :+ "-Ymacro-annotations"

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"io.suzaku::boopickle::$booPickleVersion",
    ivy"com.github.cornerman::sloth::$slothVersion",
    ivy"com.github.cornerman.covenant::covenant-http::$covenantVersion"
  )

  // Needed for Covenant
  override def repositories = super.repositories ++ Seq(
    MavenRepository("https://jitpack.io")
  )

}

trait CommonScalaJsModule extends ScalaJSModule with CommonScalaModule {
  def platformSegment = "js"
}

object shared extends Module {
  object jvm extends CommonScalaModule {
    override def millSourcePath = super.millSourcePath / up
  }

  object js extends CommonScalaJsModule {
    override def millSourcePath = super.millSourcePath / up
  }

}

object frontend extends CommonScalaJsModule with ScalaJSWebpackModule {

  override def moduleDeps: Seq[ScalaJSModule] = Seq(shared.js)

  override def npmDeps = Agg(
    "uuid" -> uuidVersion
  )

  override def npmDevDeps = Agg(
    "postcss-loader" -> postCssLoaderVersion,
    "style-loader" -> styleLoaderVersion,
    "tailwindcss" -> tailwindCssVersion,
    "autoprefixer" -> autoPrefixerVersion,
  )

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scala-js::scalajs-dom::$scalaJsDomVersion",
    ivy"com.raquo::laminar::$laminarVersion"
  )

  override def maybeCustomWebpackConfig = Some(millSourcePath /  "custom-webpack.config")

}

object backend extends CommonScalaModule {

  override def moduleDeps = Seq(shared.jvm)

  override def resources = T.sources {
    super.resources() :+ frontend.fastOptWp()
  }

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.lihaoyi::os-lib:$osLibVersion",
    ivy"com.typesafe.akka::akka-http-xml:$akkaHttpVersion",
    ivy"com.typesafe.akka::akka-http2-support:$akkaHttpVersion",
    ivy"com.typesafe.akka::akka-stream:$akkaStreamVersion"
  )

  override def mainClass = Some("webapp.WebServer")

}
