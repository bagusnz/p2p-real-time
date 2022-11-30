enablePlugins(ScalaJSPlugin)

name := "P2P Real-Time"

scalaVersion := "2.13.7"
resolvers += ("STG old bintray repo" at "http://www.st.informatik.tu-darmstadt.de/maven/").withAllowInsecureProtocol(true)

scalacOptions += "-Ymacro-annotations"

libraryDependencies += "de.tuda.stg" %%% "rescala" % "0.30.0"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.3.0"
libraryDependencies += "com.lihaoyi" %%% "scalatags" % "0.12.0"
libraryDependencies ++= Seq(
  "io.github.scala-loci" %%% "scala-loci-language" % "0.5.0" % "compile-internal",
  "io.github.scala-loci" %%% "scala-loci-language-runtime" % "0.5.0")

libraryDependencies += "io.github.scala-loci" %%% "scala-loci-language-transmitter-rescala" % "0.5.0"
libraryDependencies += "io.github.scala-loci" %%% "scala-loci-communicator-webrtc" % "0.5.0"
libraryDependencies += "io.github.scala-loci" %%% "scala-loci-serializer-jsoniter-scala" % "0.5.0"

libraryDependencies ++= Seq(
  // Use the %%% operator instead of %% for Scala.js and Scala Native
  "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-core"   % "2.17.9",
  // Use the "provided" scope instead when the "compile-internal" scope is not supported
  "com.github.plokhotnyuk.jsoniter-scala" %%% "jsoniter-scala-macros" % "2.17.9"
)

// This is an application with a main method
scalaJSUseMainModuleInitializer := true
