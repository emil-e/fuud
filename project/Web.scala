import sbt._
import sbt.Keys._

object Web {

  object Keys {
    lazy val npmInstall = taskKey[File](
      "Installs Node.js modules")

    lazy val webCompilers = settingKey[Seq[Task[Seq[File]]]](
      "Compiler for web stuff")
    lazy val compileWeb = taskKey[Seq[File]](
      "Compiles web stuff (JS, CSS et.c.)")

    lazy val browserify = taskKey[Seq[File]](
      "Browserify")
    lazy val browserifyOptions = taskKey[Seq[String]](
      "Options to browserify")
    lazy val browserifyTransforms = taskKey[Seq[Seq[String]]](
      "Transforms (with options) browserify")

    lazy val stylus = taskKey[Seq[File]](
      "Stylus")
    lazy val stylusOptions = taskKey[Seq[String]](
      "Options to stylus")
  }

  def settings = {
    import Keys._

    def webSourceMappings[T](task: TaskKey[T])(mapTarget: File => File = identity) = Def.task {
      import Keys._
      val baseDir = (sourceDirectory in compileWeb).value
      val inputFiles = (baseDir ** (includeFilter in task).value).get
      inputFiles.map { input =>
        val relative = baseDir.relativize(input).get.toString
        (input, mapTarget((sourceManaged in compileWeb).value / relative))
      }
    }

    def npmBin(name: String) = Def.task((npmInstall.value / name).toString)

    def withExt(file: File, ext: String) = file.getParentFile / (file.base + "." + ext)

    Seq(
      npmInstall := {
        if ("npm install".! != 0)
          sys.error("npm install failed")

        file("npm bin".!!.stripLineEnd)
      },
      update := {
        npmInstall.value; update.value
      },

      sourceDirectory in compileWeb := (sourceDirectory in Compile).value / "web",
      sourceManaged in compileWeb := (resourceManaged in Compile).value / "public",
      webCompilers := Seq.empty,
      compileWeb := webCompilers(_.join).map(_.flatten).value,
      resourceGenerators in Compile += compileWeb.taskValue,

      webCompilers += browserify.taskValue,
      includeFilter in browserify := GlobFilter("main.jsx"),
      watchSources ++= ((sourceDirectory in compileWeb).value ** ("*.js" || "*.jsx")).get,
      browserifyOptions := Seq.empty,
      browserifyTransforms := Seq.empty,
      browserify := {
        val transformOpts = browserifyTransforms.value.flatMap("-t" +: "[" +: _ :+ "]")
        val baseCmd = npmBin("browserify").value +: (transformOpts ++ browserifyOptions.value)

        webSourceMappings(browserify)(withExt(_, "js")).value.map { case (in, out) =>
          out.getParentFile.mkdirs()
          val cmd = Process(baseCmd :+ in.toString :+ "-o" :+ out.toString)
          if (cmd.! != 0)
            sys.error(s"Failed to browserify $in")
          out
        }
      },

      webCompilers += stylus.taskValue,
      includeFilter in stylus := GlobFilter("main.styl"),
      watchSources ++= ((sourceDirectory in compileWeb).value ** ("*.css" || "*.styl")).get,
      stylusOptions := Seq.empty,
      stylus := {
        val baseCmd = npmBin("stylus").value +: stylusOptions.value
        webSourceMappings(stylus)(withExt(_, "css")).value.map { case (in, out) =>
          out.getParentFile.mkdirs()
          val cmd = in #> Process(baseCmd :+ "-I" :+ in.getParent) #> out
          if (cmd.! != 0)
            sys.error(s"Stylus failed for $in")
          out
        }
      })
  }
}