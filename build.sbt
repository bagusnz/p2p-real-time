enablePlugins(ScalaJSPlugin)

name := "P2P Real-Time"

scalaVersion := "3.2.1"

// used for rescala & loci snapshots
resolvers += "jitpack" at "https://jitpack.io"

scalacOptions += "-Ymacro-annotations"

libraryDependencies ++= Seq(
  "com.github.rescala-lang.rescala" %%% "rescala" % "d5c1215bc2",
  "com.github.rescala-lang.rescala" %%% "kofre"   % "d5c1215bc2"
)
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.3.0"
libraryDependencies += "com.lihaoyi"  %%% "scalatags"   % "0.12.0"
//libraryDependencies ++= Seq(
//  "com.github.scala-loci.scala-loci" %%% "scala-loci-language"         % "03ddfb7ca9" % "compile-internal",
//  "com.github.scala-loci.scala-loci" %%% "scala-loci-language-runtime" % "03ddfb7ca9"
//)

libraryDependencies ++= Seq(
  "com.github.scala-loci.scala-loci" %%% "scala-loci-communicator-webrtc" % "03ddfb7ca9",
  "com.github.scala-loci.scala-loci" %%% "scala-loci-serializer-jsoniter-scala" % "03ddfb7ca9"
)

libraryDependencies ++= Seq(
  // Use the %%% operator instead of %% for Scala.js and Scala Native
  "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core" % "2.18.1",
  // Use the "provided" scope instead when the "compile-internal" scope is not supported
  "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % "2.18.1"
)

// This is an application with a main method
scalaJSUseMainModuleInitializer := true
