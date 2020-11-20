object Names {
    val secrets = "secrets.tar"
    val privateKey = "secring.asc"
    val publicKey = "pubring.asc"
    val credentials = "credentials.sbt"
    val tmpFiles = List(secrets, privateKey, publicKey, credentials)
    val secretsEncoded = "secrets.tar.enc"
    def deleteTmpFiles() = tmpFiles.map(f => os.pwd / f).foreach(f => os.remove(f))
}

@main
def createKeyAndGenerateTravis(githubToken: String): Unit = {
    val pgpKeyId = createPgpKey()

    generateTravis(githubToken, pgpKeyId)
}

@main
def generateTravis(githubToken: String, pgpKeyId: String): Unit = {
    publish(pgpKeyIds) // Publishing is idempotent so we do that even for existing key
    exportPublicKey(pgpKeyId, Names.publicKey)
    exportPrivateKey(pgpKeyId, Names.privateKey)

    CredentialsFile.create()
    
    os.proc("tar", "cvf", Names.secrets, Names.privateKey, Names.publicKey, Names.credentials).call()
    // Travis.login(githubToken)
    // val travisEnvVarName = Travis.encryptFile(Names.secrets)
    // Names.deleteTmpFiles()

    // DecryptFile.create(travisEnvVarName)
}

object Gpg {
    private val gpg = "gpg2"

    private val keyservers = List(
        "hkp://hkps.pool.sks-keyservers.net",
        "hkps://keys.openpgp.org",
        "hkps://pgp.mit.edu"
    )

    def createAndPublish(): Option[String] = 
        generateKey().map(id => publish(id))

    def generateKey(): Option[String] = {
        val out = os.proc(gpg, "--gen-key").call().out.lines
        out.find(l => l.contains("pub") && l.contains("rsa") && l.contains("expires"))
    }

    def publish(keyId: String) = 
        keyservers.map(ks => publish(keyId, ks))

    def publish(keyId: String, keyServer: String) =
        os.proc(gpg, "--keyserver", keyServer, "--send-keys", keyId)

    def exportPublicKey(keyId: String, outputFileName: String) =
        os.proc(gpg, "--output", outputFileName, "--armor", "--export", keyId)

    def exportPrivateKey(keyId: String, outputFileName: String) =
        os.proc(gpg, "--output", outputFileName, "--armor", "--export-secret-key", keyId)
    
}

object Travis {
    def login(githubToken: String) = 
        os.proc("travis", "login", "--pro", "--github-token", githubToken).call()

    // parse travis env var name
    def encryptFile(filename: String): String = 
        os.proc("travis", "encrypt-file", "--pro", filename)
}

object CredentialsFile {
    def template(sonatypePassword: String, pgpPassphrase: String): String = s"""
    |credentials += Credentials("Sonatype Nexus Repository Manager",
    |  "oss.sonatype.org",
    |  "$sonatype_username$",
    |  "$sonatypePassword")
    |
    |pgpPassphrase := Some("$pgpPassphrase").map(_.toArray)
    """.stripMargin
    
    def create() = {
        println("Sonatype password: ")
        val sonatypePassword = Console.readPassword()
        println("PGP passphrase: ")
        val pgpPassphrase = Console.readPassword()

        val str = template(sonatypePassword, pgpPassphrase)
        os.write(os.pwd / Names.credentials)
    }
}

object Console {
    def readPassword(): String = String.valueOf(System.console.readPassword)
}

object DecryptFile {
    def template(travisEnvVarName: String) = 
        s"""#!/usr/bin/env bash
            |
            |if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then
            |  openssl aes-256-cbc -K $travisEnvVarName -iv $travisEnvVarName -in ${Names.secretsEncoded} -out ${Names.secrets} -d
            |  tar xvf ${Names.secrets}
            |fi
        """.stripMargin

    def create(travisEnvVarName: String) = {
        val scriptsDir = os.pwd / "scripts"
        os.makeDir(scriptsDir)
        os.write(scriptsDir / Names.secretsEncoded, template(travisEnvVarName))
    }
        

}

