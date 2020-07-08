import java.io._
import java.util.zip.{ZipEntry, ZipInputStream}

import ammonite.ops
import mill._
import mill.define.{Sources, Target, Task}
import mill.eval.PathRef
import mill.scalajslib._
import mill.scalajslib.api.ModuleKind
import mill.util.Ctx
import os.ReadablePath

/** Trait for Scala.js modules that create a webpack bundle from their NPM dependencies.
 *
 * Usage example:
 * {{{
 * import mill.contrib.ScalaJSWebpackModule._
 *
 * object myModule extends ScalaJSWebpackModule {
 *     override def npmDeps = Agg("uuid" -> "8.1.0")
 *   }
 * }}}
 */
trait ScalaJSWebpackModule extends ScalaJSModule {

  // Direct npm dependencies
  def npmDeps: T[Agg[(String, String)]] = Agg.empty[(String, String)]

  // Direct npm development dependencies
  def npmDevDeps: T[Agg[(String, String)]] = Agg.empty[(String, String)]

  def webpackVersion: Target[String] = "4.43.0"

  def webpackMergeVersion: Target[String] = "4.2.2"

  def webpackCliVersion: Target[String] = "3.3.11"

  def sourceMapLoaderVersion: Target[String] = "1.0.0"

  def scalaJsFriendlySourceMapLoaderVersion: Target[String] = "0.1.5"

  override def moduleKind: T[ModuleKind] = T {
    ModuleKind.CommonJSModule
  }

  // The name of the bundle generated by webpack
  def webpackBundleFilename: Target[String] = "out-bundle.js"

  // Webpack output path for generated config files and the bundle
  def webpackOutputPath: Target[os.Path] = Ctx.taskCtx.dest

  // The name of the generated webpack config file
  def webpackConfigFilename: Target[String] = "webpack.config.js"

  // Custom webpack configuration objects that get merged with the generated config
  def customWebpackConfigs: Sources = T.sources()

  // All JS dependencies
  def jsDeps: T[JsDeps] = T {
    val jsDepsFromIvyDeps =
      resolveDeps(transitiveIvyDeps)().flatMap(pathRef =>
        jsDepsFromJar(pathRef.path.toIO))
    val allJsDeps = jsDepsFromIvyDeps ++ transitiveJsDeps() ++ Agg(
      JsDeps(
        npmDeps().iterator.toList,
        devDependencies = npmDevDeps().iterator.toList)
    )
    allJsDeps.iterator.foldLeft(JsDeps.empty)(_ ++ _)
  }

  def transitiveJsDeps: Task.Sequence[JsDeps] =
    T.sequence(recursiveModuleDeps.collect {
      case mod: ScalaJSWebpackModule => mod.jsDeps
    })

  def writePackageSpec: Task[(JsDeps, ops.Path) => Unit] = T.task {
    (jsDeps: JsDeps, dst: ops.Path) =>
      val compileDeps = jsDeps.dependencies
      val compileDevDeps =
        jsDeps.devDependencies ++ Seq(
          "webpack" -> webpackVersion(),
          "webpack-merge" -> webpackMergeVersion(),
          "webpack-cli" -> webpackCliVersion(),
          "source-map-loader" -> sourceMapLoaderVersion(),
          "scalajs-friendly-source-map-loader" -> scalaJsFriendlySourceMapLoaderVersion
        )

      ops.write.over(
        dst / "package.json",
        ujson
          .Obj(
            "dependencies" -> compileDeps,
            "devDependencies" -> compileDevDeps)
          .render(2) + "\n")
  }

  def writeBundleSources: Task[(JsDeps, ops.Path) => Unit] = T.task {
    (jsDeps: JsDeps, dst: ops.Path) =>
      jsDeps.jsSources foreach { case (n, s) => ops.write.over(dst / n, s) }
  }

  def writeResources: Task[ops.Path => Unit] = T.task { (dst: ops.Path) =>
    resources() foreach { resourcePath: PathRef =>
      os.copy.over(resourcePath.path, dst)
    }
  }

  def writeWpConfig: Task[
    (ops.Path, String, Seq[ReadablePath], String, String, Boolean) => Unit] =
    T.task {
      (
        outputPath: ops.Path,
        cfgFileName: String,
        customCfgs: Seq[ReadablePath],
        entry: String,
        outputFilename: String,
        opt: Boolean
      ) =>
        val generatedCfgName = "generatedWebpackCfg"
        val generatedCfg =
          s"""|const merge = require('webpack-merge');
              |
              |// Webpack config generated by ScalaJSWebpackModule
              |const $generatedCfgName = {
              |  "mode": "${if (opt) "production" else "development"}",
              |  "devtool": "${if (opt) "eval" else "source-map"}",
              |  "entry": "$entry",
              |  "output": {
              |    "path": "$outputPath",
              |    "filename": "$outputFilename"
              |  },
              |  "module": {
              |    "rules": [
              |       // Scala JS source map support
              |       {
              |           "test": /\\.js$$/,
              |           "use": ["scalajs-friendly-source-map-loader"],
              |           "enforce": "pre"
              |       },
              |    ]
              |  }
              |};
              |""".stripMargin
        val mergedCfg = customCfgs match {
          case Nil =>
            s"""|$generatedCfg
                |module.exports = $generatedCfgName;
                |""".stripMargin
          case customCfgPaths =>
            val customCfgs: Seq[(String, String)] = // Seq(name, cfgString)
              customCfgPaths.zipWithIndex.map {
                case (customCfg, i) =>
                  val customCfgName = s"customWebpackCfg$i"
                  val customCfgString =
                    s"""|// Custom webpack config from '$customCfg', defined in ScalaJSWebpackModule
                        |const $customCfgName = ${readStringFromInputStream(
                      customCfg.getInputStream).trim};
                        |""".stripMargin
                  customCfgName -> customCfgString
              }
            s"""|$generatedCfg
                |${customCfgs
              .map { case (_, cfgString) => cfgString }
              .mkString("\n")}
                |module.exports = merge($generatedCfgName, ${customCfgs
              .map { case (cfgName, _) => cfgName }
              .mkString(",")});
                |""".stripMargin
        }
        ops.write.over(outputPath / cfgFileName, mergedCfg)
    }

  def runWebpack: Task[(ops.Path, String) => Unit] = T.task {
    (dst: ops.Path, cfg: String) =>
      print(ops.%%("npm", "install", "--no-fund")(dst).out.string)
      print(
        ops
          .%%(
            "node",
            dst / "node_modules" / "webpack" / "bin" / "webpack",
            "--bail",
            "--profile",
            "--config",
            cfg)(dst)
          .out
          .string)
  }

  def webpack: Task[(ops.Path, ops.Path, Boolean) => Unit] = T.task {
    (src: ops.Path, dst: ops.Path, opt: Boolean) =>
      val jsFilename = src.segments.toSeq.last
      val sourceMapFilename = s"$jsFilename.map"
      val entryPointJs = dst / jsFilename
      val deps = jsDeps()
      val cfg = webpackConfigFilename()
      writeBundleSources().apply(deps, dst)
      writeResources().apply(dst)
      ops.cp.over(src, entryPointJs)
      val sourceMapPath = src / os.up / sourceMapFilename
      if (os.exists(sourceMapPath)) {
        ops.cp.over(sourceMapPath, dst / sourceMapFilename)
      }
      writeWpConfig()
        .apply(
          dst,
          cfg,
          customWebpackConfigs().map(_.path),
          entryPointJs.toString,
          webpackBundleFilename(),
          opt)
      writePackageSpec().apply(deps, dst)
      runWebpack().apply(dst, cfg)
  }

  def fastOptWp: Target[PathRef] = T.persistent {
    val dst = webpackOutputPath()
    webpack().apply(fastOpt().path, dst, false)
    PathRef(dst)
  }

  def fullOptWp: Target[PathRef] = T.persistent {
    val dst = webpackOutputPath()
    webpack().apply(fullOpt().path, dst, true)
    PathRef(dst)
  }

  @scala.annotation.tailrec
  private def readStringFromInputStream(
    in: InputStream,
    buffer: Array[Byte] = new Array[Byte](8192),
    out: ByteArrayOutputStream = new ByteArrayOutputStream
  ): String = {
    val byteCount = in.read(buffer)
    if (byteCount < 0) {
      out.toString
    } else {
      out.write(buffer, 0, byteCount)
      readStringFromInputStream(in, buffer, out)
    }
  }

  private def collectZipEntries[R](jar: File)(
    f: PartialFunction[(ZipEntry, ZipInputStream), R]
  ): List[R] = {
    val stream = new ZipInputStream(
      new BufferedInputStream(new FileInputStream(jar)))
    try Iterator
      .continually(stream.getNextEntry)
      .takeWhile(_ != null)
      .map(_ -> stream)
      .collect(f)
      .toList
    finally stream.close()
  }

  private def jsDepsFromJar(jar: File): Seq[JsDeps] = {
    collectZipEntries(jar) {
      case (zipEntry, stream) if zipEntry.getName == "NPM_DEPENDENCIES" =>
        val contentsAsJson = ujson.read(readStringFromInputStream(stream)).obj

        def dependenciesOfType(key: String): List[(String, String)] =
          contentsAsJson
            .getOrElse(key, ujson.Arr())
            .arr
            .flatMap(_.obj.map {
              case (s: String, v: ujson.Value) => s -> v.str
            })
            .toList

        JsDeps(
          dependenciesOfType("compileDependencies") ++ dependenciesOfType(
            "compile-dependencies"),
          dependenciesOfType("compileDevDependencies") ++ dependenciesOfType(
            "compile-devDependencies")
        )
      case (zipEntry, stream)
        if zipEntry.getName.endsWith(".js") && !zipEntry.getName.startsWith(
          "scala/") =>
        JsDeps(
          jsSources = Map(zipEntry.getName -> readStringFromInputStream(stream))
        )
    }
  }

}

case class JsDeps(
  dependencies: List[(String, String)] = Nil,
  devDependencies: List[(String, String)] = Nil,
  jsSources: Map[String, String] = Map.empty
) {

  def ++(that: JsDeps): JsDeps =
    JsDeps(
      dependencies ++ that.dependencies,
      devDependencies ++ that.devDependencies,
      jsSources ++ that.jsSources)
}

object JsDeps {

  lazy val empty: JsDeps = JsDeps()

  implicit def rw: upickle.default.ReadWriter[JsDeps] =
    upickle.default.macroRW
}
