Giter8 template for OSS projects with sonatype publish on Travis CI.

## How to use that template to generate a new project

Such process is assumed:

1. Create new Github repository. Just leave it empty for a moment, we will push the result of running the script at the end.
2. Run `sbt new note/oss-seed.g8`
3. Enter newly created directory.
4. Create rersonal access token on Github:
  * Go to https://github.com/settings/tokens and click "Generate new token"
  * Select such scopes: `read:org, read:user, repo, user:email, write:repo_hook`
  * Note down the token somewhere. You will need it in the next step.
5. Depending whether you have GPG key you want to use to sign artifacts published by travis, choose from:
  * Run `amm travis.sc createKeyAndGenerateTravis --githubToken YOUR_GITHUB_TOKEN`
  * Run `amm travis.sc generateTravis --githubToken GITHUB_TOKEN --pgpKeyId YOUR_PGP_KEY_ID`

## Release process

Now, after generating the project you can focus on writing your library for some time. When the moment of the first release come, you need to:

```
TODO
```

## Debugging

In case something hasn't worked out out of the box you may find yourself in frustrating position of waiting for Travis to verify possible fixes.
In such situations, to shorten the feedback cycle, you can run the Travis part on your own laptop:

```
TODO
```
