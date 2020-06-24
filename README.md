# scala-fullstack

ScalaJS/Scala single page application template using the Mill build tool that supports npm dependencies with webpack. The server is implemented with Akka HTTP and the project contains examples for calling JavaScript from npm dependencies, doing RPC with sloth, and updating the UI with Laminar.

### Usage
Start the server with:
```
./mill -watch backend.runBackground
```

The website is served at `http://localhost:8080` and the server restarts automatically when files are changed.

### IntelliJ IDEA Import
Run
```
./mill mill.scalalib.GenIdea/idea
```
and open the project in IntelliJ IDEA by using the `Open or import` option and selecting the project folder.

### Thanks
Thanks @lolgab for the example of how to create a Scala full stack project with Mill: https://github.com/lolgab/scala-fullstack
