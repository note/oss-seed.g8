import java.util.jar.Attributes.Name
object Names {
    val secrets = "secrets.tar"
    val privateKey = "secring.asc"
    val publicKey = "pubring.asc"
    val credentials = "credentials.sbt"
    val tmpFiles = List(secrets, privateKey, publicKey, credentials)
    val decryptFile = "decrypt_files_if_not_pr.sh"
    def deleteTmpFiles() = tmpFiles.map(f => os.pwd / f).foreach(f => os.remove(f))
    val secretsEncrypted = s"$secrets.enc"
}

@main
def createKeyAndGenerateTravis(githubToken: String): Unit = {
    Gpg.generateKey() match {
        case Some(key) => generateTravis(githubToken, key)
        case None => sys.error("Could not parse output of Gpg.generateKey")
    }
}

@main
def generateTravis(githubToken: String, pgpKeyId: String): Unit = {
    Gpg.publish(pgpKeyId) // Publishing is idempotent so we do that even for existing key
    Gpg.exportPublicKey(pgpKeyId, Names.publicKey)
    Gpg.exportPrivateKey(pgpKeyId, Names.privateKey)

    CredentialsFile.create()
    
    os.proc("tar", "cvf", Names.secrets, Names.privateKey, Names.publicKey, Names.credentials).call()
    Travis.login(githubToken)
    val encryptLineInBash = Travis.encryptFile(Names.secrets)
    Names.deleteTmpFiles()

    DecryptFile.create(encryptLineInBash)
}

object Gpg {
    private val gpg = "gpg2"

    private val keyservers = List(
        "hkp://hkps.pool.sks-keyservers.net",
        "hkps://keys.openpgp.org",
        "hkps://pgp.mit.edu"
    )

    def generateKey(): Option[String] = {
        val out = os.proc(gpg, "--gen-key").call().out.lines
        out.find(l => l.contains("pub") && l.contains("rsa") && l.contains("expires"))
    }

    def publish(keyId: String): Unit = 
        keyservers.map(ks => publish(keyId, ks))

    def publish(keyId: String, keyServer: String): Unit =
        os.proc(gpg, "--keyserver", keyServer, "--send-keys", keyId).call()

    def exportPublicKey(keyId: String, outputFileName: String) =
        os.proc(gpg, "--output", outputFileName, "--armor", "--export", keyId).call()

    def exportPrivateKey(keyId: String, outputFileName: String) =
        os.proc(gpg, "--output", outputFileName, "--armor", "--export-secret-key", keyId).call()
    
}

object Travis {
    def login(githubToken: String) = 
        os.proc("travis", "login", "--pro", "--github-token", githubToken).call()

    def encryptFile(filename: String): String = 
        os.proc("travis", "encrypt-file", "--pro", filename).call().out.lines.find(_.contains("openssl")) match {
            case Some(l) => l.trim
            case None => throw new RuntimeException("Could not parse output of travis encrypt-file")
        }
}

object CredentialsFile {
    // see more about giter8 and string interpolation issue: https://github.com/foundweekends/giter8/issues/333
    def template(sonatypePassword: String, pgpPassphrase: String): String = s"""
    |credentials += Credentials("Sonatype Nexus Repository Manager",
    |  "oss.sonatype.org",
    |  "TODO",
    |  "\$sonatypePassword")
    |
    |pgpPassphrase := Some("\$pgpPassphrase").map(_.toArray)
    """.stripMargin

    def create() = {
        println("Sonatype password: ")
        val sonatypePassword = Console.readPassword()
        println("PGP passphrase: ")
        val pgpPassphrase = Console.readPassword()

        val str = template(sonatypePassword, pgpPassphrase)
        os.write.over(os.pwd / Names.credentials, str)
    }
}

object Console {
    def readPassword(): String = String.valueOf(System.console.readPassword)
}

object DecryptFile {
    def create(bashCode: String) = {
        val scriptsDir = os.pwd / "scripts"
        val template = os.read(scriptsDir / s"${Names.decryptFile}.template")
        val replaced = template.replace("%encryptLine%", bashCode).replace("%encryptedFile%", Names.secretsEncrypted)
        os.write(scriptsDir / Names.decryptFile, replaced)
        os.proc("chmod", "u+x", scriptsDir / Names.decryptFile).call()
    }
}
