// sbt-coursier parallelizes downloads of dependencies.
// If you haven't tried it yet, do so - the productivity boost it provides is insane.
// It's especially useful for scala.meta, which contains more than a dozen of modules.
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
