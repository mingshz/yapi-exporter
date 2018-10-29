package com.mingshz.tools.yapi

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.MissingOptionException
import org.apache.commons.cli.Options
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.system.exitProcess


/**
 * 入口
 * @author CJ
 */
fun main(args: Array<String>) {
    // create Options object
    val options = Options()

    options.addOption("server", true, "yapi server url(default:http://api.mingshz.com)")
    options.addOption("user", true, "username(default:api@mingshz.com)")
    options.addRequiredOption("password", null, true, "password of this user.")
    options.addRequiredOption("modules", null, true, "modules(id:name[:baseUri],...)")
    options.addOption("statics", null, true, "statics(uris>serverTarget,...)")
    options.addOption("apiServer", true, "api server(default: server:8080)")
    options.addOption("cwd", true, "working cd. (default: ./)")
    options.addOption("tee", true, "tee target path.")
    options.addOption("schema", true, "fixed schema.")

    val parser = DefaultParser()
    try {
        val cmd = parser.parse(options, args)

        val home = File(cmd.getOptionValue("cwd", "./"))
        val downloader = ApiDownloader(home = home
                , url = cmd.getOptionValue("server", "http://api.mingshz.com")
                , username = cmd.getOptionValue("user", "api@mingshz.com")
                , password = cmd.getOptionValue("password")
        )

        cmd.getOptionValue("modules")
                .split(",")
                .forEach {
                    val ds = it.split(":")
                    downloader.downloadTo(ds[0].toInt(), ds[1])
                }

        // 处理nginx
        val servers = File(home, "nginx")
        servers.mkdir()

        cmd.getOptionValue("modules")
                .split(",")
                .forEach {
                    val ds = it.split(":")
                    val name = ds[1]
                    val baseUri = if (ds.size > 2) ds[2] else ""

                    FileInputStream(File(home, "$name-api.json"))
                            .use {
                                // 输出位置
                                ServerGenerator(
                                        alias = name,
                                        home = servers,
                                        stream = it,
                                        baseUri = baseUri,
                                        fixedSchema = if (cmd.hasOption("schema")) cmd.getOptionValue("schema") else null
                                ).work()
                            }
                }

        val lastStaticFile = File(servers, "_statics.nginx")
        if (lastStaticFile.exists())
            lastStaticFile.delete()

        if (cmd.hasOption("statics")) {
            cmd.getOptionValue("statics")
                    .split(",")
                    .forEach { originString ->
                        // uri1[,uri2]>serverTarget
                        val l1 = originString.split(">")
                        val target = l1[1]
                        FileOutputStream(File(servers, "_statics.nginx"), true)
                                .use {
                                    StaticServerGenerator(
                                            uris = l1[0],
                                            target = target,
                                            stream = it
                                    )
                                            .work()
                                }
                    }
        }

        if (cmd.hasOption("tee")) {
            val teeTarget = cmd.getOptionValue("tee")
//            teeTarget.mkdirs()
            servers.listFiles().forEach {

                // tee /etc/docker/daemon.json <<-'EOF'
                println("tee $teeTarget/${it.name} <<-'EOF'")
                it.reader(Charset.forName("UTF-8"))
                        .use {
                            it.forEachLine {
                                println(it)
                            }
                        }
                // EOF
                println("EOF")
            }
        }

    } catch (e: MissingOptionException) {
        val formatter = HelpFormatter()
        formatter.printHelp("exec", options)
        exitProcess(2)
    }


}