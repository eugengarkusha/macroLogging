// scala.meta macros are at the moment only supported in 2.11.
scalaVersion in ThisBuild := "2.11.8"

lazy val compilerOptions = Seq[String]() // Include favorite compiler flags here.

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  // New-style macro annotations are under active development.  As a result, in
  // this build we'll be referring to snapshot versions of both scala.meta and
  // macro paradise.
  resolvers += Resolver.url(
    "scalameta",
    url("http://dl.bintray.com/scalameta/maven"))(Resolver.ivyStylePatterns),
  // A dependency on macro paradise 3.x is required to both write and expand
  // new-style macros.  This is similar to how it works for old-style macro
  // annotations and a dependency on macro paradise 2.x.  A new release is
  // published on every merged PR into paradise.  To find the latest PR number,
  // see https://github.com/scalameta/paradise/commits/master and replace "109"
  addCompilerPlugin(
    "org.scalameta" % "paradise" % "3.0.0.109" cross CrossVersion.full),
  scalacOptions ++= compilerOptions,
  scalacOptions += "-Xplugin-require:macroparadise",
  // temporary workaround for https://github.com/scalameta/paradise/issues/10
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based", // necessary to use console
  // temporary workaround for https://github.com/scalameta/paradise/issues/55
  sources in (Compile, doc) := Nil
)

// Define macros in this project.
lazy val macros = project.settings(
  metaMacroSettings,
  // A dependency on scala.meta is required to write new-style macros, but not
  // to expand such macros.  This is similar to how it works for old-style
  // macros and a dependency on scala.reflect.  To find the latest version, see
  // MetaVersion in:
  // https://github.com/scalameta/paradise/blob/master/build.sbt
  libraryDependencies += "org.scalameta" %% "scalameta" % "1.3.0.522"
)

// Use macros in this project.
lazy val app = project.settings(metaMacroSettings).dependsOn(macros)
