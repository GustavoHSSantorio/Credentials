import groovy.util.XmlSlurper
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild
import groovy.util.slurpersupport.NodeChildren
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.buildscript
import java.io.File
import java.io.FileNotFoundException


data class Credential(var versionEnvironment: String, var versionToken: String)

object Credentials{
    val map : HashMap<String, Credential> = hashMapOf()
}

abstract class CredentialsTask : DefaultTask(){

    @get:Input
    abstract val credentialFile : Property<String>

    @TaskAction
    fun getAccessToken() {
        val settings = getMavenSettingsCredentials()

        settings?.forEach {
            (it as? NodeChild)?.run {
                Credentials.map[getProperty("id").toString()] = Credential(
                    getProperty("username").toString(),
                    getProperty("password").toString()
                )
            }
        }
    }

    private fun getMavenSettingsCredentials() : NodeChildren? {
        val userHome = System.getProperty("user.home")
        val mavenSettings = File(userHome, ".m2/${credentialFile.getOrElse("settings.xml")}")
        val xmlSlurper = XmlSlurper()
        return try{
            val output = xmlSlurper.parse(mavenSettings)
            (output.getProperty("servers") as GPathResult).getProperty("server") as NodeChildren
        }catch (e: FileNotFoundException){
            logger.warn(e.message + " - Please add your credential file to maven local and rebuild")
            null
        }
    }
}

class CredentialsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
            target.buildscript {
                target.tasks.register("getCredentials", CredentialsTask::class.java){
                    credentialFile.set("settings.xml")
                    getAccessToken()
                }

                println(target.tasks.getByPath("getCredentials").path)
            }
    }
}