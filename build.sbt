val rescalaKofreVersion = "d5c1215bc2"
val scalaJsDomVersion = "2.3.0"
val scalaTagsVersion = "0.12.0"
val jsoniterScalaVersion = "2.18.1"
val scalaLociVersion = "03ddfb7ca9"
val scala3 = "3.2.1"

val p2pRealTime = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "P2P Real-Time",
    resolvers += "jitpack" at "https://jitpack.io",
    scalaVersion := scala3,
    scalacOptions += "-Ymacro-annotations",
    libraryDependencies ++= Seq(
      "com.github.rescala-lang.rescala" %%% "rescala" % rescalaKofreVersion,
      "com.github.rescala-lang.rescala" %%% "kofre" % rescalaKofreVersion,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
      "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
      "com.github.scala-loci.scala-loci" %%% "scala-loci-communicator-webrtc" % scalaLociVersion,
      "com.github.scala-loci.scala-loci" %%% "scala-loci-serializer-jsoniter-scala" % scalaLociVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % jsoniterScalaVersion,
      "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % jsoniterScalaVersion,
    ),
    scalaJSUseMainModuleInitializer := true
  )