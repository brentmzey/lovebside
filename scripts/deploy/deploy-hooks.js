const ftp = require("basic-ftp")
const path = require("path")
const fs = require("fs")
const { execSync } = require("child_process")

/**
 * PocketHost is retiring SFTP/FTP file access on 2027-02-06 in favor of the `pockethost` CLI over
 * HTTPS (see pockethost.io/blog/sftp-sunset). Prefer it whenever POCKETHOST_TOKEN is set (a personal
 * access token from the PocketHost dashboard, sourced only from ~/.zshrc or ~/.zshenv - never
 * hardcoded, never printed, never committed), and only fall back to the legacy FTP path below when
 * it isn't, so deploys never silently break mid-transition.
 */
function deployHooksViaCli(instanceName) {
    console.log(`[PocketHost CLI] POCKETHOST_TOKEN found; deploying pb_hooks/ via \`pockethost deploy\`.`)
    execSync(
        `pockethost deploy ${instanceName} --include pb_hooks "pb_hooks/**/*" --no-node-modules --message "pb_hooks deploy from lovebside repo"`,
        { stdio: "inherit" }
    )
    console.log("[PocketHost CLI] Hook deployment successful.")
}

async function deployHooks() {
    const instanceName = process.env.POCKETHOST_INSTANCE || "bside"
    if (process.env.POCKETHOST_TOKEN) {
        deployHooksViaCli(instanceName)
        return
    }
    console.warn(
        "[PocketHost FTP] POCKETHOST_TOKEN not set - falling back to legacy FTP, which PocketHost retires " +
        "2027-02-06. Run `pockethost login <token>` (token from the PocketHost dashboard) and add " +
        '`export POCKETHOST_TOKEN="ph_pat_..."` to ~/.zshrc to switch to the supported CLI path.'
    )

    const client = new ftp.Client()
    client.ftp.verbose = true

    const host = process.env.POCKETHOST_FTP_HOST || "ftp.pockethost.io"
    const user = process.env.POCKETHOST_FTP_USER
    const password = process.env.POCKETHOST_FTP_PASSWORD

    if (!user || !password) {
        console.error("Error: POCKETHOST_FTP_USER and POCKETHOST_FTP_PASSWORD env vars must be set.")
        process.exit(1)
    }

    try {
        console.log(`Connecting to ${host} as ${user}...`)
        await client.access({
            host: host,
            user: user,
            password: password,
            secure: true,
            secureOptions: { rejectUnauthorized: false }
        })

        // Determine remote path
        // Usually hooks are in /pb_hooks relative to instance root
        // If user is scoped to instance, it's just /pb_hooks
        // If user is root, it's /instanceName/pb_hooks

        let targetDir = "pb_hooks"
        const list = await client.list()
        const instanceDir = list.find(f => f.name === instanceName && f.isDirectory)

        if (instanceDir) {
            console.log(`Entering instance directory: ${instanceName}`)
            await client.cd(instanceName)
        }

        console.log(`Ensuring remote directory '${targetDir}' exists...`)
        await client.ensureDir(targetDir)
        // ensureDir enters the directory

        const localHooksDir = path.join(__dirname, "../pocketbase/pb_hooks")
        if (!fs.existsSync(localHooksDir)) {
            throw new Error("Local pb_hooks directory not found.")
        }

        console.log(`Uploading hooks from ${localHooksDir}...`)
        const files = fs.readdirSync(localHooksDir)

        for (const file of files) {
            if (file.endsWith(".js") || file.endsWith(".pb.js")) {
                console.log(`Uploading ${file}...`)
                await client.uploadFrom(path.join(localHooksDir, file), file)
            }
        }

        console.log("Hooks deployment complete.")
        console.log("Restart your PocketHost instance to load new hooks.")

    } catch (err) {
        console.error("Deploy failed:", err)
        process.exit(1)
    }
    client.close()
}

deployHooks()
