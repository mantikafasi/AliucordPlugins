// include(":MyFirstPlugin")

include("")

include(":POGlugin")
project(":POGlugin").projectDir = File("./POGlugin")

include(":LightShotRoulette")
project(":LightShotRoulette").projectDir = File("./LightShotRoulette")


include(":Someone")
project(":Someone").projectDir= File("./Someone")

include(":byebyeSlashCommands")
project(":byebyeSlashCommands").projectDir = File("./byebyeSlashCommands")

include(":BetterSilentTyping")
project(":BetterSilentTyping").projectDir = File("./BetterSilentTyping")

include(":HighlightReplies")
project(":HighlightReplies").projectDir = File("./HighlightReplies")

include(":EditServersLocally")
project(":EditServersLocally").projectDir = File("./EditServersLocally")

include(":SusCord")
project(":SusCord").projectDir = File("./SusCord")



rootProject.name = "AliucordPlugins"

