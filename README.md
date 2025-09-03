## sbt project compiled with Scala 3


### make sure to run sbt pack to create *.bat files and copying jar dependencies to folder
### to bring PSCompleter.ps1 to the environment, use "dot sourcing"
. .\PSCompleter.ps1
### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).

---

# Steps to follow

### Need PowerShell 7
> winget install Microsoft.Powershell  

> Check version by  firing `$PSVersionTable`, if still shows older version, kill all the terminals and do a complete restart of the system!

then  
> Fire `sbt pack`  
That creates the *bat files* in other words **fat jars** inside the ./target/pack directory

> Then fire `. ./PSCompleter.ps1` to get autocomplete suggestions on terminal

> Now we are ready to fire command right from powerShell ***without*** need of `sbt run`!

## To record
`whisper record`

## To convert .wav to text
`whisper whisper RECORDING.wav`

## To delete all recordings
`whisper deleterecordings`