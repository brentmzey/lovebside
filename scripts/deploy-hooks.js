const ftp = require("basic-ftp")
const path = require("path")
const fs = require("fs")

async function deployHooks() {
    const client = new ftp.Client()
    client.ftp.verbose = true

    const host = process.env.POCKETHOST_FTP_HOST || "ftp.pockethost.io"
    const user = process.env.POCKETHOST_FTP_USER
    const password = process.env.POCKETHOST_FTP_PASSWORD
    const instanceName = process.env.POCKETHOST_INSTANCE || "bside"

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
